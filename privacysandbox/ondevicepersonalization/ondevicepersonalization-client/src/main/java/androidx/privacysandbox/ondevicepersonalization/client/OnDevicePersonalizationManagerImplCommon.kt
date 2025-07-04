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

package androidx.privacysandbox.ondevicepersonalization.client

import android.adservices.ondevicepersonalization.SurfacePackageToken
import android.annotation.SuppressLint
import android.os.Build
import android.os.IBinder
import android.os.ext.SdkExtensions
import android.view.SurfaceControlViewHost
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.core.os.asOutcomeReceiver
import kotlinx.coroutines.suspendCancellableCoroutine

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 12)
@RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 12)
public open class OnDevicePersonalizationManagerImplCommon(
    private val mOdpManager:
        android.adservices.ondevicepersonalization.OnDevicePersonalizationManager
) : OnDevicePersonalizationManager() {
    @DoNotInline
    override suspend fun executeInIsolatedService(
        executeInIsolatedServiceRequest: ExecuteInIsolatedServiceRequest
    ): ExecuteInIsolatedServiceResponse {
        val result =
            suspendCancellableCoroutine<
                android.adservices.ondevicepersonalization.OnDevicePersonalizationManager.ExecuteResult
            > { continuation ->
                mOdpManager.execute(
                    executeInIsolatedServiceRequest.service,
                    executeInIsolatedServiceRequest.appParams,
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        return ExecuteInIsolatedServiceResponse(surfacePackageToken = result.surfacePackageToken)
    }

    @DoNotInline
    override suspend fun requestSurfacePackage(
        surfacePackageToken: SurfacePackageToken,
        surfaceViewHostToken: IBinder,
        displayId: Int,
        width: Int,
        height: Int,
    ): SurfaceControlViewHost.SurfacePackage =
        suspendCancellableCoroutine<SurfaceControlViewHost.SurfacePackage> { continuation ->
            mOdpManager.requestSurfacePackage(
                surfacePackageToken,
                surfaceViewHostToken,
                displayId,
                width,
                height,
                Runnable::run,
                continuation.asOutcomeReceiver(),
            )
        }
}
