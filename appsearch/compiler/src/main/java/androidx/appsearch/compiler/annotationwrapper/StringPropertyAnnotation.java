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

import static com.google.auto.common.MoreTypes.asElement;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.compiler.IntrospectionHelper;
import androidx.appsearch.compiler.ProcessingException;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * An instance of the {@code @Document.StringProperty} annotation.
 */
@AutoValue
public abstract class StringPropertyAnnotation extends DataPropertyAnnotation {
    public static final ClassName CLASS_NAME =
            DOCUMENT_ANNOTATION_CLASS.nestedClass("StringProperty");

    public static final ClassName CONFIG_CLASS =
            APPSEARCH_SCHEMA_CLASS.nestedClass("StringPropertyConfig");

    private static final ClassName DEFAULT_SERIALIZER_CLASS =
            CLASS_NAME.nestedClass("DefaultSerializer");

    public StringPropertyAnnotation() {
        super(
                CLASS_NAME,
                CONFIG_CLASS,
                /* genericDocGetterName= */"getPropertyString",
                /* genericDocArrayGetterName= */"getPropertyStringArray",
                /* genericDocSetterName= */"setPropertyString");
    }

    /**
     * @param defaultName The name to use for the annotated property in case the annotation
     *                    params do not mention an explicit name.
     * @throws ProcessingException If the annotation points to an Illegal serializer class.
     */
    @NonNull
    static StringPropertyAnnotation parse(
            @NonNull Map<String, Object> annotationParams,
            @NonNull String defaultName) throws ProcessingException {
        String name = (String) annotationParams.get("name");
        SerializerClass customSerializer = null;
        TypeMirror serializerInAnnotation = (TypeMirror) annotationParams.get("serializer");
        String typeName = TypeName.get(serializerInAnnotation).toString();
        if (!typeName.equals(DEFAULT_SERIALIZER_CLASS.canonicalName())) {
            customSerializer = SerializerClass.create(
                    (TypeElement) asElement(serializerInAnnotation),
                    SerializerClass.Kind.STRING_SERIALIZER);
        }
        return new AutoValue_StringPropertyAnnotation(
                name.isEmpty() ? defaultName : name,
                (boolean) annotationParams.get("required"),
                (int) annotationParams.get("tokenizerType"),
                (int) annotationParams.get("indexingType"),
                (int) annotationParams.get("joinableValueType"),
                customSerializer);
    }

    /**
     * Specifies how tokens should be extracted from this property.
     */
    public abstract int getTokenizerType();

    /**
     * Specifies how a property should be indexed.
     */
    public abstract int getIndexingType();

    /**
     * Specifies how a property should be processed so that the document can be joined.
     */
    public abstract int getJoinableValueType();

    /**
     * An optional {@link androidx.appsearch.app.StringSerializer}.
     *
     * <p>This is specified in the annotation when the annotated getter/field is of some custom
     * type that should boil down to a String in the database.
     *
     * @see androidx.appsearch.annotation.Document.StringProperty#serializer()
     */
    @Nullable
    public abstract SerializerClass getCustomSerializer();

    @NonNull
    @Override
    public final Kind getDataPropertyKind() {
        return Kind.STRING_PROPERTY;
    }

    @NonNull
    @Override
    public TypeMirror getUnderlyingTypeWithinGenericDoc(@NonNull IntrospectionHelper helper) {
        return helper.mStringType;
    }
}
