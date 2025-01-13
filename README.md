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

## App Usage
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

---
# Setting Up and Configuring AWS S3 for `vbs_recorder`

## Prerequisites
- AWS account ([Sign up](https://aws.amazon.com/free/)).
- AWS CLI installed ([Installation Guide](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)).
- Proper IAM permissions for S3 ([IAM Policy Guide](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html)).
- Install `boto3` library for Python:  
  ```bash
  pip install boto3
  ```
- Configure AWS credentials ([AWS CLI Config Guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html)):  
  ```bash
  aws configure
  ```

---

## Steps to Set Up AWS S3

### 1. Create an S3 Bucket
- Follow the official AWS guide: [Create Bucket](https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-bucket-overview.html).

### 2. Configure Bucket Permissions
- Enable public or private access as needed: [Bucket Policy Examples](https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html).

### 3. Set Up AWS CLI
- Configure CLI with your AWS credentials:
  ```bash
  aws configure
  ```
- Use the [AWS CLI Configuration Guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html).

### 4. Upload Files to S3
- Upload files using CLI:
  ```bash
  aws s3 cp /local/path s3://your-bucket-name/ --recursive
  ```
- For detailed instructions, see [Uploading Objects](https://docs.aws.amazon.com/AmazonS3/latest/userguide/upload-objects.html).

### 5. Integrate S3 with `vbs_recorder`
- Update `config.json` in the repository with:
  - **Bucket Name**
  - **AWS Region**


## Steps for Integration Using `boto3`

### 1. Initialize the S3 Client
Use `boto3` to create an S3 client instance for interacting with AWS S3.
```python
import boto3

s3 = boto3.client('s3')
```

### 2. Upload Recordings to S3
Use the `upload_file` method to upload recordings from the local system to your S3 bucket.
```python
bucket_name = 'your-bucket-name'
file_name = 'local/path/to/recording.mp4'
s3_key = 'recordings/recording.mp4'

s3.upload_file(file_name, bucket_name, s3_key)
print(f"Uploaded {file_name} to {bucket_name}/{s3_key}")
```
For more details, refer to the [Boto3 Upload File Documentation](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html#S3.Client.upload_file).

### 3. Retrieve Recordings from S3
Download a recording from S3 to the local system using the `download_file` method.
```python
download_path = 'local/path/to/save/recording.mp4'

s3.download_file(bucket_name, s3_key, download_path)
print(f"Downloaded {bucket_name}/{s3_key} to {download_path}")
```
More details: [Download File Documentation](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html#S3.Client.download_file).

### 4. List Recordings in S3
List all recordings in your bucket using `list_objects_v2`.
```python
response = s3.list_objects_v2(Bucket=bucket_name, Prefix='recordings/')
if 'Contents' in response:
    for obj in response['Contents']:
        print(f"Found: {obj['Key']}")
else:
    print("No recordings found.")
```
Reference: [List Objects Documentation](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html#S3.Client.list_objects_v2).

### 5. Delete Recordings from S3
Delete a specific recording using `delete_object`.
```python
s3.delete_object(Bucket=bucket_name, Key=s3_key)
print(f"Deleted {bucket_name}/{s3_key}")
```
Reference: [Delete Object Documentation](https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html#S3.Client.delete_object).


## License
This project is licensed under GNU General Public License v3.0.

Initial development of this project was supported by [BBSRC Impact Acceleration Account (IAA) awarded to the University of Cambridge](https://www.research-strategy.admin.cam.ac.uk/bbsrc-impact-acceleration-account-0) under the project titled: [Accelerating the adoption of wingbeat-based sensing to quantify mosquito species abundance in the tropics](https://hatio.github.io/mosquitoSensing/) led by Dr. Angkana T. Huang.
