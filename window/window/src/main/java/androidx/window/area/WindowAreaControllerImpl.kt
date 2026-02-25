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
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.area.WindowArea.Type.Companion.TYPE_REAR_FACING
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_PRESENT_ON_AREA
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNKNOWN
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.area.WindowAreaControllerImpl.Companion.REAR_DISPLAY_WINDOW_AREA_TOKEN
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

/**
 * Implementation of WindowAreaController for devices that do implement the WindowAreaComponent on
 * device.
 *
 * Requires [Build.VERSION_CODES.N] due to the use of [Consumer]. Will not be created though on API
 * levels lower than [Build.VERSION_CODES.S] as that's the min level of support for this
 * functionality.
 */
@RequiresWindowSdkExtension(3)
@RequiresApi(Build.VERSION_CODES.Q)
internal class WindowAreaControllerImpl(private val windowAreaComponent: WindowAreaComponent) :
    WindowAreaController() {

    private lateinit var rearDisplaySessionConsumer: Consumer2<Int>
    private var currentRearDisplayModeStatus: WindowAreaCapability.Status =
        WINDOW_AREA_STATUS_UNKNOWN
    private var currentRearDisplayPresentationStatus: WindowAreaCapability.Status =
        WINDOW_AREA_STATUS_UNKNOWN

    private var activeWindowAreaSession: Boolean = false
    private var presentationSessionActive: Boolean = false

    private val lock = Any()

    @GuardedBy("lock") private val currentWindowAreaMap = HashMap<WindowAreaToken, WindowArea>()

    private val listeners = ArrayList<Pair<Executor, Consumer<List<WindowArea>>>>()

    val rearDisplayExtensionListener =
        Consumer2<Int> { status ->
            updateRearDisplayAvailability(status)
            updateWindowAreaListeners()
        }
    val rearDisplayPresentationExtensionListener =
        Consumer2<ExtensionWindowAreaStatus> { extensionWindowAreaStatus ->
            updateRearDisplayPresentationAvailability(extensionWindowAreaStatus)
            updateWindowAreaListeners()
        }

    override fun addWindowAreasListener(executor: Executor, listener: Consumer<List<WindowArea>>) {
        synchronized(lock) {
            if (listeners.isEmpty()) {
                listeners.add(Pair(executor, listener))
                initializeExtensionListeners()
            } else {
                listeners.add(Pair(executor, listener))
                executor.execute { listener.accept(currentWindowAreaMap.values.toList()) }
            }
        }
    }

    override fun removeWindowAreasListener(listener: Consumer<List<WindowArea>>) {
        synchronized(lock) {
            val valueToRemove = listeners.firstOrNull { it.second == listener }
            valueToRemove?.let { listenerToRemove -> listeners.remove(listenerToRemove) }
            if (listeners.isEmpty()) {
                removeExtensionListeners()
            }
        }
    }

    private fun initializeExtensionListeners() {
        windowAreaComponent.addRearDisplayStatusListener(rearDisplayExtensionListener)
        windowAreaComponent.addRearDisplayPresentationStatusListener(
            rearDisplayPresentationExtensionListener
        )
    }

    private fun removeExtensionListeners() {
        windowAreaComponent.removeRearDisplayStatusListener(rearDisplayExtensionListener)
        windowAreaComponent.removeRearDisplayPresentationStatusListener(
            rearDisplayPresentationExtensionListener
        )
    }

    private fun updateRearDisplayAvailability(status: @WindowAreaComponent.WindowAreaStatus Int) {
        val rearDisplayWindowMetrics = getRearDisplayMetrics(windowAreaComponent)

        currentRearDisplayModeStatus = WindowAreaAdapter.translate(status, activeWindowAreaSession)
        updateRearDisplayWindowArea(
            OPERATION_TRANSFER_TO_AREA,
            currentRearDisplayModeStatus,
            rearDisplayWindowMetrics,
        )
    }

    private fun updateRearDisplayPresentationAvailability(
        extensionWindowAreaStatus: ExtensionWindowAreaStatus
    ) {
        currentRearDisplayPresentationStatus =
            WindowAreaAdapter.translate(
                extensionWindowAreaStatus.windowAreaStatus,
                presentationSessionActive,
            )
        val rearDisplayWindowMetrics = getRearDisplayMetrics(windowAreaComponent)

        updateRearDisplayWindowArea(
            OPERATION_PRESENT_ON_AREA,
            currentRearDisplayPresentationStatus,
            rearDisplayWindowMetrics,
        )
    }

    /**
     * Updates the [WindowArea] object with the [REAR_DISPLAY_WINDOW_AREA_TOKEN] token with the
     * updated [status] corresponding to the [operation] and with the updated [metrics] received
     * from the device for this window area.
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
        val rearDisplayAreaInfo: WindowArea? =
            synchronized(lock) { currentWindowAreaMap[REAR_DISPLAY_WINDOW_AREA_TOKEN] }

        if (
            status == WINDOW_AREA_STATUS_UNSUPPORTED &&
                (rearDisplayAreaInfo == null || shouldRemoveWindowAreaInfo(rearDisplayAreaInfo))
        ) {
            synchronized(lock) { currentWindowAreaMap.remove(REAR_DISPLAY_WINDOW_AREA_TOKEN) }
            return
        }

        val capability = WindowAreaCapability(operation, status)
        val capabilityMap: MutableMap<WindowAreaCapability.Operation, WindowAreaCapability> =
            rearDisplayAreaInfo?.capabilityMap?.toMutableMap() ?: HashMap()
        capabilityMap[operation] = capability

        val rearDisplayWindowArea =
            WindowArea(
                windowMetrics = metrics,
                type = TYPE_REAR_FACING,
                capabilityMap = capabilityMap.toMap(),
                token = REAR_DISPLAY_WINDOW_AREA_TOKEN,
            )
        synchronized(lock) {
            currentWindowAreaMap[rearDisplayWindowArea.token] = rearDisplayWindowArea
        }
    }

    /**
     * Determines if a [WindowArea] should be removed from [currentWindowAreaMap] if all
     * [WindowAreaCapability] are currently [WINDOW_AREA_STATUS_UNSUPPORTED]
     */
    private fun shouldRemoveWindowAreaInfo(windowArea: WindowArea): Boolean {
        for (capability: WindowAreaCapability in windowArea.capabilityMap.values) {
            if (capability.status != WINDOW_AREA_STATUS_UNSUPPORTED) {
                return false
            }
        }
        return true
    }

    override fun transferToWindowArea(windowAreaToken: WindowAreaToken?, activity: Activity) {
        if (windowAreaToken == null) {
            moveDeviceToDefaultWindowArea()
        } else {
            if (currentWindowAreaMap[windowAreaToken]?.type != TYPE_REAR_FACING) {
                throw IllegalArgumentException("Invalid WindowAreaInfo type")
            }
            startRearDisplayMode(activity)
        }
    }

    private fun moveDeviceToDefaultWindowArea() {
        val activeTransferWindowArea =
            synchronized(lock) {
                currentWindowAreaMap.values.firstOrNull { windowAreaInfo ->
                    windowAreaInfo.getCapability(OPERATION_TRANSFER_TO_AREA).status ==
                        WINDOW_AREA_STATUS_ACTIVE
                } ?: return
            }

        if (activeTransferWindowArea.type == TYPE_REAR_FACING) {
            windowAreaComponent.endRearDisplaySession()
        }
    }

    @ExperimentalWindowApi
    override fun presentContentOnWindowArea(
        windowAreaToken: WindowAreaToken,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
    ) {
        val windowArea = synchronized(lock) { currentWindowAreaMap[windowAreaToken] }

        if (windowArea?.type != TYPE_REAR_FACING) {
            executor.execute {
                windowAreaPresentationSessionCallback.onSessionEnded(
                    IllegalArgumentException("Invalid WindowAreaInfo token")
                )
            }
            return
        }

        startRearDisplayPresentationMode(activity, executor, windowAreaPresentationSessionCallback)
    }

    private fun startRearDisplayMode(activity: Activity) {
        // If the capability is currently active, provide an error pointing the developer on how to
        // get access to the current session
        if (currentRearDisplayModeStatus == WINDOW_AREA_STATUS_ACTIVE) {
            throw IllegalStateException(
                "The WindowArea feature is currently active, WindowAreaInfo#getActiveSession" +
                    "can be used to get an instance of the current active session"
            )
        }

        // If we already have an availability value that is not
        // [Availability.WINDOW_AREA_CAPABILITY_AVAILABLE] we should end the session and pass an
        // exception to indicate they tried to enable rear display mode when it was not available.
        if (currentRearDisplayModeStatus != WINDOW_AREA_STATUS_AVAILABLE) {
            throw IllegalStateException(
                "The WindowArea feature is currently not available to be entered"
            )
        }

        activeWindowAreaSession = true
        rearDisplaySessionConsumer = RearDisplaySessionConsumer()
        windowAreaComponent.startRearDisplaySession(activity, rearDisplaySessionConsumer)
    }

    @ExperimentalWindowApi
    private fun startRearDisplayPresentationMode(
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
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
                windowAreaComponent,
            ),
        )
    }

    @ExperimentalWindowApi
    override fun getActivePresentationSession(
        windowAreaToken: WindowAreaToken
    ): WindowAreaSessionPresenter? {
        val windowArea = synchronized(lock) { currentWindowAreaMap[windowAreaToken] }

        if (
            windowArea?.type == TYPE_REAR_FACING &&
                windowArea.getCapability(OPERATION_PRESENT_ON_AREA).status ==
                    WINDOW_AREA_STATUS_ACTIVE
        ) {
            return createRearFacingPresentationSession()
        }
        return null
    }

    @ExperimentalWindowApi
    private fun createRearFacingPresentationSession(): WindowAreaSessionPresenter {
        return RearDisplayPresentationSessionPresenterImpl(
            windowAreaComponent,
            windowAreaComponent.rearDisplayPresentation!!,
            ExtensionsUtil.safeVendorApiLevel,
        )
    }

    private fun getRearDisplayMetrics(windowAreaComponent: WindowAreaComponent): WindowMetrics {
        val rearDisplayMetrics =
            try {
                windowAreaComponent.rearDisplayMetrics
            } catch (_: ClassCastException) {
                DisplayMetrics()
            }

        return WindowMetricsCalculator.fromDisplayMetrics(displayMetrics = rearDisplayMetrics)
    }

    private fun updateWindowAreaListeners() {
        synchronized(lock) {
            listeners.forEach { listener ->
                listener.first.execute {
                    listener.second.accept(currentWindowAreaMap.values.toList())
                }
            }
        }
    }

    internal inner class RearDisplaySessionConsumer() : Consumer2<Int> {

        override fun accept(value: Int) {
            when (value) {
                SESSION_STATE_ACTIVE -> return
                SESSION_STATE_INACTIVE -> activeWindowAreaSession = false
                else -> {
                    if (BuildConfig.verificationMode == VerificationMode.STRICT) {
                        Log.d(TAG, "Received an unknown session status value: $value")
                    }
                    activeWindowAreaSession = false
                }
            }
        }
    }

    @ExperimentalWindowApi
    internal inner class RearDisplayPresentationSessionConsumer(
        private val executor: Executor,
        private val windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
        private val windowAreaComponent: WindowAreaComponent,
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
                                    ExtensionsUtil.safeVendorApiLevel,
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

        internal val REAR_DISPLAY_WINDOW_AREA_TOKEN =
            WindowAreaToken(Binder("WINDOW_AREA_REAR_DISPLAY"))
    }
}
