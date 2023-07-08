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
import androidx.annotation.Nullable;
import androidx.appsearch.compiler.IntrospectionHelper;

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

/**
 * An instance of an annotation for a data property e.g. {@code @Document.StringProperty}.
 *
 * <p>Is one of:
 * <ul>
 *     <li>{@link StringPropertyAnnotation}</li>
 *     <li>{@link DocumentPropertyAnnotation}</li>
 *     <li>{@link LongPropertyAnnotation}</li>
 *     <li>{@link DoublePropertyAnnotation}</li>
 *     <li>{@link BooleanPropertyAnnotation}</li>
 *     <li>{@link BytesPropertyAnnotation}</li>
 * </ul>
 */
public abstract class DataPropertyAnnotation implements PropertyAnnotation {
    @NonNull
    private final String mSimpleClassName;

    DataPropertyAnnotation(@NonNull String simpleClassName) {
        mSimpleClassName = simpleClassName;
    }

    /**
     * Attempts to parse an {@link AnnotationMirror} into a {@link DataPropertyAnnotation}, or null.
     *
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     */
    @Nullable
    public static DataPropertyAnnotation tryParse(
            @NonNull AnnotationMirror annotation,
            @NonNull String defaultName,
            @NonNull IntrospectionHelper helper) {
        Map<String, Object> annotationParams = helper.getAnnotationParams(annotation);
        String qualifiedClassName = annotation.getAnnotationType().toString();
        switch (qualifiedClassName) {
            case BooleanPropertyAnnotation.CLASS_NAME:
                return BooleanPropertyAnnotation.parse(annotationParams, defaultName);
            case BytesPropertyAnnotation.CLASS_NAME:
                return BytesPropertyAnnotation.parse(annotationParams, defaultName);
            case DocumentPropertyAnnotation.CLASS_NAME:
                return DocumentPropertyAnnotation.parse(annotationParams, defaultName);
            case DoublePropertyAnnotation.CLASS_NAME:
                return DoublePropertyAnnotation.parse(annotationParams, defaultName);
            case LongPropertyAnnotation.CLASS_NAME:
                return LongPropertyAnnotation.parse(annotationParams, defaultName);
            case StringPropertyAnnotation.CLASS_NAME:
                return StringPropertyAnnotation.parse(annotationParams, defaultName);
            default:
                return null;
        }
    }

    /**
     * The serialized name for the property in the database.
     */
    @NonNull
    public abstract String getName();

    /**
     * Denotes whether this property must be specified for the document to be valid.
     */
    public abstract boolean isRequired();

    @NonNull
    @Override
    public final String getSimpleClassName() {
        return mSimpleClassName;
    }
}
