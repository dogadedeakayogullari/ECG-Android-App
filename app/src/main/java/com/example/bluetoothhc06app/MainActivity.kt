package com.example.bluetoothhc06app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val hc06Address = "00:22:09:02:22:95" // HC-06 MAC adresi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kullanıcı bilgisi yoksa Login ekranına yönlendir
        val prefs = getSharedPreferences("UserInfo", MODE_PRIVATE)
        val name = prefs.getString("name", null)
        if (name == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        checkPermissions()

        val statusText: TextView = findViewById(R.id.statusText)
        val connectButton: Button = findViewById(R.id.connectButton)
        val sendButton: Button = findViewById(R.id.sendButton)
        val secondActivityButton: Button = findViewById(R.id.DataActivityButton)

        connectButton.setOnClickListener {
            if (checkBluetoothPermissions()) {
                connectToHC06(statusText)
            }
        }

        sendButton.setOnClickListener {
            sendDataToHC06("Hello HC-06")
        }

        secondActivityButton.setOnClickListener {
            val intent = Intent(this@MainActivity, DataActivity::class.java)
            startActivity(intent)
        }
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

    private fun checkBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                    1
                )
                return false
            }
        }
        return true
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToHC06(statusText: TextView) {
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Bluetooth desteklenmiyor!")
            Toast.makeText(this, "Cihaz Bluetooth desteklemiyor!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices.find { it.address == hc06Address }

        if (device == null) {
            statusText.text = "HC-06 bulunamadı! Önce eşleştirin."
            return
        }

        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter.cancelDiscovery()
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            BluetoothConnectionHolder.socket = bluetoothSocket

            statusText.text = "Bağlantı başarılı!"
            Toast.makeText(this, "Bağlantı başarılı!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            statusText.text = "Bağlantı hatası: ${e.message}"
            Toast.makeText(this, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDataToHC06(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            Toast.makeText(this, "Veri gönderildi: $data", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Veri gönderme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Bağlantı kapatma hatası: ${e.message}")
        }
    }
}
