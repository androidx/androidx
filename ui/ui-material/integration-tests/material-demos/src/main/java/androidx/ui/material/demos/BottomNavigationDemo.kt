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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.preferredHeight
import androidx.ui.material.RadioGroup
import androidx.ui.material.samples.BottomNavigationSample
import androidx.ui.material.samples.BottomNavigationWithOnlySelectedLabelsSample
import androidx.ui.unit.dp

@Composable
fun BottomNavigationDemo() {
    var alwaysShowLabels by state { false }

    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
        RadioGroup {
            RadioGroupTextItem(
                selected = !alwaysShowLabels,
                onSelect = { alwaysShowLabels = false },
                text = "Only show labels when selected"
            )
            RadioGroupTextItem(
                selected = alwaysShowLabels,
                onSelect = { alwaysShowLabels = true },
                text = "Always show labels"
            )
        }

        Spacer(Modifier.preferredHeight(50.dp))

        if (alwaysShowLabels) {
            BottomNavigationSample()
        } else {
            BottomNavigationWithOnlySelectedLabelsSample()
        }
    }
}
