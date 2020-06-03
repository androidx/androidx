/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.integration.test.view

import android.app.Activity
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.ui.benchmark.android.AndroidTestCase
import androidx.ui.integration.test.RandomTextGenerator
import kotlin.math.roundToInt

/**
 * Version of [androidx.ui.integration.test.core.text.TextBasicTestCase] using Android views.
 */
class AndroidTextViewTestCase(
    val textLength: Int,
    val randomTextGenerator: RandomTextGenerator
) : AndroidTestCase {

    private var fontSize = 8f

    override fun getContent(activity: Activity): ViewGroup {
        val linearLayout = LinearLayout(activity)
        val textView = TextView(activity)
        textView.text = randomTextGenerator.nextParagraph(textLength)
        textView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        textView.width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            160f,
            activity.resources.displayMetrics
        ).roundToInt()

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        linearLayout.addView(textView)
        return linearLayout
    }
}
