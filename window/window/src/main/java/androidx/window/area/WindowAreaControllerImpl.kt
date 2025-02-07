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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.window.RequiresWindowSdkExtension
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNKNOWN
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.area.adapter.WindowAreaAdapter
import androidx.window.core.BuildConfig
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.ExtensionsUtil
import androidx.window.core.VerificationMode
import androidx.window.extensions.area.ExtensionWindowAreaStatus
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_CONTENT_VISIBLE
import androidx.window.extensions.area.WindowAreaComponent.SESSION_STATE_INACTIVE
import androidx.window.extensions.area.WindowAreaComponent.WindowAreaSessionState
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.reflection.Consumer2
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Implementation of WindowAreaController for devices that do implement the WindowAreaComponent on
 * device.
 *
 * Requires [Build.VERSION_CODES.N] due to the use of [Consumer]. Will not be created though on API
 * levels lower than [Build.VERSION_CODES.S] as that's the min level of support for this
 * functionality.
 */
@ExperimentalWindowApi
@RequiresWindowSdkExtension(3)
@RequiresApi(Build.VERSION_CODES.Q)
internal class WindowAreaControllerImpl(
    private val windowAreaComponent: WindowAreaComponent,
) : WindowAreaController() {

    private lateinit var rearDisplaySessionConsumer: Consumer2<Int>
    private var currentRearDisplayModeStatus: WindowAreaCapability.Status =
        WINDOW_AREA_STATUS_UNKNOWN
    private var currentRearDisplayPresentationStatus: WindowAreaCapability.Status =
        WINDOW_AREA_STATUS_UNKNOWN

    private var activeWindowAreaSession: Boolean = false
    private var presentationSessionActive: Boolean = false

    private val currentWindowAreaInfoMap = HashMap<String, WindowAreaInfo>()

    override val windowAreaInfos: Flow<List<WindowAreaInfo>>
        get() {
            return callbackFlow {
                val rearDisplayListener =
                    Consumer2<Int> { status ->
                        updateRearDisplayAvailability(status)
                        channel.trySend(currentWindowAreaInfoMap.values.toList())
                    }
                val rearDisplayPresentationListener =
                    Consumer2<ExtensionWindowAreaStatus> { extensionWindowAreaStatus ->
                        updateRearDisplayPresentationAvailability(extensionWindowAreaStatus)
                        channel.trySend(currentWindowAreaInfoMap.values.toList())
                    }

                windowAreaComponent.addRearDisplayStatusListener(rearDisplayListener)
                windowAreaComponent.addRearDisplayPresentationStatusListener(
                    rearDisplayPresentationListener
                )

                awaitClose {
                    windowAreaComponent.removeRearDisplayStatusListener(rearDisplayListener)
                    windowAreaComponent.removeRearDisplayPresentationStatusListener(
                        rearDisplayPresentationListener
                    )
                }
            }
        }

    private fun updateRearDisplayAvailability(status: @WindowAreaComponent.WindowAreaStatus Int) {
        val windowMetrics =
            WindowMetricsCalculator.fromDisplayMetrics(
                displayMetrics = windowAreaComponent.rearDisplayMetrics
            )

        currentRearDisplayModeStatus = WindowAreaAdapter.translate(status, activeWindowAreaSession)
        updateRearDisplayWindowArea(
            WindowAreaCapability.Operation.OPERATION_TRANSFER_ACTIVITY_TO_AREA,
            currentRearDisplayModeStatus,
            windowMetrics
        )
    }

    private fun updateRearDisplayPresentationAvailability(
        extensionWindowAreaStatus: ExtensionWindowAreaStatus
    ) {
        currentRearDisplayPresentationStatus =
            WindowAreaAdapter.translate(
                extensionWindowAreaStatus.windowAreaStatus,
                presentationSessionActive
            )
        val windowMetrics =
            WindowMetricsCalculator.fromDisplayMetrics(
                displayMetrics = extensionWindowAreaStatus.windowAreaDisplayMetrics
            )

        updateRearDisplayWindowArea(
            WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA,
            currentRearDisplayPresentationStatus,
            windowMetrics,
        )
    }

    /**
     * Updates the [WindowAreaInfo] object with the [REAR_DISPLAY_BINDER_DESCRIPTOR] binder token
     * with the updated [status] corresponding to the [operation] and with the updated [metrics]
     * received from the device for this window area.
     *
     * @param operation Operation that we are updating the status of.
     * @param status New status for the operation provided on this window area.
     * @param metrics Updated [WindowMetrics] for this window area.
     */
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
                rearDisplayAreaInfo =
                    WindowAreaInfo(
                        metrics = metrics,
                        type = WindowAreaInfo.Type.TYPE_REAR_FACING,
                        // TODO(b/273807238): Update extensions to send the binder token and type
                        token = Binder(REAR_DISPLAY_BINDER_DESCRIPTOR),
                        windowAreaComponent = windowAreaComponent
                    )
            }
            val capability = WindowAreaCapability(operation, status)
            rearDisplayAreaInfo.capabilityMap[operation] = capability
            rearDisplayAreaInfo.metrics = metrics
            currentWindowAreaInfoMap[REAR_DISPLAY_BINDER_DESCRIPTOR] = rearDisplayAreaInfo
        }
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
        if (token.interfaceDescriptor != REAR_DISPLAY_BINDER_DESCRIPTOR) {
            executor.execute {
                windowAreaSessionCallback.onSessionEnded(
                    IllegalArgumentException("Invalid WindowAreaInfo token")
                )
            }
            return
        }

        if (currentRearDisplayModeStatus == WINDOW_AREA_STATUS_UNKNOWN) {
            Log.d(TAG, "Force updating currentRearDisplayModeStatus")
            // currentRearDisplayModeStatus may be null if the client has not queried
            // WindowAreaController.windowAreaInfos using this instance. In this case, we query
            // it for a single value to force update currentRearDisplayModeStatus.
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                windowAreaInfos.first()
                startRearDisplayMode(activity, executor, windowAreaSessionCallback)
            }
        } else {
            startRearDisplayMode(activity, executor, windowAreaSessionCallback)
        }
    }

    override fun presentContentOnWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback
    ) {
        if (token.interfaceDescriptor != REAR_DISPLAY_BINDER_DESCRIPTOR) {
            executor.execute {
                windowAreaPresentationSessionCallback.onSessionEnded(
                    IllegalArgumentException("Invalid WindowAreaInfo token")
                )
            }
            return
        }

        if (currentRearDisplayPresentationStatus == WINDOW_AREA_STATUS_UNKNOWN) {
            Log.d(TAG, "Force updating currentRearDisplayPresentationStatus")
            // currentRearDisplayModeStatus may be null if the client has not queried
            // WindowAreaController.windowAreaInfos using this instance. In this case, we query
            // it for a single value to force update currentRearDisplayPresentationStatus.
            CoroutineScope(executor.asCoroutineDispatcher()).launch {
                windowAreaInfos.first()
                startRearDisplayPresentationMode(
                    activity,
                    executor,
                    windowAreaPresentationSessionCallback
                )
            }
        } else {
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

        activeWindowAreaSession = true
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

        presentationSessionActive = true
        windowAreaComponent.startRearDisplayPresentationSession(
            activity,
            RearDisplayPresentationSessionConsumer(
                executor,
                windowAreaPresentationSessionCallback,
                windowAreaComponent
            )
        )
    }

    internal inner class RearDisplaySessionConsumer(
        private val executor: Executor,
        private val appCallback: WindowAreaSessionCallback,
        private val extensionsComponent: WindowAreaComponent
    ) : Consumer2<Int> {

        private var session: WindowAreaSession? = null

        override fun accept(value: Int) {
            when (value) {
                SESSION_STATE_ACTIVE -> onSessionStarted()
                SESSION_STATE_INACTIVE -> onSessionFinished()
                else -> {
                    if (BuildConfig.verificationMode == VerificationMode.STRICT) {
                        Log.d(TAG, "Received an unknown session status value: $value")
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
            activeWindowAreaSession = false
            session = null
            executor.execute { appCallback.onSessionEnded(null) }
        }
    }

    internal inner class RearDisplayPresentationSessionConsumer(
        private val executor: Executor,
        private val windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
        private val windowAreaComponent: WindowAreaComponent
    ) : Consumer2<@WindowAreaSessionState Int> {

        private var lastReportedSessionStatus: @WindowAreaSessionState Int = SESSION_STATE_INACTIVE

        override fun accept(value: @WindowAreaSessionState Int) {
            val previousStatus: @WindowAreaSessionState Int = lastReportedSessionStatus
            lastReportedSessionStatus = value

            executor.execute {
                when (value) {
                    SESSION_STATE_ACTIVE -> {
                        // If the last status was visible, then ACTIVE infers the content is no
                        // longer visible.
                        if (previousStatus == SESSION_STATE_CONTENT_VISIBLE) {
                            windowAreaPresentationSessionCallback.onContainerVisibilityChanged(
                                false /* isVisible */
                            )
                        } else {
                            // Presentation should never be null if the session is active
                            windowAreaPresentationSessionCallback.onSessionStarted(
                                RearDisplayPresentationSessionPresenterImpl(
                                    windowAreaComponent,
                                    windowAreaComponent.rearDisplayPresentation!!,
                                    ExtensionsUtil.safeVendorApiLevel
                                )
                            )
                        }
                    }
                    SESSION_STATE_CONTENT_VISIBLE ->
                        windowAreaPresentationSessionCallback.onContainerVisibilityChanged(true)
                    SESSION_STATE_INACTIVE -> {
                        presentationSessionActive = false
                        windowAreaPresentationSessionCallback.onSessionEnded(null)
                    }
                    else -> {
                        Log.e(TAG, "Invalid session state value received: $value")
                    }
                }
            }
        }
    }

    internal companion object {
        private val TAG = WindowAreaControllerImpl::class.simpleName

        private const val REAR_DISPLAY_BINDER_DESCRIPTOR = "WINDOW_AREA_REAR_DISPLAY"
    }
}
