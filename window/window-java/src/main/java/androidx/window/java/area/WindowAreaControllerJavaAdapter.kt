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

package androidx.window.java.area

import android.app.Activity
import androidx.core.util.Consumer
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.area.WindowAreaStatus
import androidx.window.area.WindowAreaController
import androidx.window.core.ExperimentalWindowApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An adapted interface for [WindowAreaController] that provides the information and
 * functionality around RearDisplay Mode via a callback shaped API.
 */
@ExperimentalWindowApi
class WindowAreaControllerJavaAdapter(
    private val controller: WindowAreaController
) : WindowAreaController by controller {

    /**
     * A [ReentrantLock] to protect against concurrent access to [consumerToJobMap].
     */
    private val lock = ReentrantLock()
    private val consumerToJobMap = mutableMapOf<Consumer<*>, Job>()

    /**
     * Registers a listener to consume [WindowAreaStatus] values defined as
     * [WindowAreaStatus.UNSUPPORTED], [WindowAreaStatus.UNAVAILABLE], and
     * [WindowAreaStatus.AVAILABLE]. The values provided through this listener should be used
     * to determine if you are able to enable rear display Mode at that time. You can use these
     * values to modify your UI to show/hide controls and determine when to enable features
     * that use rear display Mode. You should only try and enter rear display mode when your
     * [consumer] is provided a value of [WindowAreaStatus.AVAILABLE].
     *
     * The [consumer] will be provided an initial value on registration, as well as any change
     * to the status as they occur. This could happen due to hardware device state changes, or if
     * another process has enabled RearDisplay Mode.
     *
     * @see WindowAreaController.rearDisplayStatus
     */
    fun addRearDisplayStatusListener(
        executor: Executor,
        consumer: Consumer<WindowAreaStatus>
    ) {
        val statusFlow = controller.rearDisplayStatus()
        lock.withLock {
            if (consumerToJobMap[consumer] == null) {
                val scope = CoroutineScope(executor.asCoroutineDispatcher())
                consumerToJobMap[consumer] = scope.launch {
                    statusFlow.collect { consumer.accept(it) }
                }
            }
        }
    }

    /**
     * Removes a listener of [WindowAreaStatus] values
     * @see WindowAreaController.rearDisplayStatus
     */
    fun removeRearDisplayStatusListener(consumer: Consumer<WindowAreaStatus>) {
        lock.withLock {
            consumerToJobMap[consumer]?.cancel()
            consumerToJobMap.remove(consumer)
        }
    }

    /**
     * Starts a RearDisplay Mode session and provides updates through the
     * [WindowAreaSessionCallback] provided. Due to the nature of moving your Activity to a
     * different display, your Activity will likely go through a configuration change. Because of
     * this, if your Activity does not override configuration changes, this method should be called
     * from a component that outlives the Activity lifecycle such as a
     * [androidx.lifecycle.ViewModel]. If your Activity does override
     * configuration changes, it is safe to call this method inside your Activity.
     *
     * This method should only be called if you have received a [WindowAreaStatus.AVAILABLE]
     * value from the listener provided through the [addRearDisplayStatusListener] method. If
     * you try and enable RearDisplay mode without it being available, you will receive an
     * [UnsupportedOperationException].
     *
     * The [windowAreaSessionCallback] provided will receive a call to
     * [WindowAreaSessionCallback.onSessionStarted] after your Activity has been moved to the
     * display corresponding to this mode. RearDisplay mode will stay active until the session
     * provided through [WindowAreaSessionCallback.onSessionStarted] is closed, or there is a device
     * state change that makes RearDisplay mode incompatible such as if the device is closed so the
     * outer-display is no longer in line with the rear camera. When this occurs,
     * [WindowAreaSessionCallback.onSessionEnded] is called to notify you the session has been
     * ended.
     *
     * @see addRearDisplayStatusListener
     * @throws UnsupportedOperationException if you try and start a RearDisplay session when
     * your [WindowAreaController.rearDisplayStatus] does not return a value of
     * [WindowAreaStatus.AVAILABLE]
     */
    fun startRearDisplayModeSession(
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    ) {
        controller.rearDisplayMode(activity, executor, windowAreaSessionCallback)
    }
}