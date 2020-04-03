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

package androidx.ui.foundation.demos

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.samples.BorderSample
import androidx.ui.foundation.samples.BorderSampleWithBrush
import androidx.ui.foundation.samples.BorderSampleWithDataClass
import androidx.ui.foundation.samples.DrawBackgroundColor
import androidx.ui.foundation.samples.DrawBackgroundShapedBrush
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.unit.dp

@Composable
fun DrawModifiersDemo() {
    Row {
        Column(Modifier.weight(1f, true).padding(10.dp)) {
            BorderSample()
            Spacer(Modifier.preferredHeight(30.dp))
            BorderSampleWithBrush()
            Spacer(Modifier.preferredHeight(30.dp))
            BorderSampleWithDataClass()
        }
        Column(Modifier.weight(1f).padding(10.dp)) {
            DrawBackgroundColor()
            Spacer(Modifier.preferredHeight(30.dp))
            DrawBackgroundShapedBrush()
        }
    }
}
