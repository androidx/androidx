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

package androidx.privacysandbox.ui.macrobenchmark.testapp.target.util

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * An [ArrayAdapter] for a drop-down list that allows to disable some of its items using the
 * [isEnabledCondition] lambda. Sets the text color of enabled items to [Color.BLACK], of disabled
 * items - to [Color.GRAY].
 *
 * Uses default Android resources to style its items.
 */
class DisabledItemsArrayAdapter(
    context: Context,
    items: Array<String>,
    private val isEnabledCondition: (Int) -> Boolean,
) : ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item, items) {
    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun isEnabled(position: Int): Boolean {
        return isEnabledCondition(position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView = super.getDropDownView(position, convertView, parent) as TextView
        textView.setTextColor(if (isEnabled(position)) Color.BLACK else Color.GRAY)
        return textView
    }
}
