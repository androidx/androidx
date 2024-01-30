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

package androidx.slidingpanelayout.widget.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.AT_MOST

class MeasureCountingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val _measureCallTraces = mutableListOf<Throwable>()
    val measureCallTraces: List<Throwable>
        get() = _measureCallTraces

    val measureCount: Int
        get() = _measureCallTraces.size

    fun resetMeasureTracking() {
        _measureCallTraces.clear()
    }

    /**
     * Run [block] and append [measureCallTraces] to the
     * [suppressed exceptions][Throwable.addSuppressed] of any [AssertionError] thrown for analysis
     */
    inline fun <R> assertReportingMeasureCallTraces(
        block: MeasureCountingView.() -> R
    ): R = try {
        block()
    } catch (ae: AssertionError) {
        for (trace in measureCallTraces) {
            ae.addSuppressed(trace)
        }
        throw ae
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        _measureCallTraces += Throwable("measure pass ${measureCount + 1}")

        // Default View measurement treats AT_MOST identically to EXACTLY; default to a
        // content size of 100px
        val modifiedWidthSpec = if (MeasureSpec.getMode(widthMeasureSpec) == AT_MOST) {
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec).coerceAtMost(100),
                MeasureSpec.EXACTLY
            )
        } else widthMeasureSpec
        super.onMeasure(modifiedWidthSpec, heightMeasureSpec)
    }
}
