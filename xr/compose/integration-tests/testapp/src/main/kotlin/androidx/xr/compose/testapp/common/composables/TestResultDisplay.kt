/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.compose.testapp.common.composables

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TestResult(val description: String, val result: Boolean)

@Composable
fun TestResultsDisplay(testResults: List<TestResult>) {
    Column {
        Text(
            text = "Tests Results",
            fontSize = 50.sp,
            color = Color(0xFF404040),
            modifier = Modifier.padding(top = 16.dp),
        )
        testResults.forEach { testResult ->
            Row {
                Text(
                    text = testResult.description + ": ",
                    fontSize = 30.sp,
                    color = Color(0xFF808080),
                    modifier = Modifier.padding(top = 16.dp),
                )
                val resultColor = if (testResult.result) Color(0xFF4CAF50) else Color.Red
                val resultPassFail = if (testResult.result) "[PASS]" else "[FAIL]"
                Text(
                    text = resultPassFail,
                    fontSize = 30.sp,
                    color = resultColor,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/**
 * A reusable utility function to add a test result to a list and log it.
 *
 * @param testResults The mutable list to add the result to.
 * @param tag The logging tag to use.
 * @param description The description of the test.
 * @param result The boolean result of the test (true for pass, false for fail).
 */
fun addTestResult(
    testResults: SnapshotStateList<TestResult>,
    tag: String,
    description: String,
    result: Boolean,
) {
    testResults.add(TestResult(description, result))
    val resultPassFail = if (result) "[PASS]" else "[FAIL]"
    Log.i(tag, "$resultPassFail $description")
}
