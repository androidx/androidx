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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.javac.kotlin.KmFunctionContainer
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeVariable

internal class JavacMethodElement(
    env: JavacProcessingEnv,
    element: ExecutableElement
) : JavacExecutableElement(env, element),
    XMethodElement {
    init {
        check(element.kind == ElementKind.METHOD) {
            "Method element is constructed with invalid type: $element"
        }
    }

    override val propertyName: String? by lazy {
        if (isKotlinPropertyMethod()) kotlinMetadata?.propertyName else null
    }

    override val name: String by lazy {
        kotlinMetadata?.name ?: jvmName
    }

    override val jvmName: String
        get() = element.simpleName.toString()

    override val typeParameters: List<XTypeParameterElement> by lazy {
        element.typeParameters.mapIndexed { index, typeParameter ->
            val typeParameterMetadata = kotlinMetadata?.typeParameters?.get(index)
            JavacTypeParameterElement(env, this, typeParameter, typeParameterMetadata)
        }
    }

    override val parameters: List<JavacMethodParameter> by lazy {
        element.parameters.mapIndexed { index, variable ->
            JavacMethodParameter(
                env = env,
                enclosingElement = this,
                element = variable,
                kotlinMetadataFactory = {
                    val metadataParamIndex = if (isExtensionFunction()) index - 1 else index
                    kotlinMetadata?.parameters?.getOrNull(metadataParamIndex)
                },
                argIndex = index
            )
        }
    }

    override val kotlinMetadata: KmFunctionContainer? by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata?.getFunctionMetadata(element)
    }

    override val executableType: JavacMethodType by lazy {
        JavacMethodType.create(
            env = env,
            element = this,
            executableType = MoreTypes.asExecutable(element.asType())
        )
    }

    override val returnType: JavacType by lazy {
        env.wrap(
            typeMirror = element.returnType,
            kotlinType = if (isSuspendFunction()) {
                // Don't use Kotlin metadata for suspend functions since we want the Java
                // perspective. In Java, a suspend function returns Object and contains an extra
                // parameter of type Continuation<? extends T> where T is the actual return type as
                // declared in the Kotlin source.
                null
            } else {
                kotlinMetadata?.returnType
            },
            elementNullability = element.nullability
        )
    }

    val defaultValue: JavacAnnotationValue? = element.defaultValue?.let {
        JavacAnnotationValue(env, this, element.defaultValue, returnType)
    }

    override fun asMemberOf(other: XType): XMethodType {
        return if (other !is JavacDeclaredType || enclosingElement.type.isSameType(other)) {
            executableType
        } else {
            val asMemberOf = env.typeUtils.asMemberOf(other.typeMirror, element)
            JavacMethodType.create(
                env = env,
                element = this,
                executableType = MoreTypes.asExecutable(asMemberOf)
            )
        }
    }

    override fun isJavaDefault() = element.modifiers.contains(Modifier.DEFAULT)

    override fun isSuspendFunction() = kotlinMetadata?.isSuspend() == true

    override fun isExtensionFunction() = kotlinMetadata?.isExtension() == true

    override fun overrides(other: XMethodElement, owner: XTypeElement): Boolean {
        check(other is JavacMethodElement)
        check(owner is JavacTypeElement)
        if (
            env.backend == XProcessingEnv.Backend.JAVAC &&
            this.isSuspendFunction() &&
            other.isSuspendFunction()
        ) {
            // b/222240938 - Special case suspend functions in KAPT
            return suspendOverrides(element, other.element, owner.element, env.typeUtils)
        }
        // Use auto-common's overrides, which provides consistency across javac and ejc (Eclipse).
        return MoreElements.overrides(element, other.element, owner.element, env.typeUtils)
    }

    override fun hasKotlinDefaultImpl(): Boolean {
        fun paramsMatch(
            ourParams: List<JavacMethodParameter>,
            theirParams: List<JavacMethodParameter>
        ): Boolean {
            if (ourParams.size != theirParams.size - 1) {
                return false
            }
            return (ourParams.indices).all { paramIndex ->
                // Plus 1 to their index because their first param is a self object.
                // We specifically use `asType` here instead of XVariableElement.type because
                // we want to ignore the containing type (so that generics etc are NOT resolved to
                // the containing type)
                val theirParamType = theirParams[paramIndex + 1].element.asType()
                val ourParamType = ourParams[paramIndex].element.asType()
                if (env.typeUtils.isSameType(ourParamType, theirParamType)) {
                    true
                } else {
                    // if isSameType returns false, check for generics. b/199888180
                    val ourTypeVar = ourParamType as? TypeVariable
                    val theirTypeVar = theirParamType as? TypeVariable
                    ourTypeVar != null && theirTypeVar != null &&
                        env.typeUtils.isSameType(
                            ourTypeVar.lowerBound,
                            theirTypeVar.lowerBound
                        )
                }
            }
        }
        return kotlinDefaultImplClass?.getDeclaredMethods()?.any {
            it.jvmName == this.jvmName && paramsMatch(parameters, it.parameters)
        } ?: false
    }

    @Suppress("UnstableApiUsage")
    private val kotlinDefaultImplClass by lazy {
        val parent = element.enclosingElement as? TypeElement
        val defaultImplElement = parent?.enclosedElements?.find {
            MoreElements.isType(it) && it.simpleName.contentEquals(DEFAULT_IMPLS_CLASS_NAME)
        } as? TypeElement
        defaultImplElement?.let {
            env.wrapTypeElement(it)
        }
    }

    override fun isKotlinPropertyMethod() = kotlinMetadata?.isPropertyFunction() ?: false

    override fun isKotlinPropertySetter() = kotlinMetadata?.isPropertySetter() ?: false

    override fun isKotlinPropertyGetter() = kotlinMetadata?.isPropertyGetter() ?: false
}
