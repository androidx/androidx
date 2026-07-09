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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.demos

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.compose.embedded.CustomPluginRegistry
import androidx.compose.remote.player.compose.embedded.LocalRcCustomPlugins
import androidx.compose.remote.player.compose.embedded.integration.previews.ExperimentalRemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Creates a sample Remote Compose document containing a `support:edit-text` custom component. */
public fun createSupportEditTextDocument(
    initialText: String = "Hello Remote Compose",
    hint: String = "Type here...",
): ByteArray {
    var textTargetId = -1
    val docContext =
        RemoteComposeContextAndroid(
            300,
            100,
            "custom-demo",
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) {
            textTargetId = writer.textCreateId("")
            writer.root {
                writer.column(Modifier, 1, 1) {
                    writer.startCustom(
                        Modifier,
                        "support:edit-text",
                        listOf(
                            Custom.CustomProperty(
                                SupportEditTextData.TEXT.id.toShort(),
                                Custom.CustomProperty.STRING_PROP,
                                writer.textCreateId(initialText),
                            ),
                            Custom.CustomProperty(
                                SupportEditTextData.HINT.id.toShort(),
                                Custom.CustomProperty.STRING_PROP,
                                writer.textCreateId(hint),
                            ),
                            Custom.CustomProperty(
                                SupportEditTextData.TEXT_COLOR.id.toShort(),
                                Custom.CustomProperty.INT_PROP,
                                Color.Black.toArgb(),
                            ),
                            Custom.CustomProperty(
                                SupportEditTextData.TEXT_SIZE.id.toShort(),
                                Custom.CustomProperty.FLOAT_PROP,
                                16f,
                            ),
                            Custom.CustomProperty(
                                SupportEditTextData.RET_TEXT.id.toShort(),
                                Custom.CustomProperty.TEXT_RETURN,
                                textTargetId,
                            ),
                        ),
                    )
                    writer.endCustom()
                }
            }
        }

    return docContext.writer.encodeToByteArray()
}

/** A preview and demo composable showing the `SupportEditTextPlugin` in action. */
@Composable
@Preview(showBackground = true)
@SuppressLint("RestrictedApiAndroidX")
public fun SupportEditTextDemo() {
    val documentBytes = remember { createSupportEditTextDocument() }
    val registry = remember { CustomPluginRegistry(SupportEditTextPlugin) }

    Column(modifier = Modifier.padding(16.dp)) {
        BasicText(
            text = "Remote Compose SupportEditText Demo:",
            style = TextStyle(fontSize = 18.sp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        CompositionLocalProvider(LocalRcCustomPlugins provides registry) {
            ExperimentalRemoteDocumentPreview(
                document = documentBytes,
                modifier = Modifier.fillMaxWidth().height(60.dp),
            )
        }
    }
}
