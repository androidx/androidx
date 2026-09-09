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

package androidx.compose.material3.integration.a2ui.ui

import android.content.ClipData
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiProtocolConstants
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.integration.a2ui.icons.CheckIcon
import androidx.compose.material3.integration.a2ui.icons.ContentCopyIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JsonBottomSheet(
    sheetState: SheetState,
    surfaceId: String,
    components: List<A2uiComponentPayload>,
    dataModel: Map<String, Any?> = emptyMap(),
    onDismissRequest: () -> Unit,
) {
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val jsonString =
        remember(surfaceId, components, dataModel) {
            formatA2uiJson(surfaceId = surfaceId, components = components, dataModel = dataModel)
        }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "A2UI Message Payload",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                FilledTonalButton(
                    onClick = {
                        val clipEntry = ClipData.newPlainText("A2UI JSON", jsonString).toClipEntry()

                        coroutineScope.launch {
                            clipboardManager.setClipEntry(clipEntry)
                            isCopied = true
                            delay(2000.milliseconds)
                            isCopied = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isCopied) CheckIcon else ContentCopyIcon,
                        contentDescription = if (isCopied) "Copied" else "Copy JSON",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isCopied) "Copied" else "Copy")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
            ) {
                val verticalScrollState = rememberScrollState()
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp)
                ) {
                    Text(
                        text = jsonString,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Pretty-formats the A2UI server-to-client JSON payload for the surface. */
internal fun formatA2uiJson(
    surfaceId: String,
    components: List<A2uiComponentPayload>,
    dataModel: Map<String, Any?> = emptyMap(),
): String {
    val root = JSONObject()
    root.put("version", A2uiProtocolConstants.PROTOCOL_VERSION)

    val updateComponents = JSONObject()
    updateComponents.put("surfaceId", surfaceId)

    val componentsArray = JSONArray()
    for (component in components) {
        val compObj = JSONObject()
        compObj.put("id", component.id)
        compObj.put("component", component.type)
        for ((k, v) in component.properties) {
            compObj.put(k, wrapJsonValue(v))
        }
        componentsArray.put(compObj)
    }
    updateComponents.put("components", componentsArray)
    root.put("updateComponents", updateComponents)

    if (dataModel.isNotEmpty()) {
        val updateData = JSONObject()
        updateData.put("surfaceId", surfaceId)
        updateData.put("path", "/")
        updateData.put("value", wrapJsonValue(dataModel))
        root.put("updateDataModel", updateData)
    }

    return root.toString(2)
}

private fun wrapJsonValue(value: Any?): Any? {
    return when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> {
            val obj = JSONObject()
            for ((k, v) in value) {
                obj.put(k.toString(), wrapJsonValue(v))
            }
            obj
        }
        is List<*> -> {
            val arr = JSONArray()
            for (item in value) {
                arr.put(wrapJsonValue(item))
            }
            arr
        }
        else -> value
    }
}
