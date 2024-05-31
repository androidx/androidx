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

package androidx.privacysandbox.sdkruntime.client.controller.impl

import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat

/** Implementation backed by [SdkSandboxManager]. */
@RequiresApi(33)
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
internal class PlatformAppOwnedSdkRegistry(context: Context) : AppOwnedSdkRegistry {

    private val sdkSandboxManager = context.getSystemService(SdkSandboxManager::class.java)

    override fun registerAppOwnedSdkSandboxInterface(
        appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat
    ) {
        val platformObj = appOwnedSdk.toAppOwnedSdkSandboxInterface()
        sdkSandboxManager.registerAppOwnedSdkSandboxInterface(platformObj)
    }

    override fun unregisterAppOwnedSdkSandboxInterface(sdkName: String) {
        sdkSandboxManager.unregisterAppOwnedSdkSandboxInterface(sdkName)
    }

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        val apiResult = sdkSandboxManager.getAppOwnedSdkSandboxInterfaces()
        return apiResult.map { AppOwnedSdkSandboxInterfaceCompat(it) }
    }
}
