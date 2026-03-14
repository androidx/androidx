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

import androidx.ink.geometry.Box
import androidx.ink.strokes.StrokeInputBatch

/**
 * Implement this interface to efficiently build a custom shape over the course of multiple
 * rendering frames with incremental freehand inputs.
 *
 * Resembles the interface of [androidx.ink.strokes.InProgressStroke], but can be implemented with
 * custom functionality to achieve low latency authoring with custom geometry and rendering.
 *
 * The internal state of this object should only be modified by the functions of this interface so
 * that its rendered state stays in sync properly with its logical state. Low latency rendering
 * avoids re-rendering the object each time, so managing this improperly could result in graphics
 * artifacts or poor performance.
 *
 * This instance will be reused for performance purposes according to the following lifecycle:
 * 1. [start] is called to set the [ShapeSpecT] on this object, reset any previous state, and do any
 *    required setup.
 * 2. This shape is repeatedly updated by:
 *     1. Calling [enqueueInputs] with any new real and predicted freehand inputs. This may happen
 *        multiple times in a row before the next call to [update].
 *     2. Calling [update] to process the enqueued inputs into a calculated shape for rendering. If
 *        [changesWithTime] returns `true`, then this may be called multiple times in a row before
 *        the next call to [enqueueInputs].
 *     3. Rendering the calculated shape using an [InProgressShapeRenderer] compatible with this
 *        shape type.
 * 3. [finishInput] is called once there are no more inputs for this stroke (e.g. the user lifts the
 *    stylus from the screen). Alternately, [cancel] may be called if the stroke is cancelled.
 * 4. [update] continues to be called and the shape is rendered until [getCompletedShape] returns a
 *    non-null value, and even afterwards if [changesWithTime] returns `true`.
 * 5. [prepareToRecycle] is called to optionally reset state early, allowing non-reused objects to
 *    be garbage collected promptly.
 *
 * @param ShapeSpecT A type that defines how inputs will be interpreted. This is typically pure,
 *   immutable data.
 * @param CompletedShapeT A type that represents the completed form of this [InProgressShape].
 */
@ExperimentalCustomShapeWorkflowApi
public interface InProgressShape<in ShapeSpecT : Any, out CompletedShapeT : Any> {

    /**
     * Prepare for this instance to be used for shape creation, by receiving inputs through
     * [enqueueInputs], applying those inputs during [update], and being rendered according to
     * [ShapeWorkflow.inProgressShapeRenderer]. The shape specified by these parameters will be
     * valid until [prepareToRecycle] is called.
     *
     * @param shapeSpec The [ShapeSpecT] to apply as stylus configuration to the upcoming inputs.
     * @param systemElapsedTimeMillis The system time in the [android.os.SystemClock] time base
     *   corresponding to a timestamp of 0 in [enqueueInputs] and [update]. It is monotonically
     *   non-decreasing. Most implementations shouldn't use this and should instead rely on the
     *   relative timestamps in [enqueueInputs] and [update], but this value can be added to those
     *   if a more "absolute" timestamp is needed, e.g. when aligning with animations in the rest of
     *   an application, similar to how [android.view.Choreographer.FrameCallback] timestamps are
     *   used.
     */
    public fun start(shapeSpec: ShapeSpecT, systemElapsedTimeMillis: Long)

    /**
     * Append the incremental [realInputs] and set the prediction to [predictedInputs], replacing
     * any previous prediction. Queued inputs must not be processed right away - they can only be
     * processed on the next call to [update].
     *
     * Implementations of this function can assume that:
     * * [start] has been called to set the current [ShapeSpecT] on this instance.
     * * [finishInput] has not been called since then, and neither has [prepareToRecycle].
     * * [realInputs] and [predictedInputs] form a valid stroke input sequence together with
     *   previously added real input. In particular, this means that the first input in [realInputs]
     *   must be valid following the last input in previously added real inputs, and the first input
     *   in [predictedInputs] must be valid following the last input in [realInputs]: They have the
     *   same [androidx.ink.brush.InputToolType], their
     *   [androidx.ink.strokes.StrokeInput.elapsedTimeMillis] values are monotonically
     *   non-decreasing, and they do not duplicate the previous input.
     *
     * Either one or both of [realInputs] and [predictedInputs] may be empty.
     *
     * The [realInputs] and [predictedInputs] instances will be recycled and cannot be retained -
     * implementations must copy the necessary data from them before this function returns.
     */
    public fun enqueueInputs(realInputs: StrokeInputBatch, predictedInputs: StrokeInputBatch)

    /**
     * Whether calls to [update] with new timestamp values may cause changes to [getUpdatedRegion],
     * even when there haven't been calls to [enqueueInputs].
     */
    public fun changesWithTime(): Boolean

    /**
     * Update internal state of this shape based on the provided timestamps as well as any inputs
     * passed to [enqueueInputs] since the last call to [update]. Will be called exactly once for
     * each call to [InProgressShapeRenderer.draw].
     *
     * @param shapeDurationMillis The current timestamp that is directly comparable to those in the
     *   [StrokeInputBatch] objects provided to [enqueueInputs], which is the time since the first
     *   input event for this stroke. This timestamp is monotonically non-decreasing. It is derived
     *   from a value that is aligned with frame timestamps, similar to
     *   [android.view.Choreographer.FrameCallback.doFrame], so that time-based effects on shapes
     *   are updated in sync with animations throughout the entire system. This relative timestamp
     *   is suitable (and preferred) for most calculations, but if the more absolute system
     *   timestamp is needed for animation synchronization, add this value to the timestamp passed
     *   to [start] to get the "current" system frame time.
     */
    public fun update(shapeDurationMillis: Long)

    /**
     * Indicates that this shape will longer be drawn. It will not receive any further calls to
     * [enqueueInputs] or [update], and it will never receive a call to [finishInput].
     *
     * After [cancel], the region returned by [getUpdatedRegion] must include the entire shape's
     * latest bounding box, plus any region removed before cancellation but after the last call to
     * [resetUpdatedRegion]. This is because everything that was removed since the last call to
     * [resetUpdatedRegion] must be erased now that the shape is cancelled. After a subsequent call
     * to [resetUpdatedRegion], further calls to [getUpdatedRegion] must return `null` until the
     * next call to [prepareToRecycle].
     *
     * [isCanceled] must return `true` from when [cancel] is called until the next call to
     * [prepareToRecycle].
     */
    public fun cancel()

    /**
     * Returns whether [cancel] has been called since the most recent call to [start], assuming no
     * intervening call to [prepareToRecycle].
     */
    public fun isCanceled(): Boolean

    /**
     * Returns an axis-aligned bounding rectangle of parts of the shape added, removed, or changed
     * since the most recent call to [start] or [resetUpdatedRegion], assuming no intervening call
     * to [prepareToRecycle]. This result may change due to calls to [update] or [cancel] (cancel
     * changes the drawn shape by removing everything that was drawn previously). The resulting
     * [Box] must be in the same coordinate system as the inputs supplied to [enqueueInputs]. If no
     * changes have occurred, this must return `null`.
     */
    public fun getUpdatedRegion(): Box?

    /**
     * Reset the bounding box returned by [getUpdatedRegion] to `null` until the next call to
     * [update] or [cancel].
     */
    public fun resetUpdatedRegion()

    /**
     * Indicate that no new calls to [enqueueInputs] will be made until the next call to [start].
     *
     * This occurs after the final call to [enqueueInputs] and before any corresponding call to
     * [update].
     */
    public fun finishInput()

    /**
     * If this is called, a subsequent call to [getCompletedShape] must return a non-null
     * [CompletedShapeT]. This can only be called after [finishInput] and [update], before a
     * subsequent call to [prepareToRecycle].
     */
    public fun forceCompletion()

    /**
     * Get the [CompletedShapeT] if this shape has been completed, or `null` otherwise. This can
     * only be called after:
     * 1. The final call to [enqueueInputs].
     * 2. The call to [finishInput] to indicate that the previous call to [enqueueInputs] was the
     *    final one.
     * 3. A call to [update].
     *
     * In most circumstances, this will return a non-null value the first time it is called. But
     * some shapes involve time-based effects after all inputs have been received, which may cause
     * this to return `null` for a little while longer, in which time more calls to [update] will
     * occur.
     *
     * If those effects are finite and prevent the shape from being considered completed, then this
     * function may return `null` until the time passed to [update] progresses enough. Some examples
     * of this include a shape "settling", e.g. to simulate motion coming to rest or the physical
     * process of ink absorbing or "bleeding" into paper.
     *
     * This must not return a non-null value until the [CompletedShapeT] instance returned by this
     * function will no longer change. Once it returns a non-null value, [InProgressShapesView] will
     * not call this function again until this instance is reused for a new shape. Note that may
     * still be before all calls to [update] are completed. For example, if [changesWithTime]
     * returns `true`, there may be more calls to [update] to increment an animation progress value
     * which is expected to be continued after the shape has been handed off via
     * [InProgressShapesCompletedListener].
     *
     * This function should not modify, reset, or clear any internal state, even once it begins
     * returning a non-null value, as the [InProgressShape] is still used for rendering until
     * [prepareToRecycle] is called. It may return `null` again after a call to [prepareToRecycle],
     * and must after a subsequent call to [start].
     */
    public fun getCompletedShape(): CompletedShapeT?

    /**
     * The optional inverse of [start], where some state can be cleaned up when this instance is not
     * currently needed but may be used again in the future. Do not actually release underlying
     * resources that would have to be reallocated on another future call to [start], as performing
     * that allocation each time can hurt performance. The behavior of reading this object between
     * this and the next call to [start] is not defined by this interface. Relevant state may be
     * cleared here, and must be reset either here or in the next call to [start].
     */
    public fun prepareToRecycle() {}
}
