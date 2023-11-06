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

package androidx.appactions.interaction.testapp

internal const val ASSISTANT_PACKAGE = "com.google.android.googlequicksearchbox"
internal const val ASSISTANT_SIGNATURE =
    "f0:fd:6c:5b:41:0f:25:cb:25:c3:b5:33:46:c8:97:2f:ae:30:f8:ee:74:11:df:91:04:80:ad:" +
        "6b:2d:60:db:83"

internal fun hex2Byte(s: String): ByteArray {
    val parts = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val result = ByteArray(parts.size)
    for (i in parts.indices) {
        result[i] = parts[i].toInt(16).toByte()
    }
    return result
}
