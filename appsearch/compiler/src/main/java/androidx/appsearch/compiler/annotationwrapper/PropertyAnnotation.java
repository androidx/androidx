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

import androidx.annotation.NonNull;

import com.squareup.javapoet.ClassName;

/**
 * An instance of an AppSearch property annotation.
 *
 * <p>Is one of:
 * <ul>
 *     <li>{@link MetadataPropertyAnnotation} e.g. {@code  @Document.Id}</li>
 *     <li>{@link DataPropertyAnnotation} e.g. {@code @Document.StringProperty}</li>
 * </ul>
 */
public interface PropertyAnnotation {
    enum Kind {
        METADATA_PROPERTY, DATA_PROPERTY
    }

    /**
     * The annotation class' name.
     *
     * <p>For example, {@code androidx.appsearch.annotation.Document.StringProperty} for a
     * {@link StringPropertyAnnotation}.
     */
    @NonNull
    ClassName getClassName();

    /**
     * The {@link Kind} of {@link PropertyAnnotation}.
     */
    @NonNull
    Kind getPropertyKind();

    /**
     * The corresponding setter within {@link androidx.appsearch.app.GenericDocument.Builder}.
     *
     * <p>For example, {@code setPropertyString} for a {@link StringPropertyAnnotation}.
     */
    @NonNull
    String getGenericDocSetterName();
}
