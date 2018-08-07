/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.example.mount

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.out.println("Parsing ${args[0]}")
            val input = CharStreams.fromFileName(args[0])
            val lexer = DartLexer(input)
            val tokens = CommonTokenStream(lexer)
            val parser = DartParser(tokens)
            val tree = parser.libraryDefinition()
            tree.topLevelDefinition()
                    .filterIsInstance<DartParser.ClassDefinitionContext>()
                    .forEach { classDefinition ->
                        val startTokenIndex = classDefinition.start.tokenIndex
                        val comments = tokens.getHiddenTokensToLeft(startTokenIndex)
                        val simpleClassName = classDefinition.identifier().text
                        val outFile = "$simpleClassName.kt"
                        val file = FileSpec.builder("androidx.ui.foundation", outFile)
                        val className = ClassName("androidx.ui.foundation", simpleClassName)
//                val classSpec = TypeSpec.classBuilder(className)
//                        .addType()
                        comments.forEach { print(it) }
                        print("class $simpleClassName")
                    }
        }
    }
}
