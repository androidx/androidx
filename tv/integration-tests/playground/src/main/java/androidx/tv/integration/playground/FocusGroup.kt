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

package androidx.tv.integration.playground

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.tv.foundation.ExperimentalTvFoundationApi

/**
 * Composable container that provides modifier extensions to allow focus to be restored to the
 * element that was previously focused within the TvFocusGroup.
 *
 * @param modifier the modifier to apply to this group.
 * @param content the content that is present within the group and can use focus-group modifier
 * extensions.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@ExperimentalTvFoundationApi
@Composable
fun FocusGroup(
    modifier: Modifier = Modifier,
    content: @Composable FocusGroupScope.() -> Unit
) {
    val focusGroupKeyHash = currentCompositeKeyHash

    // TODO: Is this the intended way to call rememberSaveable
    //  with key set to parentHash?
    val previousFocusedItemHash: MutableState<Int?> = rememberSaveable(
        key = focusGroupKeyHash.toString()
    ) {
        mutableStateOf(null)
    }

    val state = FocusGroupState(previousFocusedItemHash = previousFocusedItemHash)

    Box(
        modifier = modifier
            .focusProperties {
                enter = {
                    if (state.hasRecordedState()) {
                        state.focusRequester
                    } else {
                        FocusRequester.Default
                    }
                }
            }
            .focusGroup()
    ) {
        FocusGroupScope(state).content()
    }
}

/**
 * Scope containing the modifier extensions to be used within [FocusGroup].
 */
@ExperimentalTvFoundationApi
class FocusGroupScope internal constructor(private val state: FocusGroupState) {
    private var currentFocusableIdIndex = 0

    private fun generateUniqueFocusableId(): Int = currentFocusableIdIndex++

    /**
     * Modifier that records if the item was in focus before it moved out of the group. When focus
     * enters the [FocusGroup], the item will be returned focus.
     */
    @SuppressLint("ComposableModifierFactory")
    @Composable
    fun Modifier.restorableFocus(): Modifier =
        this.restorableFocus(focusId = rememberSaveable { generateUniqueFocusableId() })

    /**
     * Modifier that marks the current composable as the item to gain focus initially when focus
     * enters the [FocusGroup]. When focus enters the [FocusGroup], the item will be returned focus.
     */
    @SuppressLint("ComposableModifierFactory")
    @Composable
    fun Modifier.initiallyFocused(): Modifier {
        val focusId = rememberSaveable { generateUniqueFocusableId() }
        if (state.noRecordedState()) {
            state.recordFocusedItemHash(focusId)
        }
        return this.restorableFocus(focusId)
    }

    @SuppressLint("ComposableModifierFactory")
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Modifier.restorableFocus(focusId: Int): Modifier {
        val focusRequester = remember { FocusRequester() }
        var isFocused = remember { false }
        val isCurrentlyFocused by rememberUpdatedState(isFocused)
        val focusManager = LocalFocusManager.current
        state.associatedWith(focusId, focusRequester)
        DisposableEffect(Unit) {
            onDispose {
                state.clearDisposedFocusRequester(focusId)
                if (isCurrentlyFocused) {
                    focusManager.moveFocus(FocusDirection.Exit)
                    focusManager.moveFocus(FocusDirection.Enter)
                }
            }
        }

        return this
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused || it.hasFocus
                if (isFocused) {
                    state.recordFocusedItemHash(focusId)
                    state.associatedWith(focusId, focusRequester)
                }
            }
    }
}

@Stable
@ExperimentalTvFoundationApi
internal class FocusGroupState(
    private var previousFocusedItemHash: MutableState<Int?>
) {
    internal var focusRequester: FocusRequester = FocusRequester.Default
        private set

    internal fun recordFocusedItemHash(itemHash: Int) {
        previousFocusedItemHash.value = itemHash
    }

    internal fun clearDisposedFocusRequester(itemHash: Int) {
        if (previousFocusedItemHash.value == itemHash) {
            focusRequester = FocusRequester.Default
        }
    }

    internal fun associatedWith(itemHash: Int, focusRequester: FocusRequester) {
        if (previousFocusedItemHash.value == itemHash) {
            this.focusRequester = focusRequester
        }
    }

    internal fun hasRecordedState(): Boolean = !noRecordedState()

    internal fun noRecordedState(): Boolean =
        previousFocusedItemHash.value == null && focusRequester == FocusRequester.Default
}
