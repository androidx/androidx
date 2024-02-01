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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation

@Composable
@Sampled
fun BasicTextWithLinks() {
    Column {
        BasicText(buildAnnotatedString {
            withAnnotation(LinkAnnotation.Url("https://developer.android.com/")) {
                append("Android for Developers")
            }
        })

        @OptIn(ExperimentalFoundationApi::class)
        BasicText(
            text = buildAnnotatedString {
                append("Click ")
                withAnnotation(LinkAnnotation.Clickable("tag")) {
                    append("here")
                }
            },
            onLinkClicked = {
                // do something with link
            }
        )
    }
}

@Suppress("UNUSED_EXPRESSION")
@Composable
@Sampled
fun BasicTextWithTextLinkClickHandler() {
    val clickHandler = { link: LinkAnnotation ->
        when (link) {
            // do something with the link
        }
    }
    @OptIn(ExperimentalFoundationApi::class)
    BasicText(
        text = buildAnnotatedString {
            append("Click ")
            withAnnotation(LinkAnnotation.Url("https://developer.android.com/")) {
                append("Android for Developers")
            }
            append(" and ")
            withAnnotation(LinkAnnotation.Clickable("tag")) {
                append("other part")
            }
            append(" of this text.")
        },
        onLinkClicked = clickHandler
    )
}
