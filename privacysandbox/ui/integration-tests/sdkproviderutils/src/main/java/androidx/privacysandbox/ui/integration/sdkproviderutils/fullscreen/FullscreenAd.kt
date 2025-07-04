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

package androidx.privacysandbox.ui.integration.sdkproviderutils.fullscreen

import android.content.Context
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.privacysandbox.activity.provider.SdkActivityLauncherFactory
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.BackNavigation
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ScreenOrientation

class FullscreenAd(private val sdkContext: Context) {

    private val controller = SdkSandboxControllerCompat.from(sdkContext)

    suspend fun show(
        launcherInfo: Bundle,
        @ScreenOrientation screenOrientation: Int,
        @BackNavigation backNavigation: Int,
    ) {
        val sdkActivityLauncher = SdkActivityLauncherFactory.fromLauncherInfo(launcherInfo)
        val handler =
            object : SdkSandboxActivityHandlerCompat {

                override fun onActivityCreated(activityHolder: ActivityHolder) {
                    val activityHandler = FullscreenActivityHandler(sdkContext, activityHolder)
                    activityHandler.buildLayout(screenOrientation, backNavigation)

                    ViewCompat.setOnApplyWindowInsetsListener(
                        activityHolder.getActivity().window.decorView
                    ) { view, windowInsets ->
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                        view.updatePadding(top = insets.top)
                        WindowInsetsCompat.CONSUMED
                    }
                }
            }

        val token = controller.registerSdkSandboxActivityHandler(handler)
        val launched = sdkActivityLauncher.launchSdkActivity(token)
        if (!launched) controller.unregisterSdkSandboxActivityHandler(handler)
    }
}
