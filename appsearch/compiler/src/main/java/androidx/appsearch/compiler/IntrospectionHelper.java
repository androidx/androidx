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
import androidx.annotation.RestrictTo;

import com.squareup.javapoet.ClassName;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Utilities for working with data structures representing parsed Java code.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntrospectionHelper {
    static final String APPSEARCH_PKG = "androidx.appsearch.app";
    static final String APPSEARCH_EXCEPTION_PKG = "androidx.appsearch.exceptions";
    static final String APPSEARCH_EXCEPTION_SIMPLE_NAME = "AppSearchException";
    static final String APP_SEARCH_DOCUMENT_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument";
    static final String URI_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Uri";
    static final String NAMESPACE_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Namespace";
    static final String CREATION_TIMESTAMP_MILLIS_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.CreationTimestampMillis";
    static final String TTL_MILLIS_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.TtlMillis";
    static final String SCORE_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Score";
    static final String PROPERTY_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Property";

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
    final TypeMirror mBytePrimitiveArrayType;

    private final ProcessingEnvironment mEnv;

    IntrospectionHelper(ProcessingEnvironment env) {
        mEnv = env;

        Elements elementUtil = env.getElementUtils();
        Types typeUtil = env.getTypeUtils();
        mCollectionType = elementUtil.getTypeElement(Collection.class.getName()).asType();
        mListType = elementUtil.getTypeElement(List.class.getName()).asType();
        mStringType = elementUtil.getTypeElement(String.class.getName()).asType();
        mIntegerBoxType = elementUtil.getTypeElement(Integer.class.getName()).asType();
        mIntPrimitiveType = typeUtil.unboxedType(mIntegerBoxType);
        mLongBoxType = elementUtil.getTypeElement(Long.class.getName()).asType();
        mLongPrimitiveType = typeUtil.unboxedType(mLongBoxType);
        mFloatBoxType = elementUtil.getTypeElement(Float.class.getName()).asType();
        mFloatPrimitiveType = typeUtil.unboxedType(mFloatBoxType);
        mDoubleBoxType = elementUtil.getTypeElement(Double.class.getName()).asType();
        mDoublePrimitiveType = typeUtil.unboxedType(mDoubleBoxType);
        mBooleanBoxType = elementUtil.getTypeElement(Boolean.class.getName()).asType();
        mBooleanPrimitiveType = typeUtil.unboxedType(mBooleanBoxType);
        mByteBoxType = elementUtil.getTypeElement(Byte.class.getName()).asType();
        mBytePrimitiveArrayType = typeUtil.getArrayType(typeUtil.getPrimitiveType(TypeKind.BYTE));
        mByteBoxArrayType = typeUtil.getArrayType(mByteBoxType);
    }

    public AnnotationMirror getAnnotation(@NonNull Element element, @NonNull String fqClass)
            throws ProcessingException {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            String annotationFq = annotation.getAnnotationType().toString();
            if (fqClass.equals(annotationFq)) {
                return annotation;
            }
        }
        throw new ProcessingException("Missing annotation " + fqClass, element);
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

    public ClassName getAppSearchClass(String clazz, String... nested) {
        return ClassName.get(APPSEARCH_PKG, clazz, nested);
    }

    public ClassName getAppSearchExceptionClass() {
        return ClassName.get(APPSEARCH_EXCEPTION_PKG, APPSEARCH_EXCEPTION_SIMPLE_NAME);
    }
}
