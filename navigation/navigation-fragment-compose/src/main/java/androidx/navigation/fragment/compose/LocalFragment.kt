/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.fragment.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.fragment.app.Fragment

/**
 * The CompositionLocal containing the containing [Fragment]. This is sett by default for
 * composables created within a [ComposableFragment].
 */
val LocalFragment =
    staticCompositionLocalOf<Fragment> {
        error(
            "CompositionLocal Fragment not present: are you sure your composable is within a " +
                "navigation-fragment-compose provided ComposableFragment?"
        )
    }
