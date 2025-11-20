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

package androidx.ink.authoring

import androidx.annotation.UiThread

/**
 * Notifies the client app when a [CompletedShapeT] (or more than one) has been completed on
 * [InProgressShapesView].
 */
@ExperimentalCustomShapeWorkflowApi
@UiThread
public interface InProgressShapesCompletedListener<CompletedShapeT : Any> {
    /**
     * Called when there are no longer any in-progress shapes in the [InProgressShapesView] for a
     * short period. All shapes that were in progress simultaneously will be delivered in the same
     * callback, running on the UI thread.
     *
     * An implementation of this function should start rendering the given shapes in an
     * [android.view.View] by calling its [android.view.View.invalidate] function and using the new
     * [CompletedShapeT] data in the next call to [android.view.View.onDraw].
     *
     * In the same UI thread run loop as calling [android.view.View.invalidate], call
     * [InProgressShapesView.removeCompletedShapes] with the IDs of the strokes that are now being
     * rendered in the other [android.view.View]. If that happens in a different run loop of the UI
     * thread, there may be brief rendering errors (appearing as flickers) between
     * [InProgressShapesView] and the other [android.view.View] - either a gap where the shape is
     * not drawn during a frame, or a double draw where the shape is drawn twice and translucent
     * strokes appear more opaque than they should.
     *
     * Example:
     * ```
     * public fun onShapesCompleted(shapes: Map<InProgressStrokeId, MyCompletedShape>) {
     *   view.addShapes(shapes.values)
     *   view.invalidate()
     *   inProgressShapesView.removeCompletedShapes(shapes.keys)
     * }
     * ```
     *
     * @param shapes The finished shapes, with map iteration order in the z-order that shapes were
     *   rendered in the [InProgressShapesView], from back to front. This is the same order that
     *   shapes were started with [InProgressShapesView.startShape].
     */
    public fun onShapesCompleted(shapes: Map<InProgressStrokeId, CompletedShapeT>) {}
}
