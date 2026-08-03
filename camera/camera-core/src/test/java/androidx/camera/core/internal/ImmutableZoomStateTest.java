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

package androidx.camera.core.internal;

import static com.google.common.truth.Truth.assertThat;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.ZoomState;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Config.TARGET_SDK})
public class ImmutableZoomStateTest {

    @Test
    public void createWithFourArguments_hasDefaultActiveIntrinsicZoomRatio() {
        ZoomState zoomState = ImmutableZoomState.create(2.0f, 10.0f, 1.0f, 0.5f);

        assertThat(zoomState.getZoomRatio()).isEqualTo(2.0f);
        assertThat(zoomState.getMaxZoomRatio()).isEqualTo(10.0f);
        assertThat(zoomState.getMinZoomRatio()).isEqualTo(1.0f);
        assertThat(zoomState.getLinearZoom()).isEqualTo(0.5f);
        assertThat(zoomState.getActiveIntrinsicZoomRatio())
                .isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN);
    }

    @Test
    public void createWithFiveArguments_setsActiveIntrinsicZoomRatio() {
        ZoomState zoomState = ImmutableZoomState.create(2.0f, 10.0f, 1.0f, 0.5f, 0.5f);

        assertThat(zoomState.getActiveIntrinsicZoomRatio()).isEqualTo(0.5f);
    }

    @Test
    public void createFromExistingZoomState_preservesActiveIntrinsicZoomRatio() {
        ZoomState original = ImmutableZoomState.create(2.0f, 10.0f, 1.0f, 0.5f, 2.0f);
        ZoomState copy = ImmutableZoomState.create(original);

        assertThat(copy.getActiveIntrinsicZoomRatio()).isEqualTo(2.0f);
    }
}
