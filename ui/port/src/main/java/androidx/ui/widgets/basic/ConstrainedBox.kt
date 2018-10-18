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

import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.RenderConstrainedBox
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

// /// A widget that imposes additional constraints on its child.
// ///
// /// For example, if you wanted [child] to have a minimum height of 50.0 logical
// /// pixels, you could use `const BoxConstraints(minHeight: 50.0)` as the
// /// [constraints].
// ///
// /// ## Sample code
// ///
// /// This snippet makes the child widget (a [Card] with some [Text]) fill the
// /// parent, by applying [BoxConstraints.expand] constraints:
// ///
// /// ```dart
// /// new ConstrainedBox(
// ///   constraints: const BoxConstraints.expand(),
// ///   child: const Card(child: const Text('Hello World!')),
// /// )
// /// ```
// ///
// /// The same behavior can be obtained using the [new SizedBox.expand] widget.
// ///
// /// See also:
// ///
// ///  * [BoxConstraints], the class that describes constraints.
// ///  * [UnconstrainedBox], a container that tries to let its child draw without
// ///    constraints.
// ///  * [SizedBox], which lets you specify tight constraints by explicitly
// ///    specifying the height or width.
// ///  * [FractionallySizedBox], which sizes its child based on a fraction of its
// ///    own size and positions the child according to an [Alignment] value.
// ///  * [AspectRatio], a widget that attempts to fit within the parent's
// ///    constraints while also sizing its child to match a given aspect ratio.
// ///  * The [catalog of layout widgets](https://flutter.io/widgets/layout/).
class ConstrainedBox(
    key: Key? = null,
    // /// The additional constraints to impose on the child.
    val constraints: BoxConstraints,
    child: Widget? = null
) : SingleChildRenderObjectWidget(key, child) {
    init {
        assert(constraints.debugAssertIsValid())
    }

    override fun createRenderObject(context: BuildContext): RenderObject {
        return RenderConstrainedBox(_additionalConstraints = constraints)
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) {
        (renderObject as RenderConstrainedBox).additionalConstraints = constraints
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("constraints", constraints, showName = false))
    }
}