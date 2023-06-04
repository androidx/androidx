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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
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
class IntrospectionHelper {
    @VisibleForTesting
    static final String GEN_CLASS_PREFIX = "$$__AppSearch__";
    static final String APPSEARCH_PKG = "androidx.appsearch.app";
    static final String APPSEARCH_EXCEPTION_PKG = "androidx.appsearch.exceptions";
    static final String APPSEARCH_EXCEPTION_SIMPLE_NAME = "AppSearchException";
    static final String DOCUMENT_ANNOTATION_CLASS = "androidx.appsearch.annotation.Document";
    static final String ID_CLASS = "androidx.appsearch.annotation.Document.Id";
    static final String NAMESPACE_CLASS = "androidx.appsearch.annotation.Document.Namespace";
    static final String CREATION_TIMESTAMP_MILLIS_CLASS =
            "androidx.appsearch.annotation.Document.CreationTimestampMillis";
    static final String TTL_MILLIS_CLASS = "androidx.appsearch.annotation.Document.TtlMillis";
    static final String SCORE_CLASS = "androidx.appsearch.annotation.Document.Score";
    final TypeMirror mCollectionType;
    final TypeMirror mListType;
    final TypeMirror mStringType;
    final TypeMirror mIntegerBoxType;
    final TypeMirror mIntPrimitiveType;
    final TypeMirror mLongBoxType;
    final TypeMirror mLongPrimitiveType;
    final TypeMirror mFloatBoxType;
    final TypeMirror mFloatPrimitiveType;
    final TypeMirror mDoubleBoxType;
    final TypeMirror mDoublePrimitiveType;
    final TypeMirror mBooleanBoxType;
    final TypeMirror mBooleanPrimitiveType;
    final TypeMirror mByteBoxType;
    final TypeMirror mByteBoxArrayType;
    final TypeMirror mBytePrimitiveType;
    final TypeMirror mBytePrimitiveArrayType;
    private final ProcessingEnvironment mEnv;
    private final Types mTypeUtils;

    IntrospectionHelper(ProcessingEnvironment env) {
        mEnv = env;

        Elements elementUtil = env.getElementUtils();
        mTypeUtils = env.getTypeUtils();
        mCollectionType = elementUtil.getTypeElement(Collection.class.getName()).asType();
        mListType = elementUtil.getTypeElement(List.class.getName()).asType();
        mStringType = elementUtil.getTypeElement(String.class.getName()).asType();
        mIntegerBoxType = elementUtil.getTypeElement(Integer.class.getName()).asType();
        mIntPrimitiveType = mTypeUtils.unboxedType(mIntegerBoxType);
        mLongBoxType = elementUtil.getTypeElement(Long.class.getName()).asType();
        mLongPrimitiveType = mTypeUtils.unboxedType(mLongBoxType);
        mFloatBoxType = elementUtil.getTypeElement(Float.class.getName()).asType();
        mFloatPrimitiveType = mTypeUtils.unboxedType(mFloatBoxType);
        mDoubleBoxType = elementUtil.getTypeElement(Double.class.getName()).asType();
        mDoublePrimitiveType = mTypeUtils.unboxedType(mDoubleBoxType);
        mBooleanBoxType = elementUtil.getTypeElement(Boolean.class.getName()).asType();
        mBooleanPrimitiveType = mTypeUtils.unboxedType(mBooleanBoxType);
        mByteBoxType = elementUtil.getTypeElement(Byte.class.getName()).asType();
        mByteBoxArrayType = mTypeUtils.getArrayType(mByteBoxType);
        mBytePrimitiveType = mTypeUtils.unboxedType(mByteBoxType);
        mBytePrimitiveArrayType = mTypeUtils.getArrayType(mBytePrimitiveType);
    }

    /**
     * Returns {@code androidx.appsearch.annotation.Document} annotation element from the input
     * element's annotations. Returns null if no such annotation is found.
     */
    @Nullable
    public static AnnotationMirror getDocumentAnnotation(@NonNull Element element) {
        Objects.requireNonNull(element);
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            String annotationFq = annotation.getAnnotationType().toString();
            if (IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.equals(annotationFq)) {
                return annotation;
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
    public boolean isFieldOfExactType(Element property, TypeMirror... validTypes) {
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
    public boolean isFieldOfBooleanType(Element property) {
        return isFieldOfExactType(property, mBooleanBoxType, mBooleanPrimitiveType);
    }

    /**
     * Checks whether the property data class has {@code androidx.appsearch.annotation.Document
     * .DocumentProperty} annotation.
     */
    public boolean isFieldOfDocumentType(Element property) {
        TypeMirror propertyType = getPropertyType(property);

        AnnotationMirror documentAnnotation = null;

        if (propertyType.getKind() == TypeKind.ARRAY) {
            documentAnnotation = getDocumentAnnotation(
                    mTypeUtils.asElement(((ArrayType) propertyType).getComponentType()));
        } else if (mTypeUtils.isAssignable(mTypeUtils.erasure(propertyType), mCollectionType)) {
            documentAnnotation = getDocumentAnnotation(mTypeUtils.asElement(
                    ((DeclaredType) propertyType).getTypeArguments().get(0)));
        } else {
            documentAnnotation = getDocumentAnnotation(mTypeUtils.asElement(propertyType));
        }
        return documentAnnotation != null;
    }

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
    public ClassName getDocumentClassFactoryForClass(String pkg, String className) {
        String genClassName = GEN_CLASS_PREFIX + className.replace(".", "$$__");
        return ClassName.get(pkg, genClassName);
    }

    /**
     * Creates the name of output class. $$__AppSearch__Foo for Foo, $$__AppSearch__Foo$$__Bar
     * for inner class Foo.Bar.
     */
    public ClassName getDocumentClassFactoryForClass(ClassName clazz) {
        String className = clazz.canonicalName().substring(clazz.packageName().length() + 1);
        return getDocumentClassFactoryForClass(clazz.packageName(), className);
    }

    public ClassName getAppSearchClass(String clazz, String... nested) {
        return ClassName.get(APPSEARCH_PKG, clazz, nested);
    }

    public ClassName getAppSearchExceptionClass() {
        return ClassName.get(APPSEARCH_EXCEPTION_PKG, APPSEARCH_EXCEPTION_SIMPLE_NAME);
    }

    /**
     * Get a list of super classes of element annotated with @Document, in order starting with the
     * class at the top of the hierarchy and descending down the class hierarchy. Note that this
     * ordering is important because super classes must appear first in the list than child classes
     * to make property overrides work.
     */
    @NonNull
    public static List<TypeElement> generateClassHierarchy(
            @NonNull TypeElement element, boolean isAutoValueDocument)
            throws ProcessingException {
        Deque<TypeElement> hierarchy = new ArrayDeque<>();
        if (isAutoValueDocument) {
            // We don't allow classes annotated with both Document and AutoValue to extend classes.
            // Because of how AutoValue is set up, there is no way to add a constructor to
            // populate fields of super classes.
            // There should just be the generated class and the original annotated class
            TypeElement superClass = MoreTypes.asTypeElement(
                    MoreTypes.asTypeElement(element.getSuperclass()).getSuperclass());

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
            generateClassHierarchyHelper(leafElement, MoreTypes.asTypeElement(superclass),
                    hierarchy, visited);
        }
        for (TypeMirror implementedInterface : currentClass.getInterfaces()) {
            generateClassHierarchyHelper(leafElement, MoreTypes.asTypeElement(implementedInterface),
                    hierarchy, visited);
        }
    }

    enum PropertyClass {
        BOOLEAN_PROPERTY_CLASS("androidx.appsearch.annotation.Document.BooleanProperty"),
        BYTES_PROPERTY_CLASS("androidx.appsearch.annotation.Document.BytesProperty"),
        DOCUMENT_PROPERTY_CLASS("androidx.appsearch.annotation.Document.DocumentProperty"),
        DOUBLE_PROPERTY_CLASS("androidx.appsearch.annotation.Document.DoubleProperty"),
        LONG_PROPERTY_CLASS("androidx.appsearch.annotation.Document.LongProperty"),
        STRING_PROPERTY_CLASS("androidx.appsearch.annotation.Document.StringProperty");

        private final String mClassFullPath;

        PropertyClass(String classFullPath) {
            mClassFullPath = classFullPath;
        }

        String getClassFullPath() {
            return mClassFullPath;
        }

        boolean isPropertyClass(String annotationFq) {
            return mClassFullPath.equals(annotationFq);
        }
    }
}
