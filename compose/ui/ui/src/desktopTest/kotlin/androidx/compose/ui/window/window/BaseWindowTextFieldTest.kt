/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.window.window

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.density
import androidx.compose.ui.window.runApplicationTest
import androidx.compose.ui.window.waitForFocusGain
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.experimental.theories.DataPoint

open class BaseWindowTextFieldTest {
    internal fun <S: TextFieldTestScope> runTextFieldTest(
        textFieldKind: TextFieldKind<S>,
        name: String,
        initialText: String = "",
        initialSelection: TextRange = TextRange.Zero,
        body: suspend S.() -> Unit
    ) = runApplicationTest {
        var scope: S? = null
        launchTestWindowApplication(
            state = WindowState(position = WindowPosition(200.dp, 200.dp)),
            undecorated = true
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (scope == null) {
                    scope = textFieldKind.createScope(
                        windowTestScope = this@runApplicationTest,
                        window = window,
                        initialText = initialText,
                        initialSelection = initialSelection
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$name ($scope)")
                    Box(Modifier.border(1.dp, Color.Black).padding(8.dp)) {
                        scope.TextField()
                    }
                }
            }
        }

        awaitIdle()
        window.waitForFocusGain()
        scope!!.body()
    }

    internal abstract class TextFieldTestScope(
        private val windowTestScope: WindowTestScope,
        val window: ComposeWindow
    ) {
        abstract val text: String
        abstract val selection: TextRange
        abstract val composition: TextRange?
        abstract val textLayoutResult: TextLayoutResult?

        var textBoundingBox: Rect = Rect.Zero
            protected set

        @Composable
        abstract fun TextField()

        suspend fun awaitIdle() {
            windowTestScope.awaitIdle()
        }

        suspend fun assertStateEquals(
            text: String,
            selection: TextRange,
            composition: TextRange?,
            awaitIdle: Boolean = true
        ) {
            if (awaitIdle) {
                windowTestScope.awaitIdle()
            }
            assertThat(this.text).isEqualTo(text)
            assertThat(this.selection).isEqualTo(selection)
            assertThat(this.composition).isEqualTo(composition)
        }

        suspend fun clickBeforeIndex(index: Int) {
            awaitIdle() // To get the latest textLayoutResult
            val localLocation = textLayoutResult!!.let {
                if (index == text.length)
                    it.getBoundingBox(index-1).centerRight
                else
                    it.getBoundingBox(index).centerLeft
            }
            val location = localLocation + textBoundingBox.topLeft
            val platformLocation = location.let {
                val scale = window.density.density
                IntOffset(
                    x = (it.x / scale).roundToInt(),
                    y = (it.y / scale).roundToInt()
                )
            }
            window.sendMousePress(
                x = platformLocation.x,
                y = platformLocation.y
            )
            window.sendMouseRelease(
                x = platformLocation.x,
                y = platformLocation.y
            )
        }
    }

    internal abstract class TextField1Scope(
        windowTestScope: WindowTestScope,
        window: ComposeWindow,
        initialText: String,
        initialSelection: TextRange,
    ): TextFieldTestScope(windowTestScope, window) {

        protected var textFieldValue by mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = initialSelection,
            )
        )

        override val text: String
            get() = textFieldValue.text

        override val selection: TextRange
            get() = textFieldValue.selection

        override val composition: TextRange?
            get() = textFieldValue.composition

        override var textLayoutResult: TextLayoutResult? = null
            protected set

        override fun toString() = "TextField1"
    }

    internal abstract class TextField2Scope(
        windowTestScope: WindowTestScope,
        window: ComposeWindow,
        initialText: String,
        initialSelection: TextRange,
    ): TextFieldTestScope(windowTestScope, window) {
        protected val textFieldState = TextFieldState(
            initialText = initialText,
            initialSelection = initialSelection,
        )
        var inputTransformation: InputTransformation? by mutableStateOf(null)

        override val text: String
            get() = textFieldState.text.toString()

        override val selection: TextRange
            get() = textFieldState.selection

        override val composition: TextRange?
            get() = textFieldState.composition

        protected var textLayoutResultGetter: (() -> TextLayoutResult?)? = null
        override val textLayoutResult: TextLayoutResult?
            get() = textLayoutResultGetter?.invoke()
    }

    internal abstract class SecureTextFieldScope(
        windowTestScope: WindowTestScope,
        window: ComposeWindow,
        textObfuscationMode: TextObfuscationMode,
        initialText: String = "",
        initialSelection: TextRange = TextRange.Zero,
    ): TextField2Scope(windowTestScope, window, initialText, initialSelection) {

        var textObfuscationMode by mutableStateOf(textObfuscationMode)

    }

    internal fun interface TextFieldKind<S: TextFieldTestScope> {
        fun createScope(
            windowTestScope: WindowTestScope,
            window: ComposeWindow,
            initialText: String,
            initialSelection: TextRange,
        ): S
    }

    companion object {
        @JvmField
        @DataPoint
        internal val TextField1 = TextFieldKind<TextField1Scope> {
            windowTestScope, window, initialText, initialSelection ->
                object : TextField1Scope(windowTestScope, window, initialText, initialSelection) {
                    @Composable
                    override fun TextField() {
                        val focusRequester = remember { FocusRequester() }
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                            },
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onPlaced {
                                    textBoundingBox = it.boundsInWindow()
                                }
                        )

                        LaunchedEffect(focusRequester) {
                            focusRequester.requestFocus()
                        }
                    }

                    override fun toString() = "TextField1"
                }
        }

        @JvmField
        @DataPoint
        internal val TextField2 = TextFieldKind<TextField2Scope> {
            windowTestScope, window, initialText, initialSelection ->
                object : TextField2Scope(windowTestScope, window, initialText, initialSelection) {
                    @Composable
                    override fun TextField() {
                        val focusRequester = remember { FocusRequester() }
                        BasicTextField(
                            state = textFieldState,
                            inputTransformation = inputTransformation,
                            onTextLayout = { textLayoutResultGetter = it },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onPlaced {
                                    textBoundingBox = it.boundsInWindow()
                                }
                        )

                        LaunchedEffect(focusRequester) {
                            focusRequester.requestFocus()
                        }
                    }

                    override fun toString() = "TextField2"
                }
        }

        @JvmField
        @DataPoint
        internal val SecureTextField = TextFieldKind<SecureTextFieldScope> {
            windowTestScope, window, initialText, initialSelection ->
                object : SecureTextFieldScope(
                    windowTestScope,
                    window,
                    TextObfuscationMode.Hidden,
                    initialText,
                    initialSelection
                ) {
                    @Composable
                    override fun TextField() {
                        val focusRequester = remember { FocusRequester() }

                        BasicSecureTextField(
                            state = textFieldState,
                            textObfuscationMode = textObfuscationMode,
                            onTextLayout = { textLayoutResultGetter = it },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onPlaced {
                                    textBoundingBox = it.boundsInWindow()
                                }
                        )

                        LaunchedEffect(focusRequester) {
                            focusRequester.requestFocus()
                        }
                    }

                    override fun toString() = "SecureTextField"
                }
        }
    }
}