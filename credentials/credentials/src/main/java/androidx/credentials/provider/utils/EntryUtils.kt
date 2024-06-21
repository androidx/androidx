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

package androidx.credentials.provider.utils

import android.os.Build

internal const val ANDROID_15_BETA_3 = "AP31.240517.022"
internal const val ANDROID_15_BETA_2_2 = "AP31.240426.023.B4"
internal const val ANDROID_15_BETA_2_1 = "AP31.240426.023"
internal const val ANDROID_15_BETA_2 = "AP31.240426.022"

internal val buildsUsingSliceProperties =
    setOf(ANDROID_15_BETA_2, ANDROID_15_BETA_2_1, ANDROID_15_BETA_2_2, ANDROID_15_BETA_3)

/**
 * The library owners aim to support early partners across beta2 and beta3 devices for Android 15,
 * requiring this to be introduced for backwards compatibility. Beyond this temporary use case, the
 * library owners aim to no longer utilize this functionality.
 */
internal fun requiresSlicePropertiesWorkaround(): Boolean =
    buildsUsingSliceProperties.contains(Build.ID)
