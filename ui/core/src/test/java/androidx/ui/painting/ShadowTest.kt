/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.painting

import androidx.ui.core.lerp
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ShadowTest {
    @Test
    fun `default value`() {
        val shadow = Shadow()
        assertThat(shadow.color, equalTo(Color(0xFF000000.toInt())))
        assertThat(shadow.blurRadius, equalTo(0.px))
        assertThat(shadow.offset, equalTo(Offset.zero))
    }

    @Test
    fun `constructor`() {
        val color = Color(0xFF00FF00.toInt())
        val offset = Offset(2f, 3f)
        val blurRadius = 1.px

        val shadow = Shadow(color, offset, blurRadius)
        assertThat(shadow.color, equalTo(color))
        assertThat(shadow.offset, equalTo(offset))
        assertThat(shadow.blurRadius, equalTo(blurRadius))
    }

    @Test
    fun `lerp`() {
        val colorA = Color(0xFF00FF00.toInt())
        val colorB = Color(0xFF0000FF.toInt())
        val offsetA = Offset(5f, 10f)
        val offsetB = Offset(0f, 5f)
        val radiusA = 0.px
        val radiusB = 3.px
        val shadowA = Shadow(colorA, offsetA, radiusA)
        val shadowB = Shadow(colorB, offsetB, radiusB)
        val t = 0.4f

        val shadow = lerp(shadowA, shadowB, t)
        assertThat(shadow.color, equalTo(lerp(colorA, colorB, t)))
        assertThat(shadow.offset, equalTo(Offset.lerp(offsetA, offsetB, t)))
        assertThat(shadow.blurRadius, equalTo(lerp(radiusA, radiusB, t)))
    }
}