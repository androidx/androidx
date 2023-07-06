/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.utils

internal const val TEST_AGP_VERSION_8_0_0 = "8.0.0-beta03"
internal const val TEST_AGP_VERSION_8_1_0 = "8.1.0-beta05"
internal const val TEST_AGP_VERSION_8_2_0 = "8.2.0-alpha04"
internal val TEST_AGP_VERSION_CURRENT = null

internal val TEST_AGP_VERSION_ALL = arrayOf<String?>(
    TEST_AGP_VERSION_8_0_0,
    TEST_AGP_VERSION_8_1_0,
    TEST_AGP_VERSION_8_2_0,
    TEST_AGP_VERSION_CURRENT,
)
