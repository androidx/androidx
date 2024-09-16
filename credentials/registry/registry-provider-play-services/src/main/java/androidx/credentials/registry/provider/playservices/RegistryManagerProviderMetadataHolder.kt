/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.provider.playservices

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.RestrictTo

/**
 * Metadata holder service for the purpose of defining the class that implements the
 * [androidx.credentials.registry.provider.RegistryManagerProvider] interface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RegistryManagerProviderMetadataHolder : Service() {
    // Binder given to clients.
    private val binder = LocalBinder()

    /**
     * Class used for the client Binder. Because we know this service always runs in the same
     * process as its clients, we don't need to deal with IPC.
     */
    private inner class LocalBinder : Binder() {
        // Return this instance of CredentialProviderMetadataHolder so clients
        // can call public methods.
        fun getService(): RegistryManagerProviderMetadataHolder =
            this@RegistryManagerProviderMetadataHolder
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}
