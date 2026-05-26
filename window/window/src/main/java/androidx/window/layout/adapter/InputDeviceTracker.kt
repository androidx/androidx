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
package androidx.window.layout.adapter

import android.hardware.input.InputManager
import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import androidx.window.layout.util.InputHelper
import androidx.window.layout.util.SerialExecutor
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A class that tracks input devices and notifies listeners when the state of qualified keyboard and
 * pointer devices changes.
 */
internal class InputDeviceTracker(private val inputHelper: InputHelper) : AutoCloseable {
    private val lock = ReentrantLock()
    @GuardedBy("lock") private val listeners = mutableMapOf<Consumer<Boolean>, Executor>()
    @GuardedBy("lock") private var activeSerialExecutor: SerialExecutor? = null
    @GuardedBy("lock") private var isMouseAndKeyboardConnected: Boolean = false
    @GuardedBy("lock") private val qualifiedKeyboards = mutableSetOf<Int>()
    @GuardedBy("lock") private val qualifiedPointers = mutableSetOf<Int>()
    private val inputListener = InputDeviceListenerImpl()

    /**
     * Registers a [Consumer] to be notified when the input device connection state changes. Note:
     * This method can trigger long-running operations over IPC during the initial scan. The
     * provided [executor] will be used to run these operations.
     */
    fun registerListener(executor: Executor, listener: Consumer<Boolean>) {
        lock.withLock {
            val isFirstListener = listeners.isEmpty()
            listeners[listener] = executor
            if (isFirstListener) {
                activeSerialExecutor = SerialExecutor(executor)
                updateTracking(true)
            }
        }
        executor.execute { listener.accept(isMouseAndKeyboardConnected()) }
    }

    /** Unregisters a [Consumer]. */
    fun unregisterListener(listener: Consumer<Boolean>) {
        lock.withLock {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                updateTracking(false)
                activeSerialExecutor = null
            }
        }
    }

    fun isMouseAndKeyboardConnected(): Boolean {
        return lock.withLock { isMouseAndKeyboardConnected }
    }

    private fun updateTracking(shouldTrack: Boolean) {
        if (shouldTrack) {
            inputHelper.registerInputDeviceListener(inputListener)
            val executor = lock.withLock { activeSerialExecutor } ?: return
            executor.execute {
                val initialDeviceIds = inputHelper.getInputDeviceIds()
                var anyChanged = false
                for (deviceId in initialDeviceIds) {
                    if (refreshInput(deviceId)) {
                        anyChanged = true
                    }
                }
                if (anyChanged) {
                    notifyListeners()
                }
            }
        } else {
            inputHelper.unregisterInputDeviceListener(inputListener)
        }
    }

    override fun close() {
        updateTracking(false)
    }

    /**
     * Recalculate and cache whether the device is connected to any qualified keyboard or pointer
     * device when listener detects that input device is added, changed or removed.
     */
    private fun refreshInput(deviceId: Int): Boolean {
        val descriptor = inputHelper.getInputDevice(deviceId)
        return lock.withLock {
            qualifiedKeyboards.remove(deviceId)
            qualifiedPointers.remove(deviceId)
            if (descriptor != null) {
                if (inputHelper.isPhysicalKeyboardDevice(descriptor)) {
                    qualifiedKeyboards.add(deviceId)
                }
                if (inputHelper.isMouseDeviceEnabled(descriptor)) {
                    qualifiedPointers.add(deviceId)
                }
            }
            val currQualifiedInputDevice =
                qualifiedKeyboards.isNotEmpty() && qualifiedPointers.isNotEmpty()
            if (isMouseAndKeyboardConnected != currQualifiedInputDevice) {
                isMouseAndKeyboardConnected = currQualifiedInputDevice
                true
            } else {
                false
            }
        }
    }

    private fun notifyListeners() {
        val currentListeners = lock.withLock { listeners.toMap() }
        val isConnected = isMouseAndKeyboardConnected()
        currentListeners.forEach { (listener, executor) ->
            executor.execute { listener.accept(isConnected) }
        }
    }

    private inner class InputDeviceListenerImpl : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            val executor = lock.withLock { activeSerialExecutor } ?: return
            executor.execute {
                val changed = refreshInput(deviceId)
                if (changed) {
                    notifyListeners()
                }
            }
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            val changed =
                lock.withLock {
                    qualifiedKeyboards.remove(deviceId)
                    qualifiedPointers.remove(deviceId)
                    val currQualifiedInputDevice =
                        qualifiedKeyboards.isNotEmpty() && qualifiedPointers.isNotEmpty()
                    if (isMouseAndKeyboardConnected != currQualifiedInputDevice) {
                        isMouseAndKeyboardConnected = currQualifiedInputDevice
                        true
                    } else {
                        false
                    }
                }
            if (changed) {
                notifyListeners()
            }
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            val executor = lock.withLock { activeSerialExecutor } ?: return
            executor.execute {
                val changed = refreshInput(deviceId)
                if (changed) {
                    notifyListeners()
                }
            }
        }
    }
}
