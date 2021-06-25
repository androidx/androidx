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
package androidx.window

import android.app.Activity
import android.graphics.Rect
import androidx.annotation.GuardedBy
import androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface
import androidx.window.FoldingFeature.State
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.FoldingFeature.Type.Companion.HINGE
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An implementation of [ExtensionInterfaceCompat] that switches the state when a consumer
 * is unregistered. Useful for testing consumers when they go through a cycle of register then
 * unregister then register again.
 */
internal class SwitchOnUnregisterExtensionInterfaceCompat : ExtensionInterfaceCompat {
    private val lock: Lock = ReentrantLock()
    private val foldBounds = Rect(0, 100, 200, 100)
    @GuardedBy("mLock")
    private var callback: ExtensionCallbackInterface = EmptyExtensionCallbackInterface()
    @GuardedBy("mLock")
    private var state = FLAT

    override fun validateExtensionInterface(): Boolean {
        return true
    }

    override fun setExtensionCallback(extensionCallback: ExtensionCallbackInterface) {
        lock.withLock { callback = extensionCallback }
    }

    override fun onWindowLayoutChangeListenerAdded(activity: Activity) {
        lock.withLock { callback.onWindowLayoutChanged(activity, currentWindowLayoutInfo()) }
    }

    override fun onWindowLayoutChangeListenerRemoved(activity: Activity) {
        lock.withLock { state = toggleState(state) }
    }

    fun currentWindowLayoutInfo(): WindowLayoutInfo {
        return WindowLayoutInfo(listOf(currentFoldingFeature()))
    }

    fun currentFoldingFeature(): FoldingFeature {
        return FoldingFeature(foldBounds, HINGE, state)
    }

    internal companion object {
        private fun toggleState(currentState: State): State {
            return if (currentState == FLAT) {
                HALF_OPENED
            } else {
                FLAT
            }
        }
    }
}
