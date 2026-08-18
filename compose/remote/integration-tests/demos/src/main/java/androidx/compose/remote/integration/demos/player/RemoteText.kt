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

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation

/**
 * Remote composable that displays a [RemoteAnnotatedString] using [RemoteCustomComponent] and
 * [SupportSpannableString].
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteText(text: RemoteAnnotatedString, modifier: RemoteModifier = RemoteModifier) {
    RemoteCustomComponent(name = "SupportSpannableString", modifier = modifier) {
        property(SupportSpannableString.PROP_TEXT.toInt(), text.text.rs)
        property(SupportSpannableString.PROP_LINK_COUNT.toInt(), text.linkAnnotations.size)

        for (index in text.linkAnnotations.indices) {
            val range = text.linkAnnotations[index]
            val url =
                when (val link = range.item) {
                    is LinkAnnotation.Url -> link.url
                    is LinkAnnotation.Clickable -> link.tag
                    else -> ""
                }
            val urlPropType = (SupportSpannableString.PROP_LINK_URL_BASE + index)
            val startPropType = (SupportSpannableString.PROP_LINK_START_BASE + index)
            val endPropType = (SupportSpannableString.PROP_LINK_END_BASE + index)

            property(urlPropType, url.rs)
            property(startPropType, range.start)
            property(endPropType, range.end)
        }
    }
}
