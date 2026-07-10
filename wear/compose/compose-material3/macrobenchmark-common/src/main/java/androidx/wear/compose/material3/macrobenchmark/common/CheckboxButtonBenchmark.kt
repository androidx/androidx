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

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.waitForStable
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.Text

object CheckboxButtonBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val isToggled = remember { mutableStateOf(false) }
            CheckboxButton(
                checked = isToggled.value,
                onCheckedChange = { isToggled.value = !isToggled.value },
                modifier =
                    Modifier.padding(top = 50.dp).semantics {
                        this@semantics.contentDescription = TOGGLE_CHECKBOX
                    },
            ) {
                Text("Static Label")
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val check =
                device.wait(Until.findObject(By.desc(TOGGLE_CHECKBOX)), FIND_OBJECT_TIMEOUT_MS)
            repeat(6) {
                check.click()
                check.waitForStable(requireStableScreenshot = false)
            }
        }
}

object SplitCheckboxButtonBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val isToggled = remember { mutableStateOf(false) }
            SplitCheckboxButton(
                checked = isToggled.value,
                onCheckedChange = { isToggled.value = !isToggled.value },
                toggleContentDescription = TOGGLE_SPLIT_CHECKBOX,
                onContainerClick = {},
                modifier = Modifier.padding(top = 50.dp),
                containerClickLabel = "Test Click",
            ) {
                Text("Static Label")
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val switch =
                device.wait(
                    Until.findObject(By.desc(TOGGLE_SPLIT_CHECKBOX)),
                    FIND_OBJECT_TIMEOUT_MS,
                )
            repeat(6) {
                switch.click()
                switch.waitForStable(requireStableScreenshot = false)
            }
        }
}

const val TOGGLE_SPLIT_CHECKBOX = "TOGGLE_SPLIT_CHECKBOX"
const val TOGGLE_CHECKBOX = "TOGGLE_CHECKBOX"
