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
    abstract val relativePath: String
    abstract val contents: String
    abstract fun toJFO(): JavaFileObject
    abstract fun toKotlinSourceFile(srcRoot: File): SourceFile

    override fun toString(): String {
        return "SourceFile[$relativePath]"
    }

    class JavaSource(
        val qName: String,
        override val contents: String
    ) : Source() {
        override fun toJFO(): JavaFileObject {
            return JavaFileObjects.forSourceString(
                qName,
                contents
            )
        }

        override fun toKotlinSourceFile(srcRoot: File): SourceFile {
            val outFile = srcRoot.resolve(relativePath)
                .also {
                    it.parentFile.mkdirs()
                    it.writeText(contents)
                }
            return SourceFile.fromPath(outFile)
        }

        override val relativePath
            get() = qName.replace(".", "/") + ".java"
    }

    class KotlinSource(
        override val relativePath: String,
        override val contents: String
    ) : Source() {
        override fun toJFO(): JavaFileObject {
            throw IllegalStateException("cannot include kotlin code in javac compilation")
        }

        override fun toKotlinSourceFile(srcRoot: File): SourceFile {
            val outFile = srcRoot.resolve(relativePath).also {
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

        /**
         * Convenience method to convert JFO's to the Source objects in XProcessing so that we can
         * convert room tests to the common API w/o major code refactor
         */
        fun fromJavaFileObject(javaFileObject: JavaFileObject): Source {
            val uri = javaFileObject.toUri()
            // parse name from uri
            val contents = javaFileObject.openReader(true).use {
                it.readText()
            }
            val qName = if (uri.scheme == "mem") {
                // in java compile testing, path includes SOURCE_OUTPUT, drop it
                uri.path.substringAfter("SOURCE_OUTPUT/").replace('/', '.')
            } else {
                uri.path.replace('/', '.')
            }
            val javaExt = ".java"
            check(qName.endsWith(javaExt)) {
                "expected a java source file, $qName does not seem like one"
            }

            return java(qName.dropLast(javaExt.length), contents)
        }

        fun loadKotlinSource(
            file: File,
            relativePath: String
        ): Source {
            check(file.exists() && file.name.endsWith(".kt"))
            return kotlin(relativePath, file.readText())
        }

        fun loadJavaSource(
            file: File,
            qName: String
        ): Source {
            check(file.exists() && file.name.endsWith(".java"))
            return java(qName, file.readText())
        }

        fun load(
            file: File,
            qName: String,
            relativePath: String
        ): Source {
            check(file.exists()) {
                "file does not exist: ${file.absolutePath}"
            }
            return when {
                file.name.endsWith(".kt") -> loadKotlinSource(file, relativePath)
                file.name.endsWith(".java") -> loadJavaSource(file, qName)
                else -> error("invalid file extension ${file.name}")
            }
        }
    }
}
