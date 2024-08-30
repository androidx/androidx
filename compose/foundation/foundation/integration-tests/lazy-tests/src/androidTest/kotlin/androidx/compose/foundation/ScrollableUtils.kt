/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.VelocityTrackerAddPointsFix
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.util.fastForEach
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import kotlinx.coroutines.coroutineScope
import org.hamcrest.CoreMatchers

// Very low tolerance on the difference
internal val VelocityTrackerCalculationThreshold = 1

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEvents(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    if (VelocityTrackerAddPointsFix) {
        savePointerInputEventsWithFix(tracker, pointerInputScope)
    } else {
        savePointerInputEventsLegacy(tracker, pointerInputScope)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEventsWithFix(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event: PointerInputChange? = awaitFirstDown()
                    while (event != null && !event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes
                            .firstOrNull()

                        if (currentEvent != null && !currentEvent.changedToUpIgnoreConsumed()) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(
                                currentEvent.uptimeMillis,
                                currentEvent.position
                            )
                        }

                        event = currentEvent
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEventsLegacy(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event = awaitFirstDown()
                    tracker.addPosition(event.uptimeMillis, event.position)
                    while (!event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes
                            .firstOrNull()

                        if (currentEvent != null) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(
                                currentEvent.uptimeMillis,
                                currentEvent.position
                            )
                            event = currentEvent
                        }
                    }
                }
            }
        }
    }
}

internal fun composeViewSwipeUp() {
    Espresso.onView(CoreMatchers.allOf(CoreMatchers.instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.TOP_CENTER
            )
        )
}

internal fun composeViewSwipeDown() {
    Espresso.onView(CoreMatchers.allOf(CoreMatchers.instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.BOTTOM_CENTER
            )
        )
}

internal fun composeViewSwipeLeft() {
    Espresso.onView(CoreMatchers.allOf(CoreMatchers.instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.CENTER_LEFT
            )
        )
}

internal fun composeViewSwipeRight() {
    Espresso.onView(CoreMatchers.allOf(CoreMatchers.instanceOf(AbstractComposeView::class.java)))
        .perform(
            espressoSwipe(
                GeneralLocation.CENTER,
                GeneralLocation.CENTER_RIGHT
            )
        )
}

private fun espressoSwipe(
    start: CoordinatesProvider,
    end: CoordinatesProvider
): GeneralSwipeAction {
    return GeneralSwipeAction(
        Swipe.FAST, start, end,
        Press.FINGER
    )
}
