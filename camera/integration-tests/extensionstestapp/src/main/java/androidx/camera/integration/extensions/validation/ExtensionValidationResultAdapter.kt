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

package androidx.camera.integration.extensions.validation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_FAILED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_NOT_SUPPORTED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_PASSED

class ExtensionValidationResultAdapter(
    private val testType: String,
    private val layoutInflater: LayoutInflater,
    private val extensionResultMap: LinkedHashMap<Int, Pair<Int, String>>
) : BaseAdapter() {

    override fun getCount(): Int {
        return extensionResultMap.size
    }

    override fun getItem(position: Int): MutableMap.MutableEntry<Int, Pair<Int, String>> {
        return extensionResultMap.entries.elementAt(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val textView: TextView
        val detailsView: TextView

        val v = if (convertView == null) {
            val layout = android.R.layout.simple_list_item_2
            val view = layoutInflater.inflate(layout, parent, false)
            textView = view.findViewById(android.R.id.text1)
            detailsView = view.findViewById(android.R.id.text2)
            view
        } else {
            textView = convertView.findViewById(android.R.id.text1)
            detailsView = convertView.findViewById(android.R.id.text2)
            convertView
        }

        val item = getItem(position)

        var backgroundResource = 0
        var iconResource = 0

        when (item.value.first) {
            TEST_RESULT_PASSED -> {
                backgroundResource = R.drawable.test_pass_gradient
                iconResource = R.drawable.outline_check_circle
            }
            TEST_RESULT_FAILED -> {
                backgroundResource = R.drawable.test_fail_gradient
                iconResource = R.drawable.outline_error
            }
            TEST_RESULT_NOT_SUPPORTED -> {
                backgroundResource = R.drawable.test_disable_gradient
            }
        }

        val padding = 10
        textView.text = TestResults.getExtensionModeStringFromId(testType, item.key)
        textView.setPadding(padding, 0, padding, 0)
        textView.compoundDrawablePadding = padding
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconResource, 0)

        detailsView.text = item.value.second
        detailsView.setPadding(padding, 0, padding, 0)

        v.setBackgroundResource(backgroundResource)
        return v
    }
}
