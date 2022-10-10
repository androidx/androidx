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
import androidx.privacysandbox.tools.core.generator.poet.AidlInterfaceSpec.Companion.aidlInterface
import androidx.privacysandbox.tools.core.generator.poet.AidlMethodSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlParcelableSpec
import androidx.privacysandbox.tools.core.generator.poet.AidlTypeSpec
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
    private val callbackMap = api.callbacks.associateBy { it.type }

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
        val service = aidlInterface(api.getOnlyService())
        val customCallbacks = api.callbacks.map(::aidlInterface)
        return transactionCallbacks +
            generateICancellationSignal() +
            service +
            values +
            customCallbacks
    }

    private fun aidlInterface(annotatedInterface: AnnotatedInterface) =
        aidlInterface(Type(annotatedInterface.type.packageName, annotatedInterface.aidlName())) {
            annotatedInterface.methods.forEach { addMethod(it) }
        }

    private fun AidlInterfaceSpec.Builder.addMethod(method: Method) {
        addMethod(method.name) {
            method.parameters.forEach { addParameter(it) }
            if (method.isSuspend) {
                addParameter("transactionCallback", transactionCallback(method.returnType))
            }
        }
    }

    private fun AidlMethodSpec.Builder.addParameter(parameter: Parameter) {
        check(parameter.type != Types.unit) {
            "Void cannot be a parameter type."
        }
        addParameter(
            parameter.name,
            getAidlTypeDeclaration(parameter.type),
            isIn = valueMap.containsKey(parameter.type)
        )
    }

    private fun generateTransactionCallbacks(): List<AidlFileSpec> {
        return api.getOnlyService().methods.filter(Method::isSuspend)
            .map(Method::returnType).toSet()
            .map { generateTransactionCallback(it) }
    }

    private fun generateTransactionCallback(type: Type): AidlFileSpec {
        return aidlInterface(Type(packageName(), type.transactionCallbackName())) {
            addMethod("onCancellable") {
                addParameter("cancellationSignal", cancellationSignalType())
            }
            addMethod("onSuccess") {
                if (type != Types.unit) addParameter(Parameter("result", type))
            }
            addMethod("onFailure") {
                addParameter("errorCode", primitive("int"))
                addParameter("errorMessage", primitive("String"))
            }
        }
    }

    private fun generateICancellationSignal() = aidlInterface(cancellationSignalType().innerType) {
        addMethod("cancel")
    }

    private fun generateValue(value: AnnotatedValue): AidlFileSpec {
        val typesToImport =
            value.properties.mapNotNull { valueMap[it.type]?.aidlType()?.innerType }.toSet()
        return AidlParcelableSpec(type = value.aidlType().innerType,
            typesToImport = typesToImport,
            properties = value.properties.map {
                "${getAidlTypeDeclaration(it.type)} ${it.name};"
            })
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
    private fun cancellationSignalType() = AidlTypeSpec(Type(packageName(), cancellationSignalName))
    private fun transactionCallback(type: Type) =
        AidlTypeSpec(Type(api.getOnlyService().type.packageName, type.transactionCallbackName()))

    private fun getAidlTypeDeclaration(type: Type): AidlTypeSpec {
        valueMap[type]?.let { return it.aidlType() }
        callbackMap[type]?.let { return it.aidlType() }
        return when (type.qualifiedName) {
            Boolean::class.qualifiedName -> primitive("boolean")
            Int::class.qualifiedName -> primitive("int")
            Long::class.qualifiedName -> primitive("long")
            Float::class.qualifiedName -> primitive("float")
            Double::class.qualifiedName -> primitive("double")
            String::class.qualifiedName -> primitive("String")
            Char::class.qualifiedName -> primitive("char")
            // TODO: AIDL doesn't support short, make sure it's handled correctly.
            Short::class.qualifiedName -> primitive("int")
            Unit::class.qualifiedName -> primitive("void")
            else -> throw IllegalArgumentException(
                "Unsupported type conversion ${type.qualifiedName}"
            )
        }
    }
}

data class GeneratedSource(val packageName: String, val interfaceName: String, val file: File)

internal fun File.ensureDirectory() {
    check(exists()) {
        "$this doesn't exist"
    }
    check(isDirectory) {
        "$this is not a directory"
    }
}

fun AnnotatedInterface.aidlName() = "I${type.simpleName}"

fun Type.transactionCallbackName() = "I${simpleName}TransactionCallback"

internal fun AnnotatedValue.aidlType() =
    AidlTypeSpec(Type(type.packageName, "Parcelable${type.simpleName}"))

internal fun AnnotatedInterface.aidlType() = AidlTypeSpec(Type(type.packageName, aidlName()))

internal fun primitive(name: String) = AidlTypeSpec(Type("", name), requiresImport = false)
