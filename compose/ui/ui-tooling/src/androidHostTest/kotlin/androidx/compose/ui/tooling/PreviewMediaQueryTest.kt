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

package androidx.compose.ui.tooling

import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMediaQueryApi::class)
class PreviewMediaQueryTest {

    private fun fakeWindowInfo(size: DpSize = DpSize.Zero): WindowInfo =
        object : WindowInfo {
            override val isWindowFocused: Boolean = true
            override val containerDpSize: DpSize = size
        }

    @Test
    fun testDefaultFallback() {
        val nullSpecScope = createPreviewUiMediaScope(fakeWindowInfo(), null)
        assertTrue(nullSpecScope.windowPosture.isFlat)
        assertEquals(UiMediaScope.PointerPrecision.Coarse, nullSpecScope.pointerPrecision)
        assertEquals(UiMediaScope.KeyboardKind.Virtual, nullSpecScope.keyboardKind)
        assertTrue(nullSpecScope.hasCamera)
        assertTrue(nullSpecScope.hasMicrophone)
        assertEquals(UiMediaScope.ViewingDistance.Near, nullSpecScope.viewingDistance)
        assertEquals(411.dp, nullSpecScope.windowWidth)
        assertEquals(891.dp, nullSpecScope.windowHeight)

        val emptySpecScope = createPreviewUiMediaScope(fakeWindowInfo(), "")
        assertTrue(emptySpecScope.windowPosture.isFlat)

        val idSpecScope = createPreviewUiMediaScope(fakeWindowInfo(), "id:pixel_7")
        assertTrue(idSpecScope.windowPosture.isFlat)
    }

    @Test
    fun testSpecParsingWithFullPropertyNames() {
        val spec =
            "spec:width=1280dp,height=800dp,windowPosture=tabletop,pointerPrecision=fine,keyboardKind=physical,hasCamera=false,hasMicrophone=false,viewingDistance=far"
        val scope = createPreviewUiMediaScope(fakeWindowInfo(), spec)
        assertTrue(scope.windowPosture.isTabletop)
        assertFalse(scope.windowPosture.isFlat)
        assertEquals(UiMediaScope.PointerPrecision.Fine, scope.pointerPrecision)
        assertEquals(UiMediaScope.KeyboardKind.Physical, scope.keyboardKind)
        assertFalse(scope.hasCamera)
        assertFalse(scope.hasMicrophone)
        assertEquals(UiMediaScope.ViewingDistance.Far, scope.viewingDistance)
        assertEquals(1280.dp, scope.windowWidth)
        assertEquals(800.dp, scope.windowHeight)
    }

    @Test
    fun testBookPosture() {
        val spec = "spec:windowPosture=book"
        val scope = createPreviewUiMediaScope(fakeWindowInfo(), spec)
        assertFalse(scope.windowPosture.isFlat)
        assertFalse(scope.windowPosture.isTabletop)
        assertEquals(1, scope.windowPosture.folds.size)
        assertEquals(
            UiMediaScope.FoldOrientation.Vertical,
            scope.windowPosture.folds[0].orientation,
        )
        assertEquals(UiMediaScope.FoldState.HalfOpened, scope.windowPosture.folds[0].state)
    }

    @Test
    fun testMediumViewingDistance() {
        val spec = "spec:width=1280dp,height=800dp,viewingDistance=medium"
        val scope = createPreviewUiMediaScope(fakeWindowInfo(), spec)
        assertEquals(UiMediaScope.ViewingDistance.Medium, scope.viewingDistance)
    }

    @Test
    fun testWindowDimensionsReflectContainerDpSizeWhenAvailable() {
        val windowInfo = fakeWindowInfo(DpSize(500.dp, 900.dp))
        val scope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=411dp,height=891dp,windowPosture=flat",
            )
        assertEquals(500.dp, scope.windowWidth)
        assertEquals(900.dp, scope.windowHeight)
    }
}
