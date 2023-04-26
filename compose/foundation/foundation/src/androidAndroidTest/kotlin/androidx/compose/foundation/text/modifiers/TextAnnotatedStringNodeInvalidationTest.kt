/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.text.AnnotatedString

class TextAnnotatedStringNodeInvalidationTest : NodeInvalidationTestParent() {
    override fun Any.updateAll(params: Params): Pair<Boolean, Boolean> {
        this as TextAnnotatedStringNode
        return updateText(AnnotatedString(params.text)) to updateLayoutRelatedArgs(
            style = params.style,
            minLines = params.minLines,
            maxLines = params.maxLines,
            softWrap = params.softWrap,
            fontFamilyResolver = params.fontFamilyResolver,
            overflow = params.overflow,
            placeholders = null
        )
    }

    override fun createSubject(params: Params): Any {
        return TextAnnotatedStringNode(
            text = AnnotatedString(text = params.text),
            style = params.style,
            fontFamilyResolver = params.fontFamilyResolver,
            onTextLayout = null,
            overflow = params.overflow,
            softWrap = params.softWrap,
            maxLines = params.maxLines,
            minLines = params.minLines
        )
    }
}