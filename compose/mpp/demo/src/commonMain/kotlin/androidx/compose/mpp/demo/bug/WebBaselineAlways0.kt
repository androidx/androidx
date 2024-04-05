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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.onGloballyPositioned

// https://github.com/JetBrains/compose-multiplatform/issues/4078
val WebBaselineAlways0 = Screen.Example(
    "Web Baseline always 0 (ok on desktop)"
) {
    Row {
        Text(
            "Heading",
            modifier = Modifier.alignByBaseline()
                // Desktop: The heading alignment line is 66
                // Web: The heading alignment line is 0
                .onGloballyPositioned { println("The heading alignment line is ${it[FirstBaseline]}") },
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            " — Subtitle",
            modifier = Modifier.alignByBaseline()
                // Desktop: The subtitle alignment line is 31
                // Web: The subtitle alignment line is 0
                .onGloballyPositioned { println("The subtitle alignment line is ${it[FirstBaseline]}") },
            style = MaterialTheme.typography.titleSmall
        )
    }
}