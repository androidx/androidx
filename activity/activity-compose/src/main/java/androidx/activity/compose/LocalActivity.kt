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

package androidx.activity.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext

/**
 * Provides the [ComponentActivity] belonging to the current [LocalContext].
 *
 * Note, when possible you should always prefer using the finer grained composition locals where
 * available. This API should be used as a fallback when the required API is only available via
 * [android.app.Activity].
 *
 * See [androidx.compose.ui.platform.LocalConfiguration]
 * [androidx.compose.ui.platform.LocalLifecycleOwner] [androidx.compose.ui.platform.LocalView]
 */
val LocalActivity =
    compositionLocalWithComputedDefaultOf<ComponentActivity?> {
        findOwner(LocalContext.currentValue)
    }
