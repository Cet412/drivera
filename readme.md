# Drivera
### Driver Monitoring System · Android · On-Device AI

> **Work in Progress** — This project is in early development. Core detection logic is functional, but features are still being refined.

Drivera is an **Android app** that monitors driver alertness in real-time using on-device AI — no cloud, no internet required. It detects drowsiness and micro-sleep by analyzing eye movements through the front camera, then triggers a loud alarm before an accident can happen.

## Screenshots

<table align="center">
  <tr>
    <td align="center"><img src="assets\main UI off.jpeg" width="180"/></td>
    <td align="center"><img src="assets\main UI on.jpeg" width="180"/></td>
    <td align="center"><img src="assets\main UI on warning state.jpeg" width="180"/></td>
    <td align="center"><img src="assets\main UI on Critical state.jpeg" width="180"/></td>
  </tr>
  <tr>
    <td align="center"><b>OFF</b></td>
    <td align="center"><b>ON</b></td>
    <td align="center"><b>WARNING</b></td>
    <td align="center"><b>CRITICAL</b></td>
  </tr>
</table>

---

## How It Works

Drivera runs a persistent background service that continuously analyzes your face using **MediaPipe Face Landmarker**. It calculates your **Eye Aspect Ratio (EAR)** to determine how closed your eyes are, and escalates through three states:

| State | Condition | Response |
|---|---|---|
| `SAFE` | Eyes open normally | No action |
| `WARNING` | Eyes beginning to close | Visual alert on screen |
| `CRITICAL` | Eyes closed for 1.5+ seconds | Max-volume alarm + vibration |

The app keeps monitoring even when the screen is off or you switch to another app.

---

## Tech Stack

- **Language:** Kotlin + Jetpack Compose
- **AI / CV:** MediaPipe Face Landmarker (on-device, LIVE_STREAM mode)
- **Camera:** CameraX
- **Architecture:** Foreground Service · State Machine · LocalBroadcastManager

---

## Status

```
[▓▓▓▓▓▓░░░░] ~60% — Core detection working, UI polish & testing in progress
```

- [x] EAR-based eye closure detection
- [x] Moving average noise filter
- [x] Dominant eye logic (head tilt tolerance)
- [x] CRITICAL alarm with max volume override + haptic
- [x] Camera isolation (screen off doesn't stop detection)
- [ ] Calibration UI per user
- [ ] Settings screen (sensitivity, alarm tone)
- [ ] Head pose / nodding detection
- [ ] Proper permission flow & onboarding

---

## Getting Started

> Requires Android Studio Hedgehog or newer, Android API 26+

```bash
git clone https://github.com/Cet412/drivera.git
```

1. Open in Android Studio
2. Let Gradle sync finish
3. Run on a physical device (camera required)
4. Grant camera & notification permissions when prompted
5. Keep the front camera facing your face while driving

---

## Architecture & Deep Dive

Want to understand the internals? The technical documentation (state machine design, EAR algorithm, component breakdown, known tech debt) lives in the **[Wiki](https://github.com/Cet412/drivera/wiki)**.

---

## Contributing

This is a personal project but feedback and ideas are welcome. Feel free to open an issue or reach out.

---

<details>
<summary>Ringkasan dalam Bahasa Indonesia</summary>

**Drivera** adalah aplikasi Android pemantau pengemudi berbasis AI lokal (*on-device*). Aplikasi ini mendeteksi kantuk dan *micro-sleep* secara *real-time* menggunakan kamera depan, tanpa koneksi internet.

Cara kerjanya: kamera merekam wajah secara terus-menerus, lalu model AI menganalisis rasio penutupan kelopak mata (*Eye Aspect Ratio*). Jika mata terdeteksi tertutup lebih dari 1,5 detik, alarm dengan volume maksimum dan getaran akan langsung aktif.

Proyek ini masih dalam tahap pengembangan awal (~60%). Logika deteksi utama sudah berjalan, tapi UI dan fitur tambahan masih dalam pengerjaan.

</details>

---

<sub>Built with MediaPipe · CameraX · Jetpack Compose · Made in Indonesia</sub>