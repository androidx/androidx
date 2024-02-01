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

import static androidx.appsearch.compiler.IntrospectionHelper.APPSEARCH_SCHEMA_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.IntrospectionHelper;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

/**
 * An instance of the {@code @Document.DocumentProperty} annotation.
 */
@AutoValue
public abstract class DocumentPropertyAnnotation extends DataPropertyAnnotation {
    public static final ClassName CLASS_NAME =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("DocumentProperty");

    public static final ClassName CONFIG_CLASS =
            APPSEARCH_SCHEMA_CLASS.nestedClass("DocumentPropertyConfig");

    public DocumentPropertyAnnotation() {
        super(
                CLASS_NAME,
                CONFIG_CLASS,
                /* genericDocGetterName= */"getPropertyDocument",
                /* genericDocArrayGetterName= */"getPropertyDocumentArray",
                /* genericDocSetterName= */"setPropertyDocument");
    }

    /**
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     */
    @NonNull
    static DocumentPropertyAnnotation parse(
            @NonNull Map<String, Object> annotationParams, @NonNull String defaultName) {
        String name = (String) annotationParams.get("name");
        List<String> indexableNestedPropertiesList = new ArrayList<>();
        Object indexableList = annotationParams.get("indexableNestedPropertiesList");
        if (indexableList instanceof List) {
            for (Object property : (List<?>) indexableList) {
                indexableNestedPropertiesList.add(property.toString());
            }
        }
        return new AutoValue_DocumentPropertyAnnotation(
                name.isEmpty() ? defaultName : name,
                (boolean) annotationParams.get("required"),
                (boolean) annotationParams.get("indexNestedProperties"),
                ImmutableList.copyOf(indexableNestedPropertiesList),
                (boolean) annotationParams.get("inheritIndexableNestedPropertiesFromSuperclass"));
    }

    /**
     * Specifies whether fields in the nested document should be indexed.
     */
    public abstract boolean shouldIndexNestedProperties();

    /**
     * Returns the list of nested properties to index for the nested document other than the
     * properties inherited from the type's parent.
     */
    @NonNull
    public abstract ImmutableList<String> getIndexableNestedPropertiesList();

    /**
     * Specifies whether to inherit the parent class's definition for the indexable nested
     * properties list.
     */
    public abstract boolean shouldInheritIndexableNestedPropertiesFromSuperClass();

    @NonNull
    @Override
    public final Kind getDataPropertyKind() {
        return Kind.DOCUMENT_PROPERTY;
    }

    @NonNull
    @Override
    public TypeMirror getUnderlyingTypeWithinGenericDoc(@NonNull IntrospectionHelper helper) {
        return helper.mGenericDocumentType;
    }
}
