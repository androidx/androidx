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
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedClick

/**
 * Demo with multiple selection of haptic effect samples.
 */
class HapticSamplesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.haptic_samples_activity)

        val hapticManager = HapticManager.create(this)
        findViewById<Button>(R.id.standard_click_btn).setOnClickListener {
            hapticManager.play(PredefinedClick)
        }
    }
}
