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
    /**
     * The annotation class' simple name.
     *
     * <p>For example, {@code StringProperty} for a {@link StringPropertyAnnotation}.
     */
    @NonNull
    String getSimpleClassName();

    /**
     * The annotation class' qualified name
     *
     * <p>{@code androidx.appsearch.annotation.Document.StringProperty} for a
     * {@link StringPropertyAnnotation}.
     */
    @NonNull
    default String getQualifiedClassName() {
        return DOCUMENT_ANNOTATION_CLASS + "." + getSimpleClassName();
    }
}
