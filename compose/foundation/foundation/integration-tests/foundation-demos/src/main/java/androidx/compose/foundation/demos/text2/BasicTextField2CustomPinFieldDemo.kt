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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.demos.text2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldBufferWithSelection
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.clearText
import androidx.compose.foundation.text2.input.maxLengthInChars
import androidx.compose.foundation.text2.input.then
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@Composable
fun BasicTextField2CustomPinFieldDemo() {
    val viewModel = remember { VerifyPinViewModel() }
    VerifyPinScreen(viewModel)
}

@Suppress("AnimateAsStateLabel")
@Composable
private fun VerifyPinScreen(viewModel: VerifyPinViewModel) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(viewModel) {
        viewModel.run()
    }

    if (!viewModel.isLoading) {
        DisposableEffect(Unit) {
            focusRequester.requestFocus()
            onDispose {}
        }
    }
    val blurRadius by animateDpAsState(if (viewModel.isLoading) 5.dp else 0.dp)
    val scale by animateFloatAsState(if (viewModel.isLoading) 0.85f else 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight()
    ) {
        PinField(
            viewModel.pinState,
            enabled = !viewModel.isLoading,
            modifier = Modifier
                .focusRequester(focusRequester)
                .graphicsLayer {
                    if (blurRadius != 0.dp) {
                        val blurRadiusPx = blurRadius.toPx()
                        renderEffect =
                            BlurEffect(blurRadiusPx, blurRadiusPx, edgeTreatment = TileMode.Decal)
                    }
                    scaleX = scale
                    scaleY = scale
                }
        )
        AnimatedVisibility(visible = viewModel.isLoading) {
            CircularProgressIndicator(Modifier.padding(top = 8.dp))
        }
    }
}

private class VerifyPinViewModel {
    val pinState = PinState(maxDigits = 6)
    val isLoading: Boolean by derivedStateOf { pinState.digits.length == 6 }

    suspend fun run() {
        snapshotFlow { pinState.digits }
            .filter { it.length == 6 }
            .collectLatest { digits ->
                validatePin(digits)
            }
    }

    private suspend fun validatePin(digits: String): Boolean {
        val random = Random(digits.toInt())
        val isValid = random.nextBoolean()

        if (isValid) {
            awaitCancellation()
        } else {
            val delay = random.nextInt(3, 8) * 250
            delay(delay.toLong())
            pinState.clear()
            return false
        }
    }
}

@Stable
private class PinState(val maxDigits: Int) {
    val digits: String by derivedStateOf {
        textState.text.toString()
    }

    /*internal*/ val textState = TextFieldState()
    /*internal*/ val filter: TextEditFilter = OnlyDigitsFilter.then(
        TextEditFilter.maxLengthInChars(maxDigits),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )

    fun clear() {
        textState.clearText()
    }

    private object OnlyDigitsFilter : TextEditFilter {
        override fun filter(
            originalValue: TextFieldCharSequence,
            valueWithChanges: TextFieldBufferWithSelection
        ) {
            if (!valueWithChanges.isDigitsOnly()) {
                valueWithChanges.revertAllChanges()
            }
        }
    }
}

@Composable
private fun PinField(
    state: PinState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.3f
    val contentColor = LocalContentColor.current.copy(alpha = contentAlpha)

    BasicTextField2(
        state = state.textState,
        filter = state.filter,
        modifier = modifier
            .border(1.dp, contentColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        enabled = enabled
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            // Ignore inner field, we'll draw it ourselves.
            PinContents(state)
        }
    }
}

@Composable
private fun PinContents(state: PinState) {
    val focusedColor = MaterialTheme.colors.secondary.copy(alpha = LocalContentAlpha.current)
    val text = buildAnnotatedString {
        val digits = state.digits
        repeat(state.maxDigits) { i ->
            withStyle(
                SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    background = if (digits.length == i) focusedColor else Color.Unspecified,
                )
            ) {
                append(if (digits.length > i) digits[i].toString() else " ")
            }
            if (i < state.maxDigits - 1) {
                append(" - ")
            }
        }
    }
    Text(text, fontFamily = FontFamily.Monospace)
}