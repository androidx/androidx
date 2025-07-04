/*
 * Copyright (C) 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalInkCustomBrushApi::class)

package androidx.ink.authoring.compose

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.strokes.Stroke
import java.util.concurrent.TimeUnit

/** An Activity to support [InProgressStrokesTest] by rendering a simple stroke. */
class InProgressStrokesTestActivity : ComponentActivity() {

    private var _finishedStrokeCohorts = mutableListOf<List<Stroke>>()
    internal val finishedStrokeCohorts: List<List<Stroke>> = _finishedStrokeCohorts

    internal lateinit var sync: (Long, TimeUnit) -> Unit
        private set

    internal lateinit var rootView: View
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        rootView = window.decorView
    }

    fun init(
        consumeMoveTouchEventOfPointerNumber: Int? = null,
        nextBrush: () -> Brush? = { null },
        nextPointerEventToWorldTransform: () -> Matrix = { Matrix() },
        nextStrokeToWorldTransform: () -> Matrix = { Matrix() },
        maskPath: Path? = null,
        textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null },
    ) {
        setContent {
            Root(
                consumeMoveTouchEventOfPointerNumber = consumeMoveTouchEventOfPointerNumber,
                nextBrush = nextBrush,
                nextPointerEventToWorldTransform = nextPointerEventToWorldTransform,
                maskPath = maskPath,
                textureBitmapStore = textureBitmapStore,
                onSyncAvailable = { sync = it },
                nextStrokeToWorldTransform = nextStrokeToWorldTransform,
            ) { finishedStrokes ->
                _finishedStrokeCohorts.add(finishedStrokes)
            }
        }
    }
}

/**
 * A parent Composable of our Composable under test, to simulate consuming particular pointers to
 * exercise the stroke cancellation logic of the InProgressStrokes composable.
 *
 * @param consumeMoveTouchEventOfPointerNumber Which pointer - indexed from 0 in the order of down
 *   events - should have its move events consumed. If null, no events will be intercepted and
 *   consumed.
 */
@Composable
private fun Root(
    consumeMoveTouchEventOfPointerNumber: Int?,
    nextBrush: () -> Brush?,
    nextPointerEventToWorldTransform: () -> Matrix,
    nextStrokeToWorldTransform: () -> Matrix,
    maskPath: Path?,
    textureBitmapStore: TextureBitmapStore,
    onSyncAvailable: ((Long, TimeUnit) -> Unit) -> Unit,
    onStrokesFinished: (List<Stroke>) -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    var pointerCount = 0
                    var pointerIdToConsumeMoveEventsOf: PointerId? = null
                    do {
                        // The default PointerEventPass.Main would deliver events to this parent
                        // after the
                        // InProgressStrokes composable receives it. Listen to the initial pass here
                        // to have a
                        // chance to consume the events before they reach InProgressStrokes.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.fastForEach { change ->
                            if (change.changedToDown()) {
                                if (pointerCount == consumeMoveTouchEventOfPointerNumber) {
                                    pointerIdToConsumeMoveEventsOf = change.id
                                }
                                pointerCount++
                            } else if (
                                change.positionChanged() &&
                                    change.id == pointerIdToConsumeMoveEventsOf
                            ) {
                                change.consume()
                            }
                        }
                    } while (event.changes.fastAny { it.pressed })
                }
            }
    ) {
        InProgressStrokes(
            nextBrush = nextBrush,
            nextPointerEventToWorldTransform = nextPointerEventToWorldTransform,
            nextStrokeToWorldTransform = nextStrokeToWorldTransform,
            maskPath = maskPath,
            textureBitmapStore = textureBitmapStore,
            onSyncAvailable = onSyncAvailable,
            onStrokesFinished = onStrokesFinished,
        )
    }
}
