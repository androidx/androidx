/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.core.adapter

import androidx.ui.core.TextComposable
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.TextSpan
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.services.text_editing.TextSelection
import com.google.r4a.Composable

/**
 * All this module is needed to work around b/120971484
 *
 * For the original logic:
 * @see androidx.ui.core.Text
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun Text(
    text: TextSpan,
    textAlign: TextAlign = TextAlign.START,
    textDirection: TextDirection = TextDirection.LTR,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.CLIP,
    textScaleFactor: Float = 1.0f,
    maxLines: Int? = null,
    selection: TextSelection? = null
) {
    TextComposable(
        text,
        textAlign,
        textDirection,
        softWrap,
        overflow,
        textScaleFactor,
        maxLines,
        selection
    )
}
