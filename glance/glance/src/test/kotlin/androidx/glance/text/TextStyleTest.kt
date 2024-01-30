/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextStyleTest {
    @Test
    fun equality() {
        assertThat(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        ).isEqualTo(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        )

        assertThat(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        ).isNotEqualTo(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic
            )
        )

        assertThat(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        ).isNotEqualTo(
            TextStyle(
                color = ColorProvider(Color.Magenta),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        )

        assertThat(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        ).isNotEqualTo(
            TextStyle(
                color = ColorProvider(Color.Red),
                fontSize = 12.sp,
                fontStyle = FontStyle.Normal
            )
        )
    }
}
