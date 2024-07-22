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
package androidx.appsearch.compiler;

import static com.google.auto.common.MoreTypes.asTypeElement;

import static java.util.stream.Collectors.toCollection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.DocumentPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Utilities for working with data structures representing parsed Java code.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IntrospectionHelper {
    static final String GEN_CLASS_PREFIX = "$$__AppSearch__";
    static final String APPSEARCH_PKG = "androidx.appsearch.app";

    public static final ClassName APPSEARCH_SCHEMA_CLASS =
            ClassName.get(APPSEARCH_PKG, "AppSearchSchema");

    static final ClassName PROPERTY_CONFIG_CLASS =
            APPSEARCH_SCHEMA_CLASS.nestedClass("PropertyConfig");

    static final String APPSEARCH_EXCEPTION_PKG = "androidx.appsearch.exceptions";

    static final ClassName APPSEARCH_EXCEPTION_CLASS =
            ClassName.get(APPSEARCH_EXCEPTION_PKG, "AppSearchException");

    public static final String APPSEARCH_ANNOTATION_PKG = "androidx.appsearch.annotation";

    public static final String DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME = "Document";

    public static final ClassName DOCUMENT_ANNOTATION_CLASS =
            ClassName.get(APPSEARCH_ANNOTATION_PKG, DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME);

    public static final ClassName GENERIC_DOCUMENT_CLASS =
            ClassName.get(APPSEARCH_PKG, "GenericDocument");

    public static final ClassName EMBEDDING_VECTOR_CLASS =
            ClassName.get(APPSEARCH_PKG, "EmbeddingVector");

    public static final ClassName BUILDER_PRODUCER_CLASS =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("BuilderProducer");

    static final ClassName DOCUMENT_CLASS_FACTORY_CLASS =
            ClassName.get(APPSEARCH_PKG, "DocumentClassFactory");

    static final ClassName RESTRICT_TO_ANNOTATION_CLASS =
            ClassName.get("androidx.annotation", "RestrictTo");

    static final ClassName RESTRICT_TO_SCOPE_CLASS =
            RESTRICT_TO_ANNOTATION_CLASS.nestedClass("Scope");

    public final TypeMirror mStringType;
    public final TypeMirror mLongPrimitiveType;
    public final TypeMirror mIntPrimitiveType;
    public final TypeMirror mBooleanPrimitiveType;
    public final TypeMirror mBytePrimitiveArrayType;
    public final TypeMirror mGenericDocumentType;
    public final TypeMirror mEmbeddingType;
    public final TypeMirror mDoublePrimitiveType;
    final TypeMirror mCollectionType;
    final TypeMirror mListType;
    final TypeMirror mIntegerBoxType;
    final TypeMirror mLongBoxType;
    final TypeMirror mFloatBoxType;
    final TypeMirror mFloatPrimitiveType;
    final TypeMirror mDoubleBoxType;
    final TypeMirror mBooleanBoxType;
    final TypeMirror mByteBoxType;
    final TypeMirror mByteBoxArrayType;
    final TypeMirror mBytePrimitiveType;
    private final ProcessingEnvironment mEnv;
    private final Types mTypeUtils;
    private final Elements mElementUtils;

    private final WeakHashMap<TypeElement, LinkedHashSet<ExecutableElement>> mAllMethodsCache =
            new WeakHashMap<>();

    IntrospectionHelper(ProcessingEnvironment env) {
        mEnv = env;

        mElementUtils = env.getElementUtils();
        mTypeUtils = env.getTypeUtils();
        mCollectionType = mElementUtils.getTypeElement(Collection.class.getName()).asType();
        mListType = mElementUtils.getTypeElement(List.class.getName()).asType();
        mStringType = mElementUtils.getTypeElement(String.class.getName()).asType();
        mIntegerBoxType = mElementUtils.getTypeElement(Integer.class.getName()).asType();
        mIntPrimitiveType = mTypeUtils.unboxedType(mIntegerBoxType);
        mLongBoxType = mElementUtils.getTypeElement(Long.class.getName()).asType();
        mLongPrimitiveType = mTypeUtils.unboxedType(mLongBoxType);
        mFloatBoxType = mElementUtils.getTypeElement(Float.class.getName()).asType();
        mFloatPrimitiveType = mTypeUtils.unboxedType(mFloatBoxType);
        mDoubleBoxType = mElementUtils.getTypeElement(Double.class.getName()).asType();
        mDoublePrimitiveType = mTypeUtils.unboxedType(mDoubleBoxType);
        mBooleanBoxType = mElementUtils.getTypeElement(Boolean.class.getName()).asType();
        mBooleanPrimitiveType = mTypeUtils.unboxedType(mBooleanBoxType);
        mByteBoxType = mElementUtils.getTypeElement(Byte.class.getName()).asType();
        mByteBoxArrayType = mTypeUtils.getArrayType(mByteBoxType);
        mBytePrimitiveType = mTypeUtils.unboxedType(mByteBoxType);
        mBytePrimitiveArrayType = mTypeUtils.getArrayType(mBytePrimitiveType);
        mGenericDocumentType =
                mElementUtils.getTypeElement(GENERIC_DOCUMENT_CLASS.canonicalName()).asType();
        mEmbeddingType = mElementUtils.getTypeElement(
                EMBEDDING_VECTOR_CLASS.canonicalName()).asType();
    }

    /**
     * Returns {@code androidx.appsearch.annotation.Document} annotation element from the input
     * element's annotations. Returns null if no such annotation is found.
     */
    @Nullable
    public static AnnotationMirror getDocumentAnnotation(@NonNull Element element) {
        Objects.requireNonNull(element);
        List<? extends AnnotationMirror> annotations = getAnnotations(element,
                DOCUMENT_ANNOTATION_CLASS);
        if (annotations.isEmpty()) {
            return null;
        } else {
            return annotations.get(0);
        }
    }

    /**
     * Returns a list of annotations of a given kind from the input element's annotations,
     * specified by the annotation's class name. Returns null if no annotation of such kind is
     * found.
     */
    @NonNull
    public static List<? extends AnnotationMirror> getAnnotations(@NonNull Element element,
            @NonNull ClassName className) {
        Objects.requireNonNull(element);
        Objects.requireNonNull(className);
        return element.getAnnotationMirrors()
                .stream()
                .filter(annotation -> annotation.getAnnotationType().toString()
                        .equals(className.canonicalName()))
                .toList();
    }

    /**
     * Returns the document property annotation that matches the given property name from a given
     * class or interface element.
     *
     * <p>Returns null if the property cannot be found in the class or interface, or if the
     * property matching the property name is not a document property.
     */
    @Nullable
    public DocumentPropertyAnnotation getDocumentPropertyAnnotation(
            @NonNull TypeElement clazz, @NonNull String propertyName) throws ProcessingException {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(propertyName);
        for (Element enclosedElement : clazz.getEnclosedElements()) {
            AnnotatedGetterOrField getterOrField =
                    AnnotatedGetterOrField.tryCreateFor(enclosedElement, mEnv);
            if (getterOrField == null || !(getterOrField.getAnnotation().getPropertyKind()
                    == PropertyAnnotation.Kind.DATA_PROPERTY)) {
                continue;
            }
            if (((DataPropertyAnnotation) getterOrField.getAnnotation()).getDataPropertyKind()
                    == DataPropertyAnnotation.Kind.DOCUMENT_PROPERTY) {
                DocumentPropertyAnnotation documentPropertyAnnotation =
                        (DocumentPropertyAnnotation) getterOrField.getAnnotation();
                if (documentPropertyAnnotation.getName().equals(propertyName)) {
                    return documentPropertyAnnotation;
                }
            }
        }
        return null;
    }

    /**
     * Returns the property type of the given property. Properties are represented by an
     * annotated Java element that is either a Java field or a getter method.
     */
    @NonNull
    public static TypeMirror getPropertyType(@NonNull Element property) {
        Objects.requireNonNull(property);

        TypeMirror propertyType = property.asType();
        if (property.getKind() == ElementKind.METHOD) {
            propertyType = ((ExecutableType) propertyType).getReturnType();
        }
        return propertyType;
    }

    /** Checks whether the property data type is one of the valid types. */
    public boolean isFieldOfExactType(
            @NonNull Element property, @NonNull TypeMirror... validTypes) {
        TypeMirror propertyType = getPropertyType(property);
        for (TypeMirror validType : validTypes) {
            if (propertyType.getKind() == TypeKind.ARRAY) {
                if (mTypeUtils.isSameType(
                        ((ArrayType) propertyType).getComponentType(), validType)) {
                    return true;
                }
            } else if (mTypeUtils.isAssignable(mTypeUtils.erasure(propertyType), mCollectionType)) {
                if (mTypeUtils.isSameType(
                        ((DeclaredType) propertyType).getTypeArguments().get(0), validType)) {
                    return true;
                }
            } else if (mTypeUtils.isSameType(propertyType, validType)) {
                return true;
            }
        }
        return false;
    }

    /** Checks whether the property data type is of boolean type. */
    public boolean isFieldOfBooleanType(@NonNull Element property) {
        return isFieldOfExactType(property, mBooleanBoxType, mBooleanPrimitiveType);
    }

    /**
     * Returns the annotation's params as a map. Includes the default values.
     */
    @NonNull
    public Map<String, Object> getAnnotationParams(@NonNull AnnotationMirror annotation) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                mEnv.getElementUtils().getElementValuesWithDefaults(annotation);
        Map<String, Object> ret = new HashMap<>();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                values.entrySet()) {
            String key = entry.getKey().getSimpleName().toString();
            ret.put(key, entry.getValue().getValue());
        }
        return ret;
    }

    /**
     * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
     * for inner class Foo.Bar.
     */
    @NonNull
    public static ClassName getDocumentClassFactoryForClass(
            @NonNull String pkg, @NonNull String className) {
        String genClassName = GEN_CLASS_PREFIX + className.replace(".", "$$__");
        return ClassName.get(pkg, genClassName);
    }

    /**
     * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
     * for inner class Foo.Bar.
     */
    @NonNull
    public static ClassName getDocumentClassFactoryForClass(@NonNull ClassName clazz) {
        String className = clazz.canonicalName().substring(clazz.packageName().length() + 1);
        return getDocumentClassFactoryForClass(clazz.packageName(), className);
    }

    /**
     * Returns all the methods within a class, whether inherited or declared directly.
     *
     * <p>Caches results internally, so it is cheap to call subsequently for the same input.
     */
    @NonNull
    public LinkedHashSet<ExecutableElement> getAllMethods(@NonNull TypeElement clazz) {
        return mAllMethodsCache.computeIfAbsent(
                clazz,
                type -> mEnv.getElementUtils().getAllMembers(type).stream()
                        .filter(element -> element.getKind() == ElementKind.METHOD)
                        .map(element -> (ExecutableElement) element)
                        .collect(toCollection(LinkedHashSet::new)));
    }

    /**
     * Whether a type is the same as {@code long[]}.
     */
    public boolean isPrimitiveLongArray(@NonNull TypeMirror type) {
        return isArrayOf(type, mLongPrimitiveType);
    }

    /**
     * Whether a type is the same as {@code double[]}.
     */
    public boolean isPrimitiveDoubleArray(@NonNull TypeMirror type) {
        return isArrayOf(type, mDoublePrimitiveType);
    }

    /**
     * Whether a type is the same as {@code boolean[]}.
     */
    public boolean isPrimitiveBooleanArray(@NonNull TypeMirror type) {
        return isArrayOf(type, mBooleanPrimitiveType);
    }

    private boolean isArrayOf(@NonNull TypeMirror type, @NonNull TypeMirror arrayComponentType) {
        return mTypeUtils.isSameType(type, mTypeUtils.getArrayType(arrayComponentType));
    }

    /**
     * Get a list of super classes of element annotated with @Document, in order starting with the
     * class at the top of the hierarchy and descending down the class hierarchy. Note that this
     * ordering is important because super classes must appear first in the list than child classes
     * to make property overrides work.
     */
    @NonNull
    public static List<TypeElement> generateClassHierarchy(
            @NonNull TypeElement element) throws ProcessingException {
        Deque<TypeElement> hierarchy = new ArrayDeque<>();
        if (element.getAnnotation(AutoValue.class) != null) {
            // We don't allow classes annotated with both Document and AutoValue to extend classes.
            // Because of how AutoValue is set up, there is no way to add a constructor to
            // populate fields of super classes.
            // There should just be the generated class and the original annotated class
            TypeElement superClass = asTypeElement(element.getSuperclass());
            if (!superClass.getQualifiedName().contentEquals(Object.class.getCanonicalName())) {
                throw new ProcessingException(
                        "A class annotated with AutoValue and Document cannot have a superclass",
                        element);
            }
            hierarchy.add(element);
        } else {
            Set<TypeElement> visited = new HashSet<>();
            generateClassHierarchyHelper(element, element, hierarchy, visited);
        }
        return new ArrayList<>(hierarchy);
    }

    /**
     * Checks if a method is a valid getter and returns any errors.
     *
     * <p>Returns an empty list if no errors i.e. the method is a valid getter.
     */
    @NonNull
    public static List<ProcessingException> validateIsGetter(@NonNull ExecutableElement method) {
        List<ProcessingException> errors = new ArrayList<>();
        if (!method.getParameters().isEmpty()) {
            errors.add(new ProcessingException(
                    "Getter cannot be used: should take no parameters", method));
        }
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            errors.add(new ProcessingException(
                    "Getter cannot be used: private visibility", method));
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            errors.add(new ProcessingException(
                    "Getter cannot be used: must not be static", method));
        }
        return errors;
    }

    /**
     * Same as {@link #validateIsGetter} but additionally verifies that the getter returns the
     * specified type.
     */
    @NonNull
    public List<ProcessingException> validateIsGetterThatReturns(
            @NonNull ExecutableElement method, @NonNull TypeMirror expectedReturnType) {
        List<ProcessingException> errors = validateIsGetter(method);
        if (!mTypeUtils.isAssignable(method.getReturnType(), expectedReturnType)) {
            errors.add(new ProcessingException(
                    "Getter cannot be used: Does not return " + expectedReturnType, method));
        }
        return errors;
    }

    /**
     * A method's type and element (i.e. declaration).
     *
     * <p>Note: The parameter and return types may differ between the type and the element.
     * For example,
     *
     * <pre>
     * {@code
     * public class StringSet implements Set<String> {...}
     * }
     * </pre>
     *
     * <p>Here, the type of {@code StringSet.add()} is {@code (String) -> boolean} and the element
     * points to the generic declaration within {@code Set<T>} with a return type of
     * {@code boolean} and a single parameter of type {@code T}.
     */
    public static class MethodTypeAndElement {
        private final ExecutableType mType;
        private final ExecutableElement mElement;

        public MethodTypeAndElement(
                @NonNull ExecutableType type, @NonNull ExecutableElement element) {
            mType = type;
            mElement = element;
        }

        @NonNull
        public ExecutableType getType() {
            return mType;
        }

        @NonNull
        public ExecutableElement getElement() {
            return mElement;
        }
    }

    /**
     * Returns a stream of all the methods (including inherited) within a {@link DeclaredType}.
     *
     * <p>Does not include constructors.
     */
    @NonNull
    public Stream<MethodTypeAndElement> getAllMethods(@NonNull DeclaredType type) {
        return mElementUtils.getAllMembers((TypeElement) type.asElement()).stream()
                .filter(el -> el.getKind() == ElementKind.METHOD)
                .map(el -> new MethodTypeAndElement(
                        (ExecutableType) mTypeUtils.asMemberOf(type, el),
                        (ExecutableElement) el));
    }

    /**
     * Whether the method returns the specified type (or subtype).
     */
    public boolean isReturnTypeMatching(
            @NonNull ExecutableType method, @NonNull TypeMirror type) {
        return mTypeUtils.isAssignable(method.getReturnType(), type);
    }

    /**
     * Returns a type that the source type should be casted to coerce it to the target type.
     *
     * <p>Handles the following cases:
     * <pre>
     * {@code
     * long|Long -> int|Integer = (int) ...
     * double|Double -> float|Float = (float) ...
     * }
     * </pre>
     *
     * <p>Returns null if no cast is necessary.
     */
    @Nullable
    public TypeMirror getNarrowingCastType(
            @NonNull TypeMirror sourceType, @NonNull TypeMirror targetType) {
        if (mTypeUtils.isSameType(targetType, mIntPrimitiveType)
                || mTypeUtils.isSameType(targetType, mIntegerBoxType)) {
            if (mTypeUtils.isSameType(sourceType, mLongPrimitiveType)
                    || mTypeUtils.isSameType(sourceType, mLongBoxType)) {
                return mIntPrimitiveType;
            }
        }
        if (mTypeUtils.isSameType(targetType, mFloatPrimitiveType)
                || mTypeUtils.isSameType(targetType, mFloatBoxType)) {
            if (mTypeUtils.isSameType(sourceType, mDoublePrimitiveType)
                    || mTypeUtils.isSameType(sourceType, mDoubleBoxType)) {
                return mFloatPrimitiveType;
            }
        }
        return null;
    }

    /**
     * Whether the element is a static method that returns the class it's enclosed within.
     */
    public boolean isStaticFactoryMethod(@NonNull Element element) {
        if (element.getKind() != ElementKind.METHOD
                || !element.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        ExecutableElement method = (ExecutableElement) element;
        TypeMirror enclosingType = method.getEnclosingElement().asType();
        return mTypeUtils.isSameType(method.getReturnType(), enclosingType);
    }

    private static void generateClassHierarchyHelper(@NonNull TypeElement leafElement,
            @NonNull TypeElement currentClass, @NonNull Deque<TypeElement> hierarchy,
            @NonNull Set<TypeElement> visited)
            throws ProcessingException {
        if (currentClass.getQualifiedName().contentEquals(Object.class.getCanonicalName())) {
            return;
        }
        // If you inherit from an AutoValue class, you have to implement the static methods.
        // That defeats the purpose of AutoValue
        if (currentClass.getAnnotation(AutoValue.class) != null) {
            throw new ProcessingException(
                    "A class annotated with Document cannot inherit from a class "
                            + "annotated with AutoValue", leafElement);
        }

        // It's possible to revisit the same interface more than once, so this check exists to
        // catch that.
        if (visited.contains(currentClass)) {
            return;
        }
        visited.add(currentClass);

        if (getDocumentAnnotation(currentClass) != null) {
            hierarchy.addFirst(currentClass);
        }
        TypeMirror superclass = currentClass.getSuperclass();
        // If currentClass is an interface, then superclass could be NONE.
        if (superclass.getKind() != TypeKind.NONE) {
            generateClassHierarchyHelper(leafElement, asTypeElement(superclass),
                    hierarchy, visited);
        }
        for (TypeMirror implementedInterface : currentClass.getInterfaces()) {
            generateClassHierarchyHelper(leafElement, asTypeElement(implementedInterface),
                    hierarchy, visited);
        }
    }
}
