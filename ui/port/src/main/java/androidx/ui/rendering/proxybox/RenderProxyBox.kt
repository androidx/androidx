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

package androidx.ui.rendering.proxybox

import androidx.ui.rendering.box.RenderBox

/**
 * A base class for render objects that resemble their children.
 *
 * A proxy box has a single child and simply mimics all the properties of that
 * child by calling through to the child for each function in the render box
 * protocol. For example, a proxy box determines its size by asking its child
 * to layout with the same constraints and then matching the size.
 *
 * A proxy box isn't useful on its own because you might as well just replace
 * the proxy box with its child. However, RenderProxyBox is a useful base class
 * for render objects that wish to mimic most, but not all, of the properties
 * of their child.
 */
open class RenderProxyBox(
    /**
     * Creates a proxy render box.
     *
     * Proxy render boxes are rarely created directly because they simply proxy
     * the render box protocol to [child]. Instead, consider using one of the
     * subclasses.
*/
    // TODO(a14n): Remove ignore once https://github.com/dart-lang/sdk/issues/30328 is fixed
    child: RenderBox? = null
) : RenderProxyBoxMixin() {

    init {
        // TODO(Migration/Andrey) we should not use "override var" in constructor as
        // original child property has an additional logic in set fun, we lose with this approach
        this.child = child
    }
}