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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI.Companion.DefaultContainerSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Class to provide a list of UIs, using the collapsible layout provided, to be used in tests. */
class CollapsibleUI(
    private val contentUnderTest:
        @Composable
        @RemoteComposable
        (modifier: RemoteModifier, content: @Composable @RemoteComposable () -> Unit) -> Unit,
    private val priorityModifier: (priority: Float) -> RemoteModifier,
) {

    fun getUIs(): List<@Composable (() -> Unit)> =
        listOf(
            ::TestFourSquares_displaysThree,
            ::TestSixSquaresWithPriorities_displaysThreeWithHighestPriorities,
            ::TestSingleContentInContainerWithSizeAndBackground_displaysContentAndBackground,
            ::TestEmptyContainerWithSizeAndBackground_displaysNothing,
            ::TestContentBiggerThanContainerWithSizeAndBackground_displaysNothing,
        )

    @RemoteComposable
    @Composable
    private fun TestFourSquares_displaysThree() {
        contentUnderTest(RemoteModifier.size(DefaultContainerSize)) {
            CustomBox('A')
            CustomBox('B')
            CustomBox('C')
            CustomBox('D')
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSixSquaresWithPriorities_displaysThreeWithHighestPriorities() {
        contentUnderTest(RemoteModifier.size(DefaultContainerSize)) {
            CustomBox('A', priorityModifier = priorityModifier(1f))
            CustomBox('B', priorityModifier = priorityModifier(4f))
            CustomBox('C', priorityModifier = priorityModifier(2f))
            CustomBox('D', priorityModifier = priorityModifier(5f))
            CustomBox('E', priorityModifier = priorityModifier(3f))
            CustomBox('F', priorityModifier = priorityModifier(6f))
        }
    }

    @RemoteComposable
    @Composable
    private fun TestSingleContentInContainerWithSizeAndBackground_displaysContentAndBackground() {
        contentUnderTest(RemoteModifier.size(DefaultContainerSize).background(Color.Red)) {
            CustomBox('A')
        }
    }

    @RemoteComposable
    @Composable
    private fun TestEmptyContainerWithSizeAndBackground_displaysNothing() {
        contentUnderTest(RemoteModifier.size(DefaultContainerSize).background(Color.Red)) {}
    }

    @RemoteComposable
    @Composable
    private fun TestContentBiggerThanContainerWithSizeAndBackground_displaysNothing() {
        contentUnderTest(RemoteModifier.size(DefaultContainerSize).background(Color.Red)) {
            CustomBox(
                'A',
                modifier = RemoteModifier.size(RemoteDp(DefaultContainerSize.value + 10.rf)),
            )
        }
    }

    @RemoteComposable
    @Composable
    private fun CustomBox(
        letter: Char,
        modifier: RemoteModifier = RemoteModifier,
        priorityModifier: RemoteModifier? = null,
    ) {
        val appliedModifier =
            modifier
                .padding(5.dp)
                .size(20.rdp)
                .background(Color.Blue)
                .then(priorityModifier ?: RemoteModifier)

        RemoteBox(modifier = appliedModifier, contentAlignment = RemoteAlignment.Center) {
            RemoteText(letter.toString())
        }
    }
}
