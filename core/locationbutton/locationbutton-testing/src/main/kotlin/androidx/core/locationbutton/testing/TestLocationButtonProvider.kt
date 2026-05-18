/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.core.locationbutton.testing

import android.annotation.SuppressLint
import android.app.Activity
import android.app.permissionui.LocationButtonClient
import android.app.permissionui.LocationButtonProvider
import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.os.IBinder
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.Executor

/**
 * A test implementation of `LocationButtonProvider` which provides a fake of the LocationButton and
 * provides additional methods to allow tests to fake user interactions so that the client side
 * flows can be tested.
 */
@SuppressLint("NewApi")
public open class TestLocationButtonProvider(private val context: Context) :
    LocationButtonProvider {
    internal val sessionClientMap: MutableMap<LocationButtonSession, LocationButtonClient> =
        mutableMapOf()

    protected open fun onSessionRequestReceived(
        request: LocationButtonRequest,
        session: LocationButtonSession,
    ) {
        // Default no-op
    }

    public fun notifyPermissionResult(session: LocationButtonSession, isGranted: Boolean) {
        val client = sessionClientMap[session] ?: return
        client.onPermissionResult(isGranted)
    }

    override fun openSession(
        activity: Activity,
        hostToken: IBinder,
        displayId: Int,
        request: LocationButtonRequest,
        executor: Executor,
        client: LocationButtonClient,
    ) {
        executor.execute {
            lateinit var session: TestLocationButtonSession
            session =
                TestLocationButtonSession(activity, hostToken, displayId, 100, 50) {
                    sessionClientMap.remove(session)
                }
            sessionClientMap[session] = client
            onSessionRequestReceived(request, session)
            client.onSessionOpened(session)
        }
    }

    public companion object {
        /**
         * Construct a [TestLocationButtonProvider] that runs a fake location button session.
         *
         * @return A test only implementation of `LocationButtonProvider`.
         */
        @JvmStatic
        public fun create(): TestLocationButtonProvider {
            return TestLocationButtonProvider(
                InstrumentationRegistry.getInstrumentation().targetContext
            )
        }
    }
}
