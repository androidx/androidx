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

package androidx.car.app.sample.navigation.common.car;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.media.CarAudioRecord;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Manages recording the microphone and accessing the stored data from the microphone. */
public class MicrophoneRecorder {
    private static final String FILE_NAME = "recording.wav";

    private final CarContext mCarContext;

    public MicrophoneRecorder(@NonNull CarContext carContext) {
        mCarContext = carContext;
    }

    /**
     * Starts recording the car microphone, then plays it back.
     */
    public void record() {
        if (mCarContext.checkSelfPermission(RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || mCarContext.checkSelfPermission(
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            CarToast.makeText(mCarContext, "Grant mic permission on phone",
                    CarToast.LENGTH_LONG).show();
            List<String> permissions = Arrays.asList(RECORD_AUDIO, WRITE_EXTERNAL_STORAGE);
            mCarContext.requestPermissions(permissions, (grantedPermissions,
                    rejectedPermissions) -> {
                if (grantedPermissions.contains(RECORD_AUDIO)) {
                    record();
                }
            });
            return;
        }
        CarAudioRecord record = CarAudioRecord.create(mCarContext);

        Thread recordingThread =
                new Thread(
                        () -> doRecord(record),
                        "AudioRecorder Thread");
        recordingThread.start();
    }

    @RequiresPermission(RECORD_AUDIO)
    private void play() {
        if (SDK_INT < VERSION_CODES.O) {
            return;
        }

        InputStream inputStream;
        try {
            inputStream = mCarContext.openFileInput(FILE_NAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
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
            while (inputStream.available() > 0) {
                byte[] audioData = new byte[AUDIO_CONTENT_BUFFER_SIZE];
                int size = inputStream.read(audioData, 0, audioData.length);

                if (size < 0) {
                    // End of file
                    return;
                }
                audioTrack.write(audioData, 0, size);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        audioTrack.stop();
    }

    @SuppressLint("ClassVerificationFailure") // runtime check for < API 26
    @RequiresPermission(RECORD_AUDIO)
    private void doRecord(CarAudioRecord record) {
        if (SDK_INT < VERSION_CODES.O) {
            return;
        }

        // Take audio focus so that user's media is not recorded
        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build();

        AudioFocusRequest audioFocusRequest =
                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(audioAttributes)
                        .setOnAudioFocusChangeListener(state -> {
                            if (state == AudioManager.AUDIOFOCUS_LOSS) {
                                // Stop recording if audio focus is lost
                                record.stopRecording();
                            }
                        })
                        .build();

        if (mCarContext.getSystemService(AudioManager.class).requestAudioFocus(audioFocusRequest)
                != AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

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
        play();
    }

    private void addHeader(OutputStream outputStream, int totalAudioLen) throws IOException {
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
}
