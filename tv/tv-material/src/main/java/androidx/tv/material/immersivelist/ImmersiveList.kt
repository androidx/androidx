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

package androidx.tv.material.immersivelist

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.tv.material.ExperimentalTvMaterialApi
import kotlinx.coroutines.launch

/**
 * Immersive List consists of a list with multiple items and a background that displays content
 * based on the item in focus.
 * To animate the background's entry and exit, use [ImmersiveListBackgroundScope.AnimatedContent].
 * To display the background only when the list is in focus, use
 * [ImmersiveListBackgroundScope.AnimatedVisibility].
 *
 * @param background Composable defining the background to be displayed for a given item's
 * index.
 * @param modifier applied to Immersive List.
 * @param listAlignment Alignment of the List with respect to the Immersive List.
 * @param list composable defining the list of items that has to be rendered.
 */
@Suppress("IllegalExperimentalApiUsage")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@ExperimentalTvMaterialApi
@Composable
fun ImmersiveList(
    background:
    @Composable ImmersiveListBackgroundScope.(index: Int, listHasFocus: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listAlignment: Alignment = Alignment.BottomEnd,
    list: @Composable ImmersiveListScope.() -> Unit,
) {
    var currentItemIndex by remember { mutableStateOf(0) }
    var listHasFocus by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    coroutineScope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
    ) {
        ImmersiveListBackgroundScope(this).background(currentItemIndex, listHasFocus)

        val focusManager = LocalFocusManager.current

        Box(Modifier.align(listAlignment).onFocusChanged { listHasFocus = it.hasFocus }) {
            ImmersiveListScope {
                currentItemIndex = it
                focusManager.moveFocus(FocusDirection.Enter)
            }.list()
        }
    }
}

@ExperimentalTvMaterialApi
object ImmersiveListDefaults {
    /**
     * Default transition used to bring the background content into view
     */
    val EnterTransition: EnterTransition = fadeIn(animationSpec = tween(300))

    /**
     * Default transition used to remove the background content from view
     */
    val ExitTransition: ExitTransition = fadeOut(animationSpec = tween(300))
}

@Immutable
@ExperimentalTvMaterialApi
public class ImmersiveListBackgroundScope internal constructor(boxScope: BoxScope) : BoxScope
by boxScope {

    /**
     * [ImmersiveListBackgroundScope.AnimatedVisibility] composable animates the appearance and
     * disappearance of its content, as [visible] value changes. Different [EnterTransition]s and
     * [ExitTransition]s can be defined in [enter] and [exit] for the appearance and disappearance
     * animation.
     *
     * @param visible defines whether the content should be visible
     * @param modifier modifier for the Layout created to contain the [content]
     * @param enter EnterTransition(s) used for the appearing animation, fading in by default
     * @param exit ExitTransition(s) used for the disappearing animation, fading out by default
     * @param content Content to appear or disappear based on the value of [visible]
     *
     * @link androidx.compose.animation.AnimatedVisibility
     * @see androidx.compose.animation.AnimatedVisibility
     * @see EnterTransition
     * @see ExitTransition
     * @see AnimatedVisibilityScope
     */
    @Composable
    fun AnimatedVisibility(
        visible: Boolean,
        modifier: Modifier = Modifier,
        enter: EnterTransition = ImmersiveListDefaults.EnterTransition,
        exit: ExitTransition = ImmersiveListDefaults.ExitTransition,
        label: String = "AnimatedVisibility",
        content: @Composable AnimatedVisibilityScope.() -> Unit
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible,
            modifier,
            enter,
            exit,
            label,
            content)
    }

    /**
     * [ImmersiveListBackgroundScope.AnimatedContent] is a container that automatically animates its
     * content when [targetState] changes. Its [content] for different target states is defined in a
     * mapping between a target state and a composable function.
     *
     * @param targetState defines the key to choose the content to be displayed
     * @param modifier modifier for the Layout created to contain the [content]
     * @param transitionSpec defines the EnterTransition(s) and ExitTransition(s) used to display
     * and remove the content, fading in and fading out by default
     * @param content Content to appear or disappear based on the value of [targetState]
     *
     * @link androidx.compose.animation.AnimatedContent
     * @see androidx.compose.animation.AnimatedContent
     * @see ContentTransform
     * @see AnimatedContentScope
     */
    @Suppress("IllegalExperimentalApiUsage")
    @ExperimentalAnimationApi
    @Composable
    fun AnimatedContent(
        targetState: Int,
        modifier: Modifier = Modifier,
        transitionSpec: AnimatedContentScope<Int>.() -> ContentTransform = {
            ImmersiveListDefaults.EnterTransition.with(ImmersiveListDefaults.ExitTransition)
        },
        contentAlignment: Alignment = Alignment.TopStart,
        content: @Composable AnimatedVisibilityScope.(targetState: Int) -> Unit
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState,
            modifier,
            transitionSpec,
            contentAlignment,
            content)
    }
}

@Immutable
@ExperimentalTvMaterialApi
public class ImmersiveListScope internal constructor(private val onFocused: (Int) -> Unit) {
    /**
     * Modifier to be added to each of the items of the list within ImmersiveList to inform the
     * ImmersiveList of the index of the item in focus.
     *
     * @param index index of the item within the list.
     */
    fun Modifier.focusableItem(index: Int): Modifier {
        return onFocusChanged { if (it.hasFocus || it.isFocused) { onFocused(index) } }
            .focusable()
    }
}