/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.sample.showcase.common.audio;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;
import static android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE;
import static android.media.AudioFormat.CHANNEL_OUT_MONO;
import static android.media.AudioFormat.ENCODING_DEFAULT;
import static android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
import static android.os.Build.VERSION.SDK_INT;

import static androidx.car.app.media.CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE;
import static androidx.car.app.media.CarAudioRecord.AUDIO_CONTENT_SAMPLING_RATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build.VERSION_CODES;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.media.CarAudioRecord;
import androidx.car.app.utils.LogTags;

import org.jspecify.annotations.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Manages recording the microphone and accessing the stored data from the microphone. */
public class VoiceInteraction {
    private static final String FILE_NAME = "recording.wav";

    private final CarContext mCarContext;

    public VoiceInteraction(@NonNull CarContext carContext) {
        mCarContext = carContext;
    }

    /**
     * Starts recording the car microphone, then plays it back.
     */
    @RequiresPermission(RECORD_AUDIO)
    @SuppressLint("ClassVerificationFailure") // runtime check for < API 26
    public void voiceInteractionDemo() {
        // Some of the functions for recording require API level 26, so verify that first
        if (SDK_INT < VERSION_CODES.O) {
            CarToast.makeText(mCarContext, "API level is less than 26, "
                            + "cannot use this functionality!",
                    CarToast.LENGTH_LONG).show();
            return;
        }

        // Check if we have permissions to record audio
        if (!checkAudioPermission()) {
            return;
        }

        // Start the thread for recording and playing back the audio
        createRecordingThread().start();
    }

    /**
     * Create thread which executes the record and the playback functions
     */
    @RequiresApi(api = VERSION_CODES.O)
    @RequiresPermission(RECORD_AUDIO)
    @SuppressLint("ClassVerificationFailure") // runtime check for < API 26
    public @NonNull Thread createRecordingThread() {
        Thread recordingThread = new Thread(
                () -> {
                    // Request audio focus
                    AudioFocusRequest audioFocusRequest = null;
                    try {
                        CarAudioRecord record = CarAudioRecord.create(mCarContext);
                        // Take audio focus so that user's media is not recorded
                        AudioAttributes audioAttributes =
                                new AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .setUsage(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                                        .build();

                        audioFocusRequest = new AudioFocusRequest.Builder(
                                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                                .setAudioAttributes(audioAttributes)
                                .setOnAudioFocusChangeListener(state -> {
                                    if (state == AudioManager.AUDIOFOCUS_LOSS) {
                                        // Stop recording if audio focus is lost
                                        record.stopRecording();
                                    }
                                })
                                .build();

                        if (mCarContext.getSystemService(AudioManager.class)
                                .requestAudioFocus(audioFocusRequest)
                                != AUDIOFOCUS_REQUEST_GRANTED) {
                            CarToast.makeText(mCarContext, "Audio Focus Request not granted",
                                    CarToast.LENGTH_LONG).show();
                            return;
                        }
                        recordAudio(record);
                        playBackAudio();
                    } catch (Exception e) {
                        Log.e(LogTags.TAG, "Voice Interaction Error: ", e);
                        throw new RuntimeException(e);
                    } finally {
                        // Abandon the FocusRequest so that user's media can be resumed
                        mCarContext.getSystemService(AudioManager.class).abandonAudioFocusRequest(
                                audioFocusRequest);
                    }
                },
                "AudioRecorder Thread");
        return recordingThread;
    }

    @RequiresPermission(RECORD_AUDIO)
    private void playBackAudio() {

        InputStream inputStream;
        try {
            inputStream = mCarContext.openFileInput(FILE_NAME);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(ENCODING_DEFAULT)
                        .setSampleRate(AUDIO_CONTENT_SAMPLING_RATE)
                        .setChannelMask(CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(AUDIO_CONTENT_BUFFER_SIZE)
                .build();
        audioTrack.play();
        try {
            byte[] audioData = new byte[AUDIO_CONTENT_BUFFER_SIZE];
            while (inputStream.available() > 0) {
                int readByteLength = inputStream.read(audioData, 0, audioData.length);

                if (readByteLength < 0) {
                    // End of file
                    break;
                }
                audioTrack.write(audioData, 0, readByteLength);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        audioTrack.stop();

    }

    @RequiresApi(api = VERSION_CODES.O)
    @SuppressLint("ClassVerificationFailure") // runtime check for < API 26
    @RequiresPermission(RECORD_AUDIO)
    private void recordAudio(CarAudioRecord record) {

        record.startRecording();

        List<Byte> bytes = new ArrayList<>();
        boolean isRecording = true;
        while (isRecording) {
            // gets the voice output from microphone to byte format
            byte[] bData = new byte[AUDIO_CONTENT_BUFFER_SIZE];
            int len = record.read(bData, 0, AUDIO_CONTENT_BUFFER_SIZE);

            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    bytes.add(bData[i]);
                }
            } else {
                isRecording = false;
            }
        }

        try {
            OutputStream outputStream = mCarContext.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            addHeader(outputStream, bytes.size());
            for (Byte b : bytes) {
                outputStream.write(b);
            }

            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        record.stopRecording();
    }

    private static void addHeader(OutputStream outputStream, int totalAudioLen) throws IOException {
        int totalDataLen = totalAudioLen + 36;
        byte[] header = new byte[44];
        int dataElementSize = 8;
        long longSampleRate = AUDIO_CONTENT_SAMPLING_RATE;

        // See http://soundfile.sapp.org/doc/WaveFormat/
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalAudioLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1 PCM
        header[21] = 0;
        header[22] = 1; // Num channels (mono)
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff); // sample rate
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (longSampleRate & 0xff); // byte rate
        header[29] = (byte) ((longSampleRate >> 8) & 0xff);
        header[30] = (byte) ((longSampleRate >> 16) & 0xff);
        header[31] = (byte) ((longSampleRate >> 24) & 0xff);
        header[32] = 1;  // block align
        header[33] = 0;
        header[34] = (byte) (dataElementSize & 0xff);  // bits per sample
        header[35] = (byte) ((dataElementSize >> 8) & 0xff);
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        outputStream.write(header, 0, 44);
    }

    // Returns whether or not the user has granted audio permissions
    private boolean checkAudioPermission() {
        if (mCarContext.checkSelfPermission(RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            CarToast.makeText(mCarContext, "Grant mic permission on device",
                    CarToast.LENGTH_LONG).show();
            List<String> permissions = Collections.singletonList(RECORD_AUDIO);
            mCarContext.requestPermissions(permissions, (grantedPermissions,
                    rejectedPermissions) -> {
                if (grantedPermissions.contains(RECORD_AUDIO)) {
                    voiceInteractionDemo();
                }
            });
            return false;
        }
        return true;
    }
}
