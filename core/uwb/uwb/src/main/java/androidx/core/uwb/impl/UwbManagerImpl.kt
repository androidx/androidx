/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb.impl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.ranging.RangingManager
import android.ranging.uwb.UwbAddress.createRandomShortAddress
import android.ranging.uwb.UwbComplexChannel.UWB_CHANNEL_5
import android.ranging.uwb.UwbComplexChannel.UWB_CHANNEL_9
import android.ranging.uwb.UwbComplexChannel.UWB_PREAMBLE_CODE_INDEX_10
import android.ranging.uwb.UwbComplexChannel.UWB_PREAMBLE_CODE_INDEX_11
import android.ranging.uwb.UwbComplexChannel.UWB_PREAMBLE_CODE_INDEX_12
import android.ranging.uwb.UwbComplexChannel.UWB_PREAMBLE_CODE_INDEX_9
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingParameters.Companion.CONFIG_MULTICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_PROVISIONED_MULTICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_PROVISIONED_UNICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_UNICAST_DS_TWR
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbAvailabilityCallback
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbManager
import androidx.core.uwb.backend.IUwb
import androidx.core.uwb.backend.IUwbAvailabilityObserver
import androidx.core.uwb.backend.IUwbClient
import androidx.core.uwb.exceptions.UwbServiceNotAvailableException
import androidx.core.uwb.helper.checkSystemFeature
import androidx.core.uwb.helper.getFailureReasonFromApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.uwb.UwbClient
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

internal class UwbManagerImpl(private val context: Context) : UwbManager {
    companion object {
        const val TAG = "UwbManagerImpl"
        private const val USE_RANGING_MODULE = false
        private const val INIT_TIMEOUT_MS = 5000L
        val PUBLIC_AVAILABLE_CONFIG_IDS =
            setOf(
                CONFIG_UNICAST_DS_TWR,
                CONFIG_MULTICAST_DS_TWR,
                CONFIG_PROVISIONED_UNICAST_DS_TWR,
                CONFIG_PROVISIONED_MULTICAST_DS_TWR,
            )
        val UWB_PREAMBLE_INDEXES =
            listOf(
                UWB_PREAMBLE_CODE_INDEX_9,
                UWB_PREAMBLE_CODE_INDEX_10,
                UWB_PREAMBLE_CODE_INDEX_11,
                UWB_PREAMBLE_CODE_INDEX_12,
            )
        var iUwb: IUwb? = null
        var aospAvailabilityClient: IUwbClient? = null
        var gmsAvailabilityClient: UwbClient? = null
        var mRangingManager: RangingManager? = null
        var mExecutor: Executor? = null
        var mTechnologyAvailability: MutableMap<Any?, Any?> =
            Collections.synchronizedMap(mutableMapOf())
        private val mRangingCapabilities = AtomicReference<android.ranging.RangingCapabilities?>()
    }

    init {
        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    iUwb = IUwb.Stub.asInterface(service)
                    Log.i(TAG, "iUwb service created successfully.")
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    iUwb = null
                }
            }
        val intent = Intent("androidx.core.uwb.backend.service")
        intent.setPackage("androidx.core.uwb.backend")
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    @Deprecated("Renamed to controleeSessionScope")
    override suspend fun clientSessionScope(): UwbClientSessionScope {
        return createClientSessionScope(false)
    }

    override suspend fun controleeSessionScope(): UwbControleeSessionScope {
        return createClientSessionScope(false) as UwbControleeSessionScope
    }

    override suspend fun controllerSessionScope(): UwbControllerSessionScope {
        return createClientSessionScope(true) as UwbControllerSessionScope
    }

    override suspend fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && USE_RANGING_MODULE) {
            initializeRangingManager()
            return mRangingCapabilities.get()?.uwbCapabilities != null
        }
        checkSystemFeature(context)
        return if (isGmsDevice()) Nearby.getUwbControllerClient(context).isAvailable.await()
        else {
            val client = iUwb?.controllerClient
            client?.isAvailable ?: false
        }
    }

    private fun isGmsDevice(): Boolean {
        val pm = context.packageManager
        val hasGmsCore =
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context, /* minApkVersion */ 230100000) ==
                ConnectionResult.SUCCESS
        val isChinaGcoreDevice =
            pm.hasSystemFeature("cn.google.services") &&
                pm.hasSystemFeature("com.google.android.feature.services_updater")
        return hasGmsCore && !isChinaGcoreDevice
    }

    private suspend fun createClientSessionScope(isController: Boolean): UwbClientSessionScope {
        checkSystemFeature(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && USE_RANGING_MODULE) {
            createRangingClientSessionScope(isController)
        } else if (isGmsDevice()) {
            createGmsClientSessionScope(isController)
        } else createAospClientSessionScope(isController)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun getComplexChannel(supportedChannels: Set<Int>): UwbComplexChannel {
        val channel: Int =
            if (supportedChannels.contains(UWB_CHANNEL_9)) {
                UWB_CHANNEL_9
            } else if (supportedChannels.contains(UWB_CHANNEL_5)) {
                UWB_CHANNEL_5
            } else {
                supportedChannels.first()
            }

        val preamble = UWB_PREAMBLE_INDEXES.random()
        return UwbComplexChannel(channel, preamble)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun initializeRangingManager() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadExecutor()
            mRangingManager = context.getSystemService(RangingManager::class.java)
            val result =
                withTimeoutOrNull(INIT_TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        val callback =
                            RangingManager.RangingCapabilitiesCallback { capabilities ->
                                Log.d(TAG, " Ranging capabilities $capabilities")
                                val availabilities: Map<out Any?, Any?> =
                                    capabilities.technologyAvailability
                                mTechnologyAvailability.putAll(availabilities)
                                mRangingCapabilities.set(capabilities)
                                val uwbStatus = availabilities[RangingManager.UWB]
                                if (uwbStatus != android.ranging.RangingCapabilities.ENABLED) {
                                    Log.v("Ranging", "UWB is not available. Status: $uwbStatus")
                                }
                                if (continuation.isActive) {
                                    continuation.resume(Unit)
                                }
                            }
                        mRangingManager?.registerCapabilitiesCallback(mExecutor!!, callback)
                    }
                }
            if (result == null) {
                throw UwbServiceNotAvailableException("Timeout waiting for ranging capabilities")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private suspend fun createRangingClientSessionScope(
        isController: Boolean
    ): UwbClientSessionScope {
        Log.i(TAG, "Creating Ranging Client session scope")

        initializeRangingManager()
        val uwbStatus = mTechnologyAvailability[RangingManager.UWB]

        if (uwbStatus != android.ranging.RangingCapabilities.ENABLED) {
            throw UwbServiceNotAvailableException(
                "Cannot start a ranging session when UWB is " + "unavailable"
            )
        }

        try {

            val platformCapabilities =
                mRangingCapabilities.get()?.uwbCapabilities
                    ?: throw UwbServiceNotAvailableException(
                        "Ranging capabilities are not available. The service may be initializing."
                    )
            val rangingCapabilities =
                RangingCapabilities(
                    isDistanceSupported = platformCapabilities.isDistanceMeasurementSupported,
                    isAzimuthalAngleSupported = platformCapabilities.isAzimuthalAngleSupported,
                    isElevationAngleSupported = platformCapabilities.isElevationAngleSupported,
                    minRangingInterval = platformCapabilities.minimumRangingInterval.nano,
                    supportedChannels = platformCapabilities.supportedChannels.toSet(),
                    supportedNtfConfigs =
                        platformCapabilities.supportedNotificationConfigurations.toSet(),
                    supportedConfigIds = platformCapabilities.supportedConfigIds.toSet(),
                    supportedSlotDurations = platformCapabilities.supportedSlotDurations.toSet(),
                    supportedRangingUpdateRates =
                        platformCapabilities.supportedRangingUpdateRates.toSet(),
                    isRangingIntervalReconfigureSupported =
                        platformCapabilities.isRangingIntervalReconfigurationSupported,
                    isBackgroundRangingSupported = platformCapabilities.isBackgroundRangingSupported,
                )

            val localAddress = UwbAddress(createRandomShortAddress().addressBytes)
            return if (isController) {
                val complexChannel =
                    getComplexChannel(platformCapabilities.supportedChannels.toSet())
                UwbControllerSessionScopeRangingImpl(
                    mRangingManager!!,
                    rangingCapabilities,
                    localAddress,
                    complexChannel,
                )
            } else {
                UwbControleeSessionScopeRangingImpl(
                    mRangingManager!!,
                    rangingCapabilities,
                    localAddress,
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Error creating ranging client session scope with exception: $e"
            )
        }
    }

    private suspend fun createGmsClientSessionScope(isController: Boolean): UwbClientSessionScope {
        Log.i(TAG, "Creating Gms Client session scope")
        val uwbClient =
            if (isController) Nearby.getUwbControllerClient(context)
            else Nearby.getUwbControleeClient(context)
        if (!uwbClient.isAvailable().await()) {
            Log.e(TAG, "Uwb availability : false")
            throw UwbServiceNotAvailableException(
                "Cannot start a ranging session when UWB is " + "unavailable"
            )
        }
        try {
            val nearbyLocalAddress = uwbClient.localAddress.await()
            val nearbyRangingCapabilities = uwbClient.rangingCapabilities.await()
            val localAddress = UwbAddress(nearbyLocalAddress.address)
            val supportedConfigIds = nearbyRangingCapabilities.supportedConfigIds.toMutableList()
            supportedConfigIds.retainAll(PUBLIC_AVAILABLE_CONFIG_IDS)
            val rangingCapabilities =
                RangingCapabilities(
                    nearbyRangingCapabilities.supportsDistance(),
                    nearbyRangingCapabilities.supportsAzimuthalAngle(),
                    nearbyRangingCapabilities.supportsElevationAngle(),
                    nearbyRangingCapabilities.minRangingInterval,
                    nearbyRangingCapabilities.supportedChannels.toSet(),
                    nearbyRangingCapabilities.supportedNtfConfigs.toSet(),
                    supportedConfigIds.toSet(),
                    nearbyRangingCapabilities.supportedSlotDurations.toSet(),
                    nearbyRangingCapabilities.supportedRangingUpdateRates.toSet(),
                    nearbyRangingCapabilities.supportsRangingIntervalReconfigure(),
                    nearbyRangingCapabilities.hasBackgroundRangingSupport(),
                )
            return if (isController) {
                val uwbComplexChannel = uwbClient.complexChannel.await()
                UwbControllerSessionScopeImpl(
                    uwbClient,
                    rangingCapabilities,
                    localAddress,
                    UwbComplexChannel(uwbComplexChannel.channel, uwbComplexChannel.preambleIndex),
                )
            } else {
                UwbControleeSessionScopeImpl(uwbClient, rangingCapabilities, localAddress)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "RangingResultFailure Reason Code: ${getFailureReasonFromApiException(e)}")
            throw RuntimeException(
                "Unexpected error. This indicates that the library is not " +
                    "up-to-date with the service backend."
            )
        }
    }

    private fun createAospClientSessionScope(isController: Boolean): UwbClientSessionScope {
        Log.i(TAG, "Creating Aosp Client session scope")
        val uwbClient = if (isController) iUwb?.controllerClient else iUwb?.controleeClient
        if (uwbClient == null) {
            Log.e(TAG, "Failed to get UwbClient. AOSP backend is not available.")
        }
        try {
            val aospLocalAddress = uwbClient!!.localAddress
            val aospRangingCapabilities = uwbClient.rangingCapabilities
            val localAddress = aospLocalAddress?.address?.let { UwbAddress(it) }
            val rangingCapabilities =
                aospRangingCapabilities?.let {
                    RangingCapabilities(
                        it.supportsDistance,
                        it.supportsAzimuthalAngle,
                        it.supportsElevationAngle,
                        it.minRangingInterval,
                        it.supportedChannels.toSet(),
                        it.supportedNtfConfigs.toSet(),
                        it.supportedConfigIds
                            .toMutableList()
                            .filter { it in PUBLIC_AVAILABLE_CONFIG_IDS }
                            .toSet(),
                        it.supportedSlotDurations.toSet(),
                        it.supportedRangingUpdateRates.toSet(),
                        it.supportsRangingIntervalReconfigure,
                        it.hasBackgroundRangingSupport,
                    )
                }
            return if (isController) {
                val uwbComplexChannel = uwbClient.complexChannel
                UwbControllerSessionScopeAospImpl(
                    uwbClient,
                    rangingCapabilities!!,
                    localAddress!!,
                    UwbComplexChannel(uwbComplexChannel!!.channel, uwbComplexChannel.preambleIndex),
                )
            } else {
                UwbControleeSessionScopeAospImpl(uwbClient, rangingCapabilities!!, localAddress!!)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override fun setUwbAvailabilityCallback(executor: Executor, observer: UwbAvailabilityCallback) {
        checkSystemFeature(context)
        val identity = Binder.clearCallingIdentity()
        if (isGmsDevice()) {
            try {
                gmsAvailabilityClient?.unsubscribeFromUwbAvailability()

                gmsAvailabilityClient = Nearby.getUwbControllerClient(context)
                gmsAvailabilityClient?.subscribeToUwbAvailability { isAvailable, reason ->
                    executor.execute { observer.onUwbStateChanged(isAvailable, reason) }
                }
            } catch (e: RuntimeException) {
                throw e
            } finally {
                Binder.restoreCallingIdentity(identity)
            }
        } else {
            try {
                val availabilityObserver =
                    object : IUwbAvailabilityObserver.Stub() {
                        override fun onUwbStateChanged(isAvailable: Boolean, reason: Int) {
                            executor.execute { observer.onUwbStateChanged(isAvailable, reason) }
                        }

                        override fun getInterfaceVersion(): Int = VERSION
                    }
                aospAvailabilityClient = iUwb?.controllerClient
                aospAvailabilityClient?.subscribeToAvailability(availabilityObserver)
            } catch (e: RuntimeException) {
                throw e
            } finally {
                Binder.restoreCallingIdentity(identity)
            }
        }
    }

    override fun clearUwbAvailabilityCallback() {
        if (isGmsDevice()) {
            gmsAvailabilityClient?.unsubscribeFromUwbAvailability()
            gmsAvailabilityClient = null
        } else {
            aospAvailabilityClient?.unsubscribeFromAvailability()
            aospAvailabilityClient = null
        }
    }
}
