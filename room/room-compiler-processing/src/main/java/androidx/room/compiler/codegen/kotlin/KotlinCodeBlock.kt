/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.KCodeBlock
import androidx.room.compiler.codegen.KCodeBlockBuilder
import androidx.room.compiler.codegen.TargetLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec

internal class KotlinCodeBlock(
    internal val actual: KCodeBlock
) : KotlinLang(), XCodeBlock {

    internal class Builder : KotlinLang(), XCodeBlock.Builder {

        internal val actual = KCodeBlockBuilder()

        override fun add(code: XCodeBlock) = apply {
            require(code is KotlinCodeBlock)
            actual.add(code.actual)
        }

        override fun add(format: String, vararg args: Any?) = apply {
            // No need to process 'format' since we use '%' as placeholders.
            val processedArgs = processArgs(args)
            actual.add(format, *processedArgs)
        }

        override fun addStatement(format: String, vararg args: Any?) = apply {
            // No need to process 'format' since we use '%' as placeholders.
            val processedArgs = processArgs(args)
            actual.addStatement(format, *processedArgs)
        }

        override fun addLocalVariable(
            name: String,
            type: XTypeName,
            isMutable: Boolean,
            assignExpr: XCodeBlock
        ) = apply {
            require(assignExpr is KotlinCodeBlock)
            val varOrVal = if (isMutable) "var" else "val"
            actual.addStatement(
                "$varOrVal %L: %T = %L",
                type.kotlin,
                name,
                assignExpr.actual
            )
        }

        override fun build(): XCodeBlock {
            return KotlinCodeBlock(actual.build())
        }

        // Unwraps room.compiler.codegen types to their KotlinPoet actual
        // TODO(b/247242375): Consider improving by wrapping args.
        private fun processArgs(args: Array<out Any?>): Array<Any?> {
            return Array(args.size) { index ->
                val arg = args[index]
                if (arg is TargetLanguage) {
                    check(arg.language == CodeLanguage.KOTLIN) { "$arg is not KotlinCode" }
                }
                when (arg) {
                    is XTypeName -> arg.kotlin
                    is XTypeSpec -> (arg as KotlinTypeSpec).actual
                    is XFunSpec -> (arg as KotlinFunSpec).actual
                    is XCodeBlock -> (arg as KotlinCodeBlock).actual
                    else -> arg
                }
            }
        }
    }
}
