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

package androidx.compose.ui.test.junit4

import android.app.Instrumentation
import android.app.UiAutomation
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.AXIS_HSCROLL
import android.view.MotionEvent.AXIS_VSCROLL
import android.view.MotionEvent.TOOL_TYPE_FINGER
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.runEmptyComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ComposeInFragmentTest {
    companion object {
        const val fragment1Text = "Compose in fragment 1"
        const val fragment2Text = "Compose in fragment 2"
    }

    private lateinit var instrumentation: Instrumentation
    private lateinit var uiAutomation: UiAutomation

    @Before
    fun setup() {
        // These are only used for the closingDialogFragmentWhileComposeInputIsProcessing test
        instrumentation = InstrumentationRegistry.getInstrumentation()
        uiAutomation = instrumentation.uiAutomation
    }

    @Test
    fun test() {
        runEmptyComposeUiTest {
            val fragment1 = Fragment1()
            val fragment2 = Fragment2()

            // Launch fragment 1
            val fragmentScenario = launchFragmentInContainer<Fragment1> { fragment1 }
            onNodeWithText(fragment1Text).assertExists()
            onNodeWithText(fragment2Text).assertDoesNotExist()

            // Add fragment 2
            fragmentScenario.withFragment {
                parentFragmentManager.commit { add(android.R.id.content, fragment2) }
            }
            onNodeWithText(fragment1Text).assertExists()
            onNodeWithText(fragment2Text).assertExists()

            // Remove fragment 1
            fragmentScenario.withFragment { parentFragmentManager.commit { remove(fragment1) } }
            onNodeWithText(fragment1Text).assertDoesNotExist()
            onNodeWithText(fragment2Text).assertExists()
        }
    }

    /*
     * Tests removing a fragment dialog from the screen WHILE the Compose (inside that dialog) is
     * actively handling events. The order of events:
     *  - Fragment dialog is shown and contains two boxes (both contain input handlers)
     *  - Input 1: Finger 1 presses the lower box
     *  - Input 2: Finger 2 presses the upper box (now two boxes are actively touched)
     *  - Input 3: Finger 1 releases the lower box (this triggers the dialog to dismiss)
     *  - Fragment dialog is removed and input cancellation is sent WHILE Compose is still handling
     *      Input 3 (this should NOT cause a NPE).
     *  - (Cancellation input event is delayed until input event is finished handling input 3.)
     */
    @Test
    fun closingDialogFragmentWhileComposeInputIsProcessing() {
        lateinit var fragmentView: View
        val buffer = 10f
        var dialogLocationOnScreenX = 0
        var dialogLocationOnScreenY = 0

        val latch = CountDownLatch(1)

        runComposeUiTest {
            val mainFragment = MainFragment()

            // Launch fragment containing fragment with dialog
            val mainFragmentScenario = launchFragmentInContainer<MainFragment> { mainFragment }

            mainFragmentScenario.withFragment {
                fragmentView = this.view!!

                // Display dialog fragment where we want to test the events
                mainFragment.dialogFragment.showNow(
                    mainFragment.childFragmentManager,
                    "compose_dialog_fragment",
                )

                // Find dialog fragment's screen location
                mainFragment.dialogFragment.dialog?.window?.decorView?.doOnNextLayout {
                    val dialogDecorView = mainFragment.dialogFragment.dialog?.window?.decorView

                    if (dialogDecorView != null) {
                        val location = IntArray(2)
                        dialogDecorView.getLocationOnScreen(location)
                        dialogLocationOnScreenX = location[0]
                        dialogLocationOnScreenY = location[1]

                        latch.countDown()
                    } else {
                        throw IllegalStateException("dialogDecorView is null")
                    }
                }
                true
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // This is important so the dialog fragment is finished before we start sending events.
            waitForIdle()

            // Calculate location of UI elements WITHIN the fragment dialog in global coordinates
            // Top box calculations:
            val topBoxLayoutCoordinates = mainFragment.dialogFragment.topButtonLayoutCoordinates
            val topBoxRelativeLocation = topBoxLayoutCoordinates?.positionInWindow()

            val topBoxRelativeX = topBoxRelativeLocation?.x ?: 0f
            val topBoxGlobalX = dialogLocationOnScreenX + topBoxRelativeX + buffer

            val topBoxRelativeY: Float = topBoxRelativeLocation?.y ?: 0f
            val topBoxGlobalY: Float = dialogLocationOnScreenY + topBoxRelativeY + buffer

            // Bottom box calculations:
            val bottomBoxLayoutCoordinates =
                mainFragment.dialogFragment.collapseButtonLayoutCoordinates
            val bottomBoxRelativeLocation = bottomBoxLayoutCoordinates?.positionInWindow()

            val bottomBoxRelativeX: Float = bottomBoxRelativeLocation?.x ?: 0f
            val bottomBoxGlobalX: Float = dialogLocationOnScreenX + bottomBoxRelativeX

            val bottomBoxRelativeY: Float = bottomBoxRelativeLocation?.y ?: 0f
            val bottomBoxGlobalY: Float = dialogLocationOnScreenY + bottomBoxRelativeY

            var numberOfPointers = 1
            val topBoxEventId = 1
            // Bottom box is zero because we want to start with hitting that box first.
            val bottomBoxEventId = 0

            val downTime = SystemClock.uptimeMillis()
            var eventTime = downTime

            var motionEvent =
                TestMotionEvent(
                    downTime = downTime,
                    eventTime = eventTime,
                    action = MotionEvent.ACTION_DOWN,
                    numPointers = numberOfPointers,
                    actionIndex = bottomBoxEventId,
                    pointerProperties =
                        arrayOf(
                            PointerProperties(bottomBoxEventId).also {
                                it.toolType = TOOL_TYPE_FINGER
                            }
                        ),
                    pointerCoords = arrayOf(PointerCoords(bottomBoxGlobalX, bottomBoxGlobalY)),
                    dispatchTarget = fragmentView,
                )

            uiAutomation.injectInputEvent(motionEvent, true)

            assertThat(mainFragment.dialogFragment.topBoxPressCount).isEqualTo(0)
            assertThat(mainFragment.dialogFragment.topBoxOtherCount).isEqualTo(0)
            assertThat(mainFragment.dialogFragment.bottomBoxPressCount).isEqualTo(1)
            assertThat(mainFragment.dialogFragment.bottomBoxReleaseCount).isEqualTo(0)
            assertThat(mainFragment.dialogFragment.bottomBoxOtherCount).isEqualTo(0)

            numberOfPointers++
            eventTime += 10

            motionEvent =
                TestMotionEvent(
                    downTime = downTime,
                    eventTime = eventTime,
                    action = MotionEvent.ACTION_POINTER_DOWN,
                    numPointers = numberOfPointers,
                    actionIndex = topBoxEventId,
                    pointerProperties =
                        arrayOf(
                            PointerProperties(bottomBoxEventId).also {
                                it.toolType = TOOL_TYPE_FINGER
                            },
                            PointerProperties(topBoxEventId).also { it.toolType = TOOL_TYPE_FINGER },
                        ),
                    pointerCoords =
                        arrayOf(
                            PointerCoords(bottomBoxGlobalX, bottomBoxGlobalY),
                            PointerCoords(topBoxGlobalX, topBoxGlobalY),
                        ),
                    dispatchTarget = fragmentView,
                )

            uiAutomation.injectInputEvent(motionEvent, true)

            assertThat(mainFragment.dialogFragment.topBoxPressCount).isEqualTo(1)
            assertThat(mainFragment.dialogFragment.topBoxOtherCount).isEqualTo(0)
            assertThat(mainFragment.dialogFragment.bottomBoxPressCount).isEqualTo(2)
            assertThat(mainFragment.dialogFragment.bottomBoxReleaseCount).isEqualTo(0)
            assertThat(mainFragment.dialogFragment.bottomBoxOtherCount).isEqualTo(0)

            eventTime += 10
            motionEvent =
                TestMotionEvent(
                    downTime = downTime,
                    eventTime = eventTime,
                    action = MotionEvent.ACTION_POINTER_UP,
                    numPointers = numberOfPointers,
                    actionIndex = bottomBoxEventId,
                    pointerProperties =
                        arrayOf(
                            PointerProperties(bottomBoxEventId).also {
                                it.toolType = TOOL_TYPE_FINGER
                            },
                            PointerProperties(topBoxEventId).also { it.toolType = TOOL_TYPE_FINGER },
                        ),
                    pointerCoords =
                        arrayOf(
                            PointerCoords(bottomBoxGlobalX, bottomBoxGlobalY),
                            PointerCoords(topBoxGlobalX, topBoxGlobalY),
                        ),
                    dispatchTarget = fragmentView,
                )

            uiAutomation.injectInputEvent(motionEvent, true)

            assertThat(mainFragment.dialogFragment.topBoxPressCount).isEqualTo(1)
            assertThat(mainFragment.dialogFragment.topBoxOtherCount).isEqualTo(0)
            assertThat(mainFragment.dialogFragment.bottomBoxPressCount).isEqualTo(2)
            assertThat(mainFragment.dialogFragment.bottomBoxReleaseCount).isEqualTo(1)
            assertThat(mainFragment.dialogFragment.bottomBoxOtherCount).isEqualTo(0)
        }
    }

    class MainFragment : Fragment() {
        // Contains main fragment with a dialog we are testing
        val dialogFragment: ComposeInDialogFragment = ComposeInDialogFragment()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View = ComposeView(requireContext()).apply { setContent { ComposeScreen() } }

        @Composable
        fun ComposeScreen() {
            Box(modifier = Modifier.background(Color.Green).fillMaxSize())
        }
    }

    class ComposeInDialogFragment : DialogFragment() {
        var topBoxPressCount = 0
        var topBoxOtherCount = 0

        var bottomBoxPressCount = 0
        var bottomBoxReleaseCount = 0
        var bottomBoxOtherCount = 0

        var topButtonLayoutCoordinates: LayoutCoordinates? = null
        var collapseButtonLayoutCoordinates: LayoutCoordinates? = null

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ) = ComposeView(inflater.context).apply { setContent { SimpleScreen() } }

        @Composable
        private fun SimpleScreen() {
            Column(modifier = Modifier.padding(10.dp).fillMaxSize().background(Color.Red)) {
                Box(
                    modifier =
                        Modifier.padding(10.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .background(Color.White)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()

                                        if (event.type == PointerEventType.Press) {
                                            topBoxPressCount++
                                        } else {
                                            topBoxOtherCount++
                                        }
                                    }
                                }
                            }
                            .onGloballyPositioned { topButtonLayoutCoordinates = it }
                )
                Box(
                    modifier =
                        Modifier.padding(10.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .background(Color.Gray)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Press) {
                                            bottomBoxPressCount++
                                        } else if (event.type == PointerEventType.Release) {
                                            bottomBoxReleaseCount++
                                            this@ComposeInDialogFragment.dismiss()
                                        } else {
                                            bottomBoxOtherCount++
                                        }
                                    }
                                }
                            }
                            .onGloballyPositioned { collapseButtonLayoutCoordinates = it }
                )
            }
        }
    }

    class Fragment1 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View? {
            return container?.let {
                ComposeView(container.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setContent {
                        Text(fragment1Text, Modifier.background(Color.White).padding(10.dp))
                    }
                }
            }
        }
    }

    class Fragment2 : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View? {
            return container?.let {
                ComposeView(container.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setContent {
                        Text(fragment2Text, Modifier.background(Color.White).padding(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * Creates a simple [MotionEvent].
 *
 * @param dispatchTarget The [View] that the [MotionEvent] is going to be dispatched to. This
 *   guarantees that the MotionEvent is created correctly for both Compose (which relies on raw
 *   coordinates being correct) and Android (which requires that local coordinates are correct).
 */
internal fun TestMotionEvent(
    downTime: Long,
    eventTime: Long,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    dispatchTarget: View,
): MotionEvent {

    val locationOnScreen = IntArray(2) { 0 }
    dispatchTarget.getLocationOnScreen(locationOnScreen)

    pointerCoords.forEach {
        it.x += locationOnScreen[0]
        it.y += locationOnScreen[1]
    }

    val motionEvent =
        MotionEvent.obtain(
                downTime,
                eventTime,
                action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                numPointers,
                pointerProperties,
                pointerCoords,
                0,
                0,
                0f,
                0f,
                0,
                0,
                0x2,
                0,
            )
            .apply {
                offsetLocation(-locationOnScreen[0].toFloat(), -locationOnScreen[1].toFloat())
            }

    pointerCoords.forEach {
        it.x -= locationOnScreen[0]
        it.y -= locationOnScreen[1]
    }

    return motionEvent
}

@Suppress("RemoveRedundantQualifierName")
internal fun PointerProperties(id: Int) = MotionEvent.PointerProperties().apply { this.id = id }

@Suppress("RemoveRedundantQualifierName")
internal fun PointerCoords(x: Float, y: Float, scrollX: Float = 0f, scrollY: Float = 0f) =
    MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
        setAxisValue(AXIS_HSCROLL, scrollX)
        setAxisValue(AXIS_VSCROLL, scrollY)
    }
