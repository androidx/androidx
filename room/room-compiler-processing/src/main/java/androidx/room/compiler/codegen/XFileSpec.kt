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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.impl.XFileSpecImpl
import androidx.room.compiler.codegen.impl.XTypeSpecImpl
import androidx.room.compiler.codegen.java.JavaFileSpec
import androidx.room.compiler.codegen.kotlin.KotlinFileSpec
import androidx.room.compiler.processing.XFiler
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec

interface XFileSpec {
    fun writeTo(
        language: CodeLanguage,
        generator: XFiler,
        mode: XFiler.Mode = XFiler.Mode.Isolating,
    )

    fun toBuilder(): Builder

    companion object {
        @JvmStatic
        fun of(packageName: String, typeSpec: XTypeSpec) = builder(packageName, typeSpec).build()

        @JvmStatic
        fun builder(packageName: String, typeSpec: XTypeSpec): Builder {
            require(typeSpec is XTypeSpecImpl)
            checkNotNull(typeSpec.name) {
                "Anonymous classes don't have a name so can't be used to create a XFileSpec."
            }
            return XFileSpecImpl.Builder(
                JavaFileSpec.Builder(JavaFile.builder(packageName, typeSpec.java.actual)),
                KotlinFileSpec.Builder(
                    FileSpec.builder(packageName, typeSpec.name!!.kotlin)
                        .addType(typeSpec.kotlin.actual)
                ),
            )
        }
    }

    interface Builder {
        fun addFileComment(format: String, vararg args: Any): Builder = apply {
            addFileComment(XCodeBlock.of(format, *args))
        }

        fun addFileComment(code: XCodeBlock): Builder

        fun build(): XFileSpec
    }
}
