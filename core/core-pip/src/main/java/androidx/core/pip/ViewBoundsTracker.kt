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

package androidx.core.pip

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.UiThread

/**
 * Tracks the global visible bounds of a [View].
 *
 * This class monitors layout changes and scrolls affecting the specified [view] and notifies
 * registered listeners when its visible bounds on the screen change. The bounds are obtained using
 * [View.getGlobalVisibleRect].
 *
 * Listening starts automatically when the view is attached to a window and stops when it's
 * detached.
 */
@UiThread
internal class ViewBoundsTracker(private val view: View) {

    /** Interface for listening to changes in the view's global visible bounds. */
    interface OnViewBoundsChangedListener {
        /**
         * Called when the view's global visible bounds have changed.
         *
         * @param view The view whose bounds changed.
         * @param newBounds The new global visible bounds of the view. This will be an empty Rect if
         *   the view is not visible.
         */
        fun onViewBoundsChanged(view: View, newBounds: Rect)
    }

    private val listeners = mutableSetOf<OnViewBoundsChangedListener>()
    private val currentBounds = Rect()
    private var isTracking = false

    // Listener for changes to the view's own layout
    private val layoutChangeListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateBounds() }

    // Listener for scroll changes within the view tree
    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener { updateBounds() }

    // Listener for global layout changes in the view tree
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener { updateBounds() }

    // Listener to start/stop tracking based on window attachment
    private val attachStateChangeListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                startTracking()
            }

            override fun onViewDetachedFromWindow(v: View) {
                stopTracking()
            }
        }

    init {
        // If the view is already attached when the tracker is created, start tracking.
        if (view.isAttachedToWindow) {
            startTracking()
        }
        // Always add the OnAttachStateChangeListener to handle future attachments/detachments.
        view.addOnAttachStateChangeListener(attachStateChangeListener)
    }

    /** Adds a listener to be notified of bounds changes. */
    fun addListener(listener: OnViewBoundsChangedListener) {
        listeners.add(listener)
    }

    /** Removes a previously added listener. */
    fun removeListener(listener: OnViewBoundsChangedListener) {
        listeners.remove(listener)
    }

    /**
     * Releases resources and stops tracking. Call this when the tracker is no longer needed to
     * prevent potential memory leaks, especially if the tracker instance lives longer than the
     * view.
     */
    fun release() {
        stopTracking()
        view.removeOnAttachStateChangeListener(attachStateChangeListener)
        listeners.clear()
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        view.addOnLayoutChangeListener(layoutChangeListener)
        val vto = view.viewTreeObserver
        if (vto.isAlive) {
            vto.addOnScrollChangedListener(scrollChangedListener)
            vto.addOnGlobalLayoutListener(globalLayoutListener)
        }
        // Perform an initial bounds check.
        updateBounds()
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        view.removeOnLayoutChangeListener(layoutChangeListener)
        val vto = view.viewTreeObserver
        if (vto.isAlive) {
            vto.removeOnScrollChangedListener(scrollChangedListener)
            vto.removeOnGlobalLayoutListener(globalLayoutListener)
        }
        // Reset currentBounds when not tracking
        currentBounds.setEmpty()
    }

    private fun updateBounds() {
        if (!view.isAttachedToWindow) return

        val newBounds = Rect()
        // getGlobalVisibleRect returns true if the view is at least partially visible.
        val isVisible = view.getGlobalVisibleRect(newBounds)

        // Use an empty Rect if the view is not visible at all.
        val effectiveBounds = if (isVisible) newBounds else Rect()

        if (effectiveBounds != currentBounds) {
            currentBounds.set(effectiveBounds)
            // Create a copy of the bounds to pass to listeners.
            val boundsCopy = Rect(currentBounds)
            // Iterate over a copy of the listeners set to avoid issues if a listener
            // modifies the set during iteration.
            listeners.toList().forEach { listener ->
                listener.onViewBoundsChanged(view, boundsCopy)
            }
        }
    }
}
