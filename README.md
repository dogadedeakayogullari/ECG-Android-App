# Android ECG Monitoring Application

This Android application was developed in **Kotlin** using **Android Studio**.  
- **Android Studio** was chosen for its rich toolset and support for real-time debugging on both physical devices and emulators.  
- **Kotlin** was preferred for its modern structure, full Android support, and ability to perform tasks with less code.  

---

## 📱 Application Workflow
When the user opens the application, they are greeted by a **log-in screen**, which provides basic user-level security.  
After logging in, the user is directed to the **main menu**, where buttons for **Bluetooth connection** and **Data Activity** are available.  

![Login page](images/login.jpg)

To use the application, the user must first connect to the ECG device via **Bluetooth**. Once connected, the app navigates to the **Data Activity screen**.  

On this screen:  
- Real-time ECG data is displayed both numerically and graphically.  
- Heart rate values are received as float data.  
- The ECG waveform (amplitude over time) is plotted on a line chart.  

![Bluetooth connection page](images/bluetooth.jpg)

The app continuously reads data via a **BluetoothSocket** using an **InputStream**.  
This operation runs in a background thread to keep the main UI responsive.  

---

## 📊 Data Visualization
Numerical data is visualized using the **MPAndroidChart** library.  

- **X-axis**: Time (seconds)  
- **Y-axis**: ECG signal amplitude (float values)  
- The chart refreshes every **50 ms** for medical accuracy and minimal latency.  

This enables users to observe irregularities (e.g., skipped beats, fluctuations) before the automatic detection system triggers an alert.  

![Heart rate chart](images/chart.jpg)

---

## 💾 Data Storage & Transmission
After visualization, data is stored and transmitted to a server.  

1. ECG data is saved as `ecg_data.txt` in the Android data directory.  
2. To avoid lag from large files, the app **uploads the file to a Flask-based server** and clears local storage after a while.  
3. The user can:  
   - Create a `.txt` file  
   - Upload it to the server  
   - View the file path  

![Data storage on phone](images/storage.jpg)  

The server is Flask-based and accessible at:  
`http://134.122.56.178:3000/upload`  

The app uses **OkHttpClient** for multipart HTTP POST requests.  

---

## ❤️ Arrhythmia Detection
The application performs **arrhythmia detection** based on R-peaks and heart rate.  

- **Normal heart rate**: 60–100 bpm  
- Conditions:  
  - **Tachycardia**: HR > 100 bpm (R-R interval < 0.6 s)  
  - **Bradycardia**: HR < 60 bpm (R-R interval > 1.2 s)  
  - **Irregular rhythm**: Irregular R-R intervals (possible atrial fibrillation)  

The app checks for arrhythmia every **150 samples** using a **threshold-based R-peak detection** method.  
Alerts are triggered if:  
- R-R interval < 0.6 s  
- R-R interval > 1.2 s  

![Arrhythmia detection](images/arrhythmia.jpg)

---

## 🛠️ Tech Stack
- **Kotlin** (Android app development)  
- **MPAndroidChart** (data visualization)  
- **OkHttpClient** (HTTP requests)  
- **Flask server on Ubuntu** (data storage)  

---

## 📂 Project Structure
