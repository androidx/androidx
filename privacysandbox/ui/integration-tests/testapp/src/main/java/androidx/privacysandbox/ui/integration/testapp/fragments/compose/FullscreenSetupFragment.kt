/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.testapp.fragments.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.privacysandbox.activity.client.createManagedSdkActivityLauncher
import androidx.privacysandbox.activity.client.toLauncherInfo
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants
import androidx.privacysandbox.ui.integration.testapp.R
import androidx.privacysandbox.ui.integration.testapp.fragments.BaseFragment

// TODO(b/399092069): add non-LifecycleOwner activity CUJ to the fragment.
class FullscreenSetupFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val setUpView = inflater.inflate(R.layout.fragment_fullscreen_setup, container, false)
        val radioNonBlocking = setUpView.findViewById<RadioButton>(R.id.radio_non_blocking)
        val radioLandscape = setUpView.findViewById<RadioButton>(R.id.radio_landscape)
        val radioPortrait = setUpView.findViewById<RadioButton>(R.id.radio_portrait)
        val radioEnableBackNav =
            setUpView.findViewById<RadioButton>(R.id.radio_enable_back_navigation_immediately)
        val radioEnableBackNavAfter5Seconds =
            setUpView.findViewById<RadioButton>(R.id.radio_enable_back_navigation_after_5s)

        val launchButton: Button = setUpView.findViewById(R.id.btn_launch_fullscreen_ad)
        launchButton.setOnClickListener {
            val screenOrientation =
                when {
                    radioLandscape.isChecked ->
                        SdkApiConstants.Companion.ScreenOrientation.Companion.LANDSCAPE
                    radioPortrait.isChecked ->
                        SdkApiConstants.Companion.ScreenOrientation.Companion.PORTRAIT
                    radioNonBlocking.isChecked ->
                        SdkApiConstants.Companion.ScreenOrientation.Companion.USER
                    else -> SdkApiConstants.Companion.ScreenOrientation.Companion.USER
                }

            val backNavigation =
                when {
                    radioEnableBackNav.isChecked ->
                        SdkApiConstants.Companion.BackNavigation.Companion.ENABLED
                    radioEnableBackNavAfter5Seconds.isChecked ->
                        SdkApiConstants.Companion.BackNavigation.Companion.ENABLED_AFTER_5_SECONDS
                    else -> SdkApiConstants.Companion.BackNavigation.Companion.ENABLED
                }

            val activityLauncher = requireActivity().createManagedSdkActivityLauncher({ true })
            getSdkApi()
                .launchFullscreenAd(
                    activityLauncher.toLauncherInfo(),
                    screenOrientation,
                    backNavigation,
                )
        }
        return setUpView
    }
}
