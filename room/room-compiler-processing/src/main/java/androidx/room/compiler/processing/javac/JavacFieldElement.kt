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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.javac.kotlin.KmPropertyContainer
import androidx.room.compiler.processing.javac.kotlin.KmTypeContainer
import androidx.room.compiler.processing.javac.kotlin.descriptor
import javax.lang.model.element.VariableElement

internal class JavacFieldElement(
    env: JavacProcessingEnv,
    element: VariableElement
) : JavacVariableElement(env, element), XFieldElement {
    override val name: String
        get() = kotlinMetadata?.name ?: element.simpleName.toString()

    override fun getAllAnnotations(): List<XAnnotation> {
        return buildList {
            addAll(super.getAllAnnotations())
            // For kotlin sources, annotations placed on properties will appear on synthetic
            // "$annotations" methods in the KAPT stub rather than on the field so we append these
            // annotations to match KSP. Note that the synthetic "$annotations" method isn't
            // accessible on precompiled classes in KAPT due to
            // https://youtrack.jetbrains.com/issue/KT-34684, so they will still be missing in that
            // case, but there's nothing we can really do about that.
            syntheticMethodForAnnotations?.let { methodForAnnotations ->
                addAll(
                    methodForAnnotations.getAllAnnotations()
                        .filter { it.qualifiedName != "java.lang.Deprecated" }
                        .toList()
                )
            }
        }
    }

    override val kotlinMetadata: KmPropertyContainer? by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata?.getPropertyMetadata(element)
    }

    private val syntheticMethodForAnnotations: JavacMethodElement? by lazy {
        (enclosingElement as? JavacTypeElement)
            ?.getSyntheticMethodsForAnnotations()
            ?.singleOrNull { it.name == kotlinMetadata?.syntheticMethodForAnnotations?.name }
    }

    override val kotlinType: KmTypeContainer?
        get() = kotlinMetadata?.type

    override val enclosingElement: JavacTypeElement by lazy {
        element.requireEnclosingType(env)
    }

    override val closestMemberContainer: JavacTypeElement
        get() = enclosingElement

    override val jvmDescriptor: String
        get() = element.descriptor(env.delegate)

    override val getter: XMethodElement? by lazy {
        kotlinMetadata?.getter?.let { getterMetadata ->
            enclosingElement.getDeclaredMethods()
                .filter { it.isKotlinPropertyMethod() }
                .firstOrNull { method -> method.jvmName == getterMetadata.jvmName }
        }
    }

    override val setter: XMethodElement? by lazy {
        kotlinMetadata?.setter?.let { setterMetadata ->
            enclosingElement.getDeclaredMethods()
                .filter { it.isKotlinPropertyMethod() }
                .firstOrNull { method -> method.jvmName == setterMetadata.jvmName }
        }
    }
}
