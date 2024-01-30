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

package bugs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private var counter = 0

// https://github.com/JetBrains/compose-multiplatform/issues/3778
//TODO: We should write autotest to check this sample: https://youtrack.jetbrains.com/issue/COMPOSE-488/iOS-test-on-CI-to-check-recompositions-count
val StartRecompositionCheck = Screen.Example("Start Recomposition Check") {
    val value = remember(LocalDensity.current) {
        (1..100).random()
    }
    println("Density: ${LocalDensity.current}")
    println("value is $value")

    Column {
        Spacer(Modifier.height(64.dp))
        Text("This demo should be launched using app arguments: `demo=StartRecompositionCheck`\n")
        Text("Recompositions count = ${++counter} (should be 1)")
        Text("Density = ${LocalDensity.current}")
    }
}
