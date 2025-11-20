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

package androidx.ink.authoring.compose

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.ink.authoring.ExperimentalCustomShapeWorkflowApi
import androidx.ink.authoring.InkShapeWorkflow
import androidx.ink.brush.Brush
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import java.util.concurrent.TimeUnit

/**
 * A [Composable] that displays in-progress ink strokes as it receives user pointer input.
 *
 * Multiple simultaneous strokes are supported. This implementation handles all best practices for
 * the fastest, lowest latency rendering of real-time user input. For an alternative compatible with
 * Android Views, see [androidx.ink.authoring.InProgressStrokesView].
 *
 * For performance reasons, it is recommended to only have one [InProgressStrokes] [Composable] on
 * screen at a time, and to avoid unnecessarily resizing it or changing whether it is present on
 * screen.
 *
 * This will process pointers (and display the resulting strokes) that are not already consumed by
 * an earlier touch listener - when
 * [androidx.compose.ui.input.pointer.PointerInputChange.isConsumed] is `false`. If a pointer's down
 * event arrives to this composable already consumed, it will not be drawn, even if later events for
 * that pointer arrive unconsumed. If a pointer's initial events are not consumed by arrival, then
 * it will be drawn, but if its later events have been consumed by arrival then the stroke for that
 * pointer will be canceled and removed from this composable's UI. If all of a pointer's events from
 * down through moves through up arrive to this composable unconsumed, then the entire stroke will
 * be drawn and finished, and the finished stroke will be made available by a call to
 * [onStrokesFinished]. All strokes that were in progress simultaneously will be delivered in the
 * same callback, running on the UI thread. To avoid visual flicker when transitioning rendering
 * from this composable to another composable, during this callback the finished strokes must be
 * added to some mutable state that triggers a recomposition that will render those strokes,
 * typically using [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer].
 *
 * @param defaultBrush [Brush] specification for the stroke being started. If this is `null`,
 *   nothing will be drawn, but the internal rendering resources will be preserved to avoid paying
 *   the cost for reinitialization when drawing is needed again. If different [Brush] values are
 *   needed for different simultaneous strokes, use [nextBrush] to return a different [Brush] each
 *   time. Note that the overall scaling factor of [pointerEventToWorldTransform] and
 *   [strokeToWorldTransform] combined should be related to the value of [Brush.epsilon] - in
 *   general, the larger the combined `pointerEventToStrokeTransform` scaling factor is, the smaller
 *   on screen the stroke units are, so [Brush.epsilon] should be a larger quantity of stroke units
 *   to maintain a similar screen size.
 * @param nextBrush A way to override [defaultBrush], which will be called at the start of each
 *   pointer.
 * @param pointerEventToWorldTransform The matrix that transforms
 *   [androidx.compose.ui.input.pointer.PointerEvent] coordinates into the caller's "world"
 *   coordinates, which typically is defined by how the caller's document is panned/zoomed/rotated.
 *   This defaults to the identity matrix, in which case the world coordinate space is the same as
 *   the input event coordinates, but the caller should pass in their own value reflecting a
 *   coordinate system that is independent of the device's pixel density (e.g. scaled by 1 /
 *   [android.util.DisplayMetrics.density]) and any pan/zoom/rotate gestures that have been applied
 *   to the "camera" which portrays the "world" on the device screen. This matrix must be
 *   invertible.
 * @param strokeToWorldTransform Allows an object-specific (stroke-specific) coordinate space to be
 *   defined in relation to the caller's "world" coordinate space. This defaults to the identity
 *   matrix, which is typical for many use cases at the time of stroke construction. In typical use
 *   cases, stroke coordinates and world coordinates may start to differ from one another only after
 *   stroke authoring as a finished stroke is manipulated within the world, e.g. it may be moved,
 *   scaled, or rotated relative to other content within an app's document. This matrix must be
 *   invertible.
 * @param maskPath An area of this composable where no ink will be visible. A value of `null`
 *   indicates that strokes will be visible anywhere they are drawn. This is useful for UI elements
 *   that float on top of (in Z order) the drawing surface - without this, a user would be able to
 *   draw in-progress ("wet") strokes on top of those UI elements, but then when the stroke is
 *   finished, it will appear as a dry stroke underneath of the UI element. If this mask is set to
 *   the shape and position of the floating UI element, then the ink will never be rendered in that
 *   area, making it appear as if it's being drawn underneath the UI element. This technique is most
 *   convincing when the UI element is opaque. Often there are parts of the UI element that are
 *   translucent, such as drop shadows, or anti-aliasing along the edges. The result will look a
 *   little different between wet and dry strokes for those cases, but it can be a worthwhile
 *   tradeoff compared to the alternative of drawing wet strokes on top of that UI element. Note
 *   that this parameter does not affect the contents of the strokes at all, nor how they appear
 *   when drawn in a separate composable after [onStrokesFinished] is called - just how the strokes
 *   appear when they are still in progress in this composable.
 * @param textureBitmapStore Allows texture asset images to be loaded, corresponding to texture
 *   layers that may be specified by [defaultBrush] or [nextBrush].
 * @param onStrokesFinished Called when there are no longer any in-progress strokes for a short
 *   period. All strokes that were in progress simultaneously will be delivered in the same
 *   callback, running on the UI thread. The callback should pass the strokes to another
 *   [Composable], triggering its recomposition within the same UI thread run loop as the callback
 *   being received, for that other [Composable] to render the strokes without any brief rendering
 *   errors (flickers). These flickers may come from a gap where a stroke is drawn in neither this
 *   [Composable] nor the app's [Composable] during a frame, or a double draw where the stroke is
 *   drawn twice and translucent strokes appear more opaque than they should. Be careful about
 *   passing strokes to asynchronous frameworks during this callback, as they may switch threads
 *   during the handoff from producer to consumer, and even if the final consumer is on the UI
 *   thread, it may not be in the same UI thread run loop and lead to a flicker.
 */
@Composable
public fun InProgressStrokes(
    defaultBrush: Brush?,
    nextBrush: () -> Brush? = { defaultBrush },
    pointerEventToWorldTransform: Matrix = IDENTITY_MATRIX,
    strokeToWorldTransform: Matrix = IDENTITY_MATRIX,
    maskPath: Path? = null,
    textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null },
    onStrokesFinished: (List<Stroke>) -> Unit,
) {
    // NOMUTANTS -- Tests need to use InProgressStrokesImpl for its onSyncAvailable parameter.
    InProgressStrokesImpl(
        nextBrush = nextBrush,
        nextPointerEventToWorldTransform = { pointerEventToWorldTransform },
        nextStrokeToWorldTransform = { strokeToWorldTransform },
        maskPath = maskPath,
        textureBitmapStore = textureBitmapStore,
        onStrokesFinished = onStrokesFinished,
    )
}

@VisibleForTesting
@Composable
internal fun InProgressStrokesImpl(
    nextBrush: () -> Brush?,
    nextPointerEventToWorldTransform: () -> Matrix = { IDENTITY_MATRIX },
    nextStrokeToWorldTransform: () -> Matrix = { IDENTITY_MATRIX },
    maskPath: Path? = null,
    textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null },
    onSyncAvailable: ((Long, TimeUnit) -> Unit) -> Unit = {},
    onStrokesFinished: (List<Stroke>) -> Unit,
) {
    @OptIn(ExperimentalCustomShapeWorkflowApi::class)
    InProgressShapesImpl(
        customShapeWorkflow = InkShapeWorkflow { CanvasStrokeRenderer.create(textureBitmapStore) },
        nextShapeSpec = nextBrush,
        nextPointerEventToWorldTransform = nextPointerEventToWorldTransform,
        nextShapeToWorldTransform = nextStrokeToWorldTransform,
        maskPath = maskPath,
        onSyncAvailable = onSyncAvailable,
        onShapesCompleted = onStrokesFinished,
    )
}

private val IDENTITY_MATRIX = Matrix()
