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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun testParseDeviceSpecParams() {
        assertNull(parseDeviceSpecParams(null))
        assertNull(parseDeviceSpecParams(""))
        assertNull(parseDeviceSpecParams("id:pixel_7"))
        assertEquals(
            mapOf("width" to "411dp", "height" to "891dp", "windowposture" to "tabletop"),
            parseDeviceSpecParams("spec: Width = 411DP , height=891dp, windowPosture = Tabletop "),
        )
        assertEquals(
            mapOf("width" to "411dp", "name" to "my=device"),
            parseDeviceSpecParams("spec:width=411dp,,name=My=Device,invalid_no_equals"),
        )
    }

    @Test
    fun testExplicitSpecDimensionsOverrideFallbacks() {
        val windowInfo = fakeWindowInfo(DpSize(500.dp, 2400.dp))
        val specHeightScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=600dp,height=900dp,expand=vertical",
                fallbackWidth = 300.dp,
                fallbackHeight = 500.dp,
                reportDeviceHeight = true,
            )
        assertEquals(500.dp, specHeightScope.windowWidth)
        assertEquals(900.dp, specHeightScope.windowHeight)

        val specWidthScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=600dp,height=900dp,expand=horizontal",
                fallbackWidth = 300.dp,
                fallbackHeight = 500.dp,
                reportDeviceWidth = true,
            )
        assertEquals(600.dp, specWidthScope.windowWidth)
        assertEquals(2400.dp, specWidthScope.windowHeight)
    }

    @Test
    fun testFallbackDimensionsUsedWhenSpecOmitsSizeOrIsNotSpec() {
        val windowInfo = fakeWindowInfo(DpSize(500.dp, 2400.dp))
        val parentSpecScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:parent=wearos_large_round,expand=vertical",
                fallbackWidth = 227.dp,
                fallbackHeight = 227.dp,
                reportDeviceHeight = true,
            )
        assertEquals(500.dp, parentSpecScope.windowWidth)
        assertEquals(227.dp, parentSpecScope.windowHeight)

        val idHeightScope =
            createPreviewUiMediaScope(
                windowInfo,
                "id:pixel_7",
                fallbackWidth = 412.dp,
                fallbackHeight = 915.dp,
                reportDeviceHeight = true,
            )
        assertEquals(500.dp, idHeightScope.windowWidth)
        assertEquals(915.dp, idHeightScope.windowHeight)

        val idWidthScope =
            createPreviewUiMediaScope(
                windowInfo,
                "id:pixel_7",
                fallbackWidth = 412.dp,
                fallbackHeight = 915.dp,
                reportDeviceWidth = true,
            )
        assertEquals(412.dp, idWidthScope.windowWidth)
        assertEquals(2400.dp, idWidthScope.windowHeight)

        val nullSpecScope =
            createPreviewUiMediaScope(
                windowInfo,
                null,
                fallbackWidth = 411.dp,
                reportDeviceWidth = true,
            )
        assertEquals(411.dp, nullSpecScope.windowWidth)
        assertEquals(2400.dp, nullSpecScope.windowHeight)

        val zeroContainerScope =
            createPreviewUiMediaScope(
                fakeWindowInfo(DpSize.Zero),
                "id:pixel_7",
                fallbackWidth = 412.dp,
                fallbackHeight = 915.dp,
            )
        assertEquals(412.dp, zeroContainerScope.windowWidth)
        assertEquals(915.dp, zeroContainerScope.windowHeight)
    }

    @Test
    fun testReportDeviceWidthAndHeightTogether() {
        val windowInfo = fakeWindowInfo(DpSize(1800.dp, 2400.dp))
        val specScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=600dp,height=900dp",
                fallbackWidth = 300.dp,
                fallbackHeight = 500.dp,
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(600.dp, specScope.windowWidth)
        assertEquals(900.dp, specScope.windowHeight)

        val idScope =
            createPreviewUiMediaScope(
                windowInfo,
                "id:pixel_7",
                fallbackWidth = 412.dp,
                fallbackHeight = 915.dp,
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(412.dp, idScope.windowWidth)
        assertEquals(915.dp, idScope.windowHeight)
    }

    @Test
    fun testSpecPixelDimensionsConvertedUsingDpi() {
        val windowInfo = fakeWindowInfo(DpSize(1800.dp, 2400.dp))

        val explicitDpiScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=1080px,height=1920px,dpi=440",
                fallbackWidth = 300.dp,
                fallbackHeight = 500.dp,
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals((1080 * 160.0 / 440.0).toFloat(), explicitDpiScope.windowWidth.value, 0.01f)
        assertEquals((1920 * 160.0 / 440.0).toFloat(), explicitDpiScope.windowHeight.value, 0.01f)

        // Omitted dpi defaults to 420 (1050 * 160 / 420 = 400dp, 1890 * 160 / 420 = 720dp).
        val defaultDpiScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=1050px,height=1890px",
                fallbackWidth = 300.dp,
                fallbackHeight = 500.dp,
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(400.dp, defaultDpiScope.windowWidth)
        assertEquals(720.dp, defaultDpiScope.windowHeight)

        // Case-insensitive, whitespace-padded, and non-bucket DPI converts directly via 160 / dpi.
        val nonBucketDpiScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec: Width = 1000PX , Height = 2000PX , DPI = 400 ",
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(400.dp, nonBucketDpiScope.windowWidth)
        assertEquals(800.dp, nonBucketDpiScope.windowHeight)
    }

    @Test
    fun testInvalidSpecDimensionsAndDpiFallBack() {
        val windowInfo = fakeWindowInfo(DpSize(1800.dp, 2400.dp))
        val invalidDimensions =
            listOf(
                "",
                "400",
                "dp",
                "px",
                "@",
                "?",
                ":",
                "#",
                "0dp",
                "0px",
                "-10dp",
                "-10px",
                "abcdp",
                "NaNdp",
                "Infinitydp",
                "1e39px",
            )
        for (invalid in invalidDimensions) {
            val scope =
                createPreviewUiMediaScope(
                    windowInfo,
                    "spec:width=$invalid,height=$invalid,dpi=1",
                    fallbackWidth = 320.dp,
                    fallbackHeight = 640.dp,
                    reportDeviceWidth = true,
                    reportDeviceHeight = true,
                )
            assertEquals("width=$invalid", 320.dp, scope.windowWidth)
            assertEquals("height=$invalid", 640.dp, scope.windowHeight)
        }

        // Explicit invalid dpi causes px dimensions to fall back while dp dimensions still resolve.
        for (invalidDpi in listOf("", "0", "-420", "abc", "0x1a4")) {
            val pxScope =
                createPreviewUiMediaScope(
                    windowInfo,
                    "spec:width=1050px,height=700dp,dpi=$invalidDpi",
                    fallbackWidth = 320.dp,
                    fallbackHeight = 640.dp,
                    reportDeviceWidth = true,
                    reportDeviceHeight = true,
                )
            assertEquals("px width with dpi=$invalidDpi", 320.dp, pxScope.windowWidth)
            assertEquals("dp height with dpi=$invalidDpi", 700.dp, pxScope.windowHeight)
        }

        // Duplicate keys keep the last entry, matching parseDeviceSpecParams.
        val duplicateKeyScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=300dp,width=600dp,height=900dp",
                reportDeviceWidth = true,
            )
        assertEquals(600.dp, duplicateKeyScope.windowWidth)
    }

    @Test
    fun testSpecOrientationSwapsDimensionsWhenDisagreeing() {
        val windowInfo = fakeWindowInfo(DpSize(500.dp, 2400.dp))

        // Portrait raw dimensions with orientation=landscape swap to 891dp x 411dp even when the
        // caller supplies already-rotated fallbacks from LayoutLib Configuration.
        val landscapeSwappedScope =
            createPreviewUiMediaScope(
                fakeWindowInfo(DpSize(891.dp, 2400.dp)),
                "spec:width=411dp,height=891dp,orientation=landscape,expand=vertical",
                fallbackWidth = 891.dp,
                fallbackHeight = 411.dp,
                reportDeviceHeight = true,
            )
        assertEquals(891.dp, landscapeSwappedScope.windowWidth)
        assertEquals(411.dp, landscapeSwappedScope.windowHeight)

        // Already-landscape raw dimensions stay unswapped.
        val alreadyLandscapeScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=891dp,height=411dp,orientation=landscape",
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(891.dp, alreadyLandscapeScope.windowWidth)
        assertEquals(411.dp, alreadyLandscapeScope.windowHeight)

        // Pixel dimensions are converted first and then swapped for orientation=landscape.
        val pxLandscapeScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=1100px,height=1980px,dpi=440,Orientation=LANDSCAPE",
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(720.dp, pxLandscapeScope.windowWidth)
        assertEquals(400.dp, pxLandscapeScope.windowHeight)

        // Landscape raw dimensions (e.g. tablet) with orientation=portrait swap to 800dp x 1280dp.
        val portraitSwappedScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=1280dp,height=800dp,orientation=portrait",
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(800.dp, portraitSwappedScope.windowWidth)
        assertEquals(1280.dp, portraitSwappedScope.windowHeight)

        // spec:parent=... with already-rotated fallbacks from LayoutLib is not swapped a second
        // time, while unrotated standalone fallbacks are swapped to match landscape.
        val parentRotatedFallbackScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:parent=pixel_7,orientation=landscape,expand=vertical",
                fallbackWidth = 915.dp,
                fallbackHeight = 412.dp,
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(915.dp, parentRotatedFallbackScope.windowWidth)
        assertEquals(412.dp, parentRotatedFallbackScope.windowHeight)

        val parentDefaultFallbackScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:parent=pixel_7,orientation=landscape",
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(891.dp, parentDefaultFallbackScope.windowWidth)
        assertEquals(411.dp, parentDefaultFallbackScope.windowHeight)

        // Unknown orientation values leave dimensions untouched.
        val squareOrientationScope =
            createPreviewUiMediaScope(
                windowInfo,
                "spec:width=411dp,height=891dp,orientation=square",
                reportDeviceWidth = true,
                reportDeviceHeight = true,
            )
        assertEquals(411.dp, squareOrientationScope.windowWidth)
        assertEquals(891.dp, squareOrientationScope.windowHeight)
    }

    @Test
    fun testReferencePhone() {
        assertReferenceDevice(
            spec = Devices.PHONE,
            isTabletop = false,
            pointerPrecision = UiMediaScope.PointerPrecision.Coarse,
            keyboardKind = UiMediaScope.KeyboardKind.Virtual,
            viewingDistance = UiMediaScope.ViewingDistance.Near,
        )
    }

    @Test
    fun testReferenceFoldable() {
        assertReferenceDevice(
            spec = Devices.FOLDABLE,
            isTabletop = true,
            pointerPrecision = UiMediaScope.PointerPrecision.Coarse,
            keyboardKind = UiMediaScope.KeyboardKind.Virtual,
            viewingDistance = UiMediaScope.ViewingDistance.Near,
        )
    }

    @Test
    fun testReferenceTablet() {
        assertReferenceDevice(
            spec = Devices.TABLET,
            isTabletop = false,
            pointerPrecision = UiMediaScope.PointerPrecision.Coarse,
            keyboardKind = UiMediaScope.KeyboardKind.Virtual,
            viewingDistance = UiMediaScope.ViewingDistance.Medium,
        )
    }

    @Test
    fun testReferenceDesktop() {
        assertReferenceDevice(
            spec = Devices.DESKTOP,
            isTabletop = false,
            pointerPrecision = UiMediaScope.PointerPrecision.Fine,
            keyboardKind = UiMediaScope.KeyboardKind.Physical,
            viewingDistance = UiMediaScope.ViewingDistance.Medium,
        )
    }

    /**
     * The parser silently ignores keys it does not know, so a misspelled key that carries a default
     * value would go unnoticed. Overriding each parameter of each reference device with a value
     * none of them use must therefore show up in the parsed scope.
     */
    @Test
    fun testReferenceDeviceParametersAreAllParsed() {
        for (spec in listOf(Devices.PHONE, Devices.FOLDABLE, Devices.TABLET, Devices.DESKTOP)) {
            fun scopeWith(key: String, value: String) =
                createPreviewUiMediaScope(fakeWindowInfo(), spec.withParameter(key, value))

            val posture = scopeWith("windowPosture", "book").windowPosture
            assertFalse(spec, posture.isFlat || posture.isTabletop)
            assertEquals(
                spec,
                UiMediaScope.PointerPrecision.Blunt,
                scopeWith("pointerPrecision", "blunt").pointerPrecision,
            )
            assertEquals(
                spec,
                UiMediaScope.KeyboardKind.None,
                scopeWith("keyboardKind", "none").keyboardKind,
            )
            assertFalse(spec, scopeWith("hasCamera", "false").hasCamera)
            assertFalse(spec, scopeWith("hasMicrophone", "false").hasMicrophone)
            assertEquals(
                spec,
                UiMediaScope.ViewingDistance.Far,
                scopeWith("viewingDistance", "far").viewingDistance,
            )
        }
    }

    private fun assertReferenceDevice(
        spec: String,
        isTabletop: Boolean,
        pointerPrecision: UiMediaScope.PointerPrecision,
        keyboardKind: UiMediaScope.KeyboardKind,
        viewingDistance: UiMediaScope.ViewingDistance,
    ) {
        val scope = createPreviewUiMediaScope(fakeWindowInfo(), spec)
        assertEquals(isTabletop, scope.windowPosture.isTabletop)
        assertEquals(!isTabletop, scope.windowPosture.isFlat)
        assertEquals(pointerPrecision, scope.pointerPrecision)
        assertEquals(keyboardKind, scope.keyboardKind)
        assertTrue(scope.hasCamera)
        assertTrue(scope.hasMicrophone)
        assertEquals(viewingDistance, scope.viewingDistance)
    }

    /** Returns this spec with the value of [key] replaced, failing unless [key] appears once. */
    private fun String.withParameter(key: String, value: String): String {
        val parameter = Regex("(?<=[:,])$key=[^,]*")
        assertEquals("$key in $this", 1, parameter.findAll(this).count())
        return replace(parameter, "$key=$value")
    }
}
