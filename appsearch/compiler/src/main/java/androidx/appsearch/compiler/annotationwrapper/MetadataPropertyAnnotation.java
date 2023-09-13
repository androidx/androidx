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

package androidx.appsearch.compiler.annotationwrapper;

import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.javapoet.ClassName;

import java.util.Arrays;

import javax.lang.model.element.AnnotationMirror;

/**
 * An annotation for a metadata property e.g. {@code @Document.Id}.
 */
public enum MetadataPropertyAnnotation implements PropertyAnnotation {
    ID(/* simpleClassName= */"Id", /* genericDocSetterName= */"setId"),
    NAMESPACE(/* simpleClassName= */"Namespace", /* genericDocSetterName= */"setNamespace"),
    CREATION_TIMESTAMP_MILLIS(
            /* simpleClassName= */"CreationTimestampMillis",
            /* genericDocSetterName= */"setCreationTimestampMillis"),
    TTL_MILLIS(/* simpleClassName= */"TtlMillis", /* genericDocSetterName= */"setTtlMillis"),
    SCORE(/* simpleClassName= */"Score", /* genericDocSetterName= */"setScore");

    /**
     * Attempts to parse an {@link AnnotationMirror} into a {@link MetadataPropertyAnnotation},
     * or null.
     */
    @Nullable
    public static MetadataPropertyAnnotation tryParse(@NonNull AnnotationMirror annotation) {
        String qualifiedClassName = annotation.getAnnotationType().toString();
        return Arrays.stream(values())
                .filter(val -> val.getClassName().canonicalName().equals(qualifiedClassName))
                .findFirst()
                .orElse(null);
    }

    @NonNull
    @SuppressWarnings("ImmutableEnumChecker") // ClassName is an immutable third-party type
    private final ClassName mClassName;

    @NonNull
    private final String mGenericDocSetterName;

    MetadataPropertyAnnotation(
            @NonNull String simpleClassName, @NonNull String genericDocSetterName) {
        mClassName = DOCUMENT_ANNOTATION_CLASS.nestedClass(simpleClassName);
        mGenericDocSetterName = genericDocSetterName;
    }

    @Override
    @NonNull
    public ClassName getClassName() {
        return mClassName;
    }



    @Override
    @NonNull
    public PropertyAnnotation.Kind getPropertyKind() {
        return Kind.METADATA_PROPERTY;
    }

    @NonNull
    @Override
    public String getGenericDocSetterName() {
        return mGenericDocSetterName;
    }
}

