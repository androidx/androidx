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

import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.integration.testaidl.IAppSdk
import androidx.privacysandbox.sdkruntime.integration.testaidl.ISdkApi
import kotlinx.coroutines.launch

class TestMainActivity : AppCompatActivity() {

    lateinit var api: TestAppApi
    private lateinit var logView: TextView

    private val appOwnedSdk =
        AppOwnedSdkSandboxInterfaceCompat(
            name = "AppOwnedSdk",
            version = 42,
            binder = AppOwnedSdk()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        api = TestAppApi(applicationContext)

        logView = findViewById(R.id.logView)
        logView.setMovementMethod(ScrollingMovementMethod())

        setupLoadSdkButton()
        setupUnloadSdkButton()
        setupRegisterAppSdkButton()
        setupUnregisterAppSdkButton()
        setupGetSandboxedSdksButton()
        setupGetAppSdksButton()
    }

    private fun addLogMessage(message: String) {
        Log.i(TAG, message)
        logView.append(message + System.lineSeparator())
    }

    private fun setupLoadSdkButton() {
        val loadSdkButton = findViewById<Button>(R.id.loadSdkButton)
        loadSdkButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    addLogMessage("Loading TestSDK...")
                    val testSdk = api.loadTestSdk()
                    addLogMessage("TestSDK Message: " + testSdk.getMessage())
                    addLogMessage("Successfully loaded TestSDK")
                } catch (ex: LoadSdkCompatException) {
                    addLogMessage("Failed to load TestSDK: " + ex.message)
                }
            }
        }
    }

    private fun setupUnloadSdkButton() {
        val unloadSdkButton = findViewById<Button>(R.id.unloadSdkButton)
        unloadSdkButton.setOnClickListener {
            api.unloadTestSdk()
            addLogMessage("Unloaded TestSDK")
        }
    }

    private fun setupRegisterAppSdkButton() {
        val registerAppSdkButton = findViewById<Button>(R.id.registerAppSdkButton)
        registerAppSdkButton.setOnClickListener {
            try {
                addLogMessage("Registering AppOwnedSdk...")
                api.registerAppOwnedSdk(appOwnedSdk)
                addLogMessage("Successfully registered AppOwnedSdk")
            } catch (ex: Throwable) {
                addLogMessage("Failed to register AppOwnedSdk: " + ex.message)
            }
        }
    }

    private fun setupUnregisterAppSdkButton() {
        val unregisterAppSdkButton = findViewById<Button>(R.id.unregisterAppSdkButton)
        unregisterAppSdkButton.setOnClickListener {
            api.unregisterAppOwnedSdk(appOwnedSdk.getName())
            addLogMessage("Unregistered AppOwnedSdk")
        }
    }

    private fun setupGetSandboxedSdksButton() {
        val getSandboxedSdksButton = findViewById<Button>(R.id.getSandboxedSdksButton)
        getSandboxedSdksButton.setOnClickListener {
            val sdks = api.getSandboxedSdks()
            addLogMessage("GetSandboxedSdks results (${sdks.size}):")
            sdks.forEach {
                addLogMessage("   SDK Package: ${it.getSdkInfo()?.name}")
                addLogMessage("   SDK Version: ${it.getSdkInfo()?.version}")
                val testSdk = toTestSdk(it.getInterface())
                if (testSdk != null) {
                    addLogMessage("   SDK Message: ${testSdk.getMessage()}")
                }
            }
        }
    }

    private fun setupGetAppSdksButton() {
        val getAppSdksButton = findViewById<Button>(R.id.getAppSdksButton)
        getAppSdksButton.setOnClickListener {
            val sdks = api.getAppOwnedSdks()
            addLogMessage("GetAppSdks results (${sdks.size}):")
            sdks.forEach {
                addLogMessage("   AppOwned SDK Package: ${it.getName()}")
                addLogMessage("   AppOwned SDK Version: ${it.getVersion()}")
                val appOwnedSdk = toAppOwnedSdk(it.getInterface())
                if (appOwnedSdk != null) {
                    addLogMessage("   AppOwned SDK Message: ${appOwnedSdk.getMessage(42)}")
                }
            }
        }
    }

    private fun toTestSdk(sdkInterface: IBinder?): ISdkApi? {
        return if (ISdkApi.DESCRIPTOR == sdkInterface?.interfaceDescriptor) {
            ISdkApi.Stub.asInterface(sdkInterface)
        } else {
            null
        }
    }

    private fun toAppOwnedSdk(appInterface: IBinder?): IAppSdk? {
        return if (IAppSdk.DESCRIPTOR == appInterface?.interfaceDescriptor) {
            IAppSdk.Stub.asInterface(appInterface)
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "TestMainActivity"
    }
}
