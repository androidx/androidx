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

import static androidx.car.app.media.CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.testing.TestAppManager;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.io.ByteArrayInputStream;

/** Tests for {@link CarAudioRecord}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ProjectedCarAudioRecordTest {
    private final TestCarContext mCarContext =
            TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
    private final CarAudioRecord mCarAudioRecord = CarAudioRecord.create(mCarContext);
    private final byte[] mArr = {'t', 'e', 's', 't'};

    @Before
    public void setUp() {
        mCarContext.getFakeHost().setMicrophoneInputData(new ByteArrayInputStream(mArr));
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

        assertThat(out).isEqualTo(mArr);
        mCarAudioRecord.stopRecording();
    }

    @Test
    public void readAfterAllRead_returns_negative_1() {
        mCarAudioRecord.startRecording();
        byte[] out = new byte[4];
        assertThat(mCarAudioRecord.read(out, 0, 4)).isEqualTo(4);

        out = new byte[4];
        assertThat(mCarAudioRecord.read(out, 0, 4)).isEqualTo(-1);

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
        assertThat(out).isEqualTo(new byte[]{'t'});

        out = new byte[2];
        assertThat(mCarAudioRecord.read(out, 0, 2)).isEqualTo(2);
        assertThat(out).isEqualTo(new byte[]{'e', 's'});

        mCarContext.getCarService(TestAppManager.class)
                .getOpenMicrophoneRequest().getCarAudioCallbackDelegate().onStopRecording();

        out = new byte[1];
        assertThat(mCarAudioRecord.read(out, 0, 1)).isEqualTo(-1);

    }
}
