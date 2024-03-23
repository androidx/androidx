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
import androidx.appsearch.compiler.ProcessingException;

import com.squareup.javapoet.ClassName;

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
 *     <li>{@link EmbeddingPropertyAnnotation}</li>
 * </ul>
 */
public abstract class DataPropertyAnnotation implements PropertyAnnotation {
    public enum Kind {
        STRING_PROPERTY, DOCUMENT_PROPERTY, LONG_PROPERTY, DOUBLE_PROPERTY, BOOLEAN_PROPERTY,
        BYTES_PROPERTY, EMBEDDING_PROPERTY
    }

    @NonNull
    private final ClassName mClassName;

    @NonNull
    private final ClassName mConfigClassName;

    @NonNull
    private final String mGenericDocGetterName;

    @NonNull
    private final String mGenericDocArrayGetterName;

    @NonNull
    private final String mGenericDocSetterName;

    DataPropertyAnnotation(
            @NonNull ClassName className,
            @NonNull ClassName configClassName,
            @NonNull String genericDocGetterName,
            @NonNull String genericDocArrayGetterName,
            @NonNull String genericDocSetterName) {
        mClassName = className;
        mConfigClassName = configClassName;
        mGenericDocGetterName = genericDocGetterName;
        mGenericDocArrayGetterName = genericDocArrayGetterName;
        mGenericDocSetterName = genericDocSetterName;
    }

    /**
     * Attempts to parse an {@link AnnotationMirror} into a {@link DataPropertyAnnotation}, or null.
     *
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     * @throws ProcessingException If the {@link AnnotationMirror} is a valid
     *                             {@link DataPropertyAnnotation} but its params are malformed
     *                             e.g. point to an illegal serializer class etc.
     */
    @Nullable
    public static DataPropertyAnnotation tryParse(
            @NonNull AnnotationMirror annotation,
            @NonNull String defaultName,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        Map<String, Object> annotationParams = helper.getAnnotationParams(annotation);
        String qualifiedClassName = annotation.getAnnotationType().toString();
        if (qualifiedClassName.equals(BooleanPropertyAnnotation.CLASS_NAME.canonicalName())) {
            return BooleanPropertyAnnotation.parse(annotationParams, defaultName);
        } else if (qualifiedClassName.equals(BytesPropertyAnnotation.CLASS_NAME.canonicalName())) {
            return BytesPropertyAnnotation.parse(annotationParams, defaultName);
        } else if (qualifiedClassName.equals(
                DocumentPropertyAnnotation.CLASS_NAME.canonicalName())) {
            return DocumentPropertyAnnotation.parse(annotationParams, defaultName);
        } else if (qualifiedClassName.equals(DoublePropertyAnnotation.CLASS_NAME.canonicalName())) {
            return DoublePropertyAnnotation.parse(annotationParams, defaultName);
        } else if (qualifiedClassName.equals(LongPropertyAnnotation.CLASS_NAME.canonicalName())) {
            return LongPropertyAnnotation.parse(annotationParams, defaultName);
        } else if (qualifiedClassName.equals(StringPropertyAnnotation.CLASS_NAME.canonicalName())) {
            return StringPropertyAnnotation.parse(annotationParams, defaultName);
        } else if (qualifiedClassName.equals(
                EmbeddingPropertyAnnotation.CLASS_NAME.canonicalName())) {
            return EmbeddingPropertyAnnotation.parse(annotationParams, defaultName);
        }
        return null;
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
    public final ClassName getClassName() {
        return mClassName;
    }

    /**
     * The class used to configure data properties of this kind.
     *
     * <p>For example, {@link androidx.appsearch.app.AppSearchSchema.StringPropertyConfig} for
     * {@link StringPropertyAnnotation}.
     */
    @NonNull
    public final ClassName getConfigClassName() {
        return mConfigClassName;
    }

    @Override
    @NonNull
    public final String getGenericDocGetterName() {
        return mGenericDocGetterName;
    }

    /**
     * The corresponding getter within {@link androidx.appsearch.app.GenericDocument} that
     * returns and array.
     *
     * <p>For example, {@code getPropertyStringArray} for a {@link StringPropertyAnnotation}.
     */
    @NonNull
    public final String getGenericDocArrayGetterName() {
        return mGenericDocArrayGetterName;
    }

    @NonNull
    @Override
    public final String getGenericDocSetterName() {
        return mGenericDocSetterName;
    }

    @NonNull
    @Override
    public final PropertyAnnotation.Kind getPropertyKind() {
        return PropertyAnnotation.Kind.DATA_PROPERTY;
    }

    /**
     * The {@link Kind} of {@link DataPropertyAnnotation}.
     */
    @NonNull
    public abstract Kind getDataPropertyKind();
}
