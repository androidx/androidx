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

package androidx.tv.material3

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Navigation drawers provide ergonomic access to destinations in an app.
 * Modal navigation drawers are good for infrequent, but more focused, switching to different
 * destinations.
 *
 * It displays content associated with the closed state when the drawer is not in focus and displays
 * content associated with the open state when the drawer or its contents are focused on.
 * Modal navigation drawers are elevated above most of the app’s UI and don’t affect the screen’s
 * layout grid.
 *
 * Example:
 * @sample androidx.tv.samples.SampleModalNavigationDrawer
 *
 * @param drawerContent Content that needs to be displayed on the drawer based on whether the drawer
 * is [DrawerValue.Open] or [DrawerValue.Closed].
 * Drawer-entries can be animated when the drawer moves from Closed to Open state and vice-versa.
 * For, e.g., the entry could show only an icon in the Closed state and slide in text to form
 * (icon + text) when in the Open state.
 *
 * To limit the width of the drawer in the open or closed state, wrap the content in a box with the
 * required width.
 *
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 */
@ExperimentalTvMaterial3Api
@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable (DrawerValue) -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    scrimColor: Color = LocalColorScheme.current.scrim.copy(alpha = 0.5f),
    content: @Composable () -> Unit
) {
    val localDensity = LocalDensity.current
    val closedDrawerWidth: MutableState<Dp?> = remember { mutableStateOf(null) }
    val internalDrawerModifier =
        Modifier
            .zIndex(Float.MAX_VALUE)
            .onSizeChanged {
                if (closedDrawerWidth.value == null &&
                    drawerState.currentValue == DrawerValue.Closed
                ) {
                    with(localDensity) {
                        closedDrawerWidth.value = it.width.toDp()
                    }
                }
            }

    Box(modifier = modifier) {
        DrawerSheet(
            modifier = internalDrawerModifier.align(Alignment.CenterStart),
            drawerState = drawerState,
            sizeAnimationFinishedListener = { _, targetSize ->
                if (drawerState.currentValue == DrawerValue.Closed) {
                    with(localDensity) {
                        closedDrawerWidth.value = targetSize.width.toDp()
                    }
                }
            },
            content = drawerContent
        )

        Box(Modifier.padding(start = closedDrawerWidth.value ?: ClosedDrawerWidth.dp)) {
            content()
            if (drawerState.currentValue == DrawerValue.Open) {
                // Scrim
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(scrimColor)
                }
            }
        }
    }
}

/**
 * Navigation drawers provide ergonomic access to destinations in an app. They’re often next to
 * app content and affect the screen’s layout grid.
 * Standard navigation drawers are good for frequent switching to different destinations.
 *
 * It displays content associated with the closed state when the drawer is not in focus and displays
 * content associated with the open state when the drawer or its contents are focused on.
 * The drawer is at the same level as the app's UI an reduces the screen size available to the
 * remaining content.
 *
 * Example:
 * @sample androidx.tv.samples.SampleNavigationDrawer
 *
 * @param drawerContent Content that needs to be displayed on the drawer based on whether the drawer
 * is [DrawerValue.Open] or [DrawerValue.Closed].
 * Drawer-entries can be animated when the drawer moves from Closed to Open state and vice-versa.
 * For, e.g., the entry could show only an icon in the Closed state and slide in text to form
 * (icon + text) when in the Open state.
 *
 * To limit the width of the drawer in the open or closed state, wrap the content in a box with the
 * required width.
 *
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param content content of the rest of the UI
 */
@ExperimentalTvMaterial3Api
@Composable
fun NavigationDrawer(
    drawerContent: @Composable (DrawerValue) -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit
) {
    Row(modifier = modifier) {
        DrawerSheet(
            drawerState = drawerState,
            content = drawerContent
        )
        content()
    }
}

/**
 * States that the drawer can exist in.
 */
@ExperimentalTvMaterial3Api
enum class DrawerValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    Open
}

/**
 * State of the [NavigationDrawer] or [ModalNavigationDrawer] composable.
 *
 * @param initialValue the initial value ([DrawerValue.Closed] or [DrawerValue.Open]) of the drawer.
 */
@ExperimentalTvMaterial3Api
class DrawerState(initialValue: DrawerValue = DrawerValue.Closed) {
    var currentValue by mutableStateOf(initialValue)
        private set

    /**
     * Updates the state of the drawer.
     *
     * @param drawerValue the value the state of the drawer should be set to.
     */
    fun setValue(drawerValue: DrawerValue) {
        currentValue = drawerValue
    }

    companion object {
        /**
         * The [Saver] used by [rememberDrawerState] to record and restore [DrawerState] across
         * activity or process recreation.
         */
        val Saver =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it) }
            )
    }
}

/**
 * Create and remember a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 */
@Composable
@ExperimentalTvMaterial3Api
fun rememberDrawerState(initialValue: DrawerValue): DrawerState {
    return rememberSaveable(saver = DrawerState.Saver) {
        DrawerState(initialValue)
    }
}

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DrawerSheet(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = remember { DrawerState() },
    sizeAnimationFinishedListener: ((initialValue: IntSize, targetValue: IntSize) -> Unit)? = null,
    content: @Composable (DrawerValue) -> Unit
) {
    // indicates that the drawer has been set to its initial state and has grabbed focus if
    // necessary. Controls whether focus is used to decide the state of the drawer going forward.
    var initializationComplete: Boolean by remember { mutableStateOf(false) }
    var focusState by remember { mutableStateOf<FocusState?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open && focusState?.hasFocus == false) {
            // used to grab focus if the drawer state is set to Open on start.
            focusRequester.requestFocus()
        }
        initializationComplete = true
    }

    val internalModifier =
        Modifier
            .focusRequester(focusRequester)
            .animateContentSize(finishedListener = sizeAnimationFinishedListener)
            .fillMaxHeight()
            // adding passed-in modifier here to ensure animateContentSize is called before other
            // size based modifiers.
            .then(modifier)
            .onFocusChanged {
                focusState = it

                if (initializationComplete) {
                    drawerState.setValue(if (it.hasFocus) DrawerValue.Open else DrawerValue.Closed)
                }
            }
            .focusGroup()

    Box(modifier = internalModifier) { content.invoke(drawerState.currentValue) }
}

private const val ClosedDrawerWidth = 80
