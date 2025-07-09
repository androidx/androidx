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

package androidx.credentials.providerevents.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.providerevents.exception.ExportCredentialsException
import androidx.credentials.providerevents.exception.GetCredentialTransferCapabilitiesException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.internal.DeviceSetupProviderFactory
import androidx.credentials.providerevents.transfer.CredentialTransferCapabilities
import androidx.credentials.providerevents.transfer.CredentialTransferCapabilitiesRequest
import androidx.credentials.providerevents.transfer.ExportCredentialsRequest
import androidx.credentials.providerevents.transfer.ExportCredentialsResponse
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse

/**
 * This service builds upon the functionality of [CredentialProviderService] by enabling support for
 * more advanced credential requests, such as device setup.
 *
 * Note that this service is distinct from [CredentialProviderService], which handles basic
 * credential saving and retrieval requests. This service is mainly used to help with user pain
 * point of credentials onboarding during device setup. When the user is setting up a new device,
 * the credential provider can assist with the onboarding by either transferring the credentials to
 * the new device, or receiving the credentials if the current device is the one doing the
 * onboarding.
 *
 * This service is bound only during the duration of an API call. To receive requests, users must
 * enable the corresponding [CredentialProviderService] from within the same package, in Android
 * Settings. This service will be invoked as part of Android onboarding and will not contain any UI
 * as part of the process.
 *
 * ## Basic Usage
 *
 * The interaction between Credential Manager and this service typically involves:
 * - The Android system forwards the request to enabled credential providers that support the
 *   requested feature.
 * - Credential providers receive the request, process it, and return an appropriate response.
 * - The Android system sends the result back to the client application.
 *
 * This flow is designed to minimize the service's lifecycle. Calls to the service are stateless. If
 * a service requires maintaining states between calls, it must implement its own state management.
 * Note that the service's process may be terminated by the Android System when unbound, such as
 * during low-memory conditions.
 *
 * ## Service Registration
 *
 * To enable Credential Manager to send requests to a provider service, the provider must:
 * - Extend this class and implement the required methods.
 * - Declare this service class within Android Manifest with corresponding intent action
 *   "androidx.credentials.DEVICE_SETUP_SERVICE_ACTION".
 */
public abstract class DeviceSetupService() : Service() {
    private val factory = DeviceSetupProviderFactory()

    final override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) {
            return null
        }
        val provider = factory.getBestAvailableProvider(intent)
        if (provider == null) {
            return null
        }
        return provider.getStubImplementation(this)
    }

    /**
     * Called when a credential provider should return credentials for device setup.
     *
     * This method should be extended by credential providers to receive credential fetch requests.
     * When the current device is used to facilitate the device setup of another device, the Android
     * System can request for the credentials stored on this device to be transferred to the new
     * device.
     *
     * @param request The request for pulling the credentials from this device.
     * @param callingAppInfo the requesting app info
     * @param callback The callback to receive the result of the credential fetching.
     */
    public abstract fun onImportCredentialsRequest(
        request: ImportCredentialsRequest,
        callingAppInfo: CallingAppInfo,
        callback: OutcomeReceiverCompat<ImportCredentialsResponse, ImportCredentialsException>,
    )

    /**
     * Called when a credential provider should receive credentials for device setup.
     *
     * This method should be extended by credential providers to receive credential push requests.
     * When the current device is being setup with a paired device, the credentials from the paired
     * device can be transferred to the current device.
     *
     * @param request The request for pushing the credentials to this device.
     * @param callingAppInfo the requesting app info
     * @param callback The callback to receive the result of the credential push.
     */
    public abstract fun onExportCredentialsRequest(
        request: ExportCredentialsRequest,
        callingAppInfo: CallingAppInfo,
        callback: OutcomeReceiverCompat<ExportCredentialsResponse, ExportCredentialsException>,
    )

    /**
     * Called when a credential provider should return the state of its credentials to the user.
     *
     * This method should be extended by credential providers to facilitate with device setup. When
     * the current device is used to facilitate the device setup of another device, the Android
     * System can request for the number of credentials that can be transferred and their size.
     * These info can be displayed to the user during the setup process.
     *
     * @param request The request for the state of the transferable credentials to this device.
     * @param callingAppInfo the requesting app info
     * @param callback The callback to receive the result of the request.
     */
    public abstract fun onGetCredentialTransferCapabilities(
        request: CredentialTransferCapabilitiesRequest,
        callingAppInfo: CallingAppInfo,
        callback:
            OutcomeReceiverCompat<
                CredentialTransferCapabilities,
                GetCredentialTransferCapabilitiesException,
            >,
    )
}
