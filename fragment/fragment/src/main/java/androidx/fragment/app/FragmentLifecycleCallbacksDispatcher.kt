/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.fragment.app

import android.os.Bundle
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList

/** Dispatcher for events to [FragmentManager.FragmentLifecycleCallbacks] instances */
internal class FragmentLifecycleCallbacksDispatcher(private val fragmentManager: FragmentManager) {
    private class FragmentLifecycleCallbacksHolder(
        val callback: FragmentManager.FragmentLifecycleCallbacks,
        val recursive: Boolean
    )

    private val lifecycleCallbacks = CopyOnWriteArrayList<FragmentLifecycleCallbacksHolder>()

    /**
     * Registers a [FragmentManager.FragmentLifecycleCallbacks] to listen to fragment lifecycle
     * events happening in this FragmentManager. All registered callbacks will be automatically
     * unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to register
     * @param recursive true to automatically register this callback for all child FragmentManagers
     */
    fun registerFragmentLifecycleCallbacks(
        cb: FragmentManager.FragmentLifecycleCallbacks,
        recursive: Boolean
    ) {
        lifecycleCallbacks.add(FragmentLifecycleCallbacksHolder(cb, recursive))
    }

    /**
     * Unregisters a previously registered [FragmentManager.FragmentLifecycleCallbacks]. If the
     * callback was not previously registered this call has no effect. All registered callbacks will
     * be automatically unregistered when this FragmentManager is destroyed.
     *
     * @param cb Callbacks to unregister
     */
    fun unregisterFragmentLifecycleCallbacks(cb: FragmentManager.FragmentLifecycleCallbacks) {
        synchronized(lifecycleCallbacks) {
            var i = 0
            val count = lifecycleCallbacks.size
            while (i < count) {
                if (lifecycleCallbacks[i].callback === cb) {
                    lifecycleCallbacks.removeAt(i)
                    break
                }
                i++
            }
        }
    }

    fun dispatchOnFragmentPreAttached(f: Fragment, onlyRecursive: Boolean) {
        val context = fragmentManager.host.context
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentPreAttached(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentPreAttached(fragmentManager, f, context)
            }
        }
    }

    fun dispatchOnFragmentAttached(f: Fragment, onlyRecursive: Boolean) {
        val context = fragmentManager.host.context
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentAttached(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentAttached(fragmentManager, f, context)
            }
        }
    }

    fun dispatchOnFragmentPreCreated(
        f: Fragment,
        savedInstanceState: Bundle?,
        onlyRecursive: Boolean
    ) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentPreCreated(
                f,
                savedInstanceState,
                true
            )
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentPreCreated(fragmentManager, f, savedInstanceState)
            }
        }
    }

    fun dispatchOnFragmentCreated(
        f: Fragment,
        savedInstanceState: Bundle?,
        onlyRecursive: Boolean
    ) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentCreated(
                f,
                savedInstanceState,
                true
            )
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentCreated(fragmentManager, f, savedInstanceState)
            }
        }
    }

    @Suppress("deprecation")
    fun dispatchOnFragmentActivityCreated(
        f: Fragment,
        savedInstanceState: Bundle?,
        onlyRecursive: Boolean
    ) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentActivityCreated(
                f,
                savedInstanceState,
                true
            )
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentActivityCreated(fragmentManager, f, savedInstanceState)
            }
        }
    }

    fun dispatchOnFragmentViewCreated(
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
        onlyRecursive: Boolean
    ) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentViewCreated(
                f,
                v,
                savedInstanceState,
                true
            )
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentViewCreated(fragmentManager, f, v, savedInstanceState)
            }
        }
    }

    fun dispatchOnFragmentStarted(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentStarted(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentStarted(fragmentManager, f)
            }
        }
    }

    fun dispatchOnFragmentResumed(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentResumed(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentResumed(fragmentManager, f)
            }
        }
    }

    fun dispatchOnFragmentPaused(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentPaused(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentPaused(fragmentManager, f)
            }
        }
    }

    fun dispatchOnFragmentStopped(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentStopped(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentStopped(fragmentManager, f)
            }
        }
    }

    fun dispatchOnFragmentSaveInstanceState(f: Fragment, outState: Bundle, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentSaveInstanceState(
                f,
                outState,
                true
            )
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentSaveInstanceState(fragmentManager, f, outState)
            }
        }
    }

    fun dispatchOnFragmentViewDestroyed(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentViewDestroyed(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentViewDestroyed(fragmentManager, f)
            }
        }
    }

    fun dispatchOnFragmentDestroyed(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentDestroyed(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentDestroyed(fragmentManager, f)
            }
        }
    }

    fun dispatchOnFragmentDetached(f: Fragment, onlyRecursive: Boolean) {
        val parent = fragmentManager.parent
        if (parent != null) {
            val parentManager = parent.getParentFragmentManager()
            parentManager.lifecycleCallbacksDispatcher.dispatchOnFragmentDetached(f, true)
        }
        for (holder in lifecycleCallbacks) {
            if (!onlyRecursive || holder.recursive) {
                holder.callback.onFragmentDetached(fragmentManager, f)
            }
        }
    }
}
