/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.appsearch.compiler.IntrospectionHelper.APPSEARCH_SCHEMA_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.IntrospectionHelper;
import androidx.appsearch.compiler.ProcessingException;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;

import java.util.Map;

import javax.lang.model.type.TypeMirror;

/**
 * An instance of the {@code @Document.EmbeddingProperty} annotation.
 */
@AutoValue
public abstract class EmbeddingPropertyAnnotation extends DataPropertyAnnotation {
    public static final ClassName CLASS_NAME =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("EmbeddingProperty");

    public static final ClassName CONFIG_CLASS =
            APPSEARCH_SCHEMA_CLASS.nestedClass("EmbeddingPropertyConfig");

    public EmbeddingPropertyAnnotation() {
        super(
                CLASS_NAME,
                CONFIG_CLASS,
                /* genericDocGetterName= */"getPropertyEmbedding",
                /* genericDocArrayGetterName= */"getPropertyEmbeddingArray",
                /* genericDocSetterName= */"setPropertyEmbedding");
    }

    /**
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     * @throws ProcessingException If the annotation points to an Illegal serializer class.
     */
    @NonNull
    static EmbeddingPropertyAnnotation parse(
            @NonNull Map<String, Object> annotationParams,
            @NonNull String defaultName) throws ProcessingException {
        String name = (String) annotationParams.get("name");
        return new AutoValue_EmbeddingPropertyAnnotation(
                name.isEmpty() ? defaultName : name,
                (boolean) annotationParams.get("required"),
                (int) annotationParams.get("indexingType"));
    }

    /**
     * Specifies how a property should be indexed.
     */
    public abstract int getIndexingType();

    @NonNull
    @Override
    public final Kind getDataPropertyKind() {
        return Kind.EMBEDDING_PROPERTY;
    }

    @NonNull
    @Override
    public TypeMirror getUnderlyingTypeWithinGenericDoc(@NonNull IntrospectionHelper helper) {
        return helper.mEmbeddingType;
    }
}
