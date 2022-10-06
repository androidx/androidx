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

import androidx.privacysandbox.tools.core.generator.poet.AidlFileSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlInterfaceSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlParcelableSpec
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.getOnlyService
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

    private val valueMap = api.values.associateBy { it.type }

    companion object {
        fun generate(
            aidlCompiler: AidlCompiler,
            api: ParsedApi,
            workingDir: Path,
        ): List<GeneratedSource> {
            return AidlGenerator(aidlCompiler, api, workingDir).generate()
        }

        const val cancellationSignalName = "ICancellationSignal"
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
            aidlFile.writeText(it.getFileContent())
            GeneratedSource(it.type.packageName, it.type.simpleName, aidlFile)
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

    private fun generateAidlContent(): List<AidlFileSpec> {
        val values = api.values.map(::generateValue)
        val transactionCallbacks = generateTransactionCallbacks()
        val typesToImport = buildSet {
            addAll(values.map { it.type })
            addAll(transactionCallbacks.map { it.type })
        }
        val service =
            AidlInterfaceSpec(
                type = Type(packageName(), api.getOnlyService().aidlName()),
                typesToImport = typesToImport,
                methods = api.getOnlyService().methods.map(::generateAidlMethod),
                oneway = false,
            )
        return transactionCallbacks + generateICancellationSignal() + service + values
    }

    private fun generateAidlMethod(method: Method): String {
        val parameters = buildList {
            addAll(method.parameters.map(::generateAidlParameter))
            if (method.isSuspend) {
                add("${method.returnType.transactionCallbackName()} transactionCallback")
            }
        }
        // TODO remove return type.
        val returnType = if (method.isSuspend) {
            "void"
        } else {
            getAidlTypeDeclaration(method.returnType)
        }

        return "$returnType ${method.name}(${parameters.joinToString()});"
    }

    private fun generateAidlParameter(parameter: Parameter): String {
        check(parameter.type != Types.unit) {
            "Void cannot be a parameter type."
        }
        val modifier = if (valueMap.containsKey(parameter.type)) "in " else ""
        return "$modifier${getAidlTypeDeclaration(parameter.type)} ${parameter.name}"
    }

    private fun generateTransactionCallbacks(): List<AidlFileSpec> {
        return api.getOnlyService().methods.filter(Method::isSuspend)
            .map(Method::returnType).toSet()
            .map { generateTransactionCallback(it) }
    }

    private fun generateTransactionCallback(type: Type): AidlFileSpec {
        val typesToImport = buildSet {
            add(cancellationSignalType())
            valueMap[type]?.let { add(it.aidlType()) }
        }

        val onSuccessParameter = if (type != Types.unit) {
            generateAidlParameter(Parameter("result", type))
        } else ""

        return AidlInterfaceSpec(
            type = Type(packageName(), type.transactionCallbackName()),
            typesToImport = typesToImport,
            methods = listOf(
                "void onCancellable(${cancellationSignalType().simpleName} cancellationSignal);",
                "void onSuccess($onSuccessParameter);",
                "void onFailure(int errorCode, String errorMessage);",
            )
        )
    }

    private fun generateICancellationSignal() = AidlInterfaceSpec(
        type = cancellationSignalType(),
        methods = listOf(
            "void cancel();"
        )
    )

    private fun generateValue(value: AnnotatedValue): AidlFileSpec {
        val typesToImport = value.properties.mapNotNull { valueMap[it.type]?.aidlType() }
            .toSet()
        return AidlParcelableSpec(
            type = value.aidlType(),
            typesToImport = typesToImport,
            properties = value.properties.map {
                "${getAidlTypeDeclaration(it.type)} ${it.name};"
            }
        )
    }

    private fun getAidlFile(rootPath: Path, aidlSource: AidlFileSpec) = Paths.get(
        rootPath.toString(),
        *aidlSource.type.packageName.split(".").toTypedArray(),
        aidlSource.type.simpleName + ".aidl"
    ).toFile()

    private fun getJavaFileForAidlFile(aidlFile: File): File {
        check(aidlFile.extension == "aidl") {
            "AIDL path has the wrong extension: ${aidlFile.extension}."
        }
        return aidlFile.resolveSibling("${aidlFile.nameWithoutExtension}.java")
    }

    private fun packageName() = api.getOnlyService().type.packageName
    private fun cancellationSignalType() = Type(packageName(), cancellationSignalName)
    private fun getAidlTypeDeclaration(type: Type): String {
        valueMap[type]?.let { return it.aidlType().simpleName }
        return when (type.qualifiedName) {
            Boolean::class.qualifiedName -> "boolean"
            Int::class.qualifiedName -> "int"
            Long::class.qualifiedName -> "long"
            Float::class.qualifiedName -> "float"
            Double::class.qualifiedName -> "double"
            String::class.qualifiedName -> "String"
            Char::class.qualifiedName -> "char"
            // TODO: AIDL doesn't support short, make sure it's handled correctly.
            Short::class.qualifiedName -> "int"
            Unit::class.qualifiedName -> "void"
            else -> throw IllegalArgumentException(
                "Unsupported type conversion ${type.qualifiedName}")
        }
    }
}

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

fun AnnotatedInterface.aidlName() = "I${type.simpleName}"

fun AnnotatedValue.aidlType() = Type(type.packageName, "Parcelable${type.simpleName}")

fun Type.transactionCallbackName() = "I${simpleName}TransactionCallback"
