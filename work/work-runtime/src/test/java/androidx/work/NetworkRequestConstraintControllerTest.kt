/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.work.WorkInfo.Companion.STOP_REASON_CONSTRAINT_CONNECTIVITY
import androidx.work.impl.constraints.ConstraintsState
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
import androidx.work.impl.constraints.ConstraintsState.ConstraintsNotMet
import androidx.work.impl.constraints.NetworkRequestConstraintController
import androidx.work.impl.model.WorkSpec
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities

@Config(
    manifest = Config.NONE,
    maxSdk = 30, // On APIs 31+ the Robolectric impl of canBeSatisfiedBy is broken.
    minSdk = 28,
    shadows = [ExtendedShadowConnectivityManager::class],
)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class NetworkRequestConstraintControllerTest {

    // First implement your own ConnectivityManager, then test NetworkRequestConstraintController
    // against it. What could possibly go wrong?
    // Integrations tests don't give us any better testing story, there ConnectivityManager
    // should be similarly mocked, thus it is just simply inferior version of shadows.
    @Test
    fun kindaButNotReallyATest() {
        val connectivityManager =
            getApplicationContext<Context>().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val connManagerShadow =
            Shadow.extract<ExtendedShadowConnectivityManager>(connectivityManager)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).apply {
            addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        connManagerShadow.setNetworkCapabilities(connectivityManager.activeNetwork, capabilities)

        val controller = NetworkRequestConstraintController(connectivityManager)
        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        val constraints =
            Constraints.Builder().setRequiredNetworkRequest(request, NetworkType.CONNECTED).build()
        runBlocking {
            val results = mutableListOf<ConstraintsState>()
            val deferred = CompletableDeferred<Unit>()
            val job = launch {
                controller.track(constraints).take(2).collectIndexed { index, value ->
                    results.add(value)
                    if (index == 0) {
                        deferred.complete(Unit)
                    }
                }
            }
            withTimeout(1000) {
                deferred.await()
                connManagerShadow.networkCallbacks.forEach {
                    it.onLost(connectivityManager.activeNetwork!!)
                }
                job.join()
            }

            assertThat(results)
                .isEqualTo(
                    listOf(ConstraintsMet, ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
                )
        }
    }

    @Test
    fun testInitialValueIfNoNetwork() {
        val connectivityManager =
            getApplicationContext<Context>().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val controller = NetworkRequestConstraintController(connectivityManager, 0)
        val connManagerShadow =
            Shadow.extract<ExtendedShadowConnectivityManager>(connectivityManager)
        connManagerShadow.setDefaultNetworkActive(false)
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkRequest(NetworkRequest.Builder().build(), NetworkType.CONNECTED)
                .build()
        runBlocking {
            val constraintsState = controller.track(constraints).first()
            assertThat(constraintsState)
                .isEqualTo(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
        }
    }

    @Test
    fun testIsCurrentlyConstrained() {
        val connectivityManager =
            getApplicationContext<Context>().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val controller = NetworkRequestConstraintController(connectivityManager, 0)
        val workSpec = WorkSpec(id = UUID.randomUUID().toString(), workerClassName = "Foo")
        assertThat(controller.isCurrentlyConstrained(workSpec)).isFalse()
    }

    @Test
    @Config(minSdk = 30)
    fun testSameConstraintDifferentTracker() = runTest {
        val connectivityManager =
            getApplicationContext<Context>().getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val controller = NetworkRequestConstraintController(connectivityManager)
        @Suppress("DEPRECATION") // due to NetworkInfo but that's what Robolectric needs
        val mobileNetwork = connectivityManager.activeNetworkInfo

        val connManagerShadow =
            Shadow.extract<ExtendedShadowConnectivityManager>(connectivityManager)
        connManagerShadow.setActiveNetworkInfo(null) // start with no network

        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).apply {
            addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        fun buildConstraint() =
            Constraints.Builder()
                .setRequiredNetworkRequest(
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                    NetworkType.CONNECTED,
                )
                .build()

        val constraints = listOf(buildConstraint(), buildConstraint())
        val initialValueBarriers = listOf(CompletableDeferred<Unit>(), CompletableDeferred<Unit>())
        val asyncResults =
            constraints.mapIndexed { index, constraint ->
                async(Dispatchers.IO) {
                    val result = CompletableDeferred<ConstraintsState>()
                    controller.track(constraints[index]).take(2).collectIndexed { i, state ->
                        when (i) {
                            // initial value is ConstraintNotMet due timeout since there is
                            // no network
                            0 -> initialValueBarriers[index].complete(Unit)
                            // second value is the one we are interested on emitted by the test
                            // network callback invocation
                            1 -> result.complete(state)
                            else -> error("Received too many results")
                        }
                    }
                    result.await()
                }
            }
        initialValueBarriers.awaitAll() // await for async initial values

        connManagerShadow.setActiveNetworkInfo(mobileNetwork)
        connManagerShadow.networkCallbacks.forEach {
            it.onCapabilitiesChanged(connectivityManager.activeNetwork!!, capabilities)
        }

        val results = asyncResults.awaitAll()
        assertThat(results).containsExactly(ConstraintsMet, ConstraintsMet)
    }
}

@RequiresApi(28)
@Implements(ConnectivityManager::class)
class ExtendedShadowConnectivityManager : ShadowConnectivityManager() {

    override fun registerNetworkCallback(
        request: NetworkRequest?,
        networkCallback: ConnectivityManager.NetworkCallback?,
        handler: Handler?,
    ) {
        super.registerNetworkCallback(request, networkCallback, handler)
        val network = activeNetwork ?: return

        networkCallback?.onAvailable(network)
        networkCallback?.onCapabilitiesChanged(network, getNetworkCapabilities(network))
    }

    override fun registerDefaultNetworkCallback(
        networkCallback: ConnectivityManager.NetworkCallback?
    ) {
        super.registerDefaultNetworkCallback(networkCallback)
        val network = activeNetwork ?: return

        networkCallback?.onAvailable(network)
        networkCallback?.onCapabilitiesChanged(network, getNetworkCapabilities(network))
    }
}
