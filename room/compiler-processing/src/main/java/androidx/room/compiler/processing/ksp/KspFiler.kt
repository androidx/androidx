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
import androidx.room.compiler.processing.XMessager
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.javapoet.JavaFile
import javax.tools.Diagnostic

internal class KspFiler(
    private val delegate: CodeGenerator,
    private val messager: XMessager,
) : XFiler {
    override fun write(javaFile: JavaFile) {
        val originatingFiles = javaFile.typeSpec.originatingElements
            .map {
                check(it is KSFileAsOriginatingElement) {
                    "Unexpected element type in originating elements. $it"
                }
                it.ksFile
            }
        val dependencies = if (originatingFiles.isEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.WARNING,
                """
                    No dependencies are reported for ${javaFile.typeSpec.name} which will prevent
                    incremental compilation. Please file a bug at:
                    https://issuetracker.google.com/issues/new?component=413107
                """.trimIndent()
            )
            Dependencies.ALL_FILES
        } else {
            Dependencies(
                aggregating = false,
                sources = originatingFiles.distinct().toTypedArray()
            )
        }

        delegate.createNewFile(
            dependencies = dependencies,
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
