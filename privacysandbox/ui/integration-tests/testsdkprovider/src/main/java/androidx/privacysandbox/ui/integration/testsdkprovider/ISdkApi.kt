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

package androidx.privacysandbox.ui.integration.testsdkprovider

import android.os.Bundle
import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface ISdkApi {
    suspend fun loadAd(
        adFormat: Int,
        adType: Int,
        mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle

    // This new method is needed since we cannot add optional params to shim methods.
    suspend fun loadBannerAdForAutomatedTests(
        adFormat: Int,
        adType: Int,
        mediationOption: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
        automatedTestCallbackBundle: Bundle,
    ): Bundle

    fun requestResize(width: Int, height: Int)

    fun triggerProcessDeath()

    fun launchFullscreenAd(launcherInfo: Bundle, screenOrientation: Int, backButtonNavigation: Int)

    /**
     * Registers the In-App mediatee adapter so that it can be used by the Mediator later to show
     * ads.
     */
    fun registerInAppMediateeAdapter(mediateeAdapter: MediateeAdapterInterface)
}
