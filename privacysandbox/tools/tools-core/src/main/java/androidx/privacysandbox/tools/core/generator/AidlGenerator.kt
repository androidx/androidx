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

class AidlGenerator private constructor(
    private val aidlCompiler: AidlCompiler,
    private val api: ParsedApi,
    private val workingDir: Path,
) {
    init {
        check(api.services.count() <= 1) { "Multiple services are not supported." }
    }

    companion object {
        fun generate(
            aidlCompiler: AidlCompiler,
            api: ParsedApi,
            workingDir: Path,
        ): List<GeneratedSource> {
            return AidlGenerator(aidlCompiler, api, workingDir).generate()
        }
    }

    private fun generate(): List<GeneratedSource> {
        if (api.services.isEmpty()) return listOf()
        return compileAidlInterfaces(generateAidlInterfaces())
    }

    private fun generateAidlInterfaces(): List<GeneratedSource> {
        workingDir.toFile().ensureDirectory()
        val aidlSources = generateAidlContent().map {
            val aidlFile = getAidlFile(workingDir, it)
            aidlFile.parentFile.mkdirs()
            aidlFile.createNewFile()
            aidlFile.writeText(it.fileContents)
            GeneratedSource(it.packageName, it.interfaceName, aidlFile)
        }
        return aidlSources
    }

    private fun compileAidlInterfaces(aidlSources: List<GeneratedSource>): List<GeneratedSource> {
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

    private fun generateAidlContent(): List<InMemorySource> {
        // TODO: implement better tooling to generate AIDL (AidlPoet).
        val transactionCallbacks = generateTransactionCallbacks()
        val service =
            InMemorySource(
                packageName(),
                aidlNameForInterface(service()),
                generateAidlService(transactionCallbacks)
            )
        return transactionCallbacks + generateICancellationSignal() + service
    }

    private fun generateAidlService(
        transactionCallbacks: List<InMemorySource>
    ): String {
        val transactionCallbackImports =
            transactionCallbacks.joinToString(separator = "\n|") {
                "import ${it.packageName}.${it.interfaceName};"
            }
        val generatedMethods = service().methods.joinToString(
            separator = "\n|    ", transform = ::generateAidlMethod
        )
        return """
                |package ${packageName()};
                |$transactionCallbackImports
                |oneway interface ${aidlNameForInterface(service())} {
                |    $generatedMethods
                |}
            """.trimMargin()
    }

    private fun generateAidlMethod(method: Method): String {
        val parameters =
            method.parameters.map(::generateAidlParameter) +
                "${callbackNameForType(method.returnType)} transactionCallback"
        return "void ${method.name}(${parameters.joinToString()});"
    }

    private fun generateAidlParameter(parameter: Parameter) =
        // TODO validate that parameter type is not Unit
        "${parameter.type.toAidlType()} ${parameter.name}"

    private fun generateTransactionCallbacks(): List<InMemorySource> {
        return service().methods.map(Method::returnType).toSet()
            .map { generateTransactionCallback(it) }
    }

    private fun generateTransactionCallback(type: Type): InMemorySource {
        val interfaceName = callbackNameForType(type)
        return InMemorySource(
            packageName = packageName(), interfaceName = interfaceName, fileContents = """
                    package ${packageName()};
                    import ${packageName()}.ICancellationSignal;
                    oneway interface $interfaceName {
                        void onCancellable(ICancellationSignal cancellationSignal);
                        void onSuccess(${type.toAidlType()?.let { "$it result" } ?: ""});
                        void onFailure(int errorCode, String errorMessage);
                    }
                """.trimIndent()
        )
    }

    private fun generateICancellationSignal() = InMemorySource(
        packageName = packageName(), interfaceName = "ICancellationSignal", fileContents = """
                package ${packageName()};
                oneway interface ICancellationSignal {
                    void cancel();
                }
            """.trimIndent()
    )

    private fun getAidlFile(rootPath: Path, aidlSource: InMemorySource) = Paths.get(
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

    private fun callbackNameForType(type: Type) =
        "I${type.name.split(".").last()}TransactionCallback"

    private fun service() = api.services.first()

    private fun packageName() = service().packageName
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

internal fun Type.toAidlType() = when (name) {
    Boolean::class.qualifiedName -> "boolean"
    Int::class.qualifiedName -> "int"
    Long::class.qualifiedName -> "long"
    Float::class.qualifiedName -> "float"
    Double::class.qualifiedName -> "double"
    String::class.qualifiedName -> "String"
    Char::class.qualifiedName -> "char"
    // TODO: AIDL doesn't support short, make sure it's handled correctly.
    Short::class.qualifiedName -> "int"
    Unit::class.qualifiedName -> null
    else -> throw IllegalArgumentException("Unsupported type conversion ${this.name}")
}
