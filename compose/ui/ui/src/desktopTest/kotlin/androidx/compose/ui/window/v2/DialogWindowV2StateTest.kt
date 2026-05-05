/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.window.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.isLinux
import androidx.compose.ui.isMacOs
import androidx.compose.ui.toDpSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.plus
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.topLeft
import androidx.compose.ui.unit.width
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.runApplicationTest
import androidx.compose.ui.window.toDpInsets
import androidx.compose.ui.window.toDpOffset
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import org.junit.Assume.assumeTrue

class DialogWindowV2StateTest {
    @Test
    fun `manually close dialog`() = runApplicationTest {
        lateinit var dialog: ComposeDialog
        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                DialogWindow(
                    onCloseRequest = { isOpen = false },
                    title = "manually close dialog"
                ) {
                    dialog = this.window
                }
            }
        }

        awaitIdle()
        assertThat(dialog.isShowing).isTrue()

        dialog.dispatchEvent(WindowEvent(dialog, WindowEvent.WINDOW_CLOSING))
        awaitIdle()
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `programmatically close dialog`() = runApplicationTest {
        lateinit var dialog: ComposeDialog
        var isOpen by mutableStateOf(true)

        launchTestApplication {
            if (isOpen) {
                DialogWindow(
                    onCloseRequest = { isOpen = false },
                    title = "programmatically close dialog"
                ) {
                    dialog = this.window
                }
            }
        }

        awaitIdle()
        assertThat(dialog.isShowing).isTrue()

        isOpen = false
        awaitIdle()
        assertThat(dialog.isShowing).isFalse()
    }

    @Test
    fun `programmatically open and close owned dialog`() = runApplicationTest(useDelay = true) {
        var parentWindow: ComposeWindow? = null
        var childDialog: ComposeDialog? = null
        var isParentOpen by mutableStateOf(true)
        var isChildOpen by mutableStateOf(false)

        launchTestApplication {
            if (isParentOpen) {
                Window(
                    onCloseRequest = {},
                    title = "(parent) programmatically open and close owned dialog"
                ) {
                    parentWindow = this.window

                    if (isChildOpen) {
                        DialogWindow(
                            onCloseRequest = {},
                            title = "(child) programmatically open and close owned dialog"
                        ) {
                            childDialog = this.window
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()

        isChildOpen = true
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childDialog?.isShowing).isTrue()

        isChildOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isTrue()
        assertThat(childDialog?.isShowing).isFalse()

        isParentOpen = false
        awaitIdle()
        assertThat(parentWindow?.isShowing).isFalse()
    }

    @Test
    fun `set size and position before show`() = runApplicationTest(useDelay = isLinux) {
        val size = Dimension(200, 200)
        val position = Point(242, 242)
        val state = DialogStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )

        lateinit var dialog: ComposeDialog

        launchTestApplication {
            DialogWindow(
                onCloseRequest = {},
                state = state,
                title = "set size and position before show"
            ) {
                dialog = this.window
            }
        }

        awaitIdle()
        assertSizesApproximatelyEqual(size, dialog.size)
        assertCoordinatesApproximatelyEqual(position, dialog.location)
    }

    @Test
    fun `change position after show`() = runApplicationTest(useDelay = isLinux) {
        val size = Dimension(200, 200)
        val position = Point(200, 200)

        val state = DialogStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )
        lateinit var dialog: ComposeDialog

        launchTestApplication {
            DialogWindow(
                onCloseRequest = {},
                state = state,
                title = "change position after show"
            ) {
                dialog = this.window
            }
        }

        awaitIdle()

        val newPosition = Point(242, 242)
        state.requestPosition(newPosition.toDpOffset())
        awaitIdle()
        assertCoordinatesApproximatelyEqual(newPosition, dialog.location)
    }

    @Test
    fun `change size after show`() = runApplicationTest(useDelay = isLinux) {
        val size = Dimension(200, 200)
        val position = Point(200, 200)

        val state = DialogStateWithBounds(
            initialSize = size.toDpSize(),
            initialPosition = position.toDpOffset()
        )
        lateinit var dialog: ComposeDialog

        launchTestApplication {
            DialogWindow(
                onCloseRequest = {},
                state = state,
                title = "change size after show"
            ) {
                dialog = this.window
            }
        }

        awaitIdle()

        val newSize = Dimension(250, 200)
        state.requestSize(newSize.toDpSize())
        awaitIdle()
        assertSizesApproximatelyEqual(newSize, dialog.size)
    }

    fun Rectangle.center() = Point(x + width / 2, y + height / 2)
    fun Window.center() = bounds.center()
    fun Window.screenCenter() = graphicsConfiguration.bounds.center()
    infix fun Point.maxDistance(other: Point) = max(abs(x - other.x), abs(y - other.y))

    @Test
    fun `center dialog on screen`() = runApplicationTest {
        val state = DialogState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(200.dp, 200.dp),
                positionProvider = WindowPositionProvider.AlignedToScreen(Alignment.Center)
            )
        )
        lateinit var dialog: ComposeDialog

        launchTestApplication {
            DialogWindow(
                onCloseRequest = {},
                state = state,
                title = "center dialog on screen"
            ) {
                dialog = this.window
            }
        }

        awaitIdle()
        assertThat(dialog.center() maxDistance dialog.screenCenter() < 250)
    }

    @Test
    fun `center dialog in parent`() = runApplicationTest {
        val windowState = WindowState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(400.dp, 400.dp),
                positionProvider = WindowPositionProvider.AlignedToScreen(Alignment.Center)
            )
        )
        lateinit var window: ComposeWindow

        val dialogState = DialogState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Fixed(200.dp, 200.dp),
                positionProvider = WindowPositionProvider.AlignedToParentWindow(Alignment.Center)
            )
        )
        lateinit var dialog: ComposeDialog

        var showDialog by mutableStateOf(false)

        launchTestApplication {
            Window(onCloseRequest = {}, windowState, title = "(parent) center dialog in parent") {
                window = this.window
                if (showDialog) {
                    DialogWindow(
                        onCloseRequest = { },
                        state = dialogState,
                        title = "(child) center dialog in parent"
                    ) {
                        dialog = this.window
                    }
                }
            }
        }

        awaitIdle()
        showDialog = true
        awaitIdle()

        assertThat(dialog.center() maxDistance window.center() <= 5)
    }

    @Test
    fun `remember position after reattach`() = runApplicationTest(useDelay = isLinux) {
        val state = DialogStateWithBounds(initialSize = DpSize(200.dp, 200.dp))
        var dialog1: ComposeDialog? = null
        var dialog2: ComposeDialog? = null
        var isDialog1 by mutableStateOf(true)

        launchTestApplication {
            if (isDialog1) {
                DialogWindow(
                    onCloseRequest = {},
                    state = state,
                    title = "remember position after reattach 1"
                ) {
                    dialog1 = this.window
                }
            } else {
                DialogWindow(
                    onCloseRequest = {},
                    state = state,
                    title = "remember position after reattach 2"
                ) {
                    dialog2 = this.window
                }
            }
        }

        awaitIdle()

        val position = Point(242, 242)
        state.requestPosition(position.toDpOffset())
        awaitIdle()
        assertThat(dialog1?.location).isEqualTo(position)

        isDialog1 = false
        awaitIdle()
        assertThat(dialog2?.location).isEqualTo(position)
    }

    @Test
    fun `state bounds should be initialized after show`() = runApplicationTest(
        useDelay = isLinux
    ) {
        val state = DialogState()
        launchTestApplication {
            DialogWindow(
                onCloseRequest = {},
                state = state,
                title = "state bounds should be initialized after show"
            ) { }
        }

        assertThat(state.isInitialized).isFalse()

        awaitIdle()
        assertThat(state.isInitialized).isTrue()
        state.bounds  // Just make sure it doesn't crash
    }

    @Test
    fun `set dialog min intrinsic height`() = runApplicationTest(useDelay = isLinux) {
        assumeTrue(!isLinux)  // Flaky on our CI

        lateinit var dialog: ComposeDialog
        val state = DialogState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.MinIntrinsicHeight(width = 300.dp)
            )
        )

        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set dialog min intrinsic height"
            ) {
                dialog = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(dialog.contentSize.width).isEqualTo(300)
        assertThat(dialog.contentSize.height).isEqualTo(200)
        assertThat(state.size).isEqualTo(DpSize(dialog.size.width.dp, dialog.size.height.dp))
    }

    @Test
    fun `set dialog min intrinsic width`() = runApplicationTest {
        assumeTrue(!isLinux)  // Flaky on our CI

        lateinit var dialog: ComposeDialog
        val state = DialogState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.MinIntrinsicWidth(height = 300.dp)
            )
        )
        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set dialog min intrinsic width"
            ) {
                dialog = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(dialog.contentSize.height).isEqualTo(300)
        assertThat(dialog.contentSize.width).isEqualTo(400)
        assertThat(state.size).isEqualTo(DpSize(dialog.size.width.dp, dialog.size.height.dp))
    }

    @Test
    fun `set unconstrained dialog size by its content`() = runApplicationTest {
        assumeTrue(!isLinux) // Flaky on our CI

        lateinit var dialog: ComposeDialog
        val state = DialogState(
            initialBoundsProvider = WindowBoundsProvider(
                sizeProvider = WindowSizeProvider.Unconstrained
            )
        )

        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set unconstrained dialog size by its content"
            ) {
                dialog = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()
        assertThat(dialog.contentSize).isEqualTo(Dimension(400, 200))
        assertThat(state.size).isEqualTo(DpSize(dialog.size.width.dp, dialog.size.height.dp))
    }

    @Test
    fun `set dialog size by its content when dialog is visible`() = runApplicationTest(
        useDelay = isLinux || isMacOs
    ) {
        lateinit var dialog: ComposeDialog
        val state = DialogStateWithBounds(initialSize = DpSize(100.dp, 100.dp))

        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                state = state,
                title = "set dialog size by its content when dialog is visible"
            ) {
                dialog = this.window

                Box(
                    Modifier
                        .width(400.dp)
                        .height(200.dp)
                )
            }
        }

        awaitIdle()

        state.requestSize(WindowSizeProvider.Unconstrained)
        awaitIdle()
        assertThat(dialog.contentSize).isEqualTo(Dimension(400, 200))
        assertThat(state.size).isEqualTo(DpSize(dialog.size.width.dp, dialog.size.height.dp))
    }

    @Test
    fun `change visibility`() = runApplicationTest {
        lateinit var dialog: ComposeDialog

        var visible by mutableStateOf(false)

        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                visible = visible,
                title = "change visible"
            ) {
                dialog = this.window
            }
        }

        awaitIdle()
        assertThat(dialog.isVisible).isEqualTo(false)

        visible = true
        awaitIdle()
        assertThat(dialog.isVisible).isEqualTo(true)
    }

    @Test
    fun `invisible dialog should be active`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                visible = false,
                title = "invisible dialog should be active"
            ) {
                LaunchedEffect(Unit) {
                    sendChannel.consumeEach {
                        receivedNumbers.add(it)
                    }
                }
            }
        }

        sendChannel.send(1)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1))

        sendChannel.send(2)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `show invisible undecorated dialog`() = runApplicationTest {
        val receivedNumbers = mutableListOf<Int>()

        val sendChannel = Channel<Int>(Channel.UNLIMITED)

        launchTestApplication {
            DialogWindow(
                onCloseRequest = ::exitApplication,
                visible = false,
                decoration = WindowDecoration.Undecorated(),
                title = "show invisible undecorated dialog"
            ) {
                LaunchedEffect(Unit) {
                    sendChannel.consumeEach {
                        receivedNumbers.add(it)
                    }
                }
            }
        }

        sendChannel.send(1)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1))

        sendChannel.send(2)
        awaitIdle()
        assertThat(receivedNumbers).isEqualTo(listOf(1, 2))
    }

    @Test
    fun dialogStateIsPreservedWhenRemovingAndAddingComposable() = runApplicationTest {
        var showDialog by mutableStateOf(true)
        lateinit var dialogState: DialogState
        var dialogVisible = false
        launchTestApplication {
            val state = rememberDialogStateWithBounds()
            dialogState = state
            if (showDialog) {
                DialogWindow(
                    state = state,
                    onCloseRequest = { },
                    title = "dialogStateIsPreservedWhenRemovingAndAddingComposable"
                ) {
                    Box(Modifier.size(32.dp))
                    DisposableEffect(Unit) {
                        dialogVisible = true
                        onDispose {
                            dialogVisible = false
                        }
                    }
                }
            }

            // Prevent app from dying when nothing is shown
            LaunchedEffect(Unit) {
                delay(Duration.INFINITE)
            }
        }
        awaitIdle()

        dialogState.requestBounds {
            val screenBounds = windowMetrics.screen.availableBounds
            val size = DpSize(400.dp, 400.dp)
            DpRect(
                origin = DpOffset(
                    (screenBounds.width - size.width) / 2,
                    (screenBounds.height - size.height) / 2
                ),
                size = size
            )
        }
        awaitIdle()
        val windowBounds = dialogState.bounds

        showDialog = false
        awaitIdle()
        assertFalse(dialogVisible)

        showDialog = true
        awaitIdle()
        assertTrue(dialogState.isInitialized)
        assertEquals(windowBounds, dialogState.bounds)
    }

    @Test
    fun dialogStateIsPreservedWhenSavingAndRestoring() = runApplicationTest {
        var showDialog by mutableStateOf(true)
        var dialogState: DialogState? = null
        launchTestApplication {
            val stateHolder = rememberSaveableStateHolder()
            stateHolder.SaveableStateProvider(showDialog) {
                if (showDialog) {
                    val state = rememberDialogStateWithBounds()
                    DisposableEffect(state) {
                        dialogState = state
                        onDispose {
                            dialogState = null
                        }
                    }
                    DialogWindow(
                        state = state,
                        onCloseRequest = { },
                        title = "dialogStateIsPreservedWhenSavingAndRestoring"
                    ) {
                        Box(Modifier.size(32.dp))
                    }
                }
            }

            // Prevent app from dying when nothing is shown
            LaunchedEffect(Unit) {
                delay(Duration.INFINITE)
            }
        }
        awaitIdle()

        dialogState!!.requestBounds {
            val screenBounds = windowMetrics.screen.availableBounds
            val size = DpSize(400.dp, 400.dp)
            DpRect(
                origin = DpOffset(
                    (screenBounds.width - size.width) / 2,
                    (screenBounds.height - size.height) / 2
                ),
                size = size
            )
        }
        awaitIdle()
        val windowBounds = dialogState!!.bounds

        showDialog = false
        awaitIdle()
        assertNull(dialogState)

        showDialog = true
        awaitIdle()
        assertTrue(dialogState!!.isInitialized)
        assertEquals(windowBounds, dialogState!!.bounds)
    }

    @Test
    fun dialogIsShownCorrectlyIfStateSavedBeforeWindowIsShown() = runApplicationTest {
        var createDialogState by mutableStateOf(true)
        var showDialog by mutableStateOf(false)
        var dialogState: DialogState? = null
        launchTestApplication {
            val stateHolder = rememberSaveableStateHolder()
            stateHolder.SaveableStateProvider(createDialogState) {
                if (createDialogState) {
                    val state = rememberDialogStateWithBounds(
                        initialSize = DpSize(300.dp, 300.dp)
                    )
                    DisposableEffect(state) {
                        dialogState = state
                        onDispose {
                            dialogState = null
                        }
                    }
                    if (showDialog) {
                        DialogWindow(
                            state = state,
                            onCloseRequest = { },
                            title = "dialogIsShownCorrectlyIfStateSavedBeforeWindowIsShown"
                        ) {
                            Box(Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Prevent app from dying when nothing is shown
            LaunchedEffect(Unit) {
                delay(Duration.INFINITE)
            }
        }

        awaitIdle()
        assertNotNull(dialogState)
        assertFalse(dialogState!!.isInitialized)
        dialogState!!.requestBounds {
            val screenBounds = windowMetrics.screen.availableBounds
            val size = DpSize(400.dp, 400.dp)
            DpRect(
                origin = DpOffset(
                    (screenBounds.width - size.width) / 2,
                    (screenBounds.height - size.height) / 2
                ),
                size = size
            )
        }

        createDialogState = false
        awaitIdle()
        assertNull(dialogState)

        createDialogState = true
        showDialog = true
        awaitIdle()

        awaitIdle()
        assertNotNull(dialogState)
        assertTrue(dialogState!!.isInitialized)
        // Size should be as the one requested in rememberDialogStateWithBounds, not the one in
        // dialogState!!.requestBounds above.
        assertEquals(DpSize(300.dp, 300.dp), dialogState!!.bounds.size)
    }

    private fun runDialogSizeTest(
        testName: String,
        sizeProvider: WindowSizeProvider,
        content: @Composable () -> Unit,
        expectedWindowSizeSansInsets: DpSize,
    ) = runApplicationTest {
        val dialogState = DialogState(
            initialBoundsProvider = WindowBoundsProvider(sizeProvider)
        )
        lateinit var dialog: ComposeDialog
        launchTestApplication {
            DialogWindow(
                state = dialogState,
                onCloseRequest = {},
                title = testName
            ) {
                dialog = this.window
                content()
            }
        }
        awaitIdle()
        assertEquals(
            expectedWindowSizeSansInsets + dialog.insets.toDpInsets(),
            dialogState.bounds.size
        )
    }

    @Test
    fun dialogMinIntrinsicWidth() = runDialogSizeTest(
        testName = "windowMinIntrinsicWidth",
        sizeProvider = WindowSizeProvider.MinIntrinsicWidth(height = 500.dp),
        content = {
            BoxWithIntrinsicSize(
                minWidth = { 400.dp.roundToPx() }
            )
        },
        expectedWindowSizeSansInsets = DpSize(400.dp, 500.dp)
    )

    @Test
    fun windowMaxIntrinsicWidth() = runDialogSizeTest(
        testName = "windowMaxIntrinsicWidth",
        sizeProvider = WindowSizeProvider.MaxIntrinsicWidth(height = 500.dp),
        content = {
            BoxWithIntrinsicSize(
                maxWidth = { 400.dp.roundToPx() }
            )
        },
        expectedWindowSizeSansInsets = DpSize(400.dp, 500.dp)
    )

    @Test
    fun windowMinIntrinsicHeight() = runDialogSizeTest(
        testName = "windowMinIntrinsicHeight",
        sizeProvider = WindowSizeProvider.MinIntrinsicHeight(width = 500.dp),
        content = {
            BoxWithIntrinsicSize(
                minHeight = { 400.dp.roundToPx() }
            )
        },
        expectedWindowSizeSansInsets = DpSize(500.dp, 400.dp)
    )

    @Test
    fun windowMaxIntrinsicHeight() = runDialogSizeTest(
        testName = "windowMaxIntrinsicHeight",
        sizeProvider = WindowSizeProvider.MaxIntrinsicHeight(width = 500.dp),
        content = {
            BoxWithIntrinsicSize(
                maxHeight = { 400.dp.roundToPx() }
            )
        },
        expectedWindowSizeSansInsets = DpSize(500.dp, 400.dp)
    )

    @Test
    fun windowMinWidthWithMatchingMinHeight() = runDialogSizeTest(
        testName = "windowMinWidthWithMatchingMinHeight",
        sizeProvider = WindowSizeProvider.IntrinsicWidthWithMatchingIntrinsicHeight(
            intrinsicWidth = WindowIntrinsicSize.Min,
            intrinsicHeight = WindowIntrinsicSize.Min,
        ),
        content = {
            BoxWithIntrinsicSize(
                minWidth = { 400.dp.roundToPx() },
                minHeight = { it }  // Return width to make it a square
            )
        },
        expectedWindowSizeSansInsets = DpSize(400.dp, 400.dp)
    )

    @Test
    fun windowMaxHeightWithMatchingMaxWidth() = runDialogSizeTest(
        testName = "windowMaxHeightWithMatchingMaxWidth",
        sizeProvider = WindowSizeProvider.IntrinsicHeightWithMatchingIntrinsicWidth(
            intrinsicWidth = WindowIntrinsicSize.Max,
            intrinsicHeight = WindowIntrinsicSize.Max,
        ),
        content = {
            BoxWithIntrinsicSize(
                maxHeight = { 400.dp.roundToPx() },
                maxWidth = { it }  // Return height to make it a square
            )
        },
        expectedWindowSizeSansInsets = DpSize(400.dp, 400.dp)
    )

    @Test
    fun `requested size is rounded up`() = runDialogSizeTest(
        testName = "requested size is rounded up",
        sizeProvider = WindowSizeProvider.IntrinsicWidthWithMatchingIntrinsicHeight(
            intrinsicWidth = WindowIntrinsicSize.Min,
            intrinsicHeight = WindowIntrinsicSize.Min,
        ),
        content = {
            BoxWithIntrinsicSize(
                minWidth = { (density * 100 + 1).toInt() },
                minHeight = { it }
            )
        },
        expectedWindowSizeSansInsets = DpSize(101.dp, 101.dp)
    )

    private fun runBoundsOverwriteTest(
        name: String,
        dialogState: DialogState,
        expectedPosition: DpOffset,
        expectedSize: DpSize
    ) = runApplicationTest {
        launchTestApplication {
            DialogWindow(
                state = dialogState,
                onCloseRequest = {},
                title = name
            ) {
                LaunchedEffect(Unit) {
                    window.addComponentListener(object: ComponentAdapter() {
                        // Verify that the bounds are set correctly immediately, not just at some
                        // point after the window is shown.
                        override fun componentShown(e: ComponentEvent) {
                            assertEquals(expectedSize, window.size.toDpSize())
                            assertEquals(expectedPosition, window.location.toDpOffset())
                        }
                    })
                }
            }
        }
        awaitIdle()

        assertEquals(expectedSize, dialogState.bounds.size)
        assertEquals(expectedPosition, dialogState.bounds.topLeft)
    }

    @Test
    fun `requesting size before initialization does not overwrite position`() {
        val position = DpOffset(300.dp, 300.dp)
        val size = DpSize(400.dp, 400.dp)
        val dialogState = DialogStateWithBounds(
            initialPosition = position,
        )
        dialogState.requestSize(size)

        runBoundsOverwriteTest(
            name = "requesting size before initialization does not overwrite position",
            dialogState = dialogState,
            expectedSize = size,
            expectedPosition = position,
        )
    }

    @Test
    fun `requesting position before initialization does not overwrite size`() {
        val position = DpOffset(300.dp, 300.dp)
        val size = DpSize(400.dp, 400.dp)
        val dialogState = DialogStateWithBounds(
            initialSize = size,
        )
        dialogState.requestPosition(position)

        runBoundsOverwriteTest(
            name = "requesting position before initialization does not overwrite size",
            dialogState = dialogState,
            expectedSize = size,
            expectedPosition = position,
        )
    }
}