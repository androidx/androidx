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

package androidx.core.performance.testlib

import android.app.Activity
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.performance.DevicePerformance

/** Sample Media Performance Class Activity.
 *
 * The user experience is different for each MPC.  In this sample it is just a text string.
 */
class MpcActivity : Activity() {

    private lateinit var resultTextView: TextView
    private lateinit var devicePerformance: DevicePerformance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mpc)
        resultTextView = findViewById(R.id.resultTextView)
        devicePerformance = (application as HasDevicePerformance).getDevicePerformance()
    }

    fun onClickDoSomething(view: View) {
        resultTextView.text = view.context.getString(getExperienceStringId())
    }

    private fun getExperienceStringId(): Int {
        when {
            devicePerformance.mediaPerformanceClass >= VERSION_CODES.TIRAMISU -> {
                return R.string.mpc_33_experience_string
            }
            devicePerformance.mediaPerformanceClass == VERSION_CODES.S -> {
                return R.string.mpc_31_experience_string
            }
            devicePerformance.mediaPerformanceClass == VERSION_CODES.R -> {
                return R.string.mpc_30_experience_string
            }
            devicePerformance.mediaPerformanceClass == VERSION_CODES.Q -> {
                return R.string.mpc_29_experience_string
            }
            else -> {
                return R.string.mpc_0_experience_string
            }
        }
    }
}
