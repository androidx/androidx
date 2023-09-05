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

package androidx.core.haptics.demos

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.haptics.HapticManager
import androidx.core.haptics.signal.CompositionSignal.Companion.click
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick
import androidx.core.haptics.signal.WaveformSignal.Companion.off
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.signal.WaveformSignal.Companion.waveformOf

/**
 * Demonstrations of multiple haptic signal samples.
 */
class HapticDemosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.haptics_demos_activity)

        val hapticManager = HapticManager.create(this)
        findViewById<Button>(R.id.standard_click_btn).setOnClickListener {
            hapticManager.play(predefinedClick())
        }
        findViewById<Button>(R.id.scaled_click_btn).setOnClickListener {
            hapticManager.play(compositionOf(click().withAmplitudeScale(0.8f)))
        }
        findViewById<Button>(R.id.on_off_pattern_btn).setOnClickListener {
            hapticManager.play(
                waveformOf(
                    on(durationMillis = 350),
                    off(durationMillis = 250),
                    on(durationMillis = 350),
                )
            )
        }
        findViewById<Button>(R.id.repeating_waveform_btn).setOnClickListener {
            hapticManager.play(
                waveformOf(
                    on(durationMillis = 20),
                    off(durationMillis = 50),
                    on(durationMillis = 20),
                ).thenRepeat(
                    // 500ms off
                    off(durationMillis = 500),
                    // 600ms ramp up with 50% increments
                    on(durationMillis = 100, amplitude = 0.1f),
                    on(durationMillis = 100, amplitude = 0.15f),
                    on(durationMillis = 100, amplitude = 0.22f),
                    on(durationMillis = 100, amplitude = 0.34f),
                    on(durationMillis = 100, amplitude = 0.51f),
                    on(durationMillis = 100, amplitude = 0.76f),
                    // 400ms at max amplitude
                    on(durationMillis = 400, amplitude = 1f),
                )
            )
        }
    }
}
