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
package androidx.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.reflect.Field

internal class ImmLeaksCleaner(private val activity: Activity) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != Lifecycle.Event.ON_DESTROY) {
            return
        }
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        with(cleaner) {
            val lock = inputMethodManager.lock ?: return
            val success =
                synchronized(lock) {
                    val servedView = inputMethodManager.servedView ?: return
                    if (servedView.isAttachedToWindow) {
                        return
                    }
                    // Here we have a detached mServedView.  Set null to mNextServedViewField so
                    // that
                    // everything will be cleared in the next InputMethodManager#checkFocus().
                    inputMethodManager.clearNextServedView()
                }
            if (success) {
                // Assume that InputMethodManager#isActive() internally triggers
                // InputMethodManager#checkFocus().
                inputMethodManager.isActive()
            }
        }
    }

    sealed class Cleaner {
        abstract val InputMethodManager.lock: Any?

        abstract val InputMethodManager.servedView: View?

        /** @return Whether the next served view was successfully cleared */
        abstract fun InputMethodManager.clearNextServedView(): Boolean
    }

    /** Cleaner that is used when reading the [InputMethodManager] fields via reflection failed. */
    object FailedInitialization : Cleaner() {
        override val InputMethodManager.lock: Any?
            get() = null

        override val InputMethodManager.servedView: View?
            get() = null

        override fun InputMethodManager.clearNextServedView(): Boolean = false
    }

    /** Cleaner that provides access to hidden fields via reflection */
    class ValidCleaner(
        private val hField: Field,
        private val servedViewField: Field,
        private val nextServedViewField: Field,
    ) : Cleaner() {
        override val InputMethodManager.lock: Any?
            get() =
                try {
                    hField.get(this)
                } catch (e: IllegalAccessException) {
                    null
                }

        override val InputMethodManager.servedView: View?
            get() =
                try {
                    servedViewField.get(this) as View?
                } catch (e: IllegalAccessException) {
                    null
                } catch (e: ClassCastException) {
                    null
                }

        override fun InputMethodManager.clearNextServedView() =
            try {
                nextServedViewField.set(this, null)
                true
            } catch (e: IllegalAccessException) {
                false
            }
    }

    @SuppressLint("SoonBlockedPrivateApi") // This class is only used API <=23
    companion object {
        val cleaner by lazy {
            try {
                val immClass = InputMethodManager::class.java
                val servedViewField =
                    immClass.getDeclaredField("mServedView").apply { isAccessible = true }
                val nextServedViewField =
                    immClass.getDeclaredField("mNextServedView").apply { isAccessible = true }
                val hField = immClass.getDeclaredField("mH").apply { isAccessible = true }
                ValidCleaner(hField, servedViewField, nextServedViewField)
            } catch (e: NoSuchFieldException) {
                // very oem much custom ¯\_(ツ)_/¯
                FailedInitialization
            }
        }
    }
}
