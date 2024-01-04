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

import com.google.auto.value.AutoValue;

import java.util.Map;

/**
 * An instance of the {@code @Document.LongProperty} annotation.
 */
@AutoValue
public abstract class LongPropertyAnnotation extends DataPropertyAnnotation {
    public static final String SIMPLE_CLASS_NAME = "LongProperty";
    public static final String CLASS_NAME = DOCUMENT_ANNOTATION_CLASS + "." + SIMPLE_CLASS_NAME;

    public LongPropertyAnnotation() {
        super(SIMPLE_CLASS_NAME);
    }

    /**
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     */
    @NonNull
    static LongPropertyAnnotation parse(
            @NonNull Map<String, Object> annotationParams, @NonNull String defaultName) {
        String name = (String) annotationParams.get("name");
        return new AutoValue_LongPropertyAnnotation(
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
        return Kind.LONG_PROPERTY;
    }
}
