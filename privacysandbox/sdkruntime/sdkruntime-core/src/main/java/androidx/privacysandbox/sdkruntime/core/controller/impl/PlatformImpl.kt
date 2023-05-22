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

package androidx.privacysandbox.sdkruntime.core.controller.impl

import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.IBinder
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

/**
 * Implementation that delegates to platform [SdkSandboxController].
 */
@RequiresApi(33)
@RequiresExtension(extension = AD_SERVICES, version = 5)
internal open class PlatformImpl(
    private val controller: SdkSandboxController
) : SdkSandboxControllerCompat.SandboxControllerImpl {

    private val appOwnedSdkProvider = AppOwnedSdkProvider.create(controller)

    override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        return controller
            .sandboxedSdks
            .map { platformSdk -> SandboxedSdkCompat(platformSdk) }
    }

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        appOwnedSdkProvider.getAppOwnedSdkSandboxInterfaces()

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        throw UnsupportedOperationException("This API only available for devices run on Android U+")
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        throw UnsupportedOperationException("This API only available for devices run on Android U+")
    }

    companion object {
        fun from(context: Context): PlatformImpl {
            val sdkSandboxController = context.getSystemService(SdkSandboxController::class.java)
            return PlatformImpl(sdkSandboxController)
        }
    }
}
