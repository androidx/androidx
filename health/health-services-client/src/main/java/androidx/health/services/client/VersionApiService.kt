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

package androidx.health.services.client

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.health.services.client.impl.IVersionApiService
import androidx.health.services.client.impl.IpcConstants

/** Service that allows querying the canonical SDK version used to compile this app. */
@RestrictTo(Scope.LIBRARY)
public class VersionApiService : Service() {

    private val stub: VersionApiServiceStub = VersionApiServiceStub()

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action != IpcConstants.VERSION_API_BIND_ACTION) {
            Log.w(TAG, "Bind request with invalid action [${intent?.action}]")
            return null
        }

        return stub
    }

    internal class VersionApiServiceStub : IVersionApiService.Stub() {
        override fun getVersionApiServiceVersion(): Int =
            VersionApiService.VERSION_API_SERVICE_VERSION

        override fun getSdkVersion(): Int = VersionApiService.CANONICAL_SDK_VERSION
    }

    private companion object {
        private const val TAG = "VersionApiService"

        /**
         * API version of _this_ service's AIDL interface. Should be incremented every time a new
         * method is added.
         *
         * Note: This is also defined within the IVersionApiService AIDL file. However, we cannot
         * update the value there due to migrating to stableAidl, so this one must be preferred.
         */
        private const val VERSION_API_SERVICE_VERSION = 1

        /**
         * Version of the SDK as a whole. Should be incremented on each release, regardless of
         * whether the API surface has changed.
         *
         * Note: This is also defined within the IVersionApiService AIDL file. However, we cannot
         * update the value there due to migrating to stableAidl, so this one should be preferred.
         */
        private const val CANONICAL_SDK_VERSION = 28
    }
}
