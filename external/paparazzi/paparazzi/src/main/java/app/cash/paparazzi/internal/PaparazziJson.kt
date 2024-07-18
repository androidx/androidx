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
package app.cash.paparazzi.internal

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.TestName
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date

internal object PaparazziJson {
  val moshi = Moshi.Builder()
    .add(Date::class.java, Rfc3339DateJsonAdapter())
    .add(this)
    .build()!!

  val listOfShotsAdapter: JsonAdapter<List<Snapshot>> =
    moshi
      .adapter<List<Snapshot>>(
        Types.newParameterizedType(List::class.java, Snapshot::class.java)
      )
      .indent("  ")

  val listOfStringsAdapter: JsonAdapter<List<String>> =
    moshi
      .adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
      )
      .indent("  ")

  @ToJson
  fun testNameToJson(testName: TestName): String {
    return "${testName.packageName}.${testName.className}#${testName.methodName}"
  }

  @FromJson
  fun testNameFromJson(json: String): TestName {
    val regex = Regex("(.*)\\.([^.]*)#([^.]*)")
    val (packageName, className, methodName) = regex.matchEntire(json)!!.destructured
    return TestName(packageName, className, methodName)
  }
}
