/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Any tests for SP need to run outside of common because they need a proper `actual` FontScalable
 * interface that doesn't depend on Android core.
 */
@RunWith(JUnit4::class)
class DesktopGraphicsLayerScopeTest {

    @Test
    fun testDpPixelConversions() {
        val scope = GraphicsLayerScope() as ReusableGraphicsLayerScope
        scope.graphicsDensity = Density(2.0f, 3.0f)
        with(scope) {
            assertEquals(4.0f, 2f.dp.toPx())
            assertEquals(6.0f, 3f.dp.toSp().toPx())
        }
    }
}
