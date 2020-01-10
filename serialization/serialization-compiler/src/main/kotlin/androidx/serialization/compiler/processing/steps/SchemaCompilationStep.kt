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

package androidx.serialization.compiler.processing.steps

import androidx.serialization.Action
import androidx.serialization.EnumValue
import androidx.serialization.Field
import androidx.serialization.Reserved
import androidx.serialization.Reserved.IdRange
import androidx.serialization.compiler.processing.asTypeElement
import androidx.serialization.compiler.processing.asVariableElement
import androidx.serialization.compiler.processing.error
import androidx.serialization.compiler.processing.get
import androidx.serialization.compiler.processing.getAnnotationArray
import androidx.serialization.compiler.processing.getAnnotationMirror
import androidx.serialization.compiler.processing.isPrivate
import androidx.serialization.compiler.schema.Enum
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementKind.ENUM_CONSTANT
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass
import androidx.serialization.schema.Reserved as SchemaReserved

/**
 * Processes and validates schema annotations.
 *
 * The discovered schema can be further processed by later steps or used for code generation or
 * schema tracking.
 */
internal class SchemaCompilationStep(
    private val processingEnv: ProcessingEnvironment
) : AbstractStep(Action::class, Field::class, EnumValue::class, Reserved::class) {
    // TODO: Replace with callbacks or similar
    /** Enums discovered in the lifetime of this step. */
    internal val enums = mutableSetOf<Enum>()

    private val messager: Messager = processingEnv.messager

    override fun process(elementsByAnnotation: Map<KClass<out Annotation>, Set<Element>>) {
        elementsByAnnotation[EnumValue::class]
            ?.let(::processEnumValues)
            ?.forEach(::processEnumType)
    }

    /**
     * Process [EnumValue] annotations present in a round.
     *
     * This method validates the placement of the annotations on enum constants, and returns a
     * set of type elements representing the enum classes that have at least one [EnumValue]
     * annotation on an enum constant.
     */
    private fun processEnumValues(elements: Set<Element>): Set<TypeElement> {
        if (elements.isEmpty()) return emptySet()

        val types = mutableSetOf<TypeElement>()

        for (element in elements) {
            if (element.kind == ENUM_CONSTANT) {
                types += element.enclosingElement.asTypeElement()
            } else {
                messager.error(element, EnumValue::class) {
                    "@${EnumValue::class.simpleName} must annotate an enum constant"
                }
            }
        }

        return types
    }

    /**
     * Process a type element representing a serializable enum class.
     *
     * This method operates by validating that the enum class is not private, then walking its
     * enum constants. It validates that all constants have an [EnumValue] annotation, and then
     * reads [EnumValue.id] and constructs an [Enum] that it adds to [enums]. It fills
     * [Enum.reserved] using [processReserved].
     */
    private fun processEnumType(typeElement: TypeElement) {
        check(typeElement.kind == ElementKind.ENUM) {
            "Expected $typeElement to be an enum class"
        }

        var hasError = false

        if (typeElement.isPrivate()) {
            messager.error(typeElement) {
                "Enum ${typeElement.qualifiedName} is private and cannot be serialized"
            }
            hasError = true
        }

        val values = mutableSetOf<Enum.Value>()

        for (element in typeElement.enclosedElements) {
            if (element.kind == ENUM_CONSTANT) {
                val annotation = element.getAnnotationMirror(EnumValue::class)

                if (annotation != null) {
                    values += Enum.Value(element.asVariableElement(), annotation[EnumValue::id])
                } else {
                    messager.error(element) {
                        "To avoid unexpected behavior, all enum constants in a serializable enum " +
                                "must be annotated with @${EnumValue::class.simpleName}"
                    }
                    hasError = true
                }
            }
        }

        if (!hasError) enums += Enum(typeElement, values, processReserved(typeElement))
    }

    /**
     * Extract the data from a [Reserved] annotation on [element].
     *
     * If no [Reserved] annotation is present, this returns an empty reserved data class. If it
     * encounters an [IdRange] with its `from` greater than its `to`, it reverses them before
     * converting them to an [IntRange], reserving the same range of IDs as if they had been
     * correctly placed.
     */
    private fun processReserved(element: Element): SchemaReserved {
        val annotation = element.getAnnotationMirror(Reserved::class)

        return if (annotation != null) {
            SchemaReserved(
                ids = annotation[Reserved::ids].toSet(),
                names = annotation[Reserved::names].toSet(),
                idRanges = annotation.getAnnotationArray(Reserved::idRanges).map { idRange ->
                    val from = idRange[IdRange::from]
                    val to = idRange[IdRange::to]

                    when {
                        from <= to -> from..to
                        else -> to..from
                    }
                }.toSet()
            )
        } else {
            SchemaReserved.empty()
        }
    }
}
