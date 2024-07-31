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

package androidx.pdf.util.persistence

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun JSONArray.toList(): List<JSONObject> {
    // TODO: Simplify
    // jsonArray is not an array so we have to loop and map ourselves
    val list = mutableListOf<JSONObject?>()
    for (i in 0 until length()) {
        list.add(optJSONObject(i))
    }
    return list.filterNotNull()
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun Collection<JSONObject>.toJSONArray(): JSONArray =
    JSONArray().apply { forEach { put(it) } }

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Json {
    public val json: JSONObject
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY)
public val Collection<Json>.jsonString: String
    get() = map { it.json }.toJSONArray().toString()
