@file:JvmName("PoolingContainer")
/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.customview.poolingcontainer

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.view.allViews
import androidx.core.view.ancestors
import androidx.core.view.children

/**
 * A callback to inform a child View within a container that manages its children's lifecycle
 * outside of the View hierarchy (such as a RecyclerView) when that child should dispose any
 * resources it holds.
 *
 * This callback is not necessarily triggered if the pooling container is disassociating the View
 * from a particular piece of data (that is, it is *not* an "unbind listener"). It is intended for
 * expensive resources that need to be cached across data items, but need a signal to be
 * disposed of.
 */
fun interface PoolingContainerListener {
    /**
     * Signals that this view should dispose any resources it may be holding onto, because its
     * container is either discarding the View or has been removed from the hierarchy itself.
     *
     * Note: This may be called multiple times. A call to this method does *not* mean the View
     * will not later be reattached.
     */
    @UiThread
    fun onRelease()
}

/**
 * Add a callback for when this View should dispose its resources.
 *
 * @receiver the child view to receive callbacks regarding
 */
@SuppressLint("ExecutorRegistration") // This is a UI thread callback
fun View.addPoolingContainerListener(listener: PoolingContainerListener) {
    this.poolingContainerListenerHolder.addListener(listener)
}

/**
 * Remove a callback that was previously added by [addPoolingContainerListener]
 */
@SuppressLint("ExecutorRegistration") // This is a UI thread callback
fun View.removePoolingContainerListener(listener: PoolingContainerListener) {
    this.poolingContainerListenerHolder.removeListener(listener)
}

/**
 * Whether this View is a container that manages the lifecycle of its child Views.
 *
 * Any View that sets this to `true` must call [callPoolingContainerOnRelease] on child Views
 * before they are discarded and may possibly not be used in the future. This includes when the
 * view itself is detached from the window, unless it is being held for possible later
 * reattachment and its children should not release their resources.
 *
 * **Warning: Failure to call [callPoolingContainerOnRelease] when a View is removed from the
 * hierarchy and discarded is likely to result in memory leaks!**
 */
var View.isPoolingContainer: Boolean
    get() = getTag(IsPoolingContainerTag) as? Boolean ?: false
    set(value) {
        setTag(IsPoolingContainerTag, value)
    }

/**
 * Whether one of this View's ancestors has `isPoolingContainer` set to `true`
 */
val View.isWithinPoolingContainer: Boolean
    get() {
        ancestors.forEach {
            if (it is View && it.isPoolingContainer) {
                return true
            }
        }
        return false
    }

/**
 * Calls [PoolingContainerListener.onRelease] on any [PoolingContainerListener]s attached to
 * this View or any of its children.
 *
 * At the point when this is called, the View should be detached from the window.
 */
fun View.callPoolingContainerOnRelease() {
    this.allViews.forEach { child ->
        child.poolingContainerListenerHolder.onRelease()
    }
}

/**
 * Calls [PoolingContainerListener.onRelease] on any [PoolingContainerListener]s attached to
 * any of its children (not including the `ViewGroup` itself)
 *
 * At the point when this is called, the View should be detached from the window.
 */
fun ViewGroup.callPoolingContainerOnReleaseForChildren() {
    this.children.forEach { child ->
        child.poolingContainerListenerHolder.onRelease()
    }
}

private val PoolingContainerListenerHolderTag = R.id.pooling_container_listener_holder_tag
private val IsPoolingContainerTag = R.id.is_pooling_container_tag

private class PoolingContainerListenerHolder {
    private val listeners = ArrayList<PoolingContainerListener>()

    fun addListener(listener: PoolingContainerListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PoolingContainerListener) {
        listeners.remove(listener)
    }

    fun onRelease() {
        for (i in listeners.lastIndex downTo 0) {
            listeners[i].onRelease()
        }
    }
}

private val View.poolingContainerListenerHolder: PoolingContainerListenerHolder
    get() {
        var lifecycle =
            getTag(PoolingContainerListenerHolderTag) as PoolingContainerListenerHolder?
        if (lifecycle == null) {
            lifecycle = PoolingContainerListenerHolder()
            setTag(PoolingContainerListenerHolderTag, lifecycle)
        }
        return lifecycle
    }
