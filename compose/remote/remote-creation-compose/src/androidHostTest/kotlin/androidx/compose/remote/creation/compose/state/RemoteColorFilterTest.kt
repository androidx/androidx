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

package androidx.compose.remote.creation.compose.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteColorFilterTest {

    @Test
    fun remoteBlendModeColorFilter_fromRemoteBlendMode() {
        val color = RemoteColor.Red
        val filter = RemoteBlendModeColorFilter(color, RemoteBlendMode.Multiply)

        assertThat(filter.color).isEqualTo(color)
        assertThat(filter.blendMode).isEqualTo(RemoteBlendMode.Multiply)
        assertThat(filter).isEqualTo(RemoteBlendModeColorFilter(color, RemoteBlendMode.Multiply))
        assertThat(filter.hashCode())
            .isEqualTo(RemoteBlendModeColorFilter(color, RemoteBlendMode.Multiply).hashCode())
        assertThat(filter).isNotEqualTo(RemoteBlendModeColorFilter(color, RemoteBlendMode.SrcIn))
    }
}
