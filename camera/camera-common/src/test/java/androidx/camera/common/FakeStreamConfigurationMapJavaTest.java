/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.ImageFormat;
import android.util.Size;

import androidx.camera.common.testing.FakeCameraCharacteristics;
import androidx.camera.common.testing.FakeStreamConfigurationMap;
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputKey;
import androidx.camera.common.testing.FakeStreamConfigurationMap.OutputValues;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.LinkedHashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 28, 30, 33})
public class FakeStreamConfigurationMapJavaTest {

    @Test
    public void exampleUsage() {
        long zeroDuration = 0L;
        long jpegStall = 200000000L; // 200ms in ns

        LinkedHashMap<OutputKey, OutputValues> outputsTable = new LinkedHashMap<>();
        outputsTable.put(
                new OutputKey(ImageFormat.YUV_420_888, new Size(1920, 1080)),
                new OutputValues()
        );
        outputsTable.put(
                new OutputKey(ImageFormat.YUV_420_888, new Size(640, 480)),
                new OutputValues()
        );
        outputsTable.put(
            new OutputKey(ImageFormat.JPEG, new Size(1920, 1080)),
            new OutputValues(zeroDuration, jpegStall) // Tests @JvmOverloads constructor
        );

        FakeStreamConfigurationMap fakeMap = new FakeStreamConfigurationMap(outputsTable);

        assertThat(fakeMap.getOutputFormats())
            .containsExactly(ImageFormat.YUV_420_888, ImageFormat.JPEG);
        assertThat(fakeMap.getOutputSizes(ImageFormat.JPEG))
            .containsExactly(new Size(1920, 1080));
        assertThat(fakeMap.getOutputStallDuration(ImageFormat.JPEG, new Size(1920, 1080)))
            .isEqualTo(jpegStall);
    }

    @Test
    public void getStreamConfigurationMapFromWrapper() {
        LinkedHashMap<OutputKey, OutputValues> outputsTable = new LinkedHashMap<>();
        outputsTable.put(
                new OutputKey(ImageFormat.YUV_420_888, new Size(1920, 1080)),
                new OutputValues()
        );

        FakeStreamConfigurationMap fakeMap = new FakeStreamConfigurationMap(outputsTable);
        FakeCameraCharacteristics fake = FakeCameraCharacteristics.create(
            "0",
            Collections.emptyMap(),
            Collections.singletonMap(
                CameraCharacteristicsWrapper.Keys.STREAM_CONFIGURATION_MAP,
                fakeMap
            )
        );

        StreamConfigurationMapWrapper map =
                CameraCharacteristicsWrappers.getStreamConfigurationMap(fake);

        assertThat(map).isSameInstanceAs(fakeMap);
    }
}
