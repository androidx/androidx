/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.basic

import androidx.ui.Type
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.text.TextDirection
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.InheritedWidget
import androidx.ui.widgets.framework.Widget

// / A widget that determines the ambient directionality of text and
// / text-direction-sensitive render objects.
// /
// / For example, [Padding] depends on the [Directionality] to resolve
// / [EdgeInsetsDirectional] objects into absolute [EdgeInsets] objects.
// /
// / Ctor comment:
// / Creates a widget that determines the directionality of text and
// / text-direction-sensitive render objects.
// /
// / The [textDirection] and [child] arguments must not be null.
class Directionality(
    key: Key,
        // / The text direction for this subtree.
    val textDirection: TextDirection,
    child: Widget
) : InheritedWidget(key = key, child = child) {

    init {
        assert(child != null)
    }

    companion object {
        // / The text direction from the closest instance of this class that encloses
        // / the given context.
        // /
        // / If there is no [Directionality] ancestor widget in the tree at the given
        // / context, then this will return null.
        // /
        // / Typical usage is as follows:
        // /
        // / ```dart
        // / TextDirection textDirection = Directionality.of(context);
        // / ```
        fun of(context: BuildContext): TextDirection {
            val widget = context.inheritFromWidgetOfExactType(
                    Type(Directionality::class.java)) as Directionality
            return widget?.textDirection
        }
    }

    override fun updateShouldNotify(oldWidget: InheritedWidget) =
            textDirection != (oldWidget as Directionality).textDirection

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty<TextDirection>("textDirection", textDirection))
    }
}