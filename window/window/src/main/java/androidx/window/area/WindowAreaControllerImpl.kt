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

package androidx.window.area

import android.app.Activity
import android.os.Binder
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.core.Bounds
import androidx.window.core.BuildConfig
import androidx.window.core.VerificationMode
import androidx.window.extensions.area.ExtensionWindowAreaStatus
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_INACTIVE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_CONTENT_INVISIBLE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_CONTENT_VISIBLE
import androidx.window.extensions.area.WindowAreaComponent.WindowAreaSessionState
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.layout.WindowMetrics
import java.util.concurrent.Executor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Implementation of WindowAreaController for devices
 * that do implement the WindowAreaComponent on device.
 *
 * Requires [Build.VERSION_CODES.N] due to the use of [Consumer].
 * Will not be created though on API levels lower than
 * [Build.VERSION_CODES.S] as that's the min level of support for
 * this functionality.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class WindowAreaControllerImpl(
    private val windowAreaComponent: WindowAreaComponent,
    private val vendorApiLevel: Int
) : WindowAreaController {

    private lateinit var rearDisplaySessionConsumer: Consumer<Int>
    private var currentRearDisplayModeStatus: WindowAreaCapability.Status =
        WINDOW_AREA_STATUS_UNSUPPORTED
    private var currentRearDisplayPresentationStatus: WindowAreaCapability.Status =
        WINDOW_AREA_STATUS_UNSUPPORTED

    private val currentWindowAreaInfoMap = HashMap<String, WindowAreaInfo>()

    override val windowAreaInfos: Flow<List<WindowAreaInfo>>
        get() {
            return callbackFlow {
                val rearDisplayListener = Consumer<Int> { status ->
                    updateRearDisplayAvailability(status)
                    channel.trySend(currentWindowAreaInfoMap.values.toList())
                }
                val rearDisplayPresentationListener =
                    Consumer<ExtensionWindowAreaStatus> { extensionWindowAreaStatus ->
                        updateRearDisplayPresentationAvailability(extensionWindowAreaStatus)
                        channel.trySend(currentWindowAreaInfoMap.values.toList())
                    }

                windowAreaComponent.addRearDisplayStatusListener(rearDisplayListener)
                if (vendorApiLevel > 2) {
                    windowAreaComponent.addRearDisplayPresentationStatusListener(
                        rearDisplayPresentationListener
                    )
                }

                awaitClose {
                    windowAreaComponent.removeRearDisplayStatusListener(rearDisplayListener)
                    if (vendorApiLevel > 2) {
                        windowAreaComponent.removeRearDisplayPresentationStatusListener(
                            rearDisplayPresentationListener
                        )
                    }
                }
            }
        }

    private fun updateRearDisplayAvailability(
        status: @WindowAreaComponent.WindowAreaStatus Int
    ) {
        currentRearDisplayModeStatus = WindowAreaAdapter.translate(status)
        updateRearDisplayWindowArea(
            WindowAreaCapability.Operation.OPERATION_TRANSFER_ACTIVITY_TO_AREA,
            currentRearDisplayModeStatus,
            createEmptyWindowMetrics() /* metrics */,
        )
    }

    private fun updateRearDisplayPresentationAvailability(
        extensionWindowAreaStatus: ExtensionWindowAreaStatus
    ) {
        currentRearDisplayPresentationStatus =
            WindowAreaAdapter.translate(extensionWindowAreaStatus.windowAreaStatus)
        val windowMetrics = WindowAreaAdapter.translate(
            displayMetrics = extensionWindowAreaStatus.windowAreaDisplayMetrics
        )

        updateRearDisplayWindowArea(
            WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA,
            currentRearDisplayPresentationStatus,
            windowMetrics,
        )
    }

    private fun updateRearDisplayWindowArea(
        operation: WindowAreaCapability.Operation,
        status: WindowAreaCapability.Status,
        metrics: WindowMetrics,
    ) {
        var rearDisplayAreaInfo: WindowAreaInfo? =
            currentWindowAreaInfoMap[REAR_DISPLAY_BINDER_DESCRIPTOR]
        if (status == WINDOW_AREA_STATUS_UNSUPPORTED) {
            rearDisplayAreaInfo?.let { info ->
                if (shouldRemoveWindowAreaInfo(info)) {
                    currentWindowAreaInfoMap.remove(REAR_DISPLAY_BINDER_DESCRIPTOR)
                } else {
                    val capability = WindowAreaCapability(operation, status)
                    info.capabilityMap[operation] = capability
                }
            }
        } else {
            if (rearDisplayAreaInfo == null) {
                rearDisplayAreaInfo = WindowAreaInfo(
                    metrics = metrics,
                    type = WindowAreaInfo.Type.TYPE_REAR_FACING,
                    // TODO(b/273807238): Update extensions to send the binder token and type
                    token = Binder(REAR_DISPLAY_BINDER_DESCRIPTOR),
                    windowAreaComponent = windowAreaComponent
                )
            }
            val capability = WindowAreaCapability(operation, status)
            rearDisplayAreaInfo.capabilityMap[operation] = capability
            currentWindowAreaInfoMap[REAR_DISPLAY_BINDER_DESCRIPTOR] = rearDisplayAreaInfo
        }
        rearDisplayAreaInfo?.metrics = metrics
    }

    /**
     * Determines if a [WindowAreaInfo] should be removed from [windowAreaInfos] if all
     * [WindowAreaCapability] are currently [WINDOW_AREA_STATUS_UNSUPPORTED]
     */
    private fun shouldRemoveWindowAreaInfo(windowAreaInfo: WindowAreaInfo): Boolean {
        for (capability: WindowAreaCapability in windowAreaInfo.capabilityMap.values) {
            if (capability.status != WINDOW_AREA_STATUS_UNSUPPORTED) {
                return false
            }
        }
        return true
    }

    override fun transferActivityToWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
        ) {
        if (token.interfaceDescriptor == REAR_DISPLAY_BINDER_DESCRIPTOR) {
            startRearDisplayMode(activity, executor, windowAreaSessionCallback)
        }
    }

    override fun presentContentOnWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback
    ) {
        if (token.interfaceDescriptor == REAR_DISPLAY_BINDER_DESCRIPTOR) {
            startRearDisplayPresentationMode(
                activity,
                executor,
                windowAreaPresentationSessionCallback
            )
        }
    }

    private fun startRearDisplayMode(
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    ) {
        // If the capability is currently active, provide an error pointing the developer on how to
        // get access to the current session
        if (currentRearDisplayModeStatus == WINDOW_AREA_STATUS_ACTIVE) {
            windowAreaSessionCallback.onSessionEnded(
                IllegalStateException(
                    "The WindowArea feature is currently active, WindowAreaInfo#getActiveSession" +
                        "can be used to get an instance of the current active session"
                )
            )
            return
        }

        // If we already have an availability value that is not
        // [Availability.WINDOW_AREA_CAPABILITY_AVAILABLE] we should end the session and pass an
        // exception to indicate they tried to enable rear display mode when it was not available.
        if (currentRearDisplayModeStatus != WINDOW_AREA_STATUS_AVAILABLE) {
            windowAreaSessionCallback.onSessionEnded(
                IllegalStateException(
                    "The WindowArea feature is currently not available to be entered"
                )
            )
            return
        }

        rearDisplaySessionConsumer =
            RearDisplaySessionConsumer(executor, windowAreaSessionCallback, windowAreaComponent)
        windowAreaComponent.startRearDisplaySession(activity, rearDisplaySessionConsumer)
    }

    private fun startRearDisplayPresentationMode(
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback
    ) {
        if (currentRearDisplayPresentationStatus != WINDOW_AREA_STATUS_AVAILABLE) {
            windowAreaPresentationSessionCallback.onSessionEnded(
                IllegalStateException(
                    "The WindowArea feature is currently not available to be entered"
                )
            )
            return
        }

        windowAreaComponent.startRearDisplayPresentationSession(
            activity,
            RearDisplayPresentationSessionConsumer(
                executor,
                windowAreaPresentationSessionCallback,
                windowAreaComponent
            )
        )
    }

    internal class RearDisplaySessionConsumer(
        private val executor: Executor,
        private val appCallback: WindowAreaSessionCallback,
        private val extensionsComponent: WindowAreaComponent
    ) : Consumer<Int> {

        private var session: WindowAreaSession? = null

        override fun accept(t: Int) {
            when (t) {
                SESSION_STATE_ACTIVE -> onSessionStarted()
                SESSION_STATE_INACTIVE -> onSessionFinished()
                else -> {
                    if (BuildConfig.verificationMode == VerificationMode.STRICT) {
                        Log.d(TAG, "Received an unknown session status value: $t")
                    }
                    onSessionFinished()
                }
            }
        }

        private fun onSessionStarted() {
            session = RearDisplaySessionImpl(extensionsComponent)
            session?.let { executor.execute { appCallback.onSessionStarted(it) } }
        }

        private fun onSessionFinished() {
            session = null
            executor.execute { appCallback.onSessionEnded(null) }
        }
    }

    internal class RearDisplayPresentationSessionConsumer(
        private val executor: Executor,
        private val windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
        private val windowAreaComponent: WindowAreaComponent
    ) : Consumer<@WindowAreaSessionState Int> {
        override fun accept(t: @WindowAreaSessionState Int) {
            executor.execute {
                when (t) {
                    // Presentation should never be null if the session is active
                    SESSION_STATE_ACTIVE -> windowAreaPresentationSessionCallback.onSessionStarted(
                        RearDisplayPresentationSessionPresenterImpl(
                            windowAreaComponent,
                            windowAreaComponent.rearDisplayPresentation!!
                        )
                    )

                    SESSION_STATE_CONTENT_VISIBLE ->
                        windowAreaPresentationSessionCallback.onContainerVisibilityChanged(true)

                    SESSION_STATE_CONTENT_INVISIBLE ->
                        windowAreaPresentationSessionCallback.onContainerVisibilityChanged(false)

                    SESSION_STATE_INACTIVE ->
                        windowAreaPresentationSessionCallback.onSessionEnded(null)

                    else -> {
                        Log.e(TAG, "Invalid session state value received: $t")
                    }
                }
            }
        }
    }

    internal companion object {
        private val TAG = WindowAreaControllerImpl::class.simpleName

        private const val REAR_DISPLAY_BINDER_DESCRIPTOR = "WINDOW_AREA_REAR_DISPLAY"

        internal fun createEmptyWindowMetrics(): WindowMetrics {
            val displayMetrics = DisplayMetrics()
            return WindowMetrics(
                Bounds(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels),
                WindowInsetsCompat.Builder().build()
            )
        }
    }
}
