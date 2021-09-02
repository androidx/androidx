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

package androidx.glance.appwidget.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.Modifier
import androidx.glance.layout.TextStyle

/**
 * Adds a check box view to the glance view.
 */
@Composable
public fun CheckBox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    text: String = "",
    textStyle: TextStyle? = null
) {
    ComposeNode<EmittableCheckBox, Applier>(
        factory = ::EmittableCheckBox,
        update = {
            this.set(checked) { this.checked = it }
            this.set(text) { this.text = it }
            this.set(modifier) { this.modifier = it }
            this.set(textStyle) { this.textStyle = it }
        }
    )
}

internal class EmittableCheckBox : Emittable {
    override var modifier: Modifier = Modifier
    var checked: Boolean = false
    var text: String = ""
    var textStyle: TextStyle? = null

    override fun toString(): String = "EmittableCheckBox(" +
        "$text, " +
        "checked=$checked, " +
        "textStyle=$textStyle, " +
        "modifier=$modifier" +
        ")"
}