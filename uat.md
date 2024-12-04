# Functional Testing

## Recording

- [ ] The user should be able to start recording by pressing the record button.
- [ ] The application should split the audio into N MB chunks. This N should be configurable. The default is 3MB.
- [ ] Each file should be prefixed with a fixed string. This string should be configurable. The default is `vbs`.
- [ ] If configured, the recorded file should be embedded with metadata.
- [ ] The user can configure the metadata on the settings page by scanning a QR code.
- [ ] Each recorded file should be accessible via the media library.
- [ ] If the user has configured the S3 bucket, key, and secret, each recorded file must be uploaded to the S3 bucket.
- [ ] The user can configure the S3 bucket, key, and secret on the settings page by scanning a QR code.
- [ ] The user can stop recording by pressing the stop button.
- [ ] This application should be able to record audio in the background.
- [ ] When launching the background recording, the application should show a notification without the notification sound.
- [ ] The recording can work even if there is no internet connection.
- [ ] After the internet connection is restored, when the user opens the application, the application should upload the recorded files to the S3 bucket. The UI should show the progress of the upload.
- [ ] The user can delete the recorded files from the application. Multiple files can be selected for deletion.
- [ ] The user can filter the recorded files by date and time.
- [ ] The application can do the audio signal processing on the recorded files.
- [ ] There are two types of audio signal processing: on the fly and after recording.
- [ ] For the on the fly audio signal processing, the application will process the audio signal in frames, each frame containing 200ms of audio.
- [ ] For the after recording processing, the processing will start after the file is split into 3MB chunks. This processing must be done in the background. After the processing is done, the processed file can be uploaded to the S3 bucket.
- [ ] We provide several examples of audio signal processing.
      -- [ ] On the fly: `Random dropout`. randomly dropping out N% of the frames.
      -- [ ] On the fly: `High pass filter`. applying a high pass filter to the audio signal.
      -- [ ] On the fly: `Rescale`. rescaling the audio signal.
      -- [ ] After recording: `Silence dropout`. dropping out the silent parts of the audio signal.
- [ ] The user can reset the S3 configuration.
- [ ] The user can reset the metadata configuration.

## Administering

- [ ] The researcher can prepare the S3 QR code for the user to scan by calling a command-line tool named `s3qr.py`.
- [ ] The researcher can prepare the metadata QR code for the user to scan by calling a command-line tool named `metadataqr.py`.

# Code Quality

- [ ] No unused imports.
- [ ] No commented-out code.
- [ ] No Print statements.
- [ ] No unused variables, methods, or classes.
- [ ] Focus on readability and maintainability.
- [ ] Minimize warnings.
- [ ] Meaningful names for variables, methods, and classes.
