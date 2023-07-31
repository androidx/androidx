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

package androidx.core.performance.testapp

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView

/** Sample Media Performance Class activity. */
class MpcActivity : Activity() {

    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mpc)
        resultTextView = findViewById(R.id.resultTextView)
    }

    fun doSomething(view: View) {
        resultTextView.text = view.context.getString(getExperienceStringId())
    }

    private fun getExperienceStringId(): Int {
        return R.string.mpc_0_experience_string
    }
}
