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

package androidx.glance.adaptive.appwidget.components.graph

import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GraphDefaultsTest {

    @Test
    fun barSpacing_widensFromW3() {
        assertThat(GraphDefaults.barSpacing(SizeTiers(WidthTier.W1, HeightTier.H4))).isEqualTo(8.dp)
        assertThat(GraphDefaults.barSpacing(SizeTiers(WidthTier.W2, HeightTier.H4))).isEqualTo(8.dp)
        assertThat(GraphDefaults.barSpacing(SizeTiers(WidthTier.W3, HeightTier.H4)))
            .isEqualTo(16.dp)
        assertThat(GraphDefaults.barSpacing(SizeTiers(WidthTier.W4, HeightTier.H4)))
            .isEqualTo(16.dp)
    }
}
