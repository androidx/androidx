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

@file:JvmName("ViewTreeObserverListeners")

package androidx.core.view

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * Handle for a [ViewTreeObserver] listener that was added with a scope.
 *
 * The listener is removed when its scope ends. Call [remove] to remove it sooner.
 *
 * @see addOnGlobalLayoutListener
 */
public fun interface ViewTreeObserverRegistration {
    /** Removes the listener. Does nothing if the listener was already removed. */
    @MainThread public fun remove()
}

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Use this when the view outlives the component that listens to it, such as a fragment listening to
 * its activity's decor view.
 * * Does nothing if [scope] is already destroyed.
 * * Removes [listener] even if this view was attached to, or detached from, a window in between.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnGlobalLayoutListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnGlobalLayoutListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnGlobalLayoutListener(listener) },
        remove = { it.removeOnGlobalLayoutListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 * * Removes [listener] the first time [scope] is detached. Attaching [scope] again does not add
 *   [listener] back.
 * * If [scope] is not attached yet, keeps [listener] until [scope] is attached and then detached.
 * * Removes [listener] even if this view was attached to, or detached from, a window in between.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnGlobalLayoutListener(
    scope: View,
    listener: ViewTreeObserver.OnGlobalLayoutListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnGlobalLayoutListener(listener) },
        remove = { it.removeOnGlobalLayoutListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnPreDrawListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnPreDrawListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnPreDrawListener(listener) },
        remove = { it.removeOnPreDrawListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnPreDrawListener(
    scope: View,
    listener: ViewTreeObserver.OnPreDrawListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnPreDrawListener(listener) },
        remove = { it.removeOnPreDrawListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * Note: [ViewTreeObserver] does not allow removing a draw listener during
 * [ViewTreeObserver.OnDrawListener.onDraw]. Do not destroy [scope] or call
 * [ViewTreeObserverRegistration.remove] from that callback.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnDrawListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnDrawListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnDrawListener(listener) },
        remove = { it.removeOnDrawListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * Note: [ViewTreeObserver] does not allow removing a draw listener during
 * [ViewTreeObserver.OnDrawListener.onDraw]. Do not detach [scope] or call
 * [ViewTreeObserverRegistration.remove] from that callback.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnDrawListener(
    scope: View,
    listener: ViewTreeObserver.OnDrawListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnDrawListener(listener) },
        remove = { it.removeOnDrawListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnScrollChangedListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnScrollChangedListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnScrollChangedListener(listener) },
        remove = { it.removeOnScrollChangedListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnScrollChangedListener(
    scope: View,
    listener: ViewTreeObserver.OnScrollChangedListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnScrollChangedListener(listener) },
        remove = { it.removeOnScrollChangedListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnGlobalFocusChangeListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnGlobalFocusChangeListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnGlobalFocusChangeListener(listener) },
        remove = { it.removeOnGlobalFocusChangeListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnGlobalFocusChangeListener(
    scope: View,
    listener: ViewTreeObserver.OnGlobalFocusChangeListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnGlobalFocusChangeListener(listener) },
        remove = { it.removeOnGlobalFocusChangeListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnTouchModeChangeListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnTouchModeChangeListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnTouchModeChangeListener(listener) },
        remove = { it.removeOnTouchModeChangeListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnTouchModeChangeListener(
    scope: View,
    listener: ViewTreeObserver.OnTouchModeChangeListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnTouchModeChangeListener(listener) },
        remove = { it.removeOnTouchModeChangeListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnWindowAttachListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnWindowAttachListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnWindowAttachListener(listener) },
        remove = { it.removeOnWindowAttachListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnWindowAttachListener(
    scope: View,
    listener: ViewTreeObserver.OnWindowAttachListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnWindowAttachListener(listener) },
        remove = { it.removeOnWindowAttachListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is destroyed.
 *
 * Does nothing if [scope] is already destroyed.
 *
 * @param scope owner that removes [listener] when it reaches [Lifecycle.State.DESTROYED]
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is destroyed
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnWindowFocusChangeListener(
    scope: LifecycleOwner,
    listener: ViewTreeObserver.OnWindowFocusChangeListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        LifecycleOwnerScope(scope.lifecycle),
        add = { it.addOnWindowFocusChangeListener(listener) },
        remove = { it.removeOnWindowFocusChangeListener(listener) },
    )

/**
 * Adds [listener] to this view's [ViewTreeObserver] and removes it when [scope] is detached from
 * its window.
 *
 * Attaching [scope] again does not add [listener] back.
 *
 * @param scope view that removes [listener] when it is detached from its window
 * @param listener callback to add
 * @return handle that removes [listener] before [scope] is detached
 */
@MainThread
// No paired remove function: scope removes the listener, or call remove() on the returned handle.
@Suppress("PairedRegistration")
public fun View.addOnWindowFocusChangeListener(
    scope: View,
    listener: ViewTreeObserver.OnWindowFocusChangeListener,
): ViewTreeObserverRegistration =
    addScopedListener(
        AttachedViewScope(scope),
        add = { it.addOnWindowFocusChangeListener(listener) },
        remove = { it.removeOnWindowFocusChangeListener(listener) },
    )

/** Decides when a scoped listener is removed. */
private sealed interface ListenerScope {
    /** Whether the scope is already over, in which case the listener is never added. */
    val isEnded: Boolean

    /** Runs [action] when the scope ends. Returns a function that cancels this. */
    fun doOnEnd(action: () -> Unit): () -> Unit
}

/** Ends when [view] is detached from its window, after it was attached if it is not yet. */
private class AttachedViewScope(private val view: View) : ListenerScope {
    override val isEnded: Boolean
        get() = false

    override fun doOnEnd(action: () -> Unit): () -> Unit {
        val listener =
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {}

                override fun onViewDetachedFromWindow(v: View) = action()
            }
        view.addOnAttachStateChangeListener(listener)
        return { view.removeOnAttachStateChangeListener(listener) }
    }
}

/** Ends when [lifecycle] is destroyed. */
private class LifecycleOwnerScope(private val lifecycle: Lifecycle) : ListenerScope {
    override val isEnded: Boolean
        get() = lifecycle.currentState == Lifecycle.State.DESTROYED

    override fun doOnEnd(action: () -> Unit): () -> Unit {
        val observer =
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) = action()
            }
        lifecycle.addObserver(observer)
        return { lifecycle.removeObserver(observer) }
    }
}

/**
 * Calls [add] with this view's [ViewTreeObserver] now, and calls [remove] with the observer that
 * holds the listener when [scope] ends or the returned handle is used, whichever comes first.
 */
private fun View.addScopedListener(
    scope: ListenerScope,
    add: (ViewTreeObserver) -> Unit,
    remove: (ViewTreeObserver) -> Unit,
): ViewTreeObserverRegistration {
    if (scope.isEnded) return ViewTreeObserverRegistration {}
    val observer = viewTreeObserver
    add(observer)
    return ViewTreeObserverRegistrationImpl(this, observer, scope, remove)
}

private class ViewTreeObserverRegistrationImpl(
    view: View,
    observer: ViewTreeObserver,
    scope: ListenerScope,
    removeFrom: (ViewTreeObserver) -> Unit,
) : ViewTreeObserverRegistration {
    private var view: View? = view
    private var observer: ViewTreeObserver? = observer
    private var removeFrom: ((ViewTreeObserver) -> Unit)? = removeFrom
    private var cancelScope: (() -> Unit)? = null

    init {
        // A view that is not attached has a floating observer. On the first attach the framework
        // merges it into the window's observer and kills it, so follow the listener there. A view
        // that is attached already uses the window's observer, which keeps the listener after a
        // detach.
        view.doOnAttach {
            if (this.removeFrom != null) {
                this.observer = it.viewTreeObserver
            }
        }
        val cancel = scope.doOnEnd(::remove)
        if (this.removeFrom != null) {
            this.cancelScope = cancel
        }
    }

    override fun remove() {
        val removeFrom = removeFrom ?: return
        val observer = observer ?: return
        val view = view ?: return
        val cancelScope = cancelScope

        // Clear all references so removal happens once and a retained handle cannot leak the view,
        // observer, scope, or listener.
        this.removeFrom = null
        this.observer = null
        this.view = null
        this.cancelScope = null

        cancelScope?.invoke()
        // The observer is dead only if this runs while the view is being attached, after the
        // merge and before the attach listener above ran. The listener is then in the observer
        // that this view has now.
        removeFrom(if (observer.isAlive) observer else view.viewTreeObserver)
    }
}
