/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

/**
 * Introduces the [CustomStyleScope] [styleLayer] method that allows styles to create a style
 * property layer.
 *
 * This is a separate interface to allow a [CustomStyle] type to decide if [styleLayer] is available
 * directly in a style.
 */
@ExperimentalFoundationStyleApi
public interface StyleLayerScope {
    /**
     * Raise the layer of properties that are set in [block]. Properties set in a layer will take
     * precedence over properties set in the layers below it. A layer is implied by
     * [StyleStateScope.state], or helpers that use it, such as [StyleStateScope.pressed] and
     * [StyleStateScope.focused].
     *
     * The last property set with the greatest style layer is the resolved value of a style
     * property.
     */
    public fun styleLayer(block: () -> Unit)
}
