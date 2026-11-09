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

package androidx.glance.adaptive.appwidget.components.ring

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RingDefaultsTest {

    @Test
    fun containedRingGeometry_scalesItsValueDownWithASmallerRing() {
        val design = RingDefaults.containedRingGeometry(88.dp)
        assertThat(design.ringSize).isEqualTo(80.dp)
        assertThat(design.fontSize.value).isEqualTo(20f)

        val small = RingDefaults.containedRingGeometry(48.dp)
        assertThat(small.ringSize).isEqualTo(40.dp)
        assertThat(small.fontSize.value).isEqualTo(10f)
    }
}
