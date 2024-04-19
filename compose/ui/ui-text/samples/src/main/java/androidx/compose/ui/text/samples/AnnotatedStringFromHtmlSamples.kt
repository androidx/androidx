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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml

@Composable
@Sampled
fun AnnotatedStringFromHtml() {
    // First, download a string as a plain text using one of the resources' methods. At this stage
    // you will be handling plurals and formatted strings in needed. Moreover, the string will be
    // resolved with respect to the current locale and available translations.
    val string = stringResource(id = R.string.example)

    // Next, convert a string marked with HTML tags into AnnotatedString to be displayed by Text
    val styledAnnotatedString = AnnotatedString.fromHtml(htmlString = string)

    BasicText(styledAnnotatedString)
}
