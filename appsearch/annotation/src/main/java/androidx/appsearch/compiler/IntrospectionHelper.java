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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/**
 * Utilities for working with data structures representing parsed Java code.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IntrospectionHelper {
    static final String APP_SEARCH_DOCUMENT_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument";
    static final String URI_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Uri";
    static final String CREATION_TIMESTAMP_MILLIS_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.CreationTimestampMillis";
    static final String TTL_MILLIS_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.TtlMillis";
    static final String SCORE_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Score";
    static final String PROPERTY_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument.Property";

    private final ProcessingEnvironment mEnv;

    IntrospectionHelper(ProcessingEnvironment env) {
        mEnv = env;
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
}
