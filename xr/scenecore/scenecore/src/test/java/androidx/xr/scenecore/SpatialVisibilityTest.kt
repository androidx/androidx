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
class SpatialVisibilityTest {

    @Test
    fun spatialVisibility_toString() {
        assertThat(SpatialVisibility.UNKNOWN.toString()).isEqualTo("UNKNOWN")
        assertThat(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW.toString())
            .isEqualTo("OUTSIDE_FIELD_OF_VIEW")
        assertThat(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW.toString())
            .isEqualTo("PARTIALLY_WITHIN_FIELD_OF_VIEW")
        assertThat(SpatialVisibility.WITHIN_FIELD_OF_VIEW.toString())
            .isEqualTo("WITHIN_FIELD_OF_VIEW")
    }
}
