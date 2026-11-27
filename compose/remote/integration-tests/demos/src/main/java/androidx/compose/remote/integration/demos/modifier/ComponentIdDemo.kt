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

package androidx.compose.remote.integration.demos.modifier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.componentId
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private const val COLUMN_ID = 100
private const val BOX_ID = 101
private const val ROW_ID = 102
private const val TEXT_ID = 103
private const val OUTER_CHAINED_ID = 200
private const val INNER_CHAINED_ID = 201

private val LookupIds =
    listOf(
        COLUMN_ID to "RemoteColumn",
        BOX_ID to "RemoteBox",
        ROW_ID to "RemoteRow",
        TEXT_ID to "RemoteText",
        OUTER_CHAINED_ID to "Chained outermost ($OUTER_CHAINED_ID then $INNER_CHAINED_ID)",
        INNER_CHAINED_ID to "Chained inner (overridden by $OUTER_CHAINED_ID)",
    )

@Suppress("RestrictedApiAndroidX") // Referring to CoreDocument
@Composable
fun ComponentIdDemo(onDocument: (CoreDocument) -> Unit = {}) {
    var capturedDocument by remember { mutableStateOf<CoreDocument?>(null) }
    val handleDocument =
        remember(onDocument) {
            { doc: CoreDocument ->
                capturedDocument = doc
                onDocument(doc)
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RemoteDemo(modifier = Modifier.fillMaxSize(), onDocument = handleDocument) {
                ComponentIdDemoContent()
            }
        }

        val doc = capturedDocument
        if (doc != null) {
            ComponentLookupPanel(document = doc)
        }
    }
}

@Suppress("RestrictedApiAndroidX") // Referring to CoreDocument
@Composable
private fun ComponentLookupPanel(document: CoreDocument, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "CoreDocument.getComponent(id)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            LookupIds.forEach { (id, description) ->
                val resolvedName = document.getComponent(id)?.javaClass?.simpleName ?: "null"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "$id ($description)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        text = resolvedName,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider()
            Text(
                text = "Tap the Document icon in the top bar to inspect all component IDs.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Suppress("RestrictedApiAndroidX") // Referring to RemoteComponentPreview, componentId
@RemoteComponentPreview
@Composable
@RemoteComposable
private fun ComponentIdDemoContent() {
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().componentId(COLUMN_ID),
        verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
    ) {
        RemoteText(
            text = "RemoteColumn (componentId = $COLUMN_ID)".rs,
            color = Color.Black.rc,
        )

        RemoteBox(
            modifier =
                RemoteModifier.componentId(BOX_ID)
                    .fillMaxWidth()
                    .height(56.rdp)
                    .background(Color(0xFF1565C0).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                text = "RemoteBox (componentId = $BOX_ID)".rs,
                color = Color.White.rc,
            )
        }

        RemoteRow(
            modifier =
                RemoteModifier.componentId(ROW_ID)
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9).rc)
                    .padding(12.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteText(
                text = "RemoteRow (id = $ROW_ID) > RemoteText (id = $TEXT_ID)".rs,
                modifier = RemoteModifier.componentId(TEXT_ID),
                color = Color(0xFF1B5E20).rc,
            )
        }

        RemoteBox(
            modifier =
                RemoteModifier.componentId(OUTER_CHAINED_ID)
                    .componentId(INNER_CHAINED_ID)
                    .fillMaxWidth()
                    .height(56.rdp)
                    .background(Color(0xFF6A1B9A).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                text =
                    "Chained ($OUTER_CHAINED_ID then $INNER_CHAINED_ID) -> id = $OUTER_CHAINED_ID"
                        .rs,
                color = Color.White.rc,
            )
        }

        RemoteBox(
            modifier = RemoteModifier.fillMaxWidth().height(56.rdp).background(Color.LightGray.rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                text = "No componentId modifier (auto-generated < -1)".rs,
                color = Color.Black.rc,
            )
        }
    }
}
