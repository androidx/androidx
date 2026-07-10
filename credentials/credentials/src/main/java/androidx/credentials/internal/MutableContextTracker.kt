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

package androidx.credentials.internal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManagerCallback
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A token returned when starting context tracking, which must be unregistered when the flow
 * completes or is cancelled to prevent callback leaks.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface TrackingToken {
    fun unregister()
}

/**
 * Helper object to automatically track and swap base contexts in a [MutableContextWrapper] during
 * activity reconstructions (configuration changes like screen rotation).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object MutableContextTracker {

    /**
     * Wraps a [CredentialManagerCallback] with automated lifecycle context tracking if the context
     * argument is a [MutableContextWrapper].
     */
    fun <T, E : Exception> wrapCallback(
        context: Context,
        cancellationSignal: CancellationSignal?,
        callback: CredentialManagerCallback<T, E>,
    ): CredentialManagerCallback<T, E> {
        if (context !is MutableContextWrapper) return callback
        val trackingToken = track(context)
        val wrappedCallback =
            object : CredentialManagerCallback<T, E> {
                override fun onResult(result: T) {
                    trackingToken?.unregister()
                    callback.onResult(result)
                }

                override fun onError(e: E) {
                    trackingToken?.unregister()
                    callback.onError(e)
                }
            }

        cancellationSignal?.setOnCancelListener { trackingToken?.unregister() }

        return wrappedCallback
    }

    private fun track(wrapper: MutableContextWrapper): TrackingToken? {
        val base = wrapper.baseContext
        val activity = getBaseActivity(base) ?: return null
        val application = activity.application ?: return null
        val callback = ReconstructedActivityLifecycleCallback(wrapper, activity)
        application.registerActivityLifecycleCallbacks(callback)
        return object : TrackingToken {
            private val unregistered = AtomicBoolean(false)

            override fun unregister() {
                if (unregistered.compareAndSet(false, true)) {
                    application.unregisterActivityLifecycleCallbacks(callback)
                }
            }
        }
    }

    private fun getBaseActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) {
                return current
            }
            current = current.baseContext
        }
        return null
    }

    private class ReconstructedActivityLifecycleCallback(
        wrapper: MutableContextWrapper,
        initialActivity: Activity,
    ) : EmptyActivityLifecycleCallbacks() {
        private val wrapperRef = WeakReference(wrapper)
        private val initialActivityClass = initialActivity.javaClass
        private val taskId = initialActivity.taskId
        private var currentActivityRef = WeakReference(initialActivity)
        private var isReconstructing = false

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            val wrapper = wrapperRef.get()
            if (wrapper == null) {
                activity.application.unregisterActivityLifecycleCallbacks(this)
                return
            }

            if (
                isReconstructing &&
                    activity.javaClass == initialActivityClass &&
                    activity.taskId == taskId
            ) {
                wrapper.setBaseContext(activity)
                currentActivityRef = WeakReference(activity)
                isReconstructing = false
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            val wrapper = wrapperRef.get()
            if (wrapper == null) {
                activity.application.unregisterActivityLifecycleCallbacks(this)
                return
            }

            val currentActivity = currentActivityRef.get()
            if (activity === currentActivity) {
                if (activity.isChangingConfigurations) {
                    isReconstructing = true
                } else {
                    wrapper.setBaseContext(activity.applicationContext)
                    activity.application.unregisterActivityLifecycleCallbacks(this)
                }
            }
        }
    }
}
