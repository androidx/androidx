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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspElement
import androidx.room.compiler.processing.ksp.KspFieldElement
import androidx.room.compiler.processing.ksp.KspHasModifiers
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspTypeElement
import java.util.Locale
import kotlin.reflect.KClass

/**
 * Kotlin properties don't have getters/setters in KSP. As Room expects Java code, we synthesize
 * them.
 *
 * @see KspSyntheticPropertyMethodElement.Getter
 * @see KspSyntheticPropertyMethodElement.Setter
 */
internal sealed class KspSyntheticPropertyMethodElement(
    env: KspProcessingEnv,
    val field: KspFieldElement
) : KspElement(
    env = env,
    declaration = field.declaration
), XMethodElement, XHasModifiers by KspHasModifiers(
    field.declaration
) {
    // NOTE: modifiers of the property are not necessarily my modifiers.
    //  that being said, it only matters if it is private in which case KAPT does not generate the
    //  synthetic hence we don't either.
    final override fun isJavaDefault() = false

    final override fun hasKotlinDefaultImpl() = false

    final override fun isSuspendFunction() = false

    final override val enclosingTypeElement: XTypeElement
        get() = this.field.enclosingTypeElement

    final override fun isVarArgs() = false

    final override val executableType: XMethodType
        get() = TODO()

    final override fun asMemberOf(other: XDeclaredType): XMethodType {
        TODO()
    }

    internal class Getter(
        env: KspProcessingEnv,
        field: KspFieldElement
    ) : KspSyntheticPropertyMethodElement(
        env = env,
        field = field
    ) {
        override val equalityItems: Array<out Any?> by lazy {
            arrayOf(field, "getter")
        }

        override val name: String by lazy {
            // see https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
            val propName = field.name
            if (propName.startsWith("is")) {
                propName
            } else {
                "get${propName.capitalize(Locale.US)}"
            }
        }

        override val returnType: XType by lazy {
            field.type
        }

        override val parameters: List<XExecutableParameterElement>
            get() = emptyList()

        override fun overrides(other: XMethodElement, owner: XTypeElement): Boolean {
            return other is Getter &&
                field.declaration.overrides(other.field.declaration)
        }

        override fun copyTo(newContainer: XTypeElement): XMethodElement {
            check(newContainer is KspTypeElement)
            return Getter(
                env = env,
                field = field.copyTo(newContainer)
            )
        }
    }

    internal class Setter(
        env: KspProcessingEnv,
        field: KspFieldElement
    ) : KspSyntheticPropertyMethodElement(
        env = env,
        field = field
    ) {
        override val equalityItems: Array<out Any?> by lazy {
            arrayOf(field, "setter")
        }

        override val name: String by lazy {
            // see https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
            val propName = field.name
            if (propName.startsWith("is")) {
                "set${propName.substring(2)}"
            } else {
                "set${propName.capitalize(Locale.US)}"
            }
        }

        override val returnType: XType by lazy {
            env.wrap(
                env.resolver.builtIns.unitType
            )
        }

        override val parameters: List<XExecutableParameterElement> by lazy {
            listOf(
                SyntheticExecutableParameterElement(
                    this
                )
            )
        }

        override fun overrides(other: XMethodElement, owner: XTypeElement): Boolean {
            return other is Setter &&
                field.declaration.overrides(other.field.declaration)
        }

        override fun copyTo(newContainer: XTypeElement): XMethodElement {
            check(newContainer is KspTypeElement)
            return Setter(
                env = env,
                field = field.copyTo(newContainer)
            )
        }

        private class SyntheticExecutableParameterElement(
            private val origin: Setter
        ) : XExecutableParameterElement {
            override val name: String
                get() = origin.name
            override val type: XType
                get() = origin.field.type

            override fun asMemberOf(other: XDeclaredType): XType {
                return origin.field.asMemberOf(other)
            }

            override fun kindName(): String {
                return "method parameter"
            }

            override fun <T : Annotation> toAnnotationBox(
                annotation: KClass<T>
            ): XAnnotationBox<T>? {
                TODO("Not yet implemented")
            }

            override fun hasAnnotationWithPackage(pkg: String): Boolean {
                TODO("Not yet implemented")
            }

            override fun hasAnnotation(annotation: KClass<out Annotation>): Boolean {
                TODO("Not yet implemented")
            }
        }
    }
}