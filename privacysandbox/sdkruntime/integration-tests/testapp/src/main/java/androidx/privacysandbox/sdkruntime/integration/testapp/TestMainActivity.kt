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

package androidx.privacysandbox.sdkruntime.integration.testapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.integration.testaidl.ISdkApi
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TestMainActivity : Activity() {

    private lateinit var sdkSandboxManager: SdkSandboxManagerCompat

    private val loadSdkLatch = CountDownLatch(1)

    private var sdkApi: ISdkApi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdkSandboxManager = SdkSandboxManagerCompat.from(applicationContext)

        if (sdkApi == null) {
            Log.i(TAG, "Loading SDK")
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val loadedSdk = sdkSandboxManager.loadSdk(SDK_NAME, Bundle())
                    onLoadedSdk(loadedSdk)
                } catch (e: LoadSdkCompatException) {
                    Log.i(
                        TAG, "loadSdk failed with errorCode: " + e.loadSdkErrorCode +
                            " and errorMsg: " + e.message
                    )
                }
            }
        }
    }

    fun waitForSdkApiLoaded(): ISdkApi {
        loadSdkLatch.await(5, TimeUnit.SECONDS)
        return sdkApi!!
    }

    private fun onLoadedSdk(sandboxedSdk: SandboxedSdkCompat) {
        Log.i(TAG, "Loaded successfully")
        sdkApi = ISdkApi.Stub.asInterface(sandboxedSdk.getInterface())
        loadSdkLatch.countDown()
    }

    companion object {
        private const val TAG = "TestMainActivity"

        /**
         * Name of the SDK to be loaded.
         */
        private const val SDK_NAME = "androidx.privacysandbox.sdkruntime.integrationtest.sdk"
    }
}
