# PitchApp

High-precision Android instrument tuner featuring a native C++ audio engine (Oboe & YIN algorithm) and a modern Jetpack Compose UI.

## Features

* **Native Audio Engine (C++):** Real-time audio capture and processing using the [Oboe](https://github.com/google/oboe) library for ultra-low latency.
* **YIN Pitch Detection:** Mathematical autocorrelation algorithm implemented in C++ to guarantee rigorous frequency detection.
* **Stroboscopic UI:** Advanced Edge-to-Edge interface built entirely with Jetpack Compose. Includes a dynamic needle, silent fade-out, a strobe window, and a musical particle system.
* **Signal Smoothing:** Kotlin-side median filter to stabilize the visual needle and prevent sudden jumps caused by background noise.
* **Advanced Settings:**
  * Configurable Noise Gate (Microphone Sensitivity).
  * A4 Calibration (e.g., 440 Hz).
  * Instrument Transposition (C, Bb, Eb, F).
  * YIN Algorithm Tolerance (Strictness).
  * Customizable visual needle width and tuning lock sound.

## Tech Stack

* **Language:** Kotlin & C++
* **UI:** Jetpack Compose, Material Design 3
* **Audio:** Oboe (C++), JNI, SoundPool, AudioManager (Focus handling)
* **Local Persistence:** Jetpack DataStore (Preferences) & Room Database (Setup ready)
* **Architecture:** MVVM (Model-View-ViewModel)

## Roadmap (Version 2)

The upcoming version will introduce:
* **Custom Tunings & Instruments:** Full integration with the existing Room Database to allow users to select specific instruments (Guitar, Bass, Violin, etc.) and specific tunings (Standard, Drop D, Half-Step Down, etc.).
* **Target Note Assistance:** The UI will guide the user towards the specific string being tuned rather than acting purely as a chromatic tuner.

## Requirements
* Android 8.0 (API level 26) or higher.
* Device with a physical microphone.


_Build with **love** by MasterOfPuppets and Gemini_