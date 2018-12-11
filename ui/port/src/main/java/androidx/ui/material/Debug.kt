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

package androidx.ui.material

import androidx.ui.Type
import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.material.material.Material
import androidx.ui.runtimeType
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.Widget

/**
 * Asserts that the given context has a [Material] ancestor.
 *
 * Used by many material design widgets to make sure that they are
 * only used in contexts where they can print ink onto some material.
 *
 * To call this function, use the following pattern, typically in the
 * relevant Widget's build method:
 *
 * ```dart
 * assert(debugCheckHasMaterial(context));
 * ```
 *
 * Does nothing if asserts are disabled. Always returns true.
 */
fun debugCheckHasMaterial(context: BuildContext): Boolean {
    assert {
        if (context.widget !is Material &&
            context.ancestorWidgetOfExactType(Type(Material::class.java)) == null
        ) {
            val message = StringBuffer()
            message.appendln("No Material widget found.")
            message.appendln(
                "${context.widget.runtimeType} widgets require a Material " +
                        "widget ancestor."
            )
            message.appendln(
                "In material design, most widgets are conceptually \"printed\" on " +
                        "a sheet of material. In Flutter\'s material library, that " +
                        "material is represented by the Material widget. It is the " +
                        "Material widget that renders ink splashes, for instance. " +
                        "Because of this, many material library widgets require that " +
                        "there be a Material widget in the tree above them."
            )
            message.appendln(
                "To introduce a Material widget, you can either directly " +
                        "include one, or use a widget that contains Material itself, " +
                        "such as a Card, Dialog, Drawer, or Scaffold."
            )
            message.appendln(
                "The specific widget that could not find a Material ancestor was:"
            )
            message.appendln("  ${context.widget}")
            val ancestors = mutableListOf<Widget>()
            context.visitAncestorElements { element ->
                ancestors.add(element.widget)
                true
            }
            if (ancestors.isNotEmpty()) {
                message.append("The ancestors of this widget were:")
                ancestors.forEach { message.append("\n  $it") }
            } else {
                message.appendln(
                    "This widget is the root of the tree, so it has no " +
                            "ancestors, let alone a \"Material\" ancestor."
                )
            }
            throw FlutterError(message.toString())
        }
        true
    }
    return true
}
