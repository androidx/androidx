/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.viewport_offset

import androidx.annotation.CallSuper
import androidx.ui.animation.Curve
import androidx.ui.core.Duration
import androidx.ui.foundation.change_notifier.ChangeNotifier
import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.toStringAsFixed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * Which part of the content inside the viewport should be visible.
 *
 * The [pixels] value determines the scroll offset that the viewport uses to select which part of
 * its content to display. As the user scrolls the viewport, this value changes, which changes the
 * content that is displayed.
 *
 * This object is a [Listenable] that notifies its listeners when [pixels] changes.
 *
 * See also:
 *
 *  * [ScrollPosition], which is a commonly used concrete subclass.
 *  * [RenderViewportBase], which is a render object that uses viewport offsets.
 *
 *  @constructor Default constructor.

 * Allows subclasses to construct this object directly.
 */
abstract class ViewportOffset : ChangeNotifier() {

    companion object {
        /**
         * Creates a viewport offset with the given [pixels] value.
         *
         * The [pixels] value does not change unless the viewport issues a correction.
         */
        fun fixed(value: Float): ViewportOffset {
            return FixedViewportOffset(value)
        }

        /**
         * Creates a viewport offset with a [pixels] value of 0.0.
         *
         * The [pixels] value does not change unless the viewport issues a correction.
         */
        fun zero(): ViewportOffset {
            return FixedViewportOffset.zero()
        }
    }

    /**
     * The number of pixels to offset the children in the opposite of the axis direction.
     *
     * For example, if the axis direction is down, then the pixel value represents the number of
     * logical pixels to move the children _up_ the screen. Similarly, if the axis direction is
     * left, then the pixels value represents the number of logical pixels to move the children to
     * _right_.
     *
     * This object notifies its listeners when this value changes (except when the value changes due
     * to [correctBy]).
     */
    abstract val pixels: Float

    /**
     * Called when the viewport's extents are established.
     *
     * The argument is the dimension of the [RenderViewport] in the main axis (e.g. the height, for
     * a vertical viewport).
     *
     * This may be called redundantly, with the same value, each frame. This is called during layout
     * for the [RenderViewport]. If the viewport is configured to shrink-wrap its contents, it may
     * be called several times, since the layout is repeated each time the scroll offset is
     * corrected.
     *
     * If this is called, it is called before [applyContentDimensions]. If this is called,
     * [applyContentDimensions] will be called soon afterwards in the same layout phase. If the
     * viewport is not configured to shrink-wrap its contents, then this will only be called when
     * the viewport recomputes its size (i.e. when its parent lays out), and not during normal
     * scrolling.
     *
     * If applying the viewport dimensions changes the scroll offset, return false. Otherwise,
     * return true. If you return false, the [RenderViewport] will be laid out again with the new
     * scroll offset. This is expensive. (The return value is answering the question "did you accept
     * these viewport dimensions unconditionally?"; if the new dimensions change the
     * [ViewportOffset]'s actual [pixels] value, then the viewport will need to be laid out again.)
     */
    abstract fun applyViewportDimension(viewportDimension: Float): Boolean

    /**
     * Called when the viewport's content extents are established.
     *
     * The arguments are the minimum and maximum scroll extents respectively. The minimum will be
     * equal to or less than zero, the maximum will be equal to or greater than zero.
     *
     * The maximum scroll extent has the viewport dimension subtracted from it.
     * For instance, if there is 100.0 pixels of scrollable content, and the viewport is 80.0 pixels
     * high, then the minimum scroll extent will typically be 0.0 and the maximum scroll extent will
     * typically be 20.0, because there's only 20.0 pixels of actual scroll slack.
     *
     * If applying the content dimensions changes the scroll offset, return false. Otherwise, return
     * true. If you return false, the [RenderViewport] will be laid out again with the new scroll
     * offset. This is expensive. (The return value is answering the question "did you accept these
     * content dimensions unconditionally?"; if the new dimensions change the [ViewportOffset]'s
     * actual [pixels] value, then the viewport will need to be laid out again.)
     *
     * This is called at least once each time the [RenderViewport] is laid out, even if the values
     * have not changed. It may be called many times if the scroll offset is corrected (if this
     * returns false). This is always called after [applyViewportDimension], if that method is
     * called.
     */
    abstract fun applyContentDimensions(minScrollExtent: Float, maxScrollExtent: Float): Boolean

    /**
     * Apply a layout-time correction to the scroll offset.
     *
     * This method should change the [pixels] value by `correction`, but without calling
     * [notifyListeners]. It is called during layout by the [RenderViewport], before
     * [applyContentDimensions]. After this method is called, the layout will be recomputed and that
     * may result in this method being called again, though this should be very rare.
     *
     * See also:
     *
     *  * [jumpTo], for also changing the scroll position when not in layout.
     *    [jumpTo] applies the change immediately and notifies its listeners.
     */
    abstract fun correctBy(correction: Float)

    /**
     * Jumps [pixels] from its current value to the given value, without animation, and without
     * checking if the new value is in range.
     *
     * See also:
     *
     *  * [correctBy], for changing the current offset in the middle of layout and that defers the
     *    notification of its listeners until after layout.
     */
    abstract fun jumpTo(pixels: Float)

    /**
     * Animates [pixels] from its current value to the given value.
     *
     * The returned [Future] will complete when the animation ends, whether it completed
     * successfully or whether it was interrupted prematurely.
     *
     * The duration must not be zero. To jump to a particular value without an animation, use
     * [jumpTo].
     */
    abstract fun animateTo(to: Float, duration: Duration?, curve: Curve?): Deferred<Unit>

    /**
     * Calls [jumpTo] if duration is null or [Duration.zero], otherwise [animateTo] is called.
     *
     * If [animateTo] is called then [curve] defaults to [Curves.ease]. The [clamp] parameter is
     * ignored by this stub implementation but subclasses like [ScrollPosition] handle it by
     * adjusting [to] to prevent over or underscroll.
     */
    fun moveTo(
        to: Float,
        duration: Duration? = null,
        curve: Curve? = null,
        clamp: Boolean? = null
    ): Deferred<Unit> {
        if (duration == null || duration == Duration.zero) {
            jumpTo(to)
            return CompletableDeferred(Unit)
        } else {
            return animateTo(to, duration = duration, curve = curve)
        }
    }

    /**
     * The direction in which the user is trying to change [pixels], relative to the viewport's
     * [RenderViewport.axisDirection].
     *
     * If the _user_ is not scrolling, this will return [ScrollDirection.idle] even if there is (for
     * example) a [ScrollActivity] currently animating the position.
     *
     * This is exposed in [SliverConstraints.userScrollDirection], which is used by some slivers to
     * determine how to react to a change in scroll offset. For example,
     * [RenderSliverFloatingPersistentHeader] will only expand a floating app bar when the
     * [userScrollDirection] is in the positive scroll offset direction.
     */
    abstract val userScrollDirection: ScrollDirection

    /**
     * Whether a viewport is allowed to change [pixels] implicitly to respond to a call to
     * [RenderObject.showOnScreen].
     *
     * [RenderObject.showOnScreen] is for example used to bring a text field fully on screen after
     * it has received focus. This property controls whether the viewport associated with this
     * offset is allowed to change the offset's [pixels] value to fulfill such a request.
     */
    abstract val allowImplicitScrolling: Boolean

    override fun toString(): String {
        val description = mutableListOf<String>()
        debugFillDescription(description)
        return "${describeIdentity(this)}(${description.joinToString(", ")})"
    }

    /**
     * Add additional information to the given description for use by [toString].
     *
     * This method makes it easier for subclasses to coordinate to provide a high-quality [toString]
     * implementation. The [toString] implementation on the [State] base class calls
     * [debugFillDescription] to collect useful information from subclasses to incorporate into its
     * return value.
     *
     * If you override this, make sure to start your method with a call to
     * `super.debugFillDescription(description)`.
     */
    @CallSuper
    fun debugFillDescription(description: MutableList<String>) {
        description.add("offset: ${pixels.toStringAsFixed(1)}")
    }
}