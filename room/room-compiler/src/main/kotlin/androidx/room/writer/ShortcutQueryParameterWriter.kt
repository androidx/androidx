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

package androidx.room.writer

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.asClassName
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter

/** Writer for [ShortcutQueryParameter] related statements. */
object ShortcutQueryParameterWriter {
    fun addNullCheckValidation(scope: CodeGenScope, parameters: List<ShortcutQueryParameter>) {
        // The null checks are only needed for Java since Kotlin instinctively adds null checks to
        // params that are not null.
        if (scope.language != CodeLanguage.JAVA) {
            return
        }
        parameters
            .filter { it.isNonNull }
            .forEach {
                scope.builder.addStatement(
                    "if (%L == null) throw %L",
                    it.name,
                    XCodeBlock.ofNewInstance(
                        scope.language,
                        NullPointerException::class.asClassName()
                    )
                )
            }
    }
}
