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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviewExpandTest {
    private fun parse(spec: String?, density: Float = 2f) =
        parsePreviewExpandSettings(spec, density)

    @Test
    fun noExpand_returnsNull() {
        assertNull(parse(null))
        assertNull(parse(""))
        assertNull(parse("spec:width=411dp,height=891dp"))
        assertNull(parse("spec:width=411dp,height=891dp,maxHeight=1000dp"))
    }

    @Test
    fun nonSpecDevice_returnsNull() {
        assertNull(parse("id:pixel_7"))
        assertNull(parse("name:Pixel 7"))
        assertNull(parse("expand=vertical"))
    }

    @Test
    fun invalidExpand_returnsNull() {
        assertNull(parse("spec:width=411dp,height=891dp,expand=both"))
        assertNull(parse("spec:width=411dp,height=891dp,expand="))
        assertNull(parse("spec:width=411dp,height=891dp,expand=true"))
    }

    @Test
    fun expandVertical_defaultMaxSize() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, DEFAULT_EXPAND_MAX_SIZE_PX),
            parse("spec:width=411dp,height=891dp,expand=vertical"),
        )
    }

    @Test
    fun expandHorizontal_defaultMaxSize() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.HORIZONTAL, DEFAULT_EXPAND_MAX_SIZE_PX),
            parse("spec:width=411dp,height=891dp,expand=horizontal"),
        )
    }

    @Test
    fun expandAndKeys_areCaseInsensitive() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 300),
            parse("spec:parent=pixel_7,Expand=VERTICAL,MAXHEIGHT=300PX"),
        )
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.HORIZONTAL, 300),
            parse("spec:parent=pixel_7,expand=Horizontal,maxWidth=300px"),
        )
    }

    @Test
    fun whitespaceAroundParams_isIgnored() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 300),
            parse("spec:parent=pixel_7, expand = vertical , maxHeight = 300px"),
        )
    }

    @Test
    fun maxHeight_dpIsConvertedWithDensity() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 1200),
            parse("spec:parent=pixel_7,expand=vertical,maxHeight=1200dp", density = 1f),
        )
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 2400),
            parse("spec:parent=pixel_7,expand=vertical,maxHeight=1200dp", density = 2f),
        )
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 3300),
            parse("spec:parent=pixel_7,expand=vertical,maxHeight=1200dp", density = 2.75f),
        )
    }

    @Test
    fun maxHeight_decimalValues() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 301),
            parse("spec:parent=pixel_7,expand=vertical,maxHeight=150.5dp", density = 2f),
        )
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, 1001),
            parse("spec:parent=pixel_7,expand=vertical,maxHeight=1000.6px"),
        )
    }

    @Test
    fun maxWidth_px() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.HORIZONTAL, 3000),
            parse("spec:width=411dp,height=891dp,expand=horizontal,maxWidth=3000px"),
        )
    }

    @Test
    fun maxSizeKeyNotMatchingTheAxis_isIgnored() {
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, DEFAULT_EXPAND_MAX_SIZE_PX),
            parse("spec:parent=pixel_7,expand=vertical,maxWidth=300px"),
        )
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.HORIZONTAL, DEFAULT_EXPAND_MAX_SIZE_PX),
            parse("spec:parent=pixel_7,expand=horizontal,maxHeight=300px"),
        )
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.HORIZONTAL, 200),
            parse("spec:parent=pixel_7,expand=horizontal,maxHeight=300px,maxWidth=200px"),
        )
    }

    @Test
    fun invalidMaxSize_fallsBackToDefault() {
        listOf("1200", "abcdp", "dp", "px", "1200in", "-100px", "0dp", "0.2px", "", "NaNpx")
            .forEach { value ->
                assertEquals(
                    "maxHeight=$value",
                    PreviewExpandSettings(PreviewExpandAxis.VERTICAL, DEFAULT_EXPAND_MAX_SIZE_PX),
                    parse("spec:parent=pixel_7,expand=vertical,maxHeight=$value"),
                )
            }
    }
}
