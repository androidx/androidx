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

package androidx.privacysandbox.sdkruntime.provider.controller.impl

import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat

/** Retrieves client app package name from [SdkSandboxController]. */
@RequiresApi(34)
internal class ClientPackageNameProvider(
    private val controller: SdkSandboxController,
    private val sdkContext: Context,
) {
    /**
     * When supported (Api 34 Extension 8) returns result from SdkSandboxController. Otherwise
     * parses data dir path to find client package name.
     */
    fun getClientPackageName(): String {
        return if (BuildCompat.AD_SERVICES_EXTENSION_INT >= 8) {
            ApiAdServicesV8.getClientPackageName(controller)
        } else {
            /**
             * At least until 34Ext8, SDK data directory path included client package name, example:
             * /data/misc_ce/0/sdksandbox/<client-package>/<sdk-package-with-random>
             */
            return sdkContext.getDataDir().getParentFile()?.name ?: ""
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    private object ApiAdServicesV8 {
        @DoNotInline
        fun getClientPackageName(controller: SdkSandboxController): String =
            controller.getClientPackageName()
    }
}
