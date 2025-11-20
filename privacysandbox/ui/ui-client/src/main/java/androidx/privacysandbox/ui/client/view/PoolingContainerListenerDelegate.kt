/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.client.view

import android.view.View
import android.view.ViewTreeObserver
import androidx.customview.poolingcontainer.PoolingContainerListener
import androidx.customview.poolingcontainer.addPoolingContainerListener
import androidx.customview.poolingcontainer.isPoolingContainer
import androidx.customview.poolingcontainer.isWithinPoolingContainer
import androidx.customview.poolingcontainer.removePoolingContainerListener

/**
 * A delegate class responsible for managing a [PoolingContainerListener] for a given [View] placed
 * inside a pooling container.
 */
internal class PoolingContainerListenerDelegate(val view: View) {
    private var viewContainingPoolingContainerListener: View? = null
    private var poolingContainerListener = PoolingContainerListener {}
    var poolingContainerViewTreeObserver: ViewTreeObserver? = null

    /**
     * Attaches a [PoolingContainerListener] to the ancestor of [view] that is the nearest direct
     * child of a [androidx.customview.poolingcontainer.isPoolingContainer] view.
     *
     * This method does nothing if [view] is not within a pooling container, or if a listener is
     * already attached to this delegate.
     */
    fun maybeAttachListener(listener: PoolingContainerListener) {
        if (!view.isWithinPoolingContainer) return
        if (viewContainingPoolingContainerListener != null) return

        val newPoolingContainerListener = PoolingContainerListener {
            listener.onRelease()
            viewContainingPoolingContainerListener?.removePoolingContainerListener(
                poolingContainerListener
            )
            viewContainingPoolingContainerListener = null
        }

        var currentView = view
        var parentView = view.parent

        while (parentView != null && !(parentView as View).isPoolingContainer) {
            currentView = parentView
            parentView = currentView.parent
        }

        poolingContainerViewTreeObserver = (parentView as View).viewTreeObserver
        currentView.addPoolingContainerListener(newPoolingContainerListener)
        viewContainingPoolingContainerListener = currentView
        poolingContainerListener = newPoolingContainerListener
    }
}
