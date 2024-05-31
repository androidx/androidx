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
import android.view.ViewGroup.LayoutParams
import androidx.slidingpanelayout.demo.R
import androidx.slidingpanelayout.widget.SlidingPaneLayout

class SlidingPaneLayoutResizeSample : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val minSize = (resources.displayMetrics.density * 64).toInt()
        setContentView(
            SlidingPaneLayout(this).apply {
                setBackgroundColor(0xff555555.toInt())
                addView(
                    PaneView(context).apply {
                        minimumWidth = minSize
                        setBackgroundColor(0xe0aa0000.toInt())
                        layoutParams =
                            SlidingPaneLayout.LayoutParams(
                                    LayoutParams.WRAP_CONTENT,
                                    LayoutParams.MATCH_PARENT
                                )
                                .apply { weight = 1f }
                    }
                )
                addView(
                    PaneView(context).apply {
                        minimumWidth = minSize
                        setBackgroundColor(0xe00000aa.toInt())
                        layoutParams =
                            SlidingPaneLayout.LayoutParams(
                                    LayoutParams.WRAP_CONTENT,
                                    LayoutParams.MATCH_PARENT
                                )
                                .apply { weight = 1f }
                    }
                )
                isUserResizingEnabled = true
                isOverlappingEnabled = false
                setUserResizingDividerDrawable(R.drawable.divider)
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}
