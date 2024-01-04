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

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;

import java.util.Map;

/**
 * An instance of the {@code @Document.DoubleProperty} annotation.
 */
@AutoValue
public abstract class DoublePropertyAnnotation extends DataPropertyAnnotation {
    public static final ClassName CLASS_NAME =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("DoubleProperty");

    public static final ClassName CONFIG_CLASS =
            APPSEARCH_SCHEMA_CLASS.nestedClass("DoublePropertyConfig");

    public DoublePropertyAnnotation() {
        super(CLASS_NAME, CONFIG_CLASS, /* genericDocSetterName= */"setPropertyDouble");
    }

    /**
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     */
    @NonNull
    static DoublePropertyAnnotation parse(
            @NonNull Map<String, Object> annotationParams, @NonNull String defaultName) {
        String name = (String) annotationParams.get("name");
        return new AutoValue_DoublePropertyAnnotation(
                name.isEmpty() ? defaultName : name, (boolean) annotationParams.get("required"));
    }

    @NonNull
    @Override
    public final Kind getDataPropertyKind() {
        return Kind.DOUBLE_PROPERTY;
    }
}
