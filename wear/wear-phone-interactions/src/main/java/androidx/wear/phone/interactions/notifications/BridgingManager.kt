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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.wearable.notifications.IBridgingManagerService

/**
 * APIs to enable/disable notification bridging at runtime.
 *
 *
 * Using a BridgingManager object, you can set a bridging mode, and optionally set tags for
 * notifications that are exempt from the bridging mode. Specifically, create a [BridgingConfig]
 * object and set is as shown in the example usages below:
 *
 *
 *  * Disable bridging at runtime:
 * ```
 * BridgingManager.fromContext(context).setConfig(
 *     new BridgingConfig.Builder(context, false).build()
 * );
 * ```
 *
 *  * Disable bridging at runtime except for the tags "foo" and "bar":
 * ```
 * BridgingManager.fromContext(context).setConfig(
 *     new BridgingConfig.Builder(context, false)
 *         .addExcludedTag("foo")
 *         .addExcludedTag("bar")
 *         .build()
 * );
 * ```
 *
 *  * Disable bridging at runtime except for the tags "foo" and "bar" and "baz":
 * ```
 * BridgingManager.fromContext(context).setConfig(
 *     new BridgingConfig.Builder(context, false)
 *         .addExcludedTags(Arrays.asList("foo", "bar", "baz"))
 *         .build()
 *  );
 * ```
 *
 *  * Adding a bridge tag to a notification posted on a phone:
 * ```
 * NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
 * // ... set other fields ...
 *     .extend(
 *         new NotificationCompat.WearableExtender()
 *             .setBridgeTag("foo")
 *      );
 * Notification notification = notificationBuilder.build();
 * ```
 *
 * See also:
 * * [BridgingConfig.Builder.addExcludedTag]
 * * [BridgingConfig.Builder.addExcludedTags]
 * * [androidx.core.app.NotificationCompat.WearableExtender.setBridgeTag]
 */
public class BridgingManager private constructor(private val context: Context) {
    /**
     * Sets the BridgingConfig object.
     *
     * @param bridgingConfig The BridgingConfig object.
     *
     * @throws RuntimeException if the service binding is failed.
     */
    @SuppressLint("SyntheticAccessor")
    public fun setConfig(bridgingConfig: BridgingConfig) {
        require(isWearableDevice(context)) { "API only supported on wearable devices" }
        val connection = BridgingConfigServiceConnection(context, bridgingConfig)
        val intent = Intent(ACTION_BIND_BRIDGING_MANAGER)
        intent.setPackage(BRIDGING_CONFIG_SERVICE_PACKAGE)
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            context.unbindService(connection)
            throw RuntimeException("BridgingManager: Failed to bind service")
        }
    }

    /** Class responsible for sending the bridge mode to ClockworkHome.  */
    private class BridgingConfigServiceConnection internal constructor(
        private val context: Context,
        bridgingConfig: BridgingConfig
    ) : ServiceConnection {
        private val bundle: Bundle = bridgingConfig.toBundle(context)

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            val service: IBridgingManagerService = IBridgingManagerService.Stub.asInterface(binder)
            try {
                service.setBridgingConfig(bundle)
            } catch (e: RemoteException) {
                throw e.cause!!
            }
            context.unbindService(this)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            context.unbindService(this)
        }
    }

    public companion object {
        internal const val ACTION_BIND_BRIDGING_MANAGER =
            "android.support.wearable.notifications.action.BIND_BRIDGING_MANAGER"

        private const val BRIDGING_CONFIG_SERVICE_PACKAGE = "com.google.android.wearable.app"

        /**
         * Create a BridgingManager instance with the passed in Context.
         * @param context Context object.
         */
        @JvmStatic
        public fun fromContext(context: Context): BridgingManager = BridgingManager(context)

        private fun isWearableDevice(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }
}