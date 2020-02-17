/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.ui.core.Text
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.samples.SimpleSnackbar
import androidx.ui.material.samples.SlotsSnackbar
import androidx.ui.unit.dp

class SnackbarActivity : MaterialDemoActivity() {
    @Composable
    override fun materialContent() {
        Column(LayoutPadding(start = 12.dp, end = 12.dp)) {
            val textSpacing = LayoutPadding(top = 12.dp, bottom = 12.dp)
            Text("Default Snackbar", modifier = textSpacing)
            SimpleSnackbar()
            Text(
                "Snackbar with custom action color with long text",
                modifier = textSpacing
            )
            SlotsSnackbar()
        }
    }
}