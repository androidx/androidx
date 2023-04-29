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

class TextStringSimpleNodeInvalidationTest : NodeInvalidationTestParent() {
    override fun Any.updateDrawArgs(drawParams: DrawParams): Boolean {
        this as TextStringSimpleNode
        return this.updateDraw(
            drawParams.color,
            drawParams.brush,
            drawParams.alpha,
            drawParams.style
        )
    }

    override fun Any.updateAll(params: Params): Pair<Boolean, Boolean> {
        this as TextStringSimpleNode
        return updateText(params.text) to updateLayoutRelatedArgs(
            style = params.style,
            minLines = params.minLines,
            maxLines = params.maxLines,
            softWrap = params.softWrap,
            fontFamilyResolver = params.fontFamilyResolver,
            overflow = params.overflow
        )
    }

    override fun createSubject(params: Params): Any {
        return TextStringSimpleNode(
            params.text,
            params.style,
            params.fontFamilyResolver,
            params.overflow,
            params.softWrap,
            params.maxLines,
            params.minLines,
        )
    }

    override fun createSubject(params: Params, drawParams: DrawParams): Any {
        return TextStringSimpleNode(
            params.text,
            params.style,
            params.fontFamilyResolver,
            params.overflow,
            params.softWrap,
            params.maxLines,
            params.minLines,
            drawParams.color,
            drawParams.brush,
            drawParams.alpha
        )
    }
}