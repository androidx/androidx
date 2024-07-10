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

import androidx.room.compiler.codegen.JArrayTypeName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KClassName
import javax.annotation.processing.ProcessingEnvironment
import kotlin.reflect.KClass

/** API for a Processor that is either backed by Java's Annotation Processing API or KSP. */
@ExperimentalProcessingApi
interface XProcessingEnv {

    val backend: Backend

    /** The logger interface to log messages */
    val messager: XMessager

    /** List of options passed into the annotation processor */
    val options: Map<String, String>

    /** The API to generate files */
    val filer: XFiler

    /** Configuration to control certain behaviors of XProcessingEnv. */
    val config: XProcessingEnvConfig

    /**
     * Java language version of the processing environment.
     *
     * Value is the common JDK version representation even for the older JVM Specs named using the
     * 1.x notation. i.e. for '1.8' this return 8, for '11' this returns 11, etc.
     */
    val jvmVersion: Int

    /**
     * Information of target platforms of the processing environment.
     *
     * There can be multiple platforms in a metadata compilation. This is due to the fact that when
     * processing `common` source sets (which will be used to compile to multiple platforms), the
     * `targetPlatforms` set will contain an entry for each of the platforms the `common` code will
     * be used for.
     *
     * If a non-common source set (e.g. linuxX64) is being processed, then `targetPlatforms` will
     * contain only one entry that corresponds to the platform.
     *
     * For details, see the official Kotlin documentation at
     * https://kotlinlang.org/docs/ksp-multiplatform.html#compilation-and-processing.
     */
    val targetPlatforms: Set<Platform>

    /**
     * Looks for the [XTypeElement] with the given qualified name and returns `null` if it does not
     * exist.
     */
    fun findTypeElement(qName: String): XTypeElement?

    /**
     * Looks for the [XType] with the given qualified name and returns `null` if it does not exist.
     */
    fun findType(qName: String): XType?

    /** Returns the [XTypeElement] for the annotation that should be added to the generated code. */
    fun findGeneratedAnnotation(): XTypeElement?

    /**
     * Returns an [XType] for the given [type] element with the type arguments specified as in
     * [types].
     */
    fun getDeclaredType(type: XTypeElement, vararg types: XType): XType

    /**
     * Returns an [XType] representing a wildcard type.
     *
     * In Java source, this represents types like `?`, `? extends T`, and `? super T`.
     *
     * In Kotlin source, this represents types like `*`, `out T`, and `in T`.
     */
    fun getWildcardType(consumerSuper: XType? = null, producerExtends: XType? = null): XType

    /** Return an [XArrayType] that has [type] as the [XArrayType.componentType]. */
    fun getArrayType(type: XType): XArrayType

    /**
     * Returns the [XTypeElement] with the given qualified name or throws an exception if it does
     * not exist.
     */
    fun requireTypeElement(qName: String): XTypeElement {
        return checkNotNull(findTypeElement(qName)) { "Cannot find required type element $qName" }
    }

    fun requireTypeElement(typeName: XTypeName): XTypeElement {
        return checkNotNull(findTypeElement(typeName)) {
            "Cannot find required type element $typeName"
        }
    }

    fun requireTypeElement(klass: KClass<*>) = requireTypeElement(klass.java.canonicalName!!)

    @Deprecated(
        message = "Prefer using XTypeName or String overload instead of JavaPoet.",
        replaceWith = ReplaceWith(expression = "requireTypeElement(typeName.toString())")
    )
    fun requireTypeElement(typeName: JTypeName) = requireTypeElement(typeName.toString())

    fun findTypeElement(typeName: XTypeName): XTypeElement? {
        if (typeName.isPrimitive) {
            return findTypeElement(typeName.java.toString())
        }
        return when (backend) {
            Backend.JAVAC -> {
                val jClassName =
                    typeName.java as? JClassName
                        ?: error("Cannot find required type element ${typeName.java}")
                findTypeElement(jClassName.canonicalName())
            }
            Backend.KSP -> {
                val kClassName =
                    typeName.kotlin as? KClassName
                        ?: error("Cannot find required type element ${typeName.kotlin}")
                findTypeElement(kClassName.canonicalName)
            }
        }
    }

    fun findTypeElement(klass: KClass<*>) = findTypeElement(klass.java.canonicalName!!)

    @Deprecated(
        message = "Prefer using XTypeName or String overload instead of JavaPoet.",
        replaceWith = ReplaceWith(expression = "findTypeElement(typeName.toString())")
    )
    fun findTypeElement(typeName: JTypeName) = findTypeElement(typeName.toString())

    /**
     * Returns the [XType] with the given qualified name or throws an exception if it does not
     * exist.
     */
    fun requireType(qName: String): XType =
        checkNotNull(findType(qName)) { "cannot find required type $qName" }

    fun requireType(typeName: XTypeName): XType =
        checkNotNull(findType(typeName)) { "cannot find required type $typeName" }

    fun requireType(klass: KClass<*>) = requireType(klass.java.canonicalName!!)

    @Deprecated(
        message = "Prefer using XTypeName or String overload instead of JavaPoet.",
        replaceWith = ReplaceWith(expression = "requireType(typeName.toString())")
    )
    fun requireType(typeName: JTypeName) =
        checkNotNull(findType(typeName.toString())) { "cannot find required type $typeName" }

    fun findType(typeName: XTypeName): XType? {
        if (typeName.isPrimitive) {
            return findType(typeName.java.toString())
        }
        val jTypeName = typeName.java
        if (jTypeName is JArrayTypeName) {
            return findType(jTypeName.componentType.toString())?.let { getArrayType(it) }
        }
        return when (backend) {
            Backend.JAVAC -> {
                val jClassName =
                    typeName.java as? JClassName
                        ?: error("Cannot find required type element ${typeName.java}")
                findType(jClassName.canonicalName())
            }
            Backend.KSP -> {
                val kClassName =
                    typeName.kotlin as? KClassName
                        ?: error("Cannot find required type ${typeName.kotlin}")
                findType(kClassName.canonicalName)
            }
        }?.let {
            when (typeName.nullability) {
                XNullability.NULLABLE -> it.makeNullable()
                XNullability.NONNULL -> it.makeNonNullable()
                XNullability.UNKNOWN -> it
            }
        }
    }

    fun findType(klass: KClass<*>) = findType(klass.java.canonicalName!!)

    @Deprecated(
        message = "Prefer using XTypeName or String overload instead of JavaPoet.",
        replaceWith = ReplaceWith(expression = "findType(typeName.toString())")
    )
    fun findType(typeName: JTypeName): XType? {
        if (typeName is JArrayTypeName) {
            return findType(typeName.componentType.toString())?.let { getArrayType(it) }
        }
        return findType(typeName.toString())
    }

    fun getArrayType(typeName: XTypeName) = getArrayType(requireType(typeName))

    @Deprecated("Prefer using XTypeName or String overload instead of JavaPoet.")
    fun getArrayType(typeName: JTypeName) = getArrayType(requireType(typeName.toString()))

    enum class Backend {
        JAVAC,
        KSP
    }

    enum class Platform {
        JVM,
        NATIVE,
        JS,
        UNKNOWN
    }

    companion object {
        /** Creates a new [XProcessingEnv] implementation derived from the given Java [env]. */
        @JvmStatic
        @JvmOverloads
        fun create(
            env: ProcessingEnvironment,
            config: XProcessingEnvConfig = XProcessingEnvConfig.DEFAULT
        ): XProcessingEnv = JavacProcessingEnv(env, config)

        /** Creates a new [XProcessingEnv] implementation derived from the given KSP environment. */
        @JvmStatic
        @JvmOverloads
        fun create(
            symbolProcessorEnvironment: SymbolProcessorEnvironment,
            resolver: Resolver,
            config: XProcessingEnvConfig = XProcessingEnvConfig.DEFAULT
        ): XProcessingEnv =
            KspProcessingEnv(delegate = symbolProcessorEnvironment, config = config).also {
                it.resolver = resolver
            }
    }

    /**
     * Returns [XTypeElement]s with the given package name. Note that this call can be expensive.
     *
     * @param packageName the package name to look up.
     * @return A list of [XTypeElement] with matching package name. This will return declarations
     *   from both dependencies and source. If the package is not found an empty list will be
     *   returned.
     */
    fun getTypeElementsFromPackage(packageName: String): List<XTypeElement>

    /**
     * Returns [XElement]s with the given package name. Note that this call can be expensive.
     *
     * @param packageName the package name to look up.
     * @return A list of [XElement] with matching package name. This will return declarations from
     *   both dependencies and source. If the package is not found an empty list will be returned.
     */
    fun getElementsFromPackage(packageName: String): List<XElement>
}
