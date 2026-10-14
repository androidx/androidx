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

package androidx.compose.markuptext.parser.block

import androidx.annotation.IntRange

/**
 * Explicitly captures blank line runs at both root and container levels. Guarantees
 * parent.lineCount == sum(children.lineCount) with zero gaps.
 */
internal class EmptyLineParseBlock(id: Long, @IntRange(from = 1) lineCount: Int = 1) :
    LeafParseBlock(id, lineCount) {
    override fun parseLine(context: ParseContext): ParseResult {
        if (context.currentLine.isEmptyLine()) {
            return ParseResult.accept()
        }
        return ParseResult.rejectAndPropose(context.startBlock())
    }
}
