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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XFiler
import com.squareup.javapoet.JavaFile
import com.google.devtools.ksp.processing.CodeGenerator

internal class KspFiler(
    private val delegate: CodeGenerator
) : XFiler {
    override fun write(javaFile: JavaFile) {
        delegate.createNewFile(
            packageName = javaFile.packageName,
            fileName = javaFile.typeSpec.name,
            extensionName = "java"
        ).use { outputStream ->
            outputStream.bufferedWriter(Charsets.UTF_8).use {
                javaFile.writeTo(it)
            }
        }
    }
}
