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

package androidx.compose.material3

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun formatDatePickerNavigateToYearString(
    template: String,
    localizedYear: String
): String = template.format(localizedYear)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun formatHeadlineDescription(
    template: String,
    verboseDateDescription: String
): String = template.format(verboseDateDescription)
