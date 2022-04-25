/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample.embedding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.window.sample.R

open class SplitActivityB : SplitActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.root_split_activity_layout)
            .setBackgroundColor(Color.parseColor("#fff3e0"))

        if (intent.getBooleanExtra(EXTRA_LAUNCH_C_TO_SIDE, false)) {
            startActivity(Intent(this, SplitActivityC::class.java))
            // Make sure that the side activity is only launched once, as the activity may be
            // recreated when the split bounds change and we need to avoid launching another
            // instance.
            intent.removeExtra(EXTRA_LAUNCH_C_TO_SIDE)
        }
    }
}