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

package androidx.glance.unit

import androidx.annotation.DimenRes
import androidx.annotation.RestrictTo

/**
 * Dimension types. This contains all the dimension types which are supported by androidx.glance.
 *
 * These should only be used internally; developers should be using the width/height Modifiers
 * below rather than this class directly.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class Dimension {
    class Dp(val dp: androidx.compose.ui.unit.Dp) : Dimension()
    object Wrap : Dimension()
    object Fill : Dimension()
    object Expand : Dimension()
    class Resource(@DimenRes val res: Int) : Dimension()
}
