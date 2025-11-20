/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AugmentedObjectStateTest {
    @Test
    fun constructor_noArguments_returnsZeroVectorAndIdentityQuaternion() {
        val underTest = AugmentedObjectState()

        assertThat(underTest.centerPose.translation.x).isEqualTo(0)
        assertThat(underTest.centerPose.translation.y).isEqualTo(0)
        assertThat(underTest.centerPose.translation.z).isEqualTo(0)
        assertThat(underTest.centerPose.rotation.x).isEqualTo(0)
        assertThat(underTest.centerPose.rotation.y).isEqualTo(0)
        assertThat(underTest.centerPose.rotation.z).isEqualTo(0)
        assertThat(underTest.centerPose.rotation.w).isEqualTo(1)
        assertThat(underTest.extents.width).isEqualTo(0)
        assertThat(underTest.extents.height).isEqualTo(0)
        assertThat(underTest.extents.depth).isEqualTo(0)
        assertThat(underTest.category).isEqualTo(0)
    }
}
