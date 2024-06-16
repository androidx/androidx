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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Sampled
@Composable
fun TextWithLinks() {
    val url = "https://developer.android.com/jetpack/compose"

    val linkColor = MaterialTheme.colorScheme.primary
    val linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)

    val annotatedString = buildAnnotatedString {
        append("Build better apps faster with ")
        withLink(LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = linkStyle))) {
            append("Jetpack Compose")
        }
    }
    // Note that if your string is defined in resources, you can pass the same link style object
    // when constructing the AnnotatedString using the AnnotatedString.fromHtml method.
    Text(annotatedString)
}
