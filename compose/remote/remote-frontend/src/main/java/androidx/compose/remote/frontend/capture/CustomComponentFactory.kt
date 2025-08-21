/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.frontend.capture

import androidx.compose.remote.core.operations.layout.Component

/** Factory interface for providers of player/platform specific implementations of components */
interface CustomComponentFactory {
    /**
     * Given a component that contains a Custom operation as the first in its list create an
     * appropriate CustomComponent for the player to use to draw the component.
     */
    fun customComponentFor(component: Component): Component
}

/** An empty/null factory that simply returns the component unspecialized back to the caller. */
class EmptyCustomComponentFactory : CustomComponentFactory {
    override fun customComponentFor(component: Component): Component {
        return component
    }
}
