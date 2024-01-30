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

package androidx.metrics.performance.janktest

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import java.util.Date

class SimpleLoggingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_layout)

        JankStats.createAndTrack(window) { volatileFrameData ->
            Log.d("MainActivity", volatileFrameData.toString())
        }

        findViewById<View>(R.id.button).setOnClickListener {
            val stateHolder = PerformanceMetricsState.getHolderForHierarchy(it).state!!
            stateHolder.putSingleFrameState("stateKey1", "stateValue")
            stateHolder.putState("stateKey2", "${Date()}")
        }
    }
}
