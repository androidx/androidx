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
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.compat.XConverters.toJavac
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isField
import androidx.room.compiler.processing.isMethod
import com.google.auto.value.AutoValue
import java.util.ArrayDeque
import java.util.Deque
import java.util.WeakHashMap
import javax.lang.model.element.Element
import kotlin.metadata.isNullable
import kotlin.metadata.jvm.KotlinClassMetadata

/** Utilities for working with data structures representing parsed Java code. */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // intentionally using java.* types
@OptIn(ExperimentalProcessingApi::class)
class IntrospectionHelper internal constructor(private val env: XProcessingEnv) {
    // Non-boxable objects
    val blobHandleType: XType = env.requireType(APPSEARCH_BLOB_HANDLE_CLASS.canonicalName)
    val collectionType: XType = env.requireType(java.util.Collection::class.java.name)
    val embeddingType: XType = env.requireType(EMBEDDING_VECTOR_CLASS.canonicalName)
    val genericDocumentType: XType = env.requireType(GENERIC_DOCUMENT_CLASS.canonicalName)
    val listType: XType = env.requireType(java.util.List::class.java.name)
    val stringType: XType = env.requireType(java.lang.String::class.java.name)

    // Boxable objects
    val booleanBoxType: XType = env.requireType(java.lang.Boolean::class.java.name)
    val byteBoxType: XType = env.requireType(java.lang.Byte::class.java.name)
    val byteBoxArrayType: XType = env.getArrayType(byteBoxType)
    val doubleBoxType: XType = env.requireType(java.lang.Double::class.java.name)
    val floatBoxType: XType = env.requireType(java.lang.Float::class.java.name)
    val integerBoxType: XType = env.requireType(java.lang.Integer::class.java.name)
    val longBoxType: XType = env.requireType(java.lang.Long::class.java.name)

    // Primitive versions of boxable objects
    @JvmField val booleanPrimitiveType: XType = env.requireType(XTypeName.PRIMITIVE_BOOLEAN)
    val bytePrimitiveType: XType = env.requireType(XTypeName.PRIMITIVE_BYTE)
    @JvmField val bytePrimitiveArrayType: XType = env.getArrayType(bytePrimitiveType)
    @JvmField val doublePrimitiveType: XType = env.requireType(XTypeName.PRIMITIVE_DOUBLE)
    val floatPrimitiveType: XType = env.requireType(XTypeName.PRIMITIVE_FLOAT)
    @JvmField val intPrimitiveType: XType = env.requireType(XTypeName.PRIMITIVE_INT)
    @JvmField val longPrimitiveType: XType = env.requireType(XTypeName.PRIMITIVE_LONG)

    private val allMethodsCache = WeakHashMap<XTypeElement, List<MethodTypeAndElement>>()

    companion object {
        const val GEN_CLASS_PREFIX: String = "$\$__AppSearch__"
        const val APPSEARCH_PKG: String = "androidx.appsearch.app"

        @JvmField
        val APPSEARCH_SCHEMA_CLASS: XClassName = XClassName.get(APPSEARCH_PKG, "AppSearchSchema")

        @JvmField
        val PROPERTY_CONFIG_CLASS: XClassName = APPSEARCH_SCHEMA_CLASS.nestedClass("PropertyConfig")

        const val APPSEARCH_EXCEPTION_PKG: String = "androidx.appsearch.exceptions"

        @JvmField
        val APPSEARCH_EXCEPTION_CLASS: XClassName =
            XClassName.get(APPSEARCH_EXCEPTION_PKG, "AppSearchException")

        const val APPSEARCH_ANNOTATION_PKG: String = "androidx.appsearch.annotation"

        const val DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME: String = "Document"

        @JvmField
        val DOCUMENT_ANNOTATION_CLASS: XClassName =
            XClassName.get(APPSEARCH_ANNOTATION_PKG, DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME)

        @JvmField
        val GENERIC_DOCUMENT_CLASS: XClassName = XClassName.get(APPSEARCH_PKG, "GenericDocument")

        val EMBEDDING_VECTOR_CLASS: XClassName = XClassName.get(APPSEARCH_PKG, "EmbeddingVector")

        val APPSEARCH_BLOB_HANDLE_CLASS: XClassName =
            XClassName.get(APPSEARCH_PKG, "AppSearchBlobHandle")

        val BUILDER_PRODUCER_CLASS: XClassName =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("BuilderProducer")

        @JvmField
        val DOCUMENT_CLASS_FACTORY_CLASS: XClassName =
            XClassName.get(APPSEARCH_PKG, "DocumentClassFactory")

        @JvmField
        val RESTRICT_TO_ANNOTATION_CLASS: XClassName =
            XClassName.get("androidx.annotation", "RestrictTo")

        @JvmField
        val RESTRICT_TO_SCOPE_CLASS: XClassName = RESTRICT_TO_ANNOTATION_CLASS.nestedClass("Scope")

        @JvmField
        val DOCUMENT_CLASS_MAPPING_CONTEXT_CLASS: XClassName =
            XClassName.get(APPSEARCH_PKG, "DocumentClassMappingContext")

        /**
         * Returns `androidx.appsearch.annotation.Document` annotation element from the input
         * element's annotations. Returns null if no such annotation is found.
         */
        @JvmStatic
        fun getDocumentAnnotation(element: XElement): XAnnotation? {
            val annotations: List<XAnnotation> = getAnnotations(element, DOCUMENT_ANNOTATION_CLASS)
            return annotations.firstOrNull()
        }

        /**
         * Returns a list of annotations of a given kind from the input element's annotations,
         * specified by the annotation's class name. Returns null if no annotation of such kind is
         * found.
         */
        fun getAnnotations(element: XElement, className: XClassName): List<XAnnotation> {
            return element
                .getAllAnnotations()
                .stream()
                .filter { it.qualifiedName == className.canonicalName }
                .toList()
        }

        /**
         * Returns the property type of the given property. Properties are represented by an
         * annotated Java element that is either a Java field or a getter method.
         */
        fun getPropertyType(property: XElement): XType {
            val propertyType: XType =
                if (property.isField()) {
                    property.type
                } else if (property.isMethod()) {
                    property.returnType
                } else {
                    throw UnsupportedOperationException(
                        "Don't know how to get type of element $property"
                    )
                }
            return propertyType
        }

        /**
         * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
         * for inner class Foo.Bar.
         */
        @JvmStatic
        fun getDocumentClassFactoryForClass(pkg: String, className: String): XClassName {
            val genClassName: String = GEN_CLASS_PREFIX + className.replace(".", "$\$__")
            return XClassName.get(pkg, genClassName)
        }

        /**
         * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
         * for inner class Foo.Bar.
         */
        @JvmStatic
        fun getDocumentClassFactoryForClass(clazz: XClassName): XClassName {
            val className = clazz.canonicalName.substring(clazz.packageName.length + 1)
            return getDocumentClassFactoryForClass(clazz.packageName, className)
        }

        /**
         * Get a list of super classes of element annotated with @Document, in order starting with
         * the class at the top of the hierarchy and descending down the class hierarchy. Note that
         * this ordering is important because super classes must appear first in the list than child
         * classes to make property overrides work.
         */
        @Throws(XProcessingException::class)
        @JvmStatic
        fun generateClassHierarchy(element: XTypeElement): List<XTypeElement> {
            val hierarchy: Deque<XTypeElement> = ArrayDeque<XTypeElement>()
            if (element.hasAnnotation(AutoValue::class)) {
                // We don't allow classes annotated with both Document and AutoValue to extend
                // classes.
                // Because of how AutoValue is set up, there is no way to add a constructor to
                // populate fields of super classes.
                // There should just be the generated class and the original annotated class
                val superClass = element.superClass?.typeElement
                if (
                    superClass != null &&
                        superClass.qualifiedName != java.lang.Object::class.java.canonicalName
                ) {
                    throw XProcessingException(
                        "A class annotated with AutoValue and Document cannot have a superclass",
                        element,
                    )
                }
                hierarchy.add(element)
            } else {
                val visited = mutableSetOf<XTypeElement>()
                generateClassHierarchyHelper(element, element, hierarchy, visited)
            }
            return hierarchy.toList()
        }

        /**
         * Checks if a method is a valid getter and returns any errors.
         *
         * Returns an empty list if no errors i.e. the method is a valid getter.
         */
        fun validateIsGetter(method: XMethodElement): MutableList<XProcessingException> {
            val errors = mutableListOf<XProcessingException>()
            if (method.parameters.isNotEmpty()) {
                errors.add(
                    XProcessingException("Getter cannot be used: should take no parameters", method)
                )
            }
            if (method.isPrivate()) {
                errors.add(
                    XProcessingException("Getter cannot be used: private visibility", method)
                )
            }
            if (method.isStatic()) {
                errors.add(
                    XProcessingException("Getter cannot be used: must not be static", method)
                )
            }
            return errors
        }

        @Throws(XProcessingException::class)
        private fun generateClassHierarchyHelper(
            leafElement: XTypeElement,
            currentClass: XTypeElement,
            hierarchy: Deque<XTypeElement>,
            visited: MutableSet<XTypeElement>,
        ) {
            if (
                currentClass.qualifiedName.contentEquals(java.lang.Object::class.java.canonicalName)
            ) {
                return
            }
            // If you inherit from an AutoValue class, you have to implement the static methods.
            // That defeats the purpose of AutoValue
            if (currentClass.hasAnnotation(AutoValue::class)) {
                throw XProcessingException(
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
            val superclass = currentClass.superClass
            // If currentClass is an interface, then superclass could be NONE.
            if (superclass != null && !superclass.isNone()) {
                generateClassHierarchyHelper(
                    leafElement,
                    superclass.typeElement!!,
                    hierarchy,
                    visited,
                )
            }
            for (implementedInterface in currentClass.superInterfaces) {
                generateClassHierarchyHelper(
                    leafElement,
                    implementedInterface.typeElement!!,
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
            val enclosingElement: Element =
                getterOrField.element.enclosingElement?.toJavac() ?: return false
            val metadata: Metadata? = enclosingElement.getAnnotation(Metadata::class.java)
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
    @Throws(XProcessingException::class)
    fun getDocumentPropertyAnnotation(
        clazz: XTypeElement,
        propertyName: String,
    ): DocumentPropertyAnnotation? {
        for (enclosedElement in clazz.getEnclosedElements()) {
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
    fun isFieldOfExactType(property: XElement, vararg validTypes: XType): Boolean {
        val propertyType: XType = getPropertyType(property)
        for (validType in validTypes) {
            if (propertyType.isArray()) {
                if (propertyType.componentType.isSameType(validType)) {
                    return true
                }
            } else if (collectionType.rawType.isAssignableFrom(propertyType.rawType)) {
                if (validType.isSameType(propertyType.typeArguments.first())) {
                    return true
                }
            } else if (propertyType.isSameType(validType)) {
                return true
            }
        }
        return false
    }

    /** Checks whether the property data type is of boolean type. */
    fun isFieldOfBooleanType(property: XElement): Boolean {
        return isFieldOfExactType(property, booleanBoxType, booleanPrimitiveType)
    }

    /** Returns the annotation's params as a map. Includes the default values. */
    fun getAnnotationParams(annotation: XAnnotation): Map<String, XAnnotationValue> {
        val ret = mutableMapOf<String, XAnnotationValue>()
        for (value in annotation.annotationValues) {
            ret[value.name] = value
        }
        return ret
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
    class MethodTypeAndElement(val type: XMethodType, val element: XMethodElement)

    /**
     * Returns a stream of all the methods (including inherited) within a type.
     *
     * Does not include constructors.
     */
    fun getAllMethods(type: XTypeElement): List<MethodTypeAndElement> {
        return allMethodsCache.computeIfAbsent(type) { type: XTypeElement ->
            type.getAllMethods().map { MethodTypeAndElement(it.asMemberOf(type.type), it) }.toList()
        }
    }

    /** Whether a type is the same as `long[]`. */
    fun isPrimitiveLongArray(type: XType): Boolean {
        return isArrayOf(type, longPrimitiveType)
    }

    /** Whether a type is the same as `double[]`. */
    fun isPrimitiveDoubleArray(type: XType): Boolean {
        return isArrayOf(type, doublePrimitiveType)
    }

    /** Whether a type is the same as `boolean[]`. */
    fun isPrimitiveBooleanArray(type: XType): Boolean {
        return isArrayOf(type, booleanPrimitiveType)
    }

    private fun isArrayOf(type: XType, arrayComponentType: XType): Boolean {
        return type.isSameType(env.getArrayType(arrayComponentType))
    }

    /**
     * Same as [.validateIsGetter] but additionally verifies that the getter returns the specified
     * type.
     */
    fun validateIsGetterThatReturns(
        method: XMethodElement,
        expectedReturnType: XType,
    ): MutableList<XProcessingException> {
        val errors: MutableList<XProcessingException> = validateIsGetter(method)
        if (!expectedReturnType.isAssignableFrom(method.returnType)) {
            errors.add(
                XProcessingException(
                    "Getter cannot be used: Does not return $expectedReturnType",
                    method,
                )
            )
        }
        return errors
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
    fun getNarrowingCastType(sourceType: XType, targetType: XType): XType? {
        if (targetType.isSameType(intPrimitiveType) || targetType.isSameType(integerBoxType)) {
            if (sourceType.isSameType(longPrimitiveType) || sourceType.isSameType(longBoxType)) {
                return intPrimitiveType
            }
        }
        if (targetType.isSameType(floatPrimitiveType) || targetType.isSameType(floatBoxType)) {
            if (
                sourceType.isSameType(doublePrimitiveType) || sourceType.isSameType(doubleBoxType)
            ) {
                return floatPrimitiveType
            }
        }
        return null
    }

    /** Whether the element is a static method that returns the class it's enclosed within. */
    fun isStaticFactoryMethod(element: XElement): Boolean {
        if (!element.isMethod() || !element.isStatic()) {
            return false
        }
        val enclosingType: XType = element.enclosingElement.type ?: return false
        return enclosingType.isAssignableFrom(element.returnType)
    }
}
