/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi

import com.squareup.moshi.JsonClass
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class Snapshot(
  val name: String?,
  val testName: TestName,
  val timestamp: Date,
  val tags: List<String> = listOf(),
  val file: String? = null
)

internal fun Snapshot.toFileName(
  delimiter: String = "_",
  extension: String
): String {
  val formattedLabel = if (name != null) {
    "$delimiter${name.lowercase(Locale.US).replace("\\s".toRegex(), delimiter)}"
  } else {
    ""
  }
  return "${testName.packageName}${delimiter}${testName.className}" +
      "${delimiter}${testName.methodName}$formattedLabel.$extension"
}
