/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CanonicalExample(
    val fileName: String,
    val name: String,
    val description: String,
    val rawJson: String,
    val surfaceId: String,
    val messages: List<String>,
)

internal suspend fun loadCanonicalExamples(context: Context): List<CanonicalExample> =
    withContext(Dispatchers.IO) {
        val files =
            context.assets.list("examples")?.filter { it.endsWith(".json") }?.sorted()
                ?: return@withContext emptyList()

        files.map { fileName ->
            val rawJson = loadExampleRawJson(context, fileName)
            parseCanonicalExample(fileName = fileName, rawJson = rawJson)
        }
    }

private fun parseCanonicalExample(fileName: String, rawJson: String): CanonicalExample {
    val jsonObj = JSONObject(rawJson)
    val name = jsonObj.optString("name", fileName)
    val description = jsonObj.optString("description", "")
    val messagesArray = jsonObj.optJSONArray("messages") ?: JSONArray()

    var surfaceId: String? = null
    val messages =
        buildList(messagesArray.length()) {
            for (i in 0 until messagesArray.length()) {
                val msgObj = messagesArray.getJSONObject(i)
                add(msgObj.toString())

                msgObj.optJSONObject("createSurface")?.let { createSurface ->
                    require(surfaceId == null) { "Example $fileName creates more than one surface" }
                    surfaceId = createSurface.getString("surfaceId")
                }
            }
        }

    return CanonicalExample(
        fileName = fileName,
        name = name,
        description = description,
        rawJson = rawJson,
        surfaceId = requireNotNull(surfaceId) { "Example $fileName doesn't create a surface" },
        messages = messages,
    )
}

private fun loadExampleRawJson(context: Context, fileName: String): String {
    return context.assets.open("examples/$fileName").bufferedReader().use { it.readText() }
}
