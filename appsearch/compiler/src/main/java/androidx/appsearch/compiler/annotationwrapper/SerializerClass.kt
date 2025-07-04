/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appsearch.compiler.annotationwrapper

import androidx.appsearch.compiler.ProcessingException
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/**
 * Represents a class that can convert between some custom type and a property's actual type.
 *
 * @see androidx.appsearch.app.StringSerializer
 * @see androidx.appsearch.app.LongSerializer
 */
data class SerializerClass(
    /** The kind of serializer. */
    val kind: Kind,

    /** The serializer class element. */
    val element: TypeElement,

    /** The zero-param constructor. Present on every serializer class. */
    val defaultConstructor: ExecutableElement,

    /** The custom type that can be serialized using the serializer class. */
    val customType: TypeMirror,
) {
    enum class Kind(
        /**
         * The actual type of the corresponding property within a `GenericDocument`.
         *
         * For example, a [.STRING_SERIALIZER] may only be used with a `@Document.StringProperty`
         * which, in turn, boils down to a [String] within a `GenericDocument`.
         */
        // TypeName is an immutable 3P type
        val actualTypeInGenericDoc: TypeName
    ) {
        STRING_SERIALIZER(actualTypeInGenericDoc = ClassName.get(String::class.java)),
        LONG_SERIALIZER(actualTypeInGenericDoc = TypeName.LONG),
    }

    companion object {
        /**
         * Creates a serializer class given its [TypeElement].
         *
         * @throws ProcessingException If the `clazz` does not have a zero-param constructor.
         */
        @Throws(ProcessingException::class)
        fun create(clazz: TypeElement, kind: Kind): SerializerClass {
            val deserializeMethod: ExecutableElement = findDeserializeMethod(clazz, kind)
            return SerializerClass(
                kind = kind,
                element = clazz,
                defaultConstructor = findDefaultConstructor(clazz),
                customType = deserializeMethod.returnType,
            )
        }

        /**
         * Returns the zero-param constructor in the `clazz`.
         *
         * @throws ProcessingException If no such constructor exists or it's private.
         */
        @Throws(ProcessingException::class)
        private fun findDefaultConstructor(clazz: TypeElement): ExecutableElement {
            val constructor: ExecutableElement =
                clazz.enclosedElements
                    .stream()
                    .filter { it.kind == ElementKind.CONSTRUCTOR }
                    .map { it as ExecutableElement }
                    .filter { it.parameters.isEmpty() }
                    .findFirst()
                    .orElseThrow<ProcessingException> {
                        ProcessingException(
                            "Serializer ${clazz.qualifiedName} must have a zero-param " +
                                "constructor",
                            clazz,
                        )
                    }
            if (constructor.modifiers.contains(Modifier.PRIVATE)) {
                throw ProcessingException(
                    "The zero-param constructor of serializer ${clazz.qualifiedName} must not " +
                        "be private",
                    constructor,
                )
            }
            return constructor
        }

        /** Returns the `T deserialize(PropertyType)` method. */
        private fun findDeserializeMethod(clazz: TypeElement, kind: Kind): ExecutableElement {
            return clazz.enclosedElements
                .stream()
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                // The type-system enforces there is one method satisfying these constraints
                .filter {
                    it.simpleName.contentEquals("deserialize") &&
                        !it.modifiers.contains(Modifier.STATIC) &&
                        // Direct equality check with the param's type should be sufficient.
                        // Don't need to allow for subtypes because mActualTypeInGenericDoc can
                        // only be a primitive type or String which is a final class.
                        hasSingleParamOfExactType(it, kind.actualTypeInGenericDoc)
                }
                .findFirst()
                // Should never throw because param type is enforced by the type-system
                .get()
        }

        private fun hasSingleParamOfExactType(
            method: ExecutableElement,
            expectedType: TypeName,
        ): Boolean {
            if (method.parameters.size != 1) {
                return false
            }
            val firstParamType = TypeName.get(method.parameters.first().asType())
            return firstParamType == expectedType
        }
    }
}
