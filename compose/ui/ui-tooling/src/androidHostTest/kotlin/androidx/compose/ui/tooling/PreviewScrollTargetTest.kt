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

class PreviewScrollTargetTest {
    private fun parse(spec: String?) = parsePreviewScrollTarget(spec)

    @Test
    fun noScrollTo_returnsNull() {
        assertNull(parse(null))
        assertNull(parse(""))
        assertNull(parse("spec:width=411dp,height=891dp"))
        assertNull(parse("spec:parent=wearos_large_round,expand=vertical"))
    }

    @Test
    fun nonSpecDevice_returnsNull() {
        assertNull(parse("id:wearos_large_round"))
        assertNull(parse("name:Wear OS Large Round"))
        assertNull(parse("scrollTo=end"))
    }

    @Test
    fun invalidScrollTo_returnsNull() {
        assertNull(parse("spec:parent=wearos_large_round,scrollTo="))
        assertNull(parse("spec:parent=wearos_large_round,scrollTo=start"))
        assertNull(parse("spec:parent=wearos_large_round,scrollTo=bottom"))
        assertNull(parse("spec:parent=wearos_large_round,scrollTo=100dp"))
        assertNull(parse("spec:parent=wearos_large_round,scrollTo"))
    }

    @Test
    fun scrollToEnd() {
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:parent=wearos_large_round,scrollTo=end"),
        )
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:width=411dp,height=891dp,scrollTo=end"),
        )
    }

    @Test
    fun keyAndValue_areCaseInsensitive() {
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:parent=wearos_large_round,SCROLLTO=End"),
        )
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:parent=wearos_large_round,scrollto=END"),
        )
    }

    @Test
    fun whitespaceAroundParams_isIgnored() {
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:parent=wearos_large_round, scrollTo = end "),
        )
    }

    @Test
    fun scrollTo_isIndependentOfExpand() {
        // The adapter ignores scrollTo when expanding vertically, but the parsing does not depend
        // on the other keys.
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:parent=pixel_7,expand=vertical,scrollTo=end"),
        )
        assertEquals(
            PreviewScrollTarget.End,
            parse("spec:parent=pixel_7,expand=horizontal,scrollTo=end"),
        )
        // And it does not affect the expansion settings.
        assertEquals(
            PreviewExpandSettings(PreviewExpandAxis.VERTICAL, DEFAULT_EXPAND_MAX_SIZE_PX),
            parsePreviewExpandSettings("spec:parent=pixel_7,expand=vertical,scrollTo=end", 2f),
        )
        assertNull(parsePreviewExpandSettings("spec:parent=pixel_7,scrollTo=end", 2f))
    }
}
