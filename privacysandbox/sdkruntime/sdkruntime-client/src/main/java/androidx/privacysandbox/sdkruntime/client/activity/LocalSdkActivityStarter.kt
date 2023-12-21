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

package androidx.privacysandbox.sdkruntime.client.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.os.BundleCompat

/**
 * Singleton helper object to start [SdkActivity].
 * Creates [Intent] with token provided by SDK.
 */
internal object LocalSdkActivityStarter {

    private const val EXTRA_ACTIVITY_TOKEN = "androidx.privacysandbox.sdkruntime.ACTIVITY_HANDLER"

    /**
     * Trying to start [SdkActivity].
     *
     * If [token] registered in [LocalSdkActivityHandlerRegistry] this method will create
     * [Intent] for starting [SdkActivity] and call [Activity.startActivity]
     *
     * @param fromActivity the [Activity] will be used to start the new [SdkActivity] by
     * calling [Activity.startActivity] against it.
     * @param token the identifier that is shared by the SDK which requests the [Activity].
     *
     * @return true if Intent was created, false if not (token wasn't registered locally).
     */
    fun tryStart(fromActivity: Activity, token: IBinder): Boolean {
        if (!LocalSdkActivityHandlerRegistry.isRegistered(token)) {
            return false
        }

        val intent = Intent(fromActivity, SdkActivity::class.java)

        val params = Bundle()
        BundleCompat.putBinder(params, EXTRA_ACTIVITY_TOKEN, token)
        intent.putExtras(params)

        fromActivity.startActivity(intent)

        return true
    }

    /**
     * Retrieve token from [Intent] used for creation [SdkActivity].
     *
     * @return token or null if [EXTRA_ACTIVITY_TOKEN] param is missing in [Intent.getExtras]
     */
    fun getTokenFromSdkActivityStartIntent(intent: Intent): IBinder? {
        val params = intent.extras ?: return null
        return BundleCompat.getBinder(params, EXTRA_ACTIVITY_TOKEN)
    }
}