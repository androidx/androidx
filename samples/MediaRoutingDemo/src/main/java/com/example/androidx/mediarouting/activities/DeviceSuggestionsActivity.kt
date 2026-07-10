/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.mediarouting.activities

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.media.SuggestedDeviceInfo
import com.example.androidx.mediarouting.DeviceSuggestionsViewModel
import com.example.androidx.mediarouting.R

/**
 * Allows the setting and clearing of device suggestions for the app.
 *
 * @see androidx.mediarouter.media.MediaRouter.setDeviceSuggestions
 * @see androidx.mediarouter.media.MediaRouter.clearDeviceSuggestions
 */
class DeviceSuggestionsActivity : AppCompatActivity() {
    val deviceSuggestionsViewModel: DeviceSuggestionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT_FULL < Build.VERSION_CODES_FULL.BAKLAVA_1) {
            Toast.makeText(
                    this,
                    "SystemUI suggestions need API 36.1+. This device is ${Build.VERSION.SDK_INT}",
                    Toast.LENGTH_LONG,
                )
                .show()
        }
        setContent {
            MaterialTheme(
                colorScheme =
                    darkColorScheme(primary = colorResource(R.color.sample_media_router_primary))
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val suggestedDevice by
                        deviceSuggestionsViewModel.suggestedDevice.collectAsStateWithLifecycle()
                    DeviceSuggestionsLayout(
                        suggestedDevice,
                        deviceSuggestionsViewModel::onChangeSuggestedDevice,
                        deviceSuggestionsViewModel::onClearSuggestedDevice,
                    )
                }
            }
        }
    }

    @Composable
    fun DeviceSuggestionsLayout(
        suggestedDevice: SuggestedDeviceInfo?,
        onChangeSuggestedDevice: () -> Unit,
        onClearSuggestedDevice: () -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { onChangeSuggestedDevice.invoke() },
                colors =
                    ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                val buttonText =
                    if (suggestedDevice == null) stringResource(R.string.suggest_a_device)
                    else stringResource(R.string.suggest_another_device)
                Text(buttonText)
            }
            if (suggestedDevice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onClearSuggestedDevice.invoke() },
                    colors =
                        ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                ) {
                    Text(stringResource(R.string.clear_suggested_device))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                        stringResource(
                            R.string.suggested_device_format,
                            stringResource(R.string.suggested_device),
                            suggestedDevice.deviceDisplayName,
                        ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
