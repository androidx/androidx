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
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.tools.integration.testsdk.MySdk
import androidx.privacysandbox.tools.integration.testsdk.MySdkFactory.wrapToMySdk

class MainActivity : AppCompatActivity() {
    internal var sdk: MySdk? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    internal suspend fun loadSdk() {
        if (this.sdk != null) return

        val sandboxManagerCompat = SdkSandboxManagerCompat.from(this)
        val sandboxedSdk =
            try {
                sandboxManagerCompat.loadSdk(
                    "androidx.privacysandbox.tools.integration.sdk",
                    Bundle.EMPTY
                )
            } catch (e: LoadSdkCompatException) {
                sandboxManagerCompat.getSandboxedSdks().first()
            }
        sdk = sandboxedSdk.getInterface()?.let { wrapToMySdk(it) }
    }
}
