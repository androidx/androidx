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

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.kotlin.KmType
import com.google.auto.common.GeneratedAnnotations
import com.google.auto.common.MoreTypes
import java.util.Locale
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal class JavacProcessingEnv(
    val delegate: ProcessingEnvironment
) : XProcessingEnv {
    override val backend: XProcessingEnv.Backend = XProcessingEnv.Backend.JAVAC

    val elementUtils: Elements = delegate.elementUtils

    val typeUtils: Types = delegate.typeUtils

    private val typeElementStore =
        XTypeElementStore(
            findElement = { qName ->
                delegate.elementUtils.getTypeElement(qName)
            },
            wrap = { typeElement ->
                JavacTypeElement.create(this, typeElement)
            },
            getQName = {
                it.qualifiedName.toString()
            }
        )

    override val messager: XMessager by lazy {
        JavacProcessingEnvMessager(delegate)
    }

    override val filer = JavacFiler(delegate)

    override val options: Map<String, String>
        get() = delegate.options

    override fun findTypeElement(qName: String): JavacTypeElement? {
        return typeElementStore[qName]
    }

    override fun getTypeElementsFromPackage(packageName: String): List<XTypeElement> {
        // Note, to support Java Modules we would need to use "getAllPackageElements",
        // but that is only available in Java 9+.
        val packageElement = delegate.elementUtils.getPackageElement(packageName)

        return packageElement.enclosedElements
            .filterIsInstance<TypeElement>()
            .map { wrapTypeElement(it) }
    }

    override fun findType(qName: String): XType? {
        // check for primitives first
        PRIMITIVE_TYPES[qName]?.let {
            return wrap(
                typeMirror = typeUtils.getPrimitiveType(it),
                kotlinType = null,
                elementNullability = XNullability.NONNULL
            )
        }
        return findTypeElement(qName)?.type
    }

    override fun findGeneratedAnnotation(): XTypeElement? {
        val element = GeneratedAnnotations.generatedAnnotation(elementUtils, delegate.sourceVersion)
        return if (element.isPresent) {
            wrapTypeElement(element.get())
        } else {
            null
        }
    }

    override fun getArrayType(type: XType): JavacArrayType {
        check(type is JavacType) {
            "given type must be from java, $type is not"
        }
        return JavacArrayType(
            env = this,
            typeMirror = typeUtils.getArrayType(type.typeMirror),
            nullability = XNullability.UNKNOWN,
            knownComponentNullability = type.nullability
        )
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): JavacType {
        check(type is JavacTypeElement)
        val args = types.map {
            check(it is JavacType)
            it.typeMirror
        }.toTypedArray()
        check(
            types.all {
                it is JavacType
            }
        )
        return wrap<JavacDeclaredType>(
            typeMirror = typeUtils.getDeclaredType(type.element, *args),
            // type elements cannot have nullability hence we don't synthesize anything here
            kotlinType = null,
            elementNullability = type.element.nullability
        )
    }

    fun wrapTypeElement(element: TypeElement) = typeElementStore[element]

    /**
     * Wraps the given java processing type into an XType.
     *
     * @param typeMirror TypeMirror from java processor
     * @param kotlinType If the type is derived from a kotlin source code, the KmType information
     *                   parsed from kotlin metadata
     * @param elementNullability The nullability information parsed from the code. This value is
     *                           ignored if [kotlinType] is provided.
     */
    inline fun <reified T : JavacType> wrap(
        typeMirror: TypeMirror,
        kotlinType: KmType?,
        elementNullability: XNullability
    ): T {
        return when (typeMirror.kind) {
            TypeKind.ARRAY ->
                if (kotlinType == null) {
                    JavacArrayType(
                        env = this,
                        typeMirror = MoreTypes.asArray(typeMirror),
                        nullability = elementNullability,
                        knownComponentNullability = null
                    )
                } else {
                    JavacArrayType(
                        env = this,
                        typeMirror = MoreTypes.asArray(typeMirror),
                        kotlinType = kotlinType
                    )
                }
            TypeKind.DECLARED ->
                if (kotlinType == null) {
                    JavacDeclaredType(
                        env = this,
                        typeMirror = MoreTypes.asDeclared(typeMirror),
                        nullability = elementNullability
                    )
                } else {
                    JavacDeclaredType(
                        env = this,
                        typeMirror = MoreTypes.asDeclared(typeMirror),
                        kotlinType = kotlinType
                    )
                }
            else ->
                if (kotlinType == null) {
                    DefaultJavacType(
                        env = this,
                        typeMirror = typeMirror,
                        nullability = elementNullability
                    )
                } else {
                    DefaultJavacType(
                        env = this,
                        typeMirror = typeMirror,
                        kotlinType = kotlinType
                    )
                }
        } as T
    }

    internal fun wrapAnnotatedElement(
        element: Element,
        annotationName: String
    ): XElement {
        return when (element) {
            is VariableElement -> {
                wrapVariableElement(element)
            }
            is TypeElement -> {
                wrapTypeElement(element)
            }
            is ExecutableElement -> {
                wrapExecutableElement(element)
            }
            is PackageElement -> {
                error(
                    "Cannot get elements with annotation $annotationName. Package " +
                        "elements are not supported by XProcessing."
                )
            }
            else -> error("Unsupported element $element with annotation $annotationName")
        }
    }

    fun wrapExecutableElement(element: ExecutableElement): JavacExecutableElement {
        val enclosingType = element.requireEnclosingType(this)

        return when (element.kind) {
            ElementKind.CONSTRUCTOR -> {
                JavacConstructorElement(
                    env = this,
                    containing = enclosingType,
                    element = element
                )
            }
            ElementKind.METHOD -> {
                JavacMethodElement(
                    env = this,
                    containing = enclosingType,
                    element = element
                )
            }
            else -> error("Unsupported kind ${element.kind} of executable element $element")
        }
    }

    fun wrapVariableElement(element: VariableElement): JavacVariableElement {
        return when (val enclosingElement = element.enclosingElement) {
            is ExecutableElement -> {
                val executableElement = wrapExecutableElement(enclosingElement)

                executableElement.parameters.find { param ->
                    param.element === element
                } ?: error("Unable to create variable element for $element")
            }
            is TypeElement -> {
                JavacFieldElement(this, wrapTypeElement(enclosingElement), element)
            }
            else -> error("Unsupported enclosing type $enclosingElement for $element")
        }
    }

    companion object {
        val PRIMITIVE_TYPES = TypeKind.values().filter {
            it.isPrimitive
        }.associateBy {
            it.name.toLowerCase(Locale.US)
        }
    }
}
