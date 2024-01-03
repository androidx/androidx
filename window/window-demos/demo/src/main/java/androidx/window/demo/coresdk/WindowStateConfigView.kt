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

package androidx.window.demo.coresdk

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.window.demo.R
import androidx.window.demo.databinding.WindowStateConfigViewBinding

/** View to show a display configuration value. */
class WindowStateConfigView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val configView: TextView
    private var configValue: String? = null

    /** Whether to highlight the value when it is changed. */
    var shouldHighlightChange = false

    init {
        val viewBinding = WindowStateConfigViewBinding.inflate(LayoutInflater.from(context),
            this, true)
        configView = viewBinding.configValue
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WindowStateConfigView,
            0, 0).apply {
            try {
                getString(R.styleable.WindowStateConfigView_configName)?.let {
                    viewBinding.configName.text = it
                }
            } finally {
                recycle()
            }
        }
    }

    /** Updates the config value. */
    fun updateValue(value: String) {
        if (shouldHighlightChange && configValue != null && configValue != value) {
            // Highlight previous value if changed.
            configView.setTextColor(Color.RED)
        } else {
            configView.setTextColor(Color.BLACK)
        }
        configValue = value
        configView.text = value
    }
}
