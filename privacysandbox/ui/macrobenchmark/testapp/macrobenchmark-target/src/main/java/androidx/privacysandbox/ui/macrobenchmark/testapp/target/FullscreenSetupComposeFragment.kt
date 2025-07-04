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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.privacysandbox.activity.client.createManagedSdkActivityLauncher
import androidx.privacysandbox.activity.client.toLauncherInfo
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.BackNavigation
import androidx.privacysandbox.ui.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.ScreenOrientation

class FullscreenSetupComposeFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Column {
                    val screenOrientationOptions =
                        listOf(OPTION_NON_BLOCKING, OPTION_PORTRAIT, OPTION_LANDSCAPE)
                    val (selectedScreenOrientationOption, onScreenOrientationOptionSelected) =
                        remember { mutableStateOf(screenOrientationOptions[0]) }

                    ScreenOrientation(
                        screenOrientationOptions,
                        selectedScreenOrientationOption,
                        onScreenOrientationOptionSelected,
                    )

                    val backNavOptions =
                        listOf(OPTION_BACK_NAV_ENABLE, OPTION_BACK_NAV_ENABLE_AFTER_5S)
                    val (selectedBackNavOption, onBackNavOptionSelected) =
                        remember { mutableStateOf(backNavOptions[0]) }
                    BackNavigation(backNavOptions, selectedBackNavOption, onBackNavOptionSelected)

                    Button(
                        onClick = {
                            launchFullScreenAd(
                                selectedScreenOrientationOption,
                                selectedBackNavOption,
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.cta_launch_fullscreen_ad))
                    }
                }
            }
        }
    }

    @Composable
    fun ScreenOrientation(
        screenOrientationOptions: List<String>,
        selectedScreenOrientationOption: String,
        onScreenOrientationOptionSelected: (String) -> Unit,
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(R.string.label_screen_orientation),
            )
            RadioGroup(
                screenOrientationOptions,
                selectedScreenOrientationOption,
                onScreenOrientationOptionSelected,
            )
        }
    }

    @Composable
    fun BackNavigation(
        backNavOptions: List<String>,
        selectedBackNavOption: String,
        onBackNavOptionSelected: (String) -> Unit,
    ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(R.string.label_back_navigation),
            )
            RadioGroup(backNavOptions, selectedBackNavOption, onBackNavOptionSelected)
        }
    }

    @Composable
    fun RadioGroup(
        radioOptions: List<String>,
        selectedOption: String,
        onOptionSelected: (String) -> Unit,
    ) {
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier.fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = (text == selectedOption), onClick = null)
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }

    private fun launchFullScreenAd(
        screenOrientationSelected: String,
        backNavOptionSelected: String,
    ) {
        val screenOrientation =
            when (screenOrientationSelected) {
                OPTION_LANDSCAPE -> ScreenOrientation.LANDSCAPE
                OPTION_PORTRAIT -> ScreenOrientation.PORTRAIT
                OPTION_NON_BLOCKING -> ScreenOrientation.USER
                else -> ScreenOrientation.USER
            }

        val backNavigation =
            when (backNavOptionSelected) {
                OPTION_BACK_NAV_ENABLE -> BackNavigation.ENABLED
                OPTION_BACK_NAV_ENABLE_AFTER_5S -> BackNavigation.ENABLED_AFTER_5_SECONDS
                else -> BackNavigation.ENABLED
            }

        val activityLauncher = requireActivity().createManagedSdkActivityLauncher({ true })
        getSdkApi()
            .launchFullscreenAd(
                activityLauncher.toLauncherInfo(),
                screenOrientation,
                backNavigation,
            )
    }

    private companion object {
        const val OPTION_LANDSCAPE = "Landscape"
        const val OPTION_PORTRAIT = "Portrait"
        const val OPTION_NON_BLOCKING = "Non blocking"
        const val OPTION_BACK_NAV_ENABLE = "Enable"
        const val OPTION_BACK_NAV_ENABLE_AFTER_5S = "Enable after 5 seconds"
    }
}
