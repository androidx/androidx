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

import android.app.Activity
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.IBinder
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat

/**
 * Implementation that delegates to platform [SdkSandboxController] for Android U.
 */
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
@RequiresApi(34)
internal class PlatformUDCImpl(
    private val controller: SdkSandboxController
) : PlatformImpl(controller) {

    private val compatToPlatformMap =
        hashMapOf<SdkSandboxActivityHandlerCompat, SdkSandboxActivityHandler>()

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        synchronized(compatToPlatformMap) {
            val platformHandler: SdkSandboxActivityHandler =
            compatToPlatformMap[handlerCompat]
                ?: SdkSandboxActivityHandler { platformActivity: Activity ->
                    val activityHolder = object : ActivityHolder {
                        override fun getActivity(): Activity {
                            return platformActivity
                        }
                    }
                    handlerCompat.onActivityCreated(activityHolder)
                }
            val token = controller.registerSdkSandboxActivityHandler(platformHandler)
            compatToPlatformMap[handlerCompat] = platformHandler
            return token
        }
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        synchronized(compatToPlatformMap) {
            val platformHandler: SdkSandboxActivityHandler =
                compatToPlatformMap[handlerCompat] ?: return
            controller.unregisterSdkSandboxActivityHandler(platformHandler)
            compatToPlatformMap.remove(handlerCompat)
        }
    }

    companion object {
        fun from(context: Context): PlatformImpl {
            val sdkSandboxController = context.getSystemService(SdkSandboxController::class.java)
            return PlatformUDCImpl(sdkSandboxController)
        }
    }
}