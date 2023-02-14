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
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_NOT_TESTED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_PARTIALLY_TESTED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_PASSED
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.getLensFacingStringFromInt

class CameraValidationResultAdapter constructor(
    private val layoutInflater: LayoutInflater,
    private val cameraLensFacingMap: LinkedHashMap<String, Int>,
    private val cameraExtensionResultMap: LinkedHashMap<Pair<String, String>,
        LinkedHashMap<Int, Int>>
) : BaseAdapter() {

    override fun getCount(): Int {
        return cameraExtensionResultMap.size
    }

    override fun getItem(position: Int): MutableMap.MutableEntry<Pair<String, String>,
        LinkedHashMap<Int, Int>> {
        return cameraExtensionResultMap.entries.elementAt(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val textView: TextView = if (convertView == null) {
            val layout = android.R.layout.simple_list_item_1
            layoutInflater.inflate(layout, parent, false) as TextView
        } else {
            convertView as TextView
        }

        val item = getItem(position)
        val (testType, cameraId) = item.key

        val testResult = getTestResult(testType, cameraId)
        var backgroundResource = 0
        var iconResource = 0

        if (testResult == TEST_RESULT_PASSED) {
            backgroundResource = R.drawable.test_pass_gradient
            iconResource = R.drawable.outline_check_circle
        } else if (testResult == TEST_RESULT_FAILED) {
            backgroundResource = R.drawable.test_fail_gradient
            iconResource = R.drawable.outline_error
        } else if (testResult == TEST_RESULT_NOT_SUPPORTED) {
            backgroundResource = R.drawable.test_disable_gradient
        }

        val padding = 10
        val lensFacingName = cameraLensFacingMap[cameraId]?.let { getLensFacingStringFromInt(it) }
        textView.text = "[$testType][$cameraId][$lensFacingName]"
        textView.setPadding(padding, 0, padding, 0)
        textView.compoundDrawablePadding = padding
        textView.setBackgroundResource(backgroundResource)
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconResource, 0)
        return textView
    }

    private fun getTestResult(testType: String, cameraId: String): Int {
        var notTestedCount = 0
        var passCount = 0
        var failCount = 0

        cameraExtensionResultMap[Pair(testType, cameraId)]?.forEach {
            if (it.value == TEST_RESULT_NOT_TESTED) {
                notTestedCount++
            } else if (it.value == TEST_RESULT_PASSED) {
                passCount++
            } else if (it.value == TEST_RESULT_FAILED) {
                failCount++
            }
        }

        if (passCount == 0 && failCount == 0 && notTestedCount == 0) {
            return TEST_RESULT_NOT_SUPPORTED
        } else if (passCount != 0 && failCount == 0 && notTestedCount == 0) {
            return TEST_RESULT_PASSED
        } else if (failCount != 0 && notTestedCount == 0) {
            return TEST_RESULT_FAILED
        } else if (passCount == 0 && failCount == 0 && notTestedCount != 0) {
            return TEST_RESULT_NOT_TESTED
        } else {
            return TEST_RESULT_PARTIALLY_TESTED
        }
    }
}
