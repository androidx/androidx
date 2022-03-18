/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering

import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [ComplicationStyle]. */
@RunWith(ComplicationsTestRunner::class)
@DoNotInstrument
class ComplicationStyleTest {
    @Test
    public fun tintTest() {
        assertThat(ComplicationStyle.tint(Color.argb(127, 255, 255, 255), Color.argb(0, 255, 0, 0)))
            .isEqualTo(Color.argb(127, 255, 0, 0))

        assertThat(ComplicationStyle.tint(Color.argb(127, 255, 255, 255), Color.argb(0, 100, 0, 0)))
            .isEqualTo(Color.argb(127, 100, 0, 0))

        assertThat(ComplicationStyle.tint(Color.argb(127, 100, 100, 100), Color.argb(0, 255, 0, 0)))
            .isEqualTo(Color.argb(127, 100, 0, 0))

        assertThat(
            ComplicationStyle.tint(Color.argb(127, 50, 100, 200), Color.argb(0, 127, 127, 255))
        ).isEqualTo(Color.argb(127, 48, 48, 97))
    }

    @Test
    public fun asTinted() {
        val complicationStyle = ComplicationStyle()
        val tintedComplicationStyle = complicationStyle.asTinted(Color.RED)

        assertThat(tintedComplicationStyle.backgroundColor).isEqualTo(Color.BLACK)
        assertThat(tintedComplicationStyle.borderColor).isEqualTo(Color.RED)
        assertThat(tintedComplicationStyle.highlightColor)
            .isEqualTo(ComplicationStyle.tint(Color.LTGRAY, Color.RED))
        assertThat(tintedComplicationStyle.iconColor).isEqualTo(Color.RED)
        assertThat(tintedComplicationStyle.rangedValuePrimaryColor).isEqualTo(Color.RED)
        assertThat(tintedComplicationStyle.rangedValueSecondaryColor)
            .isEqualTo(ComplicationStyle.tint(Color.LTGRAY, Color.RED))
        assertThat(tintedComplicationStyle.textColor).isEqualTo(Color.RED)
        assertThat(tintedComplicationStyle.titleColor)
            .isEqualTo(ComplicationStyle.tint(Color.LTGRAY, Color.RED))
    }
}