/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.appsearch.compiler

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.compat.XConverters.toJavac
import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import com.google.auto.value.AutoValue
import com.squareup.javapoet.JavaFile
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.tools.Diagnostic

/**
 * Processes `androidx.appsearch.annotation.Document` annotations.
 *
 * Only plain Java objects and AutoValue Document classes without builders are supported.
 */
@SupportedAnnotationTypes(
    "${IntrospectionHelper.APPSEARCH_ANNOTATION_PKG}." +
        IntrospectionHelper.DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME
)
@SupportedOptions(
    AppSearchCompiler.OUTPUT_DIR_OPTION,
    AppSearchCompiler.RESTRICT_GENERATED_CODE_TO_LIB_OPTION,
)
@OptIn(ExperimentalProcessingApi::class)
class AppSearchCompiler : JavacBasicAnnotationProcessor() {
    companion object {
        /**
         * This property causes us to write output to a different folder instead of the usual filer
         * location. It should only be used for testing.
         */
        @VisibleForTesting const val OUTPUT_DIR_OPTION = "AppSearchCompiler.OutputDir"

        /**
         * This property causes us to annotate the generated classes with
         * [androidx.annotation.RestrictTo] with the scope
         * [androidx.annotation.RestrictTo.Scope.LIBRARY].
         *
         * In practice, this annotation only affects AndroidX tooling and so this option may only be
         * useful to other AndroidX libs.
         */
        @VisibleForTesting
        const val RESTRICT_GENERATED_CODE_TO_LIB_OPTION =
            "AppSearchCompiler.RestrictGeneratedCodeToLib"
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun processingSteps(): Iterable<XProcessingStep> {
        return listOf(AppSearchCompileStep(xProcessingEnv))
    }
}

@OptIn(ExperimentalProcessingApi::class)
private class AppSearchCompileStep(private val env: XProcessingEnv) : XProcessingStep {
    private val restrictGeneratedCodeToLibOption: Boolean =
        env.options[AppSearchCompiler.RESTRICT_GENERATED_CODE_TO_LIB_OPTION].toBoolean()

    private val outputDirOption: String? = env.options[AppSearchCompiler.OUTPUT_DIR_OPTION]

    private val documentClassMap = mutableMapOf<String, MutableList<String>>()
    // Annotation processing can be run in multiple rounds. This tracks the index of current
    // round starting from 0.
    private var roundIndex = -1

    override fun annotations() = setOf(IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.canonicalName)

    override fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<String, Set<XElement>>,
        isLastRound: Boolean,
    ): Set<XElement> {
        documentClassMap.clear()
        roundIndex += 1

        val documentElements: Set<XTypeElement> =
            elementsByAnnotation[IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.canonicalName]
                ?.filterIsInstance<XTypeElement>()
                ?.toSet() ?: emptySet()

        val nextRound = mutableSetOf<XElement>()
        var documentMapClassPackage: String? = null
        val classNames = mutableSetOf<String>()
        for (document in documentElements) {
            try {
                processDocument(document)
            } catch (_: MissingXTypeException) {
                // Save it for next round to wait for the AutoValue annotation processor to
                // be run first.
                nextRound.add(document)
            } catch (e: XProcessingException) {
                // Prints error message.
                e.printDiagnostic(env.messager)
            }
            classNames.add(document.qualifiedName)
            val packageName = document.packageElement.qualifiedName
            // We must choose a deterministic package to place the generated document map
            // class. Given multiple packages, we have no real preference between them. So
            // for the sake of making a deterministic selection, we always choose to generate
            // the map in the lexicographically smallest package.
            if (documentMapClassPackage == null || packageName < documentMapClassPackage) {
                documentMapClassPackage = packageName
            }
        }

        try {
            if (!classNames.isEmpty()) {
                checkNotNull(documentMapClassPackage)

                // Append the hash code of classNames and the index of the current round as a
                // suffix to the name of the generated document map class. This will prevent
                // the generation of two classes with the same name, which could otherwise
                // happen when there are two Java modules that contain classes in the same
                // package name, or there are multiple rounds of annotation processing for some
                // module.
                val classSuffix: String =
                    generateStringSetHash(classNames, /* delimiter= */ ",") + "_" + roundIndex
                writeJavaFile(
                    DocumentMapGenerator.generate(
                        env.toJavac(),
                        documentMapClassPackage,
                        classSuffix,
                        documentClassMap,
                        restrictGeneratedCodeToLibOption,
                    )
                )
            }
        } catch (e: NoSuchAlgorithmException) {
            env.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to create the AppSearch document map class: $e",
            )
        } catch (e: IOException) {
            env.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to create the AppSearch document map class: $e",
            )
        }

        // Pass elements to next round of processing.
        return nextRound
    }

    @Throws(IOException::class)
    fun writeJavaFile(javaFile: JavaFile) {
        if (outputDirOption.isNullOrBlank()) {
            javaFile.writeTo(env.toJavac().filer)
        } else {
            env.messager.printMessage(
                Diagnostic.Kind.NOTE,
                "Writing output to \"$outputDirOption\" " +
                    "due to the presence of -A${AppSearchCompiler.OUTPUT_DIR_OPTION}",
            )
            javaFile.writeTo(File(outputDirOption))
        }
    }

    /**
     * Process the document class by generating a factory class for it and properly update
     * [.mDocumentClassMap].
     */
    @Throws(XProcessingException::class, MissingXTypeException::class)
    fun processDocument(element: XTypeElement) {
        if (!element.isClass() && !element.isInterface()) {
            throw XProcessingException(
                "@Document annotation on something other than a class or an interface",
                element,
            )
        }

        val model: DocumentModel =
            if (element.hasAnnotation(AutoValue::class)) {
                // Document class is annotated as AutoValue class. For processing the AutoValue
                // class, we also need the generated class from AutoValue annotation processor.
                val generatedElement: XTypeElement? =
                    env.findTypeElement(getAutoValueGeneratedClassName(element))
                if (generatedElement == null) {
                    // Generated class is not found.
                    throw MissingXTypeException(element)
                } else {
                    DocumentModel.createAutoValueModel(env, element, generatedElement)
                }
            } else {
                // Non-AutoValue AppSearch Document class.
                DocumentModel.createPojoModel(env, element)
            }

        val generator = CodeGenerator(env, model, restrictGeneratedCodeToLibOption)
        try {
            writeJavaFile(generator.createJavaFile())
        } catch (e: IOException) {
            val pe = XProcessingException("Failed to write output", model.classElement)
            pe.initCause(e)
            throw pe
        }

        val documentClassList =
            documentClassMap.computeIfAbsent(model.schemaName) { mutableListOf() }
        documentClassList.add(
            env.toJavac().elementUtils.getBinaryName(element.toJavac()).toString()
        )
    }

    /**
     * Gets the generated class name of an AutoValue annotated class.
     *
     * This is the same naming strategy used by AutoValue's processor.
     */
    fun getAutoValueGeneratedClassName(element: XTypeElement): String {
        var type = element
        var name = type.name
        while (type.enclosingElement is XTypeElement) {
            type = type.enclosingElement as XTypeElement
            name = type.name + "_" + name
        }
        val pkg = type.packageElement.qualifiedName
        val dot = if (pkg.isEmpty()) "" else "."
        return pkg + dot + "AutoValue_" + name
    }

    /**
     * Generate a SHA-256 hash for a given string set.
     *
     * @param set The set of the strings.
     * @param delimiter The delimiter used to separate the strings, which should not have appeared
     *   in any of the strings in the set.
     */
    @Throws(NoSuchAlgorithmException::class)
    private fun generateStringSetHash(set: Set<String>, delimiter: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        for (s in set.sorted()) {
            md.update(s.toByteArray(StandardCharsets.UTF_8))
            md.update(delimiter.toByteArray(StandardCharsets.UTF_8))
        }
        val result = StringBuilder()
        for (b in md.digest()) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (hex.length == 1) {
                result.append('0')
            }
            result.append(hex)
        }
        return result.toString()
    }
}
