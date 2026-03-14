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

package androidx.glance.wear

import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearWidgetBrushTest {

    @Test
    fun foldIn_ifElement_returnsElement() {
        val initial = listOf<WearWidgetBrush.Element>()
        val element = WearWidgetBrush.color(Color.Black.rc)

        val brushes = element.foldIn(initial) { acc, current -> acc + current }

        assertThat(brushes).hasSize(1)
        assertThat(brushes[0].color.constantValueOrNull).isEqualTo(Color.Black)
    }

    @Test
    fun foldIn_ifCompanion_returnsInitial() {
        val initial = listOf<WearWidgetBrush>()

        val brushes = WearWidgetBrush.foldIn(initial) { acc, current -> acc + current }

        assertThat(brushes).isEqualTo(initial)
    }

    @Test
    fun foldIn_ifCombined_returnsOuterAndInner() {
        val initial = listOf<WearWidgetBrush.Element>()
        val combined = WearWidgetBrush.color(Color.Black.rc).color(Color.White.rc)

        val brushes = combined.foldIn(initial) { acc, current -> acc + current }

        assertThat(brushes).hasSize(2)
        assertThat(brushes[0].color.constantValueOrNull).isEqualTo(Color.Black)
        assertThat(brushes[1].color.constantValueOrNull).isEqualTo(Color.White)
    }
}
