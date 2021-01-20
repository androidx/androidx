/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import com.google.testing.compile.JavaFileObjects
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import java.io.File
import javax.tools.JavaFileObject

/**
 * Common abstraction for test sources in kotlin and java
 */
sealed class Source {
    abstract fun toJFO(): JavaFileObject
    abstract fun toKotlinSourceFile(srcRoot: File): SourceFile
    class JavaSource(
        val qName: String,
        val contents: String
    ) : Source() {
        override fun toJFO(): JavaFileObject {
            return JavaFileObjects.forSourceString(
                qName,
                contents
            )
        }

        override fun toKotlinSourceFile(srcRoot: File): SourceFile {
            val outFile = srcRoot.resolve(relativePath())
                .also {
                    it.parentFile.mkdirs()
                    it.writeText(contents)
                }
            return SourceFile.fromPath(outFile)
        }

        private fun relativePath(): String {
            return qName.replace(".", "/") + ".java"
        }
    }

    class KotlinSource(
        val filePath: String,
        val contents: String
    ) : Source() {
        override fun toJFO(): JavaFileObject {
            throw IllegalStateException("cannot include kotlin code in javac compilation")
        }

        override fun toKotlinSourceFile(srcRoot: File): SourceFile {
            val outFile = srcRoot.resolve(filePath).also {
                it.parentFile.mkdirs()
                it.writeText(contents)
            }
            return SourceFile.fromPath(
                outFile
            )
        }
    }

    companion object {
        fun java(
            qName: String,
            @Language("java")
            code: String
        ): Source {
            return JavaSource(
                qName,
                code
            )
        }

        fun kotlin(
            filePath: String,
            @Language("kotlin")
            code: String
        ): Source {
            return KotlinSource(
                filePath,
                code
            )
        }
    }
}
