/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.appsearch.compiler

import androidx.appsearch.compiler.AnnotatedGetterOrField.Companion.tryCreateFor
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.DocumentPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation
import com.google.auto.common.MoreTypes
import com.google.auto.value.AutoValue
import com.squareup.javapoet.ClassName
import java.util.ArrayDeque
import java.util.Deque
import java.util.WeakHashMap
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.metadata.isNullable
import kotlin.metadata.jvm.KotlinClassMetadata

/** Utilities for working with data structures representing parsed Java code. */
class IntrospectionHelper internal constructor(private val env: ProcessingEnvironment) {
    private val typeUtils: Types = env.typeUtils
    private val elementUtils: Elements = env.elementUtils

    // Non-boxable objects
    val blobHandleType: TypeMirror =
        elementUtils.getTypeElement(APPSEARCH_BLOB_HANDLE_CLASS.canonicalName()).asType()
    val collectionType: TypeMirror =
        elementUtils.getTypeElement(java.util.Collection::class.java.name).asType()
    val embeddingType: TypeMirror =
        elementUtils.getTypeElement(EMBEDDING_VECTOR_CLASS.canonicalName()).asType()
    val genericDocumentType: TypeMirror =
        elementUtils.getTypeElement(GENERIC_DOCUMENT_CLASS.canonicalName()).asType()
    val listType: TypeMirror = elementUtils.getTypeElement(java.util.List::class.java.name).asType()
    @JvmField
    val stringType: TypeMirror =
        elementUtils.getTypeElement(java.lang.String::class.java.name).asType()

    // Boxable objects
    val booleanBoxType: TypeMirror =
        elementUtils.getTypeElement(java.lang.Boolean::class.java.name).asType()
    val byteBoxType: TypeMirror =
        elementUtils.getTypeElement(java.lang.Byte::class.java.name).asType()
    val byteBoxArrayType: TypeMirror = typeUtils.getArrayType(byteBoxType)
    val doubleBoxType: TypeMirror =
        elementUtils.getTypeElement(java.lang.Double::class.java.name).asType()
    val floatBoxType: TypeMirror =
        elementUtils.getTypeElement(java.lang.Float::class.java.name).asType()
    val integerBoxType: TypeMirror =
        elementUtils.getTypeElement(java.lang.Integer::class.java.name).asType()
    val longBoxType: TypeMirror =
        elementUtils.getTypeElement(java.lang.Long::class.java.name).asType()

    // Primitive versions of boxable objects
    @JvmField val booleanPrimitiveType: TypeMirror = typeUtils.unboxedType(booleanBoxType)
    val bytePrimitiveType: TypeMirror = typeUtils.unboxedType(byteBoxType)
    @JvmField val bytePrimitiveArrayType: TypeMirror = typeUtils.getArrayType(bytePrimitiveType)
    @JvmField val doublePrimitiveType: TypeMirror = typeUtils.unboxedType(doubleBoxType)
    val floatPrimitiveType: TypeMirror = typeUtils.unboxedType(floatBoxType)
    @JvmField val intPrimitiveType: TypeMirror = typeUtils.unboxedType(integerBoxType)
    @JvmField val longPrimitiveType: TypeMirror = typeUtils.unboxedType(longBoxType)

    private val allMethodsCache = WeakHashMap<TypeElement, LinkedHashSet<ExecutableElement>>()

    companion object {
        const val GEN_CLASS_PREFIX: String = "$\$__AppSearch__"
        const val APPSEARCH_PKG: String = "androidx.appsearch.app"

        @JvmField
        val APPSEARCH_SCHEMA_CLASS: ClassName = ClassName.get(APPSEARCH_PKG, "AppSearchSchema")

        @JvmField
        val PROPERTY_CONFIG_CLASS: ClassName = APPSEARCH_SCHEMA_CLASS.nestedClass("PropertyConfig")

        const val APPSEARCH_EXCEPTION_PKG: String = "androidx.appsearch.exceptions"

        @JvmField
        val APPSEARCH_EXCEPTION_CLASS: ClassName =
            ClassName.get(APPSEARCH_EXCEPTION_PKG, "AppSearchException")

        const val APPSEARCH_ANNOTATION_PKG: String = "androidx.appsearch.annotation"

        const val DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME: String = "Document"

        @JvmField
        val DOCUMENT_ANNOTATION_CLASS: ClassName =
            ClassName.get(APPSEARCH_ANNOTATION_PKG, DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME)

        @JvmField
        val GENERIC_DOCUMENT_CLASS: ClassName = ClassName.get(APPSEARCH_PKG, "GenericDocument")

        val EMBEDDING_VECTOR_CLASS: ClassName = ClassName.get(APPSEARCH_PKG, "EmbeddingVector")

        val APPSEARCH_BLOB_HANDLE_CLASS: ClassName =
            ClassName.get(APPSEARCH_PKG, "AppSearchBlobHandle")

        val BUILDER_PRODUCER_CLASS: ClassName =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("BuilderProducer")

        @JvmField
        val DOCUMENT_CLASS_FACTORY_CLASS: ClassName =
            ClassName.get(APPSEARCH_PKG, "DocumentClassFactory")

        @JvmField
        val RESTRICT_TO_ANNOTATION_CLASS: ClassName =
            ClassName.get("androidx.annotation", "RestrictTo")

        @JvmField
        val RESTRICT_TO_SCOPE_CLASS: ClassName = RESTRICT_TO_ANNOTATION_CLASS.nestedClass("Scope")

        @JvmField
        val DOCUMENT_CLASS_MAPPING_CONTEXT_CLASS: ClassName =
            ClassName.get(APPSEARCH_PKG, "DocumentClassMappingContext")

        /**
         * Returns `androidx.appsearch.annotation.Document` annotation element from the input
         * element's annotations. Returns null if no such annotation is found.
         */
        @JvmStatic
        fun getDocumentAnnotation(element: Element): AnnotationMirror? {
            val annotations: List<AnnotationMirror> =
                getAnnotations(element, DOCUMENT_ANNOTATION_CLASS)
            return annotations.firstOrNull()
        }

        /**
         * Returns a list of annotations of a given kind from the input element's annotations,
         * specified by the annotation's class name. Returns null if no annotation of such kind is
         * found.
         */
        fun getAnnotations(element: Element, className: ClassName): List<AnnotationMirror> {
            return element.annotationMirrors
                .stream()
                .filter { it.annotationType.toString() == className.canonicalName() }
                .toList()
        }

        /**
         * Returns the property type of the given property. Properties are represented by an
         * annotated Java element that is either a Java field or a getter method.
         */
        fun getPropertyType(property: Element): TypeMirror {
            var propertyType = property.asType()
            if (property.kind == ElementKind.METHOD) {
                propertyType = (propertyType as ExecutableType).returnType
            }
            return propertyType
        }

        /**
         * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
         * for inner class Foo.Bar.
         */
        @JvmStatic
        fun getDocumentClassFactoryForClass(pkg: String, className: String): ClassName {
            val genClassName: String = GEN_CLASS_PREFIX + className.replace(".", "$\$__")
            return ClassName.get(pkg, genClassName)
        }

        /**
         * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
         * for inner class Foo.Bar.
         */
        @JvmStatic
        fun getDocumentClassFactoryForClass(clazz: ClassName): ClassName {
            val className = clazz.canonicalName().substring(clazz.packageName().length + 1)
            return getDocumentClassFactoryForClass(clazz.packageName(), className)
        }

        /**
         * Get a list of super classes of element annotated with @Document, in order starting with
         * the class at the top of the hierarchy and descending down the class hierarchy. Note that
         * this ordering is important because super classes must appear first in the list than child
         * classes to make property overrides work.
         */
        @Throws(ProcessingException::class)
        @JvmStatic
        fun generateClassHierarchy(element: TypeElement): List<TypeElement> {
            val hierarchy: Deque<TypeElement> = ArrayDeque<TypeElement>()
            if (element.getAnnotation(AutoValue::class.java) != null) {
                // We don't allow classes annotated with both Document and AutoValue to extend
                // classes.
                // Because of how AutoValue is set up, there is no way to add a constructor to
                // populate fields of super classes.
                // There should just be the generated class and the original annotated class
                val superClass = MoreTypes.asTypeElement(element.superclass)
                if (
                    !superClass.qualifiedName.contentEquals(
                        java.lang.Object::class.java.canonicalName
                    )
                ) {
                    throw ProcessingException(
                        "A class annotated with AutoValue and Document cannot have a superclass",
                        element,
                    )
                }
                hierarchy.add(element)
            } else {
                val visited = mutableSetOf<TypeElement>()
                generateClassHierarchyHelper(element, element, hierarchy, visited)
            }
            return hierarchy.toList()
        }

        /**
         * Checks if a method is a valid getter and returns any errors.
         *
         * Returns an empty list if no errors i.e. the method is a valid getter.
         */
        fun validateIsGetter(method: ExecutableElement): MutableList<ProcessingException> {
            val errors = mutableListOf<ProcessingException>()
            if (method.parameters.isNotEmpty()) {
                errors.add(
                    ProcessingException("Getter cannot be used: should take no parameters", method)
                )
            }
            if (method.modifiers.contains(Modifier.PRIVATE)) {
                errors.add(ProcessingException("Getter cannot be used: private visibility", method))
            }
            if (method.modifiers.contains(Modifier.STATIC)) {
                errors.add(ProcessingException("Getter cannot be used: must not be static", method))
            }
            return errors
        }

        @Throws(ProcessingException::class)
        private fun generateClassHierarchyHelper(
            leafElement: TypeElement,
            currentClass: TypeElement,
            hierarchy: Deque<TypeElement>,
            visited: MutableSet<TypeElement>,
        ) {
            if (
                currentClass.qualifiedName.contentEquals(java.lang.Object::class.java.canonicalName)
            ) {
                return
            }
            // If you inherit from an AutoValue class, you have to implement the static methods.
            // That defeats the purpose of AutoValue
            if (currentClass.getAnnotation(AutoValue::class.java) != null) {
                throw ProcessingException(
                    "A class annotated with Document cannot inherit from a class " +
                        "annotated with AutoValue",
                    leafElement,
                )
            }

            // It's possible to revisit the same interface more than once, so this check exists to
            // catch that.
            if (visited.contains(currentClass)) {
                return
            }
            visited.add(currentClass)

            if (getDocumentAnnotation(currentClass) != null) {
                hierarchy.addFirst(currentClass)
            }
            val superclass = currentClass.superclass
            // If currentClass is an interface, then superclass could be NONE.
            if (superclass.kind != TypeKind.NONE) {
                generateClassHierarchyHelper(
                    leafElement,
                    MoreTypes.asTypeElement(superclass),
                    hierarchy,
                    visited,
                )
            }
            for (implementedInterface in currentClass.interfaces) {
                generateClassHierarchyHelper(
                    leafElement,
                    MoreTypes.asTypeElement(implementedInterface),
                    hierarchy,
                    visited,
                )
            }
        }

        /**
         * Determines if a field is from Kotlin and is NonNull by checking for a Metadata
         * annotation.
         */
        @JvmStatic
        fun isNonNullKotlinField(getterOrField: AnnotatedGetterOrField): Boolean {
            val metadata =
                getterOrField.element.enclosingElement.getAnnotation(Metadata::class.java)
            if (metadata != null) {
                val kotlinMetadata: KotlinClassMetadata = KotlinClassMetadata.readStrict(metadata)
                if (kotlinMetadata is KotlinClassMetadata.Class) {
                    val kmClass = kotlinMetadata.kmClass
                    val properties = kmClass.properties
                    for (property in properties) {
                        if (property.name == getterOrField.jvmName) {
                            return !property.returnType.isNullable
                        }
                    }
                }
            }
            // It is not a kotlin property.
            return false
        }
    }

    /**
     * Returns the document property annotation that matches the given property name from a given
     * class or interface element.
     *
     * Returns null if the property cannot be found in the class or interface, or if the property
     * matching the property name is not a document property.
     */
    @Throws(ProcessingException::class)
    fun getDocumentPropertyAnnotation(
        clazz: TypeElement,
        propertyName: String,
    ): DocumentPropertyAnnotation? {
        for (enclosedElement in clazz.enclosedElements) {
            val getterOrField = tryCreateFor(enclosedElement, env)
            if (
                getterOrField == null ||
                    getterOrField.annotation.propertyKind != PropertyAnnotation.Kind.DATA_PROPERTY
            ) {
                continue
            }
            if (
                (getterOrField.annotation as DataPropertyAnnotation).dataPropertyKind ==
                    DataPropertyAnnotation.Kind.DOCUMENT_PROPERTY
            ) {
                val documentPropertyAnnotation =
                    getterOrField.annotation as DocumentPropertyAnnotation
                if (documentPropertyAnnotation.name == propertyName) {
                    return documentPropertyAnnotation
                }
            }
        }
        return null
    }

    /** Checks whether the property data type is one of the valid types. */
    fun isFieldOfExactType(property: Element, vararg validTypes: TypeMirror): Boolean {
        val propertyType: TypeMirror = getPropertyType(property)
        for (validType in validTypes) {
            if (propertyType.kind == TypeKind.ARRAY) {
                if (typeUtils.isSameType((propertyType as ArrayType).componentType, validType)) {
                    return true
                }
            } else if (typeUtils.isAssignable(typeUtils.erasure(propertyType), collectionType)) {
                if (
                    typeUtils.isSameType(
                        (propertyType as DeclaredType).typeArguments.first(),
                        validType,
                    )
                ) {
                    return true
                }
            } else if (typeUtils.isSameType(propertyType, validType)) {
                return true
            }
        }
        return false
    }

    /** Checks whether the property data type is of boolean type. */
    fun isFieldOfBooleanType(property: Element): Boolean {
        return isFieldOfExactType(property, booleanBoxType, booleanPrimitiveType)
    }

    /** Returns the annotation's params as a map. Includes the default values. */
    fun getAnnotationParams(annotation: AnnotationMirror): Map<String, Any?> {
        val values = env.elementUtils.getElementValuesWithDefaults(annotation)
        val ret = mutableMapOf<String, Any?>()
        for (entry in values.entries) {
            val key = entry.key.simpleName.toString()
            ret[key] = entry.value.value
        }
        return ret
    }

    /**
     * Returns all the methods within a class, whether inherited or declared directly.
     *
     * Caches results internally, so it is cheap to call subsequently for the same input.
     */
    fun getAllMethods(clazz: TypeElement): LinkedHashSet<ExecutableElement> {
        return allMethodsCache.computeIfAbsent(clazz) { type: TypeElement ->
            env.elementUtils
                .getAllMembers(type)
                .stream()
                .filter { it.kind == ElementKind.METHOD }
                .map { it as ExecutableElement }
                .collect(Collectors.toCollection { LinkedHashSet() })
        }
    }

    /** Whether a type is the same as `long[]`. */
    fun isPrimitiveLongArray(type: TypeMirror): Boolean {
        return isArrayOf(type, longPrimitiveType)
    }

    /** Whether a type is the same as `double[]`. */
    fun isPrimitiveDoubleArray(type: TypeMirror): Boolean {
        return isArrayOf(type, doublePrimitiveType)
    }

    /** Whether a type is the same as `boolean[]`. */
    fun isPrimitiveBooleanArray(type: TypeMirror): Boolean {
        return isArrayOf(type, booleanPrimitiveType)
    }

    private fun isArrayOf(type: TypeMirror, arrayComponentType: TypeMirror): Boolean {
        return typeUtils.isSameType(type, typeUtils.getArrayType(arrayComponentType))
    }

    /**
     * Same as [.validateIsGetter] but additionally verifies that the getter returns the specified
     * type.
     */
    fun validateIsGetterThatReturns(
        method: ExecutableElement,
        expectedReturnType: TypeMirror,
    ): MutableList<ProcessingException> {
        val errors: MutableList<ProcessingException> = validateIsGetter(method)
        if (!typeUtils.isAssignable(method.returnType, expectedReturnType)) {
            errors.add(
                ProcessingException(
                    "Getter cannot be used: Does not return $expectedReturnType",
                    method,
                )
            )
        }
        return errors
    }

    /**
     * A method's type and element (i.e. declaration).
     *
     * Note: The parameter and return types may differ between the type and the element. For
     * example,
     * <pre>
     * public class StringSet implements Set<String> {...}
     * </pre>
     *
     * Here, the type of `StringSet.add()` is `(String) -> boolean` and the element points to the
     * generic declaration within `Set<T>` with a return type of `boolean` and a single parameter of
     * type `T`.
     */
    class MethodTypeAndElement(val type: ExecutableType, val element: ExecutableElement)

    /**
     * Returns a stream of all the methods (including inherited) within a [DeclaredType].
     *
     * Does not include constructors.
     */
    fun getAllMethods(type: DeclaredType): Stream<MethodTypeAndElement> {
        return elementUtils
            .getAllMembers(type.asElement() as TypeElement)
            .stream()
            .filter { it.kind == ElementKind.METHOD }
            .map {
                MethodTypeAndElement(
                    typeUtils.asMemberOf(type, it) as ExecutableType,
                    it as ExecutableElement,
                )
            }
    }

    /** Whether the method returns the specified type (or subtype). */
    fun isReturnTypeMatching(method: ExecutableType, type: TypeMirror): Boolean {
        return typeUtils.isAssignable(method.returnType, type)
    }

    /**
     * Returns a type that the source type should be casted to coerce it to the target type.
     *
     * Handles the following cases:
     * <pre>
     * long|Long -> int|Integer = (int) ...
     * double|Double -> float|Float = (float) ...
     * </pre>
     *
     * Returns null if no cast is necessary.
     */
    fun getNarrowingCastType(sourceType: TypeMirror, targetType: TypeMirror): TypeMirror? {
        if (
            typeUtils.isSameType(targetType, intPrimitiveType) ||
                typeUtils.isSameType(targetType, integerBoxType)
        ) {
            if (
                typeUtils.isSameType(sourceType, longPrimitiveType) ||
                    typeUtils.isSameType(sourceType, longBoxType)
            ) {
                return intPrimitiveType
            }
        }
        if (
            typeUtils.isSameType(targetType, floatPrimitiveType) ||
                typeUtils.isSameType(targetType, floatBoxType)
        ) {
            if (
                typeUtils.isSameType(sourceType, doublePrimitiveType) ||
                    typeUtils.isSameType(sourceType, doubleBoxType)
            ) {
                return floatPrimitiveType
            }
        }
        return null
    }

    /** Whether the element is a static method that returns the class it's enclosed within. */
    fun isStaticFactoryMethod(element: Element): Boolean {
        if (element.kind != ElementKind.METHOD || !element.modifiers.contains(Modifier.STATIC)) {
            return false
        }
        val method = element as ExecutableElement
        val enclosingType = method.enclosingElement.asType()
        return typeUtils.isSameType(method.returnType, enclosingType)
    }
}
