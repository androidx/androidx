/*
 * Copyright 2020 The Android Open Source Project
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

@file:JvmName("StringUtils")
package androidx.build

import java.util.Locale

// TODO: Replace this with the function from the Kotlin stdlib once the API becomes stable
fun String.capitalize(locale: Locale): String = if (isNotEmpty() && this[0].isLowerCase()) {
    substring(0, 1).toUpperCase(locale) + substring(1)
} else  {
    this
}

// TODO: Replace this with the function from the Kotlin stdlib once the API becomes stable
fun String.decapitalize(locale: Locale): String = if (isNotEmpty() && this[0].isUpperCase()) {
    substring(0, 1).toLowerCase(locale) + substring(1)
} else {
    this
}
