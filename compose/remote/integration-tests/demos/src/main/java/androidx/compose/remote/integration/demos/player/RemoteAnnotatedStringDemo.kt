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

package androidx.compose.remote.integration.demos.player

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.LinkAnnotation

@Suppress("RestrictedApiAndroidX")
private val experimentalProfile =
    Profile(
        RcPlatformProfiles.ANDROIDX.apiLevel,
        RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
        RcPlatformProfiles.ANDROIDX.platform,
        RcPlatformProfiles.ANDROIDX.profileFactory,
    )

@Composable
@RemoteComposable
fun LinkedTextSample() {
    val annotatedText = buildRemoteAnnotatedString {
        append("Please review our ")

        withLink(LinkAnnotation.Url("https://example.com/terms")) { append("Terms of Service") }

        append(" and ")

        withLink(LinkAnnotation.Url("https://example.com/privacy")) { append("Privacy Policy") }

        append(".")
    }

    RemoteText(text = annotatedText)
}

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteAnnotatedStringDemo() {
    val customSupport = remember {
        AndroidCustomContextImpl().apply {
            registerDelegate("SupportSpannableString", SupportSpannableString())
        }
    }
    RemoteDemo(profile = experimentalProfile, customSupport = customSupport) {
        RemoteColumn(modifier = RemoteModifier.padding(16.rdp)) { LinkedTextSample() }
    }
}
