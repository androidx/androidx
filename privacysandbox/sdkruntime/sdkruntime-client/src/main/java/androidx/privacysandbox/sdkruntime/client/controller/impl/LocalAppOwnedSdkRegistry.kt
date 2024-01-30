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

import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat

/**
 * Local implementation for platform versions without AppOwnedSdkSandboxInterface support.
 */
internal class LocalAppOwnedSdkRegistry : AppOwnedSdkRegistry {

    private val appOwnedInterfaces = HashMap<String, AppOwnedSdkSandboxInterfaceCompat>()

    override fun registerAppOwnedSdkSandboxInterface(
        appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat
    ) = synchronized(appOwnedInterfaces) {
        val interfaceName = appOwnedSdk.getName()
        if (appOwnedInterfaces.containsKey(interfaceName)) {
            throw IllegalStateException("Already registered interface of name $interfaceName")
        }
        appOwnedInterfaces[interfaceName] = appOwnedSdk
    }

    override fun unregisterAppOwnedSdkSandboxInterface(
        sdkName: String
    ): Unit = synchronized(appOwnedInterfaces) {
        appOwnedInterfaces.remove(sdkName)
    }

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        synchronized(appOwnedInterfaces) {
            return appOwnedInterfaces.values.toList()
        }
}
