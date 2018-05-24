/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.ktx

import androidx.work.Data

/**
 * Converts a [Map] to a [Data] object.
 */
@Deprecated(
        replaceWith = ReplaceWith(
                expression = "toWorkData()",
                imports = arrayOf("androidx.work.toWorkData")),
        level = DeprecationLevel.WARNING,
        message = "Use androidx.work.toWorkData instead")
inline fun <V> Map<String, V>.toWorkData(): Data {
    val dataBuilder = Data.Builder()
    dataBuilder.putAll(this)
    return dataBuilder.build()
}
