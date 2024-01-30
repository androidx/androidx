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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.TextView

/**
 * Custom view that represents a TextView that invalidates itself during every draw which
 * is mainly used for testing invalidation paths.
 *
 * The view invalidates up to the amount of times specified in [timesToInvalidate] then it will
 * no longer invalidate upon drawing.
 */
class InvalidatedTextView : TextView {
    var timesDrawn: Int = 0
    var timesToInvalidate: Int = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun draw(canvas: Canvas) {
        if (timesDrawn < timesToInvalidate) {
            invalidate()
        }
        super.draw(canvas)
        ++timesDrawn
    }
}
