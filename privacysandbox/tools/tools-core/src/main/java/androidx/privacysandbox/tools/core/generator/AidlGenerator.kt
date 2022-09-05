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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.Parameter
import androidx.privacysandbox.tools.core.ParsedApi
import androidx.privacysandbox.tools.core.Type
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class AidlGenerator(
    private val aidlCompiler: AidlCompiler
) {
    fun generate(api: ParsedApi, workingDir: Path): List<GeneratedSource> {
        val aidlInterfaces = generateAidlInterfaces(api, workingDir)
        return compileAidlInterfaces(aidlInterfaces, workingDir)
    }

    private fun generateAidlInterfaces(api: ParsedApi, workingDir: Path): List<GeneratedSource> {
        workingDir.toFile().ensureDirectory()
        val aidlSources = generateAidlContent(api).map {
            val aidlFile = getAidlFile(workingDir, it)
            aidlFile.parentFile.mkdirs()
            aidlFile.createNewFile()
            aidlFile.writeText(it.fileContents)
            GeneratedSource(it.packageName, it.interfaceName, aidlFile)
        }
        return aidlSources
    }

    private fun compileAidlInterfaces(
        aidlSources: List<GeneratedSource>,
        workingDir: Path
    ): List<GeneratedSource> {
        aidlCompiler.compile(workingDir, aidlSources.map { it.file.toPath() })
        val javaSources = aidlSources.map {
            GeneratedSource(
                packageName = it.packageName,
                interfaceName = it.interfaceName,
                file = getJavaFileForAidlFile(it.file)
            )
        }
        javaSources.forEach {
            check(it.file.exists()) {
                "Missing AIDL compilation output ${it.file.absolutePath}"
            }
        }
        return javaSources
    }

    private fun generateAidlContent(api: ParsedApi) =
        api.services.map { service ->
            InMemorySource(
                service.packageName,
                aidlNameForInterface(service),
                generateAidlService(service)
            )
        }

    private fun generateAidlService(service: AnnotatedInterface): String {
        val generatedMethods = service.methods.joinToString(
            separator = "\n\t",
            transform = ::generateAidlMethod
        )
        return """
                package ${service.packageName};
                oneway interface ${aidlNameForInterface(service)} {
                    $generatedMethods
                }
            """.trimIndent()
    }

    private fun generateAidlMethod(method: Method) =
        "void ${method.name}" +
            "(${method.parameters.joinToString(transform = ::generateAidlParameter)});"

    private fun generateAidlParameter(parameter: Parameter) =
        "${parameter.type.toAidlType()} ${parameter.name}"

    private fun getAidlFile(rootPath: Path, aidlSource: InMemorySource) =
        Paths.get(
            rootPath.toString(),
            *aidlSource.packageName.split(".").toTypedArray(),
            aidlSource.interfaceName + ".aidl"
        ).toFile()

    private fun getJavaFileForAidlFile(aidlFile: File): File {
        check(aidlFile.extension == "aidl") {
            "AIDL path has the wrong extension: ${aidlFile.extension}."
        }
        return aidlFile.resolveSibling("${aidlFile.nameWithoutExtension}.java")
    }

    private fun aidlNameForInterface(annotatedInterface: AnnotatedInterface) =
        "I${annotatedInterface.name}"
}

data class InMemorySource(
    val packageName: String,
    val interfaceName: String,
    val fileContents: String
)

data class GeneratedSource(
    val packageName: String,
    val interfaceName: String,
    val file: File
)

internal fun File.ensureDirectory() {
    check(exists()) {
        "$this doesn't exist"
    }
    check(isDirectory) {
        "$this is not a directory"
    }
}

internal fun Type.toAidlType() =
    when (name) {
        Boolean::class.qualifiedName -> "boolean"
        Int::class.qualifiedName -> "int"
        Long::class.qualifiedName -> "long"
        Float::class.qualifiedName -> "float"
        Double::class.qualifiedName -> "double"
        String::class.qualifiedName -> "string"
        Char::class.qualifiedName -> "char"
        Short::class.qualifiedName -> "short"
        Unit::class.qualifiedName -> "void"
        else -> throw IllegalArgumentException("Unsupported type conversion ${this.name}")
    }
