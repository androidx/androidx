/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.view

import android.content.Context
import android.view.View
import android.widget.OverScroller

/**
 * This Scroller applies relative changes to the View's scroll position ([View.scrollBy]), so that
 * it doesn't override any scroll change coming from another source
 */
internal class RelativeScroller(ctx: Context?) : OverScroller(ctx) {
    private var prevX = 0
    private var prevY = 0

    fun reset() {
        prevY = 0
        prevX = 0
    }

    override fun fling(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
    ) {
        reset()
        super.fling(
            0,
            0,
            velocityX,
            velocityY,
            minX - startX,
            maxX - startX,
            minY - startY,
            maxY - startY,
        )
    }

    override fun fling(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        overX: Int,
        overY: Int,
    ) {
        reset()
        super.fling(
            0,
            0,
            velocityX,
            velocityY,
            minX - startX,
            maxX - startX,
            minY - startY,
            maxY - startY,
            overX,
            overY,
        )
    }

    fun apply(v: View) {
        val x = currX - prevX
        val y = currY - prevY
        v.scrollBy(x, y)
        prevX = currX
        prevY = currY
    }
}
