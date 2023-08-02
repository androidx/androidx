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

package androidx.car.app.media;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import static androidx.car.app.media.CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.media.AudioRecord;

import androidx.car.app.testing.TestAppManager;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowAudioRecord;

import java.nio.ByteBuffer;

/** Tests for {@link CarAudioRecord}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AutomotiveCarAudioRecordTest implements ShadowAudioRecord.AudioRecordSourceProvider {
    private final TestCarContext mCarContext =
            TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
    private CarAudioRecord mCarAudioRecord;

    @Before
    public void setUp() {
        shadowOf(mCarContext.getPackageManager()).setSystemFeature(FEATURE_AUTOMOTIVE, true);
        mCarAudioRecord = CarAudioRecord.create(mCarContext);
        ShadowAudioRecord.setSourceProvider(this);
    }

    @Test
    public void readNotStarted_throws() {
        byte[] arr = new byte[AUDIO_CONTENT_BUFFER_SIZE];
        assertThrows(IllegalStateException.class, () -> mCarAudioRecord.read(arr, 0,
                AUDIO_CONTENT_BUFFER_SIZE));
    }

    @Test
    public void readAfterStop_throws() {
        mCarAudioRecord.startRecording();
        mCarAudioRecord.stopRecording();

        byte[] arr = new byte[AUDIO_CONTENT_BUFFER_SIZE];
        assertThrows(IllegalStateException.class, () -> mCarAudioRecord.read(arr, 0,
                AUDIO_CONTENT_BUFFER_SIZE));
    }

    @Test
    public void read() {
        mCarAudioRecord.startRecording();

        byte[] out = new byte[4];
        mCarAudioRecord.read(out, 0, 4);
        assertThat(out).isEqualTo(new byte[]{'a', 'a', 'a', 'a'});
        mCarAudioRecord.stopRecording();
    }

    @Test
    public void stopRecording_tellsHostToStop() {
        mCarAudioRecord.startRecording();

        assertThat(mCarContext.getFakeHost().hasToldHostToStopRecording()).isFalse();

        mCarAudioRecord.stopRecording();

        assertThat(mCarContext.getFakeHost().hasToldHostToStopRecording()).isTrue();
    }

    @Test
    public void hostTellsToStop_noLongerReadsBytes() {
        mCarAudioRecord.startRecording();

        byte[] out = new byte[1];
        assertThat(mCarAudioRecord.read(out, 0, 1)).isEqualTo(1);
        assertThat(out).isEqualTo(new byte[]{'a'});

        out = new byte[2];
        assertThat(mCarAudioRecord.read(out, 0, 2)).isEqualTo(2);
        assertThat(out).isEqualTo(new byte[]{'a', 'a'});

        mCarContext.getCarService(TestAppManager.class)
                .getOpenMicrophoneRequest().getCarAudioCallbackDelegate().onStopRecording();

        out = new byte[1];
        assertThat(mCarAudioRecord.read(out, 0, 1)).isEqualTo(-1);

    }

    @Override
    public ShadowAudioRecord.AudioRecordSource get(AudioRecord audioRecord) {
        return new ShadowAudioRecord.AudioRecordSource() {
            @Override
            public int readInByteArray(byte[] audioData, int offsetInBytes, int sizeInBytes,
                    boolean isBlocking) {
                for (int i = offsetInBytes; i < offsetInBytes + sizeInBytes; i++) {
                    audioData[i] = 'a';
                }
                return sizeInBytes;
            }

            @Override
            public int readInShortArray(short[] audioData, int offsetInShorts, int sizeInShorts,
                    boolean isBlocking) {
                return ShadowAudioRecord.AudioRecordSource.super.readInShortArray(audioData,
                        offsetInShorts, sizeInShorts, isBlocking);
            }

            @Override
            public int readInFloatArray(float[] audioData, int offsetInFloats, int sizeInFloats,
                    boolean isBlocking) {
                return ShadowAudioRecord.AudioRecordSource.super.readInFloatArray(audioData,
                        offsetInFloats, sizeInFloats, isBlocking);
            }

            @Override
            public int readInDirectBuffer(ByteBuffer buffer, int sizeInBytes, boolean isBlocking) {
                return ShadowAudioRecord.AudioRecordSource.super.readInDirectBuffer(buffer,
                        sizeInBytes, isBlocking);
            }
        };
    }
}
