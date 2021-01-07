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
package androidx.metrics.performance.test

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * This custom view is used to inject an artificial, random delay during drawing, to simulate
 * jank on the UI thread.
 */
public class MyCustomView : View {
    public constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onDraw(canvas: Canvas) {
        /**
         * Inject random delay to cause jank in the app.
         * For any given item, there should be a 30% chance of jank (>32ms), and a 2% chance of
         * extreme jank (>500ms).
         * Regular jank will be between 32 and 82ms, extreme from 500-700ms.
         */
        val probability = Math.random()
        if (probability > .7) {
            val delay: Long
            delay = if (probability > .98) {
                500 + (Math.random() * 200).toLong()
            } else {
                32 + (Math.random() * 50).toLong()
            }
            try {
                Thread.sleep(delay)
            } catch (e: Exception) {
            }
        }
        super.onDraw(canvas)
    }
}