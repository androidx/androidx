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

package androidx.slidingpanelayout

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.animation.Interpolator
import androidx.slidingpanelayout.widget.SlidingPaneLayout

class SlidingPaneLayoutSample : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(androidx.slidingpanelayout.demo.R.layout.activity_slidingpanelayout_sample)

        val spl = findViewById<SlidingPaneLayout>(androidx.slidingpanelayout.demo.R.id.spl)
        findViewById<View>(androidx.slidingpanelayout.demo.R.id.view1).also {
            it.setOnClickListener { spl.openPane(3000, testInterpolator) }
        }

        findViewById<View>(androidx.slidingpanelayout.demo.R.id.view2).also {
            it.setOnClickListener { spl.closePane(100, testInterpolator) }
        }
    }

    private val testInterpolator =
        object : Interpolator {
            override fun getInterpolation(input: Float): Float {
                val t = input - 1f
                return t * t * t * t * t + 1f
            }
        }
}
