/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import kotlin.reflect.KClass

/**
 * API for a Processor that is either backed by Java's Annotation Processing API or KSP.
 */
@ExperimentalProcessingApi
interface XProcessingEnv {

    val backend: Backend

    /**
     * The target language the processor is set to expect
     */
    val targetLanguage: Language

    /**
     * The logger interface to log messages
     */
    val messager: XMessager

    /**
     * List of options passed into the annotation processor
     */
    val options: Map<String, String>

    /**
     * The API to generate files
     */
    val filer: XFiler

    /**
     * Looks for the [XTypeElement] with the given qualified name and returns `null` if it does not
     * exist.
     */
    fun findTypeElement(qName: String): XTypeElement?

    /**
     * Looks for the [XType] with the given qualified name and returns `null` if it does not exist.
     */
    fun findType(qName: String): XType?

    /**
     * Returns the [XType] with the given qualified name or throws an exception if it does not
     * exist.
     */
    fun requireType(qName: String): XType = checkNotNull(findType(qName)) {
        "cannot find required type $qName"
    }

    /**
     * Returns the [XTypeElement] for the annotation that should be added to the generated code.
     */
    fun findGeneratedAnnotation(): XTypeElement?

    /**
     * Returns an [XType] for the given [type] element with the type arguments specified
     * as in [types].
     */
    fun getDeclaredType(type: XTypeElement, vararg types: XType): XType

    /**
     * Return an [XArrayType] that has [type] as the [XArrayType.componentType].
     */
    fun getArrayType(type: XType): XArrayType

    /**
     * Returns the [XTypeElement] with the given qualified name or throws an exception if it does
     * not exist.
     */
    fun requireTypeElement(qName: String): XTypeElement {
        return checkNotNull(findTypeElement(qName)) {
            "Cannot find required type element $qName"
        }
    }

    // helpers for smooth migration, these could be extension methods
    fun requireType(typeName: TypeName) = checkNotNull(findType(typeName)) {
        "cannot find required type $typeName"
    }

    fun requireType(klass: KClass<*>) = requireType(klass.java.canonicalName!!)

    fun findType(typeName: TypeName): XType? {
        // TODO we probably need more complicated logic here but right now room only has these
        //  usages.
        if (typeName is ArrayTypeName) {
            return findType(typeName.componentType)?.let {
                getArrayType(it)
            }
        }
        return findType(typeName.toString())
    }

    fun findType(klass: KClass<*>) = findType(klass.java.canonicalName!!)

    fun requireTypeElement(typeName: TypeName) = requireTypeElement(typeName.toString())

    fun requireTypeElement(klass: KClass<*>) = requireTypeElement(klass.java.canonicalName!!)

    fun findTypeElement(typeName: TypeName) = findTypeElement(typeName.toString())

    fun findTypeElement(klass: KClass<*>) = findTypeElement(klass.java.canonicalName!!)

    fun getArrayType(typeName: TypeName) = getArrayType(requireType(typeName))

    enum class Backend {
        JAVAC,
        KSP
    }

    /**
     * The target language for XProcessingEnv.
     * This value controls certain logic when processing code,
     * see the documentation for [XProcessingEnv.targetLanguage] for details.
     */
    enum class Language {
        JAVA,
        KOTLIN
    }

    companion object {
        /**
         * Creates a new [XProcessingEnv] implementation derived from the given Java [env].
         */
        @JvmStatic
        fun create(
            env: ProcessingEnvironment,
            targetLanguage: Language = Language.JAVA
        ): XProcessingEnv = JavacProcessingEnv(env, targetLanguage)

        /**
         * Creates a new [XProcessingEnv] implementation derived from the given KSP environment.
         */
        @JvmStatic
        fun create(
            options: Map<String, String>,
            resolver: Resolver,
            codeGenerator: CodeGenerator,
            logger: KSPLogger,
            targetLanguage: Language
        ): XProcessingEnv = KspProcessingEnv(
            options = options,
            codeGenerator = codeGenerator,
            logger = logger,
            resolver = resolver,
            targetLanguage = targetLanguage
        )
    }

    /**
     * Returns [XTypeElement]s with the given package name. Note that this call can be expensive.
     *
     * @param packageName the package name to look up.
     *
     * @return A list of [XTypeElement] with matching package name. This will return declarations
     * from both dependencies and source.
     */
    fun getTypeElementsFromPackage(packageName: String): List<XTypeElement>

    // TODO: Add support for getting top level members in a package
}
