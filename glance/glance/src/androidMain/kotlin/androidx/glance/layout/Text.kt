
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
import androidx.glance.text.TextStyle

/**
 * Adds a text view to the glance view.
 */
@Composable
public fun Text(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
    ComposeNode<EmittableText, Applier>(
        factory = ::EmittableText,
        update = {
            this.set(text) { this.text = it }
            this.set(modifier) { this.modifier = it }
            this.set(style) { this.style = it }
        }
    )
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmittableText : Emittable {
    override var modifier: Modifier = Modifier
    public var text: String = ""
    public var style: TextStyle? = null

    override fun toString(): String = "EmittableText($text, style=$style, modifier=$modifier)"
}
