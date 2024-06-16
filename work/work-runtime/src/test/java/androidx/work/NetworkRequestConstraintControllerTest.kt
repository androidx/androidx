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
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowConnectivityManager

@Config(
    manifest = Config.NONE,
    maxSdk = 32,
    minSdk = 28,
    shadows = [ExtendedShadowConnectivityManager::class]
) // Robolectric uses wrong maxSdk by default b/285714232
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

        val controller = NetworkRequestConstraintController(connectivityManager)
        // doesn't bother to set it up, because it is ignored by shadow anyway.
        val request = NetworkRequest.Builder().build()
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
}

@RequiresApi(28)
@Implements(ConnectivityManager::class)
class ExtendedShadowConnectivityManager : ShadowConnectivityManager() {

    override fun registerNetworkCallback(
        request: NetworkRequest?,
        networkCallback: ConnectivityManager.NetworkCallback?,
        handler: Handler?
    ) {
        super.registerNetworkCallback(request, networkCallback, handler)
        val network = activeNetwork ?: return

        networkCallback?.onAvailable(network)
        networkCallback?.onCapabilitiesChanged(network, getNetworkCapabilities(network))
    }
}
