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

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialCapabilityTest {

    @Test
    fun spatialCapability_toString() {
        assertThat(SpatialCapability.SPATIAL_UI.toString()).isEqualTo("SPATIAL_UI")
        assertThat(SpatialCapability.SPATIAL_3D_CONTENT.toString()).isEqualTo("SPATIAL_3D_CONTENT")
        assertThat(SpatialCapability.PASSTHROUGH_CONTROL.toString())
            .isEqualTo("PASSTHROUGH_CONTROL")
        assertThat(SpatialCapability.APP_ENVIRONMENT.toString()).isEqualTo("APP_ENVIRONMENT")
        assertThat(SpatialCapability.SPATIAL_AUDIO.toString()).isEqualTo("SPATIAL_AUDIO")
        assertThat(SpatialCapability.EMBED_ACTIVITY.toString()).isEqualTo("EMBED_ACTIVITY")
    }
}
