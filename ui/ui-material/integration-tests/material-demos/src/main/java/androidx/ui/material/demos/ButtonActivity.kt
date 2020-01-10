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

import android.util.Log
import androidx.compose.Composable
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.material.Button
import androidx.ui.material.ContainedButtonStyle
import androidx.ui.material.MaterialTheme
import androidx.ui.material.samples.ButtonWithTextSample
import androidx.ui.material.samples.ContainedButtonSample
import androidx.ui.material.samples.OutlinedButtonSample
import androidx.ui.material.samples.TextButtonSample

class ButtonActivity : MaterialDemoActivity() {

    @Composable
    override fun materialContent() {
        val onClick: () -> Unit = { Log.e("ButtonDemo", "onClick") }
        Center {
            Column(LayoutHeight.Fill, arrangement = Arrangement.SpaceEvenly) {
                ContainedButtonSample(onClick)

                OutlinedButtonSample(onClick)

                TextButtonSample(onClick)

                Button(
                    text = "SECONDARY COLOR",
                    onClick = onClick,
                    style = ContainedButtonStyle(MaterialTheme.colors().secondary))

                ButtonWithTextSample(onClick)

                // TODO(Andrey): Disabled button has wrong bg and text color for now.
                // Need to figure out where will we store their styling. Not a part of
                // ColorPalette right now and specs are not clear about this.
                Button("DISABLED. TODO")
            }
        }
    }
}
