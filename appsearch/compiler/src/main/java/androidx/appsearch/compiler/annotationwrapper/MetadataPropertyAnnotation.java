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

import androidx.appsearch.compiler.IntrospectionHelper;
import androidx.room.compiler.codegen.XClassName;
import androidx.room.compiler.processing.XAnnotation;
import androidx.room.compiler.processing.XType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * An annotation for a metadata property e.g. {@code @Document.Id}.
 */
public enum MetadataPropertyAnnotation implements PropertyAnnotation {
    ID(
            /* simpleClassName= */"Id",
            /* genericDocGetterName= */ "getId",
            /* genericDocSetterName= */"setId"),
    NAMESPACE(
            /* simpleClassName= */"Namespace",
            /* genericDocGetterName= */ "getNamespace",
            /* genericDocSetterName= */"setNamespace"),
    CREATION_TIMESTAMP_MILLIS(
            /* simpleClassName= */"CreationTimestampMillis",
            /* genericDocGetterName= */ "getCreationTimestampMillis",
            /* genericDocSetterName= */"setCreationTimestampMillis"),
    TTL_MILLIS(
            /* simpleClassName= */"TtlMillis",
            /* genericDocGetterName= */ "getTtlMillis",
            /* genericDocSetterName= */"setTtlMillis"),
    SCORE(
            /* simpleClassName= */"Score",
            /* genericDocGetterName= */ "getScore",
            /* genericDocSetterName= */"setScore");

    /**
     * Attempts to parse an {@link XAnnotation} into a {@link MetadataPropertyAnnotation},
     * or null.
     */
    public static @Nullable MetadataPropertyAnnotation tryParse(
            @NonNull XAnnotation annotation) {
        String qualifiedClassName = annotation.getQualifiedName();
        return Arrays.stream(values())
                .filter(val -> val.getClassName().getCanonicalName().equals(qualifiedClassName))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("ImmutableEnumChecker") // ClassName is an immutable third-party type
    private final @NonNull XClassName mClassName;

    private final @NonNull String mGenericDocGetterName;

    private final @NonNull String mGenericDocSetterName;

    MetadataPropertyAnnotation(
            @NonNull String simpleClassName,
            @NonNull String genericDocGetterName,
            @NonNull String genericDocSetterName) {
        mClassName = DOCUMENT_ANNOTATION_CLASS.nestedClass(simpleClassName);
        mGenericDocGetterName = genericDocGetterName;
        mGenericDocSetterName = genericDocSetterName;
    }

    @Override
    public @NonNull XClassName getClassName() {
        return mClassName;
    }

    @Override
    public PropertyAnnotation.@NonNull Kind getPropertyKind() {
        return Kind.METADATA_PROPERTY;
    }

    @Override
    public @NonNull XType getUnderlyingTypeWithinGenericDoc(
            @NonNull IntrospectionHelper helper) {
        switch (this) {
            case ID: // fall-through
            case NAMESPACE:
                return helper.getStringType();
            case CREATION_TIMESTAMP_MILLIS: // fall-through
            case TTL_MILLIS:
                return helper.longPrimitiveType;
            case SCORE:
                return helper.intPrimitiveType;
            default:
                throw new IllegalStateException("Unhandled metadata property annotation: " + this);
        }
    }

    @Override
    public @NonNull String getGenericDocGetterName() {
        return mGenericDocGetterName;
    }


    @Override
    public @NonNull String getGenericDocSetterName() {
        return mGenericDocSetterName;
    }
}

