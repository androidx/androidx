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

package androidx.glance.layout

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.Modifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.text.TextStyle

/**
 * Adds a button view to the glance view.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param style The style to be applied to the text in this button.
 */
@Composable
fun Button(
    text: String,
    onClick: Action,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle? = null
) {
    val finalModifier = if (enabled) modifier.clickable(onClick) else modifier
    ComposeNode<EmittableButton, Applier>(
        factory = ::EmittableButton,
        update = {
            this.set(text) { this.text = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(style) { this.style = it }
            this.set(enabled) { this.enabled = it }
        }
    )
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmittableButton : Emittable {
    override var modifier: Modifier = Modifier
    var text: String = ""
    var style: TextStyle? = null
    var enabled: Boolean = true

    override fun toString(): String = "EmittableButton('$text', enabled=$enabled, style=$style, " +
        "modifier=$modifier)"
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun EmittableButton.toEmittableText() = EmittableText().also {
    it.modifier = modifier
    it.text = text
    it.style = style
}
