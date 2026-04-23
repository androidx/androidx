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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat

/**
 * Draw content with modified alpha that may be less than 1.
 *
 * @param alpha the fraction of children's alpha value and must be between `0` and `1`, inclusive.
 * @see graphicsLayer
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.alpha(alpha: RemoteFloat): RemoteModifier = graphicsLayer(alpha = alpha)
