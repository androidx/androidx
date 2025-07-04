/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.work.impl.constraints.controllers

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkInfo.Companion.STOP_REASON_CONSTRAINT_CONNECTIVITY
import androidx.work.impl.constraints.ConstraintsState
import androidx.work.impl.constraints.NetworkRequestConstraintController
import androidx.work.impl.constraints.WorkConstraintsTracker
import androidx.work.impl.model.WorkSpec
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class NetworkRequestConstraintControllerTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val controller =
        NetworkRequestConstraintController(connManager = connectivityManager, timeoutMs = 1000)

    @Test
    fun testRequestWifi() =
        runBlockingWithWifi(enabled = true) {
            val workConstraintsTracker = WorkConstraintsTracker(listOf(controller))
            val state =
                async(Dispatchers.IO) {
                    workConstraintsTracker.track(createWorkSpecWithWifiConstraint("A")).first()
                }
            assertThat(state.await()).isEqualTo(ConstraintsState.ConstraintsMet)
        }

    @Test
    fun testRequestWifiTimeout() =
        runBlockingWithWifi(enabled = false) {
            val workConstraintsTracker = WorkConstraintsTracker(listOf(controller))
            val state =
                async(Dispatchers.IO) {
                    workConstraintsTracker.track(createWorkSpecWithWifiConstraint("A")).first()
                }
            assertThat(state.await())
                .isEqualTo(ConstraintsState.ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
        }

    @Test
    fun testTooManyTrackers() =
        runBlockingWithWifi(enabled = true) {
            // Current OS limit of network callback is 100 per app, we register more to test
            // https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Connectivity/service/src/com/android/server/ConnectivityService.java;bpv=0;bpt=1?q=MAX_NETWORK_REQUESTS_PER_UID&sq=&ss=android
            val states =
                List(120) {
                    val spec = createWorkSpecWithWifiConstraint("$it")
                    val workConstraintsTracker = WorkConstraintsTracker(listOf(controller))
                    async(Dispatchers.IO) { workConstraintsTracker.track(spec).first() }
                }
            states.awaitAll()
        }

    private fun isWifiConnected(): Boolean {
        val current = connectivityManager.activeNetwork
        val currentCapabilities = connectivityManager.getNetworkCapabilities(current)
        return currentCapabilities != null &&
            currentCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            currentCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            currentCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun runBlockingWithWifi(
        enabled: Boolean,
        block: suspend CoroutineScope.() -> Unit,
    ): Unit = runBlocking {
        val initialState = isWifiConnected()
        toggleWifi(enabled)
        try {
            block()
        } finally {
            toggleWifi(initialState)
        }
    }

    private suspend fun toggleWifi(enable: Boolean) {
        // toggle wifi via adb shell cmd
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand("svc wifi ${if (enable) "enable" else "disable"}")

        // wait for toggle
        withTimeoutOrNull(5.seconds) {
            suspendCancellableCoroutine<Unit> { cont ->
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            if (enable && cont.isActive) cont.resume(Unit)
                        }

                        override fun onLost(network: Network) {
                            if (!enable && cont.isActive) cont.resume(Unit)
                        }
                    }
                connectivityManager.registerNetworkCallback(createWifiNetworkRequest(), callback)
                if (enable == isWifiConnected() && cont.isActive) {
                    // already enabled / disabled
                    cont.resume(Unit)
                }
                cont.invokeOnCancellation {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }
        } ?: throw AssumptionViolatedException("Timeout waiting for wifi toggle.")
    }

    private fun createWorkSpecWithWifiConstraint(id: String): WorkSpec {
        return WorkSpec(id, "Worker$id")
            .copy(
                constraints =
                    Constraints.Builder()
                        .setRequiredNetworkRequest(
                            createWifiNetworkRequest(),
                            NetworkType.CONNECTED,
                        )
                        .build()
            )
    }

    private fun createWifiNetworkRequest() =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
}
