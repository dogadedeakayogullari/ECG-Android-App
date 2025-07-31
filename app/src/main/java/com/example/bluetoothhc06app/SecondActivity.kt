package com.example.bluetoothhc06app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.sqrt

class DataActivity : AppCompatActivity() {

    private var inputStream: InputStream? = null
    private lateinit var receivedDataText: TextView
    private lateinit var lineChart: LineChart
    private lateinit var lineDataSet: LineDataSet
    private var reading = true
    private var currentTimeInSeconds = 0f
    private val timeStep = 1f / 150f
    private val chartEntries = mutableListOf<Entry>()
    private val ecgDataBuffer = mutableListOf<Float>()
    private val detectedRRIntervals = mutableListOf<Float>()

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateChart()
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        // izin kontrolleri vs

        receivedDataText = findViewById(R.id.receivedDataText)
        lineChart = findViewById(R.id.lineChart)

        findViewById<Button>(R.id.copyButton).setOnClickListener { copyToExternal() }
        findViewById<Button>(R.id.sendButton).setOnClickListener { sendFileToServer() }
        findViewById<Button>(R.id.showPathButton).setOnClickListener {
            val file = File(getExternalFilesDir(null), "ecg_data.txt")
            Toast.makeText(this, "Dosya yolu:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
        }

        val socket = BluetoothConnectionHolder.socket
        inputStream = socket?.inputStream

        setupChart()
        startReadingData()
        handler.post(updateRunnable) // sadece 1 kez çağrılmalı





    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profileMenu -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupChart() {
        lineDataSet = LineDataSet(mutableListOf(), "ECG Verisi").apply {
            color = resources.getColor(android.R.color.holo_red_light, theme)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f


        }
        val lineData = LineData(lineDataSet)
        lineChart.data = lineData


        lineChart.apply {
            data = LineData(lineDataSet)
            description.isEnabled = false
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            axisRight.isEnabled = false
            isAutoScaleMinMaxEnabled = false

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawLabels(true)
                isGranularityEnabled = true
                axisMinimum = 300f
                axisMaximum = 700f
            }

            xAxis.apply {
                isEnabled = true
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawLabels(true)
                setDrawAxisLine(true)
                setDrawGridLines(true)
                granularity = 1f
                labelRotationAngle = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%.1f s", value)
                    }
                }
            }
        }
    }

    private fun startReadingData() {
        Thread {
            try {
                val buffer = ByteArray(1024)
                var bytes: Int
                while (reading) {
                    try {
                        bytes = inputStream?.read(buffer) ?: -1
                        if (bytes > 0) {
                            val incoming = String(buffer, 0, bytes)
                            processIncomingData(incoming)
                        }
                    } catch (e: IOException) {
                        Log.e("BT-READ", "Veri okuma hatası: ${e.message}")
                        // Okuma hatası olsa da thread’i bitirme, devam et
                    }
                }
            } catch (e: Exception) {
                Log.e("BT-THREAD", "Bluetooth thread hatası: ${e.message}")
            }
        }.start()
    }


    private fun processIncomingData(data: String) {
        data.split("\n").forEach { line ->
            val number = line.trim().toFloatOrNull()
            if (number != null) {
                onNewECGData(number)
                appendDataToFile(number.toString())
                synchronized(chartEntries) {
                    chartEntries.add(Entry(currentTimeInSeconds, number))
                    currentTimeInSeconds += timeStep
                }
                runOnUiThread { receivedDataText.append("$number\n") }
            }
        }
    }

    private fun updateChart() {
        synchronized(chartEntries) {
            if (chartEntries.isNotEmpty()) {
                val data = lineChart.data ?: return
                val dataSet = data.getDataSetByIndex(0) ?: return

                for (entry in chartEntries) {
                    dataSet.addEntry(entry)
                }

                // 1500 örnek = 10 saniyelik veri
                while (dataSet.entryCount > 1500) {
                    try {
                        dataSet.removeFirst()
                    } catch (e: Exception) {
                        Log.e("Chart", "removeFirst hatası: ${e.message}")
                        break
                    }
                }

                chartEntries.clear()
                data.notifyDataChanged()
                lineChart.notifyDataSetChanged()
                lineChart.setVisibleXRangeMaximum(5f)
                lineChart.moveViewToX(currentTimeInSeconds)
                lineChart.invalidate()
            }
        }
    }

    private fun onNewECGData(value: Float) {
        ecgDataBuffer.add(value)
        if (ecgDataBuffer.size > 1000) ecgDataBuffer.removeAt(0)
        //if (ecgDataBuffer.size % 150 == 0) analyzeForArrhythmia()
    }

    private fun analyzeForArrhythmia() {
        if (ecgDataBuffer.size < 100) return

        val mean = ecgDataBuffer.average().toFloat()
        val std = sqrt(ecgDataBuffer.map { (it - mean) * (it - mean) }.average()).toFloat()
        val threshold = mean + 0.5f * std

        val peaks = (1 until ecgDataBuffer.size - 1).filter {
            ecgDataBuffer[it] > threshold &&
                    ecgDataBuffer[it] > ecgDataBuffer[it - 1] &&
                    ecgDataBuffer[it] > ecgDataBuffer[it + 1]
        }

        val fs = 150f
        val rrList = peaks.zipWithNext { a, b -> (b - a) / fs }

        detectedRRIntervals.clear()
        detectedRRIntervals.addAll(rrList)

        // ✅ Yeni: En az 3 RR intervali varsa kontrol et
        if (rrList.size >= 3) {
            val avgRR = rrList.average().toFloat()
            val abnormalCount = rrList.count { it < 0.6f || it > 1.2f }

            // ✅ Yeni: Ortalama + %50'den fazlası sınır dışı ise aritmi
            val isArrhythmia = (avgRR < 0.6f || avgRR > 1.2f) && (abnormalCount > rrList.size * 0.5)

            if (isArrhythmia) {
                runOnUiThread {
                    Toast.makeText(this, "⚠️ Aritmi tespit edildi!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun appendDataToFile(data: String) {
        try {
            val file = File(getExternalFilesDir(null), "ecg_data.txt")
            file.appendText("$data\n")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun copyToExternal() {
        try {
            val src = File(getExternalFilesDir(null), "ecg_data.txt")
            val dest = File(getExternalFilesDir(null), "ecg_data_copy.txt")
            src.copyTo(dest, overwrite = true)
            Toast.makeText(this, "Kopyalandı:\n${dest.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun sendFileToServer() {
        val file = File(getExternalFilesDir(null), "ecg_data.txt")
        if (!file.exists()) {
            Toast.makeText(this, "❌ ecg_data.txt bulunamadı", Toast.LENGTH_LONG).show()
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("text/plain".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("http://134.122.56.178:3000/upload")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DataActivity, "❌ Gönderme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    val msg = if (response.isSuccessful)
                        "✅ Dosya sunucuya yüklendi"
                    else
                        "❌ Sunucu hatası: ${response.code}"
                    Toast.makeText(this@DataActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        reading = false
        handler.removeCallbacks(updateRunnable)
    }
}