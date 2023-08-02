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

package androidx.camera.integration.camera2.pipe.visualizations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.camera.integration.camera2.pipe.dataholders.KeyValueDataHolder
import kotlin.properties.Delegates

/** View for key value pair visualizations. Only responsible for updating the value field */
class KeyValueView(
    context: Context,
    private val keyValueDataHolder: KeyValueDataHolder,
    private val paints: Paints
) : View(context) {

    private var widthFloat by Delegates.notNull<Float>()
    private var heightFloat by Delegates.notNull<Float>()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        widthFloat = (right - left).toFloat()
        heightFloat = (bottom - top).toFloat()
    }

    /** Draws the value text and adjusts the size to fit the container */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var desiredTextSize = 20f
        val value = keyValueDataHolder.value
        val valueString = value ?: "Missing Data"

        val paint = paints.keyValueValuePaint

        paint.textSize = desiredTextSize
        val bounds = Rect()
        paint.getTextBounds(valueString, 0, valueString.length, bounds)

        if (bounds.width() > widthFloat) {
            desiredTextSize = desiredTextSize * widthFloat / bounds.width()
        }

        paint.textSize = desiredTextSize

        val bottom = heightFloat / 2 + bounds.height() / 2

        canvas.drawText(valueString, widthFloat, bottom, paint)
        postInvalidate()
    }
}
