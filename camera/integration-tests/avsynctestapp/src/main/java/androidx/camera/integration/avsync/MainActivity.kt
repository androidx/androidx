/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.avsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.core.util.Preconditions

private const val KEY_BEEP_FREQUENCY = "beep_frequency"
private const val KEY_BEEP_ENABLED = "beep_enabled"
private const val DEFAULT_BEEP_FREQUENCY = 1500
private const val DEFAULT_BEEP_ENABLED = true
private const val MIN_SCREEN_BRIGHTNESS = 0F
private const val MAX_SCREEN_BRIGHTNESS = 1F
private const val DEFAULT_SCREEN_BRIGHTNESS = 0.5F

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setScreenBrightness()
        setContent {
            App(getBeepFrequency(), getBeepEnabled())
        }
    }

    private fun getBeepFrequency(): Int {
        val frequency = intent.getStringExtra(KEY_BEEP_FREQUENCY)

        if (frequency != null) {
            try {
                return Integer.parseInt(frequency)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }

        return DEFAULT_BEEP_FREQUENCY
    }

    private fun getBeepEnabled(): Boolean {
        return intent.getBooleanExtra(KEY_BEEP_ENABLED, DEFAULT_BEEP_ENABLED)
    }

    private fun setScreenBrightness(brightness: Float = DEFAULT_SCREEN_BRIGHTNESS) {
        Preconditions.checkArgument(brightness in MIN_SCREEN_BRIGHTNESS..MAX_SCREEN_BRIGHTNESS)

        val layoutParam = window.attributes
        layoutParam.screenBrightness = brightness
        window.attributes = layoutParam
    }
}

@Composable
fun App(beepFrequency: Int, beepEnabled: Boolean) {
    MaterialTheme {
        SignalGeneratorScreen(beepFrequency, beepEnabled)
    }
}