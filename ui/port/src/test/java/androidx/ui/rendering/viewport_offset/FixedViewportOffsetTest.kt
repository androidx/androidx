/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.viewport_offset

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FixedViewportOffsetTest {

    @Test
    fun `correctBy() test`() {
        val fixedViewportOffset = FixedViewportOffset(100.0)
        fixedViewportOffset.correctBy(50.0)
        assertThat(fixedViewportOffset.pixels).isEqualTo(150.0)
    }

    @Test
    fun `animateTo should be finished`() {
        val fixedViewportOffset = FixedViewportOffset(100.0)
        val job = fixedViewportOffset.animateTo(200.0, null, null)
        assertThat(job.isCompleted).isTrue()
    }
}