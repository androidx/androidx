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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.isValidJavaSourceName

/**
 * Represents a method in a class / interface.
 *
 * @see XConstructorElement
 * @see XMethodElement
 */
interface XMethodElement : XExecutableElement {
    /**
     * The name of the method in JVM.
     *
     * Use this property when you need to generate Java code accessing this method.
     *
     * For Kotlin sources, this might be different from name in source if:
     * * Function is annotated with @JvmName
     * * Function has a value class as a parameter or return type
     * * Function is internal
     *
     * Note that accessing this property requires resolving jvmName for Kotlin sources, which is an
     * expensive operation that includes type resolution (in KSP).
     */
    val jvmName: String

    /**
     * The return type for the method. Note that it might be [XType.isNone] if it does not return or
     * [XType.isError] if the return type cannot be resolved.
     */
    val returnType: XType

    /**
     * The property name if this is a setter/getter method for a kotlin property.
     */
    val propertyName: String?

    /**
     * The type representation of the method where more type parameters might be resolved.
     */
    override val executableType: XMethodType

    override val fallbackLocationText: String
        get() = buildString {
            append(enclosingElement.fallbackLocationText)
            append(".")
            append(jvmName)
            append("(")
            // don't report last parameter if it is a suspend function
            append(
                parameters.dropLast(
                    if (isSuspendFunction()) 1 else 0
                ).joinToString(", ") {
                    it.type.asTypeName().java.toString()
                }
            )
            append(")")
        }

    /**
     * Returns true if this method has the default modifier.
     *
     * @see [hasKotlinDefaultImpl]
     */
    fun isJavaDefault(): Boolean

    /**
     * Returns the method as if it is declared in [other].
     *
     * This is specifically useful if you have a method that has type arguments and there is a
     * subclass ([other]) where type arguments are specified to actual types.
     */
    override fun asMemberOf(other: XType): XMethodType

    /**
     * Returns true if this method has a default implementation in Kotlin.
     *
     * To support default methods in interfaces, Kotlin generates a delegate implementation. In
     * Java, we find the DefaultImpls class to delegate the call. In kotlin, we get this information
     * from KSP.
     */
    fun hasKotlinDefaultImpl(): Boolean

    /**
     * Returns true if this is a suspend function.
     *
     * @see XSuspendMethodType
     */
    fun isSuspendFunction(): Boolean

    /**
     * Returns true if this is an extension function.
     */
    fun isExtensionFunction(): Boolean

    /**
     * Returns true if this method can be overridden without checking its enclosing [XElement].
     */
    fun isOverrideableIgnoringContainer(): Boolean {
        return !isFinal() && !isPrivate() && !isStatic()
    }

    /**
     * Returns `true` if this method overrides the [other] method when this method is viewed as
     * member of the [owner].
     */
    fun overrides(other: XMethodElement, owner: XTypeElement): Boolean

    /**
     * If true, this method can be invoked from Java sources. This is especially important for
     * Kotlin functions that receive value class as a parameter as they cannot be called from Java
     * sources.
     *
     * Note that calling this method requires resolving jvmName for Kotlin sources, which is an
     * expensive operation that includes type resolution (in KSP).
     */
    fun hasValidJvmSourceName() = jvmName.isValidJavaSourceName()

    /**
     * Returns true if this method is a Kotlin property getter or setter.
     *
     * Note that if the origin is a Java source this function will always return `false` even if
     * the method name matches the property naming convention.
     */
    fun isKotlinPropertyMethod(): Boolean

    /**
     * Returns true if this method is a Kotlin property setter.
     */
    fun isKotlinPropertySetter(): Boolean

    /**
     * Returns true if this method is a Kotlin property getter.
     */
    fun isKotlinPropertyGetter(): Boolean
}

internal fun <T : XMethodElement> List<T>.filterMethodsByConfig(
    env: XProcessingEnv
): List<T> = if (env.config.excludeMethodsWithInvalidJvmSourceNames) {
    filter {
        it.hasValidJvmSourceName()
    }
} else {
    this
}
