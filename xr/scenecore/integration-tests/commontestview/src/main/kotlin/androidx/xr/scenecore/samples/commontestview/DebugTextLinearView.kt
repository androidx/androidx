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

package androidx.xr.scenecore.samples.commontestview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView

/**
 * A panel view that displays a list of text lines.
 *
 * <p>Each line is associated with a key and can be edited or set. The view is intended to be used
 * for debugging purposes and takes in an optional name to display at the top of the panel.
 */
class DebugTextLinearView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    private var textLines = mutableMapOf<String, AppCompatTextView>()
    private var linearLayout: LinearLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.debug_text_panel, this, true)
        linearLayout = findViewById(R.id.debugTextPanel)
        linearLayout.setBackgroundColor(Color.WHITE)
    }

    fun setName(name: String) {
        val nameTextView = findViewById<AppCompatTextView>(R.id.panelName)
        nameTextView.text = name
    }

    @SuppressLint("SetTextI18n")
    fun addLine(key: String, text: String) {
        val newTextLine = AppCompatTextView(context)
        newTextLine.text = "$key: $text"
        newTextLine.tag = key
        newTextLine.setAutoSizeTextTypeUniformWithConfiguration(
            /* autoSizeMinTextSize= */ 1,
            /* autoSizeMaxTextSize= */ 10000,
            /* autoSizeStepGranularity= */ 1,
            /* autoSizeUnit= */ TypedValue.COMPLEX_UNIT_DIP,
        )
        newTextLine.typeface = Typeface.DEFAULT
        val params = LayoutParams(LayoutParams.MATCH_PARENT, 0)
        params.weight = 1.0f
        newTextLine.layoutParams = params
        linearLayout.addView(newTextLine)
        textLines[key] = newTextLine
    }

    @SuppressLint("SetTextI18n")
    fun editLine(key: String, newText: String): Boolean {
        if (textLines.containsKey(key)) {
            textLines[key]?.text = "$key: $newText"
            return true
        }
        return false
    }

    fun setLine(key: String, newText: String) {
        if (!editLine(key, newText)) {
            addLine(key, newText)
        }
    }
}
