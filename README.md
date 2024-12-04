# VBS Recorder

VBS Recorder is an Android application for recording and managing audio files. It includes features such as splitting audio files into segments and saving them to the media store.

## Installation

To install the project, follow these steps:

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/vbs_recorder.git
    ```
2. Open the project in Android Studio.
3. Build the project to download all dependencies.

## Usage

To use the application, follow these steps:

1. Launch the application on your Android device.
2. Use the record button to start recording audio.
3. The recorded audio will be split into segments and saved to the media store.

## Dependencies

The project uses the following dependencies:

- AndroidX Core KTX
- AndroidX AppCompat
- Material Components
- AndroidX ConstraintLayout
- AndroidX Navigation
- AndroidX Activity
- AndroidX Room
- AWS SDK for Android (S3 and Core)
- IIRJ (Infinite Impulse Response filters)

Add the following to your `build.gradle.kts` file:

```kotlin
dependencies {
   implementation(libs.androidx.core.ktx)
   implementation(libs.androidx.appcompat)
   implementation(libs.material)
   implementation(libs.androidx.constraintlayout)
   implementation(libs.androidx.navigation.fragment.ktx)
   implementation(libs.androidx.navigation.ui.ktx)
   implementation(libs.androidx.activity)
   implementation(libs.androidx.room.common)
   implementation(libs.androidx.room.ktx)
   implementation(libs.androidx.room.runtime)
   annotationProcessor(libs.androidx.room.compiler)
   ksp(libs.androidx.room.compiler)
   implementation(libs.aws.android.sdk.s3)
   implementation(libs.aws.android.sdk.core)
   implementation("uk.me.berndporr:iirj:1.7")
   testImplementation(libs.junit)
   androidTestImplementation(libs.androidx.junit)
   androidTestImplementation(libs.androidx.espresso.core)
}
```

## Permissions

Ensure the following permissions are added to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
```

## Usage
1. Recording Audio: Press the record button to start recording audio.
2. Playing Audio: Press the play button to play the recorded audio. Use the seek bar to navigate through the audio.

## Code Overview

### MainActivity
Handles permission requests and basic app setup.  

### HomeFragment
Contains the UI and logic for recording and playing audio.  

### Dependencies
- MediaRecorder for recording audio.
- MediaPlayer for playing audio.

## License
This project is licensed under the MIT License.