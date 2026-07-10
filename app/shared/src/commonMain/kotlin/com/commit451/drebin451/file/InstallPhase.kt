package com.commit451.drebin451.file

/** Where an in-flight APK action is: idle, downloading the APK, handing off to the installer, or failed. */
enum class InstallPhase { Idle, Downloading, Installing, Error }
