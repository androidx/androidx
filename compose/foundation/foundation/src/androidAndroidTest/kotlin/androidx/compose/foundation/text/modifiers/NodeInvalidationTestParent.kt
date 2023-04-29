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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorLambda
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

abstract class NodeInvalidationTestParent {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun colorChange_doesNotInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(style = params.style.copy(color = Color.Red))
        )
        assertThat(layoutChange).isFalse()
        assertThat(textChange).isFalse()
    }

    @Test
    fun colorChanged_usingLambda_doesInvalidateDraw() {
        val params = generateParams()
        val redFactory = { Color.Red }
        val blueFactory = { Color.Blue }
        val drawParams = DrawParams(params.style, redFactory)
        val subject = createSubject(params, drawParams)
        val drawChanged = subject.updateDrawArgs(drawParams.copy(color = blueFactory))
        assertThat(drawChanged).isTrue()
    }

    @Test
    fun colorChanged_usingStyle_doesInvalidateDraw() {
        val params = generateParams()
        val drawParams = DrawParams(params.style, { Color.Unspecified })
        val subject = createSubject(params, drawParams)
        val drawChanged = subject.updateDrawArgs(
            drawParams = drawParams.copy(style = drawParams.style.copy(color = Color.Red))
        )
        assertThat(drawChanged).isTrue()
    }

    @Test
    fun brushChanged_usingLambda_doesInvalidateDraw() {
        val params = generateParams()
        val redFactory = { Brush.linearGradient(listOf(Color.Red)) }
        val blueFactory = { Brush.linearGradient(listOf(Color.Blue)) }
        val alpha = { 1.0f }
        val drawParams = DrawParams(params.style, null, redFactory, alpha)
        val subject = createSubject(params, drawParams)
        val drawChanged = subject.updateDrawArgs(drawParams.copy(brush = blueFactory))
        assertThat(drawChanged).isTrue()
    }

    @Test
    fun brushChanged_usingStyle_doesInvalidateDraw() {
        val params = generateParams()
        val drawParams = DrawParams(params.style, null, null, null)
        val subject = createSubject(params, drawParams)
        val drawChanged = subject.updateDrawArgs(
            drawParams = drawParams.copy(
                style = drawParams.style.copy(brush = Brush.linearGradient(listOf(Color.Blue)))
            )
        )
        assertThat(drawChanged).isTrue()
    }

    @Test
    fun brushChange_doesNotInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(style = params.style.copy(brush = Brush.horizontalGradient()))
        )
        assertThat(layoutChange).isFalse()
        assertThat(textChange).isFalse()
    }

    @Test
    fun fontSizeChange_doesInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(style = params.style.copy(fontSize = params.style.fontSize * 2))
        )
        assertThat(layoutChange).isTrue()
        assertThat(textChange).isFalse()
    }

    @Test
    fun textChange_doesInvalidateText() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(text = params.text + " goodbye")
        )
        assertThat(layoutChange).isFalse()
        assertThat(textChange).isTrue()
    }

    @Test
    fun minLinesChange_doesInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(minLines = params.minLines + 1)
        )
        assertThat(layoutChange).isTrue()
        assertThat(textChange).isFalse()
    }

    @Test
    fun maxLinesChange_doesInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(maxLines = params.minLines + 1)
        )
        assertThat(layoutChange).isTrue()
        assertThat(textChange).isFalse()
    }

    @Test
    fun softWrapChange_doesInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(softWrap = !params.softWrap)
        )
        assertThat(layoutChange).isTrue()
        assertThat(textChange).isFalse()
    }

    @Test
    fun overflowChange_doesInvalidateLayout() {
        val params = generateParams()
        val subject = createSubject(params)
        val (textChange, layoutChange) = subject.updateAll(
            params = params.copy(overflow = TextOverflow.Clip)
        )
        assertThat(layoutChange).isTrue()
        assertThat(textChange).isFalse()
    }

    abstract fun Any.updateDrawArgs(drawParams: DrawParams): Boolean
    abstract fun Any.updateAll(params: Params): Pair<Boolean, Boolean>
    abstract fun createSubject(params: Params): Any
    abstract fun createSubject(params: Params, drawParams: DrawParams): Any
    private fun generateParams(): Params {
        return Params(
            "text",
            TextStyle.Default.copy(color = Color.Cyan, fontSize = 10.sp),
            createFontFamilyResolver(context),
            TextOverflow.Ellipsis,
            true,
            10,
            1
        )
    }

    data class Params(
        val text: String,
        val style: TextStyle,
        val fontFamilyResolver: FontFamily.Resolver,
        val overflow: TextOverflow,
        val softWrap: Boolean,
        val maxLines: Int,
        val minLines: Int
    )

    data class DrawParams(
        val style: TextStyle,
        val color: ColorLambda? = null,
        val brush: (() -> Brush)? = null,
        val alpha: (() -> Float)? = null
    )
}