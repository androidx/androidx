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

package androidx.privacysandbox.tools.integration.testapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.tools.integration.testsdk.MySdk
import androidx.privacysandbox.tools.integration.testsdk.MySdkFactory.wrapToMySdk
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sdk: MySdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            sdk = loadSdk()
            Log.e("Test App MainActivity", "Sum = ${sdk.doSum(42, 2)}")
        }
    }

    suspend fun loadSdk(): MySdk {
        val sandboxManagerCompat = SdkSandboxManagerCompat.from(this)
        val sandboxedSdk =
            sandboxManagerCompat.loadSdk(
                "androidx.privacysandbox.tools.integration.sdk",
                Bundle.EMPTY
            )
        return wrapToMySdk(sandboxedSdk.getInterface()!!)
    }
}
