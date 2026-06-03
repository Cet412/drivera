# Drivera: Driver Monitoring System (DMS)

Drivera adalah sistem pemantauan pengemudi berbasis Android (*on-device AI*) yang dirancang untuk mendeteksi tingkat kelelahan (*fatigue detection*) dan kantuk secara *real-time*. Sistem ini memanfaatkan pemrosesan citra lokal menggunakan **MediaPipe Face Landmarker** dan **CameraX**, yang berjalan secara persisten di latar belakang melalui *Foreground Service* untuk memastikan keselamatan berkendara tanpa interupsi.

---

## 1. Arsitektur & Arsitektur Komponen (*As-Is*)

Aplikasi ini mengadopsi pemisahan tanggung jawab (*separation of concerns*) yang terbagi ke dalam empat modul utama dengan interaksi berbasis *event-driven broadcast*:

[ UI Layer: MainActivity ] <--- (Local Broadcast) --- [ Core Service: DmsForegroundService ]
|                                                            |
(CameraX Preview)                                            (CameraX ImageAnalysis)
|                                                            |
[ Face Calibration Frame ]                                  [ AI Inference: FaceAnalyzer ]
|
(Triggers Alarm)
v
[ Hardware: AudioAlertManager ]

### Tabel Komponen Utama

| Komponen | Layer | Tanggung Jawab Utama |
| :--- | :--- | :--- |
| `MainActivity.kt` | **UI Layer (Jetpack Compose)** | Mengelola perizinan runtime, merender status keselamatan (`SAFE`, `WARNING`, `CRITICAL`), dan mengisolasi pratinjau kamera untuk kalibrasi wajah pengguna. |
| `DmsForegroundService.kt` | **Core State Machine** | Berjalan sebagai `LifecycleService` persisten, mengikat siklus hidup analisis CameraX, mengelola logika transisi status kantuk, dan memancarkan pembaruan status. |
| `FaceAnalyzer.kt` | **Data & Inference Layer** | Memproses bingkai gambar mentah dari kamera menjadi koordinat *face landmarks*, menghitung nilai *Eye Aspect Ratio* (EAR), menerapkan penyaringan derau (*noise filtering*), serta menangani toleransi kemiringan wajah. |
| `AudioAlertManager.kt` | **Hardware Interfacing** | Mengambil alih kontrol audio perangkat untuk memaksa volume maksimal pada saluran alarm dan mengeksekusi pola getaran haptik berulang. |

---

## 2. Aliran Data & Logika State Machine

Sistem Drivera bekerja sebagai sebuah mesin status (*state machine*) yang digerakkan oleh durasi penutupan kelopak mata pengemudi.

### Mekanisme Transisi Status
* **`SAFE`**: Kondisi normal di mana mata pengemudi terdeteksi terbuka secara konsisten (Nilai rata-rata EAR $\ge 0.16$).
* **`WARNING`**: Dipicu seketika saat nilai rata-rata EAR turun di bawah ambang batas ($< 0.16$). Status ini menandakan mata mulai terpejam atau berkedip.
* **`CRITICAL`**: Dipicu apabila kondisi nilai EAR berada di bawah ambang batas terus-menerus selama lebih dari **1500 milidetik (`CRITICAL_DURATION_MS`)**. Status ini mengindikasikan pengemudi tertidur (*micro-sleep*) dan langsung mengaktifkan alarm perangkat keras.

### Komunikasi Antar Komponen
Komoditas data status dari `DmsForegroundService` dikirimkan kembali ke UI (`MainActivity`) secara asinkron menggunakan komponen `LocalBroadcastManager` dengan aksi filter `"DMS_STATE_UPDATE"`. String status dikemas dalam *intent extra* dengan kunci `"STATUS"`.

---

## 3. Algoritma Pemrosesan Citra & AI (*On-Device*)

Komponen `FaceAnalyzer` memegang peran sentral dalam pemrosesan inferensi visi komputer lokal dengan memuat model `face_landmarker.task` dalam mode `RunningMode.LIVE_STREAM`.

### Perhitungan Eye Aspect Ratio (EAR)
EAR dihitung dengan mengukur rasio jarak vertikal antara kelopak mata terhadap jarak horizontalnya. Persamaan matematika yang diterapkan untuk masing-masing mata adalah:

$$\text{EAR} = \frac{||p_2 - p_6|| + ||p_3 - p_5||}{2 \cdot ||p_1 - p_4||}$$

Di mana koordinat indeks landmark MediaPipe yang dipetakan pada kode adalah:
* **Mata Kiri:** Horizontal ($p_1=33, p_4=133$), Vertikal ($p_2=160, p_6=144$ dan $p_3=158, p_5=153$).
* **Mata Kanan:** Horizontal ($p_1=362, p_4=263$), Vertikal ($p_2=385, p_6=380$ dan $p_3=387, p_5=373$).

### Logika Mata Dominan (*Dominant Eye Logic*)
Untuk mengatasi distorsi perspektif ketika wajah pengemudi menoleh atau miring (*tilt*), sistem menerapkan aturan isolasi mata berdasarkan ketinggian geometris kontur kelopak mata (`leftH` dan `rightH`):
* Jika $\text{rightH} > \text{leftH} \times 1.10$, maka mata kanan mendominasi bingkai (posisi lebih dekat ke kamera). Nilai EAR kiri diisolasi, dan sistem hanya merujuk pada `rightEAR`.
* Jika $\text{leftH} > \text{rightH} \times 1.10$, maka mata kiri mendominasi bingkai. Sistem hanya merujuk pada `leftEAR`.
* Jika variasi tinggi di bawah toleransi 10%, wajah dianggap menghadap lurus ke depan dan nilai diambil dari rata-rata keduanya: 

$$\text{EAR}_{\text{raw}} = \frac{\text{rightEAR} + \text{leftEAR}}{2}$$

### Peredam Derau (*Moving Average Filter*)
Guna menghindari kesalahan deteksi akibat kedipan mata normal atau fluktuasi pencahayaan (*flicker*), nilai mentah $\text{EAR}_{\text{raw}}$ disaring menggunakan struktur data antrean berantai (`LinkedList`) bertindak sebagai **Moving Average Filter** dengan ukuran jendela riwayat dinamis $\mathbf{N = 5}$.

$$\text{EAR}_{\text{smoothed}} = \frac{1}{N} \sum_{i=1}^{N} \text{EAR}_{\text{raw}}[i]$$

---

## 4. Manajemen Perangkat Keras & Respon Haptic

Ketika sistem memasuki status `CRITICAL`, komponen `AudioAlertManager` melakukan intervensi tingkat rendah (*low-level*) terhadap subsistem perangkat keras Android:

1. **Pengambilalihan Volume Suara:** Sistem memanggil `AudioManager` dan memaksa volume saluran suara `STREAM_ALARM` ke tingkat maksimum absolut yang diizinkan perangkat sebelum memutar audio.
2. **Media Playback Persisten:** Memutar file audio kustom `R.raw.dms_alarm` menggunakan `MediaPlayer` dengan konfigurasi parameter `AudioAttributes.USAGE_ALARM` dan `CONTENT_TYPE_SONIFICATION` dalam kondisi perulangan (*looping*) aktif.
3. **Pola Getaran Haptik:** Mengeksekusi modul getar melalui `Vibrator` (atau `VibratorManager` pada API level $\ge 31$) dengan pola larik gelombang (*waveform pattern*) berulang:
   * **`longArrayOf(0, 500, 200, 500)`** dengan indeks pengulangan ke-1 (indeks `0` diam, `500ms` bergetar, `200ms` jeda, `500ms` bergetar).

---

## 5. Isolasi Kamera dan Manajemen Memori

Aplikasi ini mengimplementasikan teknik **Surgical Cleanup** pada sisi UI (`MainActivity`). Komponen pratinjau `FaceCalibrationPreview` menggunakan blok fungsi `DisposableEffect`. 

Saat pengemudi menutup layar aplikasi atau berpindah aktivitas, siklus hidup Jetpack Compose memicu pembersihan *use-case* `Preview` secara terisolasi (`cameraProvider.unbind(preview)`). Tindakan ini memastikan bahwa aliran video visual ke layar dimatikan demi menghemat daya baterai, namun **tidak menghentikan** *use-case* `ImageAnalysis` milik `DmsForegroundService` yang bertugas melakukan deteksi AI di latar belakang.

---

## 6. Catatan Teknis & Utang Arsitektur (*Technical Debt - As-Is*)

Sebagai dokumentasi dasar kondisi awal (*as-is*) sebelum pengembangan repositori lebih lanjut, berikut adalah beberapa poin komponen teknis internal yang tercatat di dalam kode saat ini:

1. **Deprecations:** Komunikasi status antara *Background Service* dan *UI Layer* masih mengandalkan `LocalBroadcastManager`. Komponen ini secara resmi telah dinyatakan usang (*deprecated*) oleh Google. Rekomendasi pengembangan masa depan adalah bermigrasi ke struktur reaktif berbasis *SharedFlow* atau *StateFlow* yang terikat pada arsitektur komponen Jetpack.
2. **Sinkronisasi Manual Berkas Deskriptor:** Pada modul `AudioAlertManager`, pemuatan file audio mentah dilakukan secara manual menggunakan metode `openRawResourceFd` dan membutuhkan penutupan manual (`afd.close()`) untuk mencegah kebocoran alokasi berkas deskriptor memori pada kernel sistem operasi.
3. **Kompatibilitas Getaran:** Implementasi fungsi umpan balik getar memiliki percabangan kondisional berbasis SDK Android (`Build.VERSION.SDK_INT`) untuk mengalihkan penggunaan `VibratorManager` modern ke metode getaran berbasis `VibrationEffect.createWaveform` guna menjamin fungsionalitas lintas generasi Android API.