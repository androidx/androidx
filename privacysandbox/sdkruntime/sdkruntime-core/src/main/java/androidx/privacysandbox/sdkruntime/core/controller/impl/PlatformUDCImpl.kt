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

package androidx.privacysandbox.sdkruntime.core.controller.impl

import android.app.Activity
import android.app.Application
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.ext.SdkExtensions
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat

/**
 * Implementation that delegates to platform [SdkSandboxController] for Android U.
 */
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
@RequiresApi(34)
internal class PlatformUDCImpl(
    private val controller: SdkSandboxController
) : PlatformImpl(controller) {

    private val compatToPlatformMap =
        hashMapOf<SdkSandboxActivityHandlerCompat, SdkSandboxActivityHandler>()

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        synchronized(compatToPlatformMap) {
            val platformHandler: SdkSandboxActivityHandler =
                compatToPlatformMap[handlerCompat]
                ?: SdkSandboxActivityHandler { platformActivity: Activity ->
                    handlerCompat.onActivityCreated(ActivityHolderImpl(platformActivity))
                }
            val token = controller.registerSdkSandboxActivityHandler(platformHandler)
            compatToPlatformMap[handlerCompat] = platformHandler
            return token
        }
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        synchronized(compatToPlatformMap) {
            val platformHandler: SdkSandboxActivityHandler =
                compatToPlatformMap[handlerCompat] ?: return
            controller.unregisterSdkSandboxActivityHandler(platformHandler)
            compatToPlatformMap.remove(handlerCompat)
        }
    }

    internal class ActivityHolderImpl(
        private val platformActivity: Activity
    ) : ActivityHolder {
        private val dispatcher = OnBackPressedDispatcher {}
        private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

        init {
            // TODO(b/276315438) Set android:enableOnBackInvokedCallback="true" when
            //  creating the manifest file
            dispatcher.setOnBackInvokedDispatcher(platformActivity.onBackInvokedDispatcher)
            proxyLifeCycleEvents()
        }

        override fun getActivity(): Activity {
            return platformActivity
        }

        override fun getOnBackPressedDispatcher(): OnBackPressedDispatcher {
            return dispatcher
        }

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        private fun proxyLifeCycleEvents() {
            val callback = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }

                override fun onActivityStarted(p0: Activity) {
                    lifecycleRegistry.currentState = Lifecycle.State.STARTED
                }

                override fun onActivityResumed(p0: Activity) {
                    lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                }

                override fun onActivityPaused(p0: Activity) {
                    lifecycleRegistry.currentState = Lifecycle.State.STARTED
                }

                override fun onActivityStopped(p0: Activity) {
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }

                override fun onActivityDestroyed(p0: Activity) {
                    lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                }

                override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
                    // No need for proxying
                }
            }
            platformActivity.registerActivityLifecycleCallbacks(callback)
        }
    }

    companion object {
        fun from(context: Context): PlatformImpl {
            val sdkSandboxController = context.getSystemService(SdkSandboxController::class.java)
            return PlatformUDCImpl(sdkSandboxController)
        }
    }
}