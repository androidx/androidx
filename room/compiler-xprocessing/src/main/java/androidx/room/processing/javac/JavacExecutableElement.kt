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

package androidx.room.processing.javac

import androidx.room.processing.XDeclaredType
import androidx.room.processing.XExecutableElement
import androidx.room.processing.XExecutableType
import androidx.room.processing.XTypeElement
import androidx.room.processing.XVariableElement
import androidx.room.processing.javac.kotlin.KotlinMetadataElement
import androidx.room.processing.javac.kotlin.descriptor
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

internal class JavacExecutableElement(
    env: JavacProcessingEnv,
    val containing: JavacTypeElement,
    override val element: ExecutableElement
) : JavacElement(
    env,
    element
), XExecutableElement {
    private val kotlinMetadata by lazy {
        KotlinMetadataElement.createFor(element)
    }

    val descriptor by lazy {
        element.descriptor()
    }

    private val isSuspend by lazy {
        kotlinMetadata?.isSuspendFunction(element) == true
    }

    override val enclosingElement: XTypeElement
        get() = super.enclosingElement as XTypeElement

    override val parameters: List<JavacVariableElement> by lazy {
        val kotlinParamNames = kotlinMetadata?.getParameterNames(element)
        element.parameters.mapIndexed { index, variable ->
            JavacMethodParameter(
                env = env,
                containing = containing,
                element = variable,
                kotlinName = kotlinParamNames?.getOrNull(index)
            )
        }
    }

    override val returnType: JavacType by lazy {
        val asMember = env.typeUtils.asMemberOf(containing.type.typeMirror, element)
        val asExec = MoreTypes.asExecutable(asMember)
        env.wrap<JavacType>(asExec.returnType)
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(element, containing)
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

    override fun findKotlinDefaultImpl(): XExecutableElement? {
        fun paramsMatch(
            ourParams: List<XVariableElement>,
            theirParams: List<XVariableElement>
        ): Boolean {
            if (ourParams.size != theirParams.size - 1) {
                return false
            }
            ourParams.forEachIndexed { i, variableElement ->
                // Plus 1 to their index because their first param is a self object.
                if (!theirParams[i + 1].type.isSameType(
                        variableElement.type
                    )
                ) {
                    return false
                }
            }
            return true
        }
        return kotlinDefaultImplClass?.getDeclaredMethods()?.find {
            it.name == this.name && paramsMatch(parameters, it.parameters)
        }
    }

    override fun isSuspendFunction() = isSuspend

    override val executableType: JavacExecutableType by lazy {
        val asMemberOf = env.typeUtils.asMemberOf(containing.type.typeMirror, element)
        JavacExecutableType(
            env = env,
            executableType = MoreTypes.asExecutable(asMemberOf)
        )
    }

    override fun isJavaDefault() = element.modifiers.contains(Modifier.DEFAULT)

    override fun isVarArgs(): Boolean {
        return element.isVarArgs
    }

    override fun asMemberOf(other: XDeclaredType): XExecutableType {
        return if (containing.type.isSameType(other)) {
            executableType
        } else {
            check(other is JavacDeclaredType)
            val asMemberOf = env.typeUtils.asMemberOf(other.typeMirror, element)
            JavacExecutableType(
                env = env,
                executableType = MoreTypes.asExecutable(asMemberOf)
            )
        }
    }

    override fun isConstructor(): Boolean {
        return element.kind == ElementKind.CONSTRUCTOR
    }

    companion object {
        internal const val DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls"
    }
}
