# PulseTorch
Audio-reactive flashlight for Android. Sync your phone torch to music and sound in real time with clean controls, safety limits, and a slick UI.

<p align="center">
    <img src="assets/pulsetorch_playstore_512.png" width="512" alt="PulseTorch icon" />
</p>

<p align="center">
  <b>Mic</b> • <b>File</b> • <b>System Audio Capture</b><br/>
  <b>Smooth</b> • <b>Pulse</b> • <b>Strobe</b>
</p>

---

## What is PulseTorch?
PulseTorch turns your phone flashlight into a music-synced light show. It listens to audio, detects intensity, and drives the torch with smooth brightness or rhythmic flashes.

No cloud, no accounts, no tracking. Everything is processed locally on your device.

---

## Key Features
### Modes
- **Mic Mode** - reacts to live microphone input (claps, music, crowds, room sound).
- **File Mode** - pick a track and sync torch effects to playback.
- **System Audio Capture Mode** - capture internal device audio (when supported) and sync the torch to what’s playing.

### Effects
- **Smooth** - continuous brightness that follows the signal.
- **Pulse** - punchy response to peaks for a strong beat feel.
- **Strobe** - crisp flashing with a configurable maximum rate.

### Controls and Tuning
- **Sensitivity** - how strongly PulseTorch reacts to sound.
- **Smoothness** - how soft or sharp the response feels.
- **Mic Gain** - boosts quiet environments.
- **Smoothing** - stabilizes signal for cleaner behavior.
- **Bass Focus** - emphasizes low frequencies for a more “club-like” beat response.
- **Max Strobe Rate (Hz)** - prevents uncomfortable or unsafe flashing speeds.
- **Auto Brightness** - uses hardware brightness levels when available.

### Safety and Reliability
- **Strobe warning** and a hard **max Hz limit** for safer usage.
- **Torch always turns off on Stop**, and the app avoids leaving the flashlight stuck on unintentionally.
- **Works across devices** with a fallback brightness strategy when native torch strength control is not supported.

### Persistence
- Remembers your last used mode, effect, and tuning settings.

---

## Notes on Compatibility
- **System Audio Capture** depends on Android device support and app restrictions.
    - Some streaming apps may block capture due to DRM.
- Torch brightness levels vary by device and Android version.

---

## Privacy
PulseTorch processes audio **only on your device**.
- No audio is uploaded.
- No accounts.
- No analytics.

---

## Permissions
- **Camera**: required to control the flashlight.
- **Microphone**: required for Mic Mode.
- **Notifications**: used only for the persistent running notification when the app is active in the background (if enabled).

---

## License
Copyright (c) 2026 Denys Shulhin. All rights reserved.  
This repository is public for viewing, but the code is not licensed for reuse. See `LICENSE`.
Unauthorized use, copying, modification, or distribution is prohibited.
