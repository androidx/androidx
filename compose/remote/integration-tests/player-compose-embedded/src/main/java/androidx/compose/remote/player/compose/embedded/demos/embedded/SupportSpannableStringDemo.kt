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

package androidx.compose.remote.player.compose.embedded.demos.embedded

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.CustomPluginRegistry
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.runBlocking

/** Sample Remote composable rendering a custom `SupportSpannableString` component. */
@Composable
@RemoteComposable
public fun RemoteSpannableText(
    text: String = "Please review our Terms of Service and Privacy Policy.",
    termsUrl: String = "https://example.com/terms",
    privacyUrl: String = "https://example.com/privacy",
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteCustomComponent(name = SupportSpannableStringPlugin.CONFIG, modifier = modifier) {
        property(SupportSpannableStringPlugin.PROP_TEXT.toInt(), text.rs)
        property(SupportSpannableStringPlugin.PROP_LINK_COUNT.toInt(), 2)
        property(SupportSpannableStringPlugin.PROP_LINK_URL_BASE.toInt(), termsUrl.rs)
        property(SupportSpannableStringPlugin.PROP_LINK_START_BASE.toInt(), 18)
        property(SupportSpannableStringPlugin.PROP_LINK_END_BASE.toInt(), 34)
        property((SupportSpannableStringPlugin.PROP_LINK_URL_BASE + 1), privacyUrl.rs)
        property((SupportSpannableStringPlugin.PROP_LINK_START_BASE + 1), 39)
        property((SupportSpannableStringPlugin.PROP_LINK_END_BASE + 1), 53)
    }
}

/** A preview and demo composable showing the [SupportSpannableStringPlugin] in action. */
@Composable
@Preview(showBackground = true)
public fun SupportSpannableStringDemo() {
    val context = LocalContext.current
    val capturedDoc = remember {
        runBlocking { captureSingleRemoteDocument(context = context) { RemoteSpannableText() } }
    }
    val registry = remember { CustomPluginRegistry(SupportSpannableStringPlugin) }

    Column(modifier = Modifier.padding(16.dp)) {
        BasicText(
            text = "Remote Compose SupportSpannableString Demo:",
            style = TextStyle(fontSize = 18.sp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        RcPlayer(
            capturedDocument = capturedDoc,
            customPlugins = registry,
            modifier = Modifier.fillMaxWidth().height(60.dp),
        )
    }
}
