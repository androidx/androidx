/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.phone.interactions.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.wearable.notifications.IBridgingManagerService

/**
 * Handler for applying the notification bridging configuration when received by the service.
 */
public fun interface BridgingConfigurationHandler {
    /**
     * Apply the notification bridging configurations.
     * @param bridgingConfig The received bridging configuration
     */
    public fun applyBridgingConfiguration(bridgingConfig: BridgingConfig)
}

/**
 * Service class receiving notification bridging configurations.
 *
 * @param context  The [Context] of the application.
 * @param bridgingConfigurationHandler The handler for applying the notification bridging
 * configuration.
 */
public class BridgingManagerService(
    private val context: Context,
    private val bridgingConfigurationHandler: BridgingConfigurationHandler
) : Service() {
    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == BridgingManager.ACTION_BIND_BRIDGING_MANAGER)
            BridgingManagerServiceImpl(context, bridgingConfigurationHandler)
        else
            null
}

internal class BridgingManagerServiceImpl(
    private val context: Context,
    private val bridgingConfigurationHandler: BridgingConfigurationHandler
) : IBridgingManagerService.Stub() {

    override fun getApiVersion(): Int = IBridgingManagerService.API_VERSION

    override fun setBridgingConfig(bridgingConfigBundle: Bundle) {
        val bridgingConfig = BridgingConfig.fromBundle(bridgingConfigBundle)
        val packageName = bridgingConfig.packageName
        val senderAppPackage: String? =
            context.packageManager.getNameForUid(Binder.getCallingUid())
        require(senderAppPackage == packageName) {
            "Package invalid: $senderAppPackage not equals $packageName"
        }
        bridgingConfigurationHandler
            .applyBridgingConfiguration(bridgingConfig)
    }
}