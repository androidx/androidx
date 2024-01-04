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

package androidx.appsearch.compiler;

import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.getDocumentAnnotation;
import static androidx.appsearch.compiler.IntrospectionHelper.getPropertyType;
import static androidx.appsearch.compiler.IntrospectionHelper.validateIsGetter;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.StringPropertyAnnotation;

import com.google.auto.value.AutoValue;

import java.util.Collection;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * A getter or field annotated with a {@link PropertyAnnotation} annotation. For example,
 *
 * <pre>
 * {@code
 * @Document("MyEntity")
 * public final class Entity {
 *     @Document.Id
 *     public String mId;
 * //                ^^^
 *
 * // OR
 *
 *     @Document.StringProperty()
 *     public String getName() {...}
 * //                ^^^^^^^
 * }
 * }
 * </pre>
 */
@AutoValue
public abstract class AnnotatedGetterOrField {
    /**
     * Specifies whether the getter/field is assigned a collection or array or a single type.
     *
     * <p>Note: {@code byte[]} are treated specially such that
     * <ul>
     *     <li>{@code byte[]} is a primitive in icing and is treated as {@link #SINGLE}.</li>
     *     <li>{@code Collection<byte[]>} is treated as a {@link #COLLECTION}.</li>
     *     <li>{@code byte[][]} is treated as an {@link #ARRAY}.</li>
     * </ul>
     *
     * The boxed {@link Byte} type is not supported by the AppSearch compiler at all.
     */
    public enum ElementTypeCategory {
        SINGLE, COLLECTION, ARRAY
    }

    /**
     * Creates a {@link AnnotatedGetterOrField} if the element is annotated with some
     * {@link PropertyAnnotation}. Otherwise returns null.
     */
    @Nullable
    public static AnnotatedGetterOrField tryCreateFor(
            @NonNull Element element,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        requireNonNull(element);
        requireNonNull(env);

        AnnotationMirror annotation = getSingleAppSearchAnnotation(element);
        if (annotation == null) {
            return null;
        }

        MetadataPropertyAnnotation metadataPropertyAnnotation =
                MetadataPropertyAnnotation.tryParse(annotation);
        if (metadataPropertyAnnotation != null) {
            return AnnotatedGetterOrField.create(metadataPropertyAnnotation, element, env);
        }

        String normalizedName = inferNormalizedName(element, env); // e.g. mField -> field
        DataPropertyAnnotation dataPropertyAnnotation =
                DataPropertyAnnotation.tryParse(
                        annotation, /* defaultName= */normalizedName, new IntrospectionHelper(env));
        if (dataPropertyAnnotation != null) {
            return AnnotatedGetterOrField.create(dataPropertyAnnotation, element, env);
        }

        return null;
    }

    /**
     * Creates a {@link AnnotatedGetterOrField} for a {@code getterOrField} annotated with the
     * specified {@code annotation}.
     */
    @NonNull
    public static AnnotatedGetterOrField create(
            @NonNull PropertyAnnotation annotation,
            @NonNull Element getterOrField,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        requireNonNull(annotation);
        requireNonNull(getterOrField);
        requireNonNull(env);
        requireIsGetterOrField(getterOrField);

        ElementTypeCategory typeCategory = inferTypeCategory(getterOrField, env);
        AnnotatedGetterOrField annotatedGetterOrField =
                new AutoValue_AnnotatedGetterOrField(
                        annotation,
                        getterOrField,
                        typeCategory,
                        /* componentType= */inferComponentType(getterOrField, typeCategory),
                        /* normalizedName= */inferNormalizedName(getterOrField, env));

        requireTypeMatchesAnnotation(annotatedGetterOrField, env);

        return annotatedGetterOrField;
    }

    /**
     * The annotation that the getter or field is annotated with.
     */
    @NonNull
    public abstract PropertyAnnotation getAnnotation();

    /**
     * The annotated getter or field.
     */
    @NonNull
    public abstract Element getElement();

    /**
     * The type-category of the getter or field.
     *
     * <p>Note: {@code byte[]} as treated specially as documented in {@link ElementTypeCategory}.
     */
    @NonNull
    public abstract ElementTypeCategory getElementTypeCategory();

    /**
     * The field/getter's return type.
     */
    @NonNull
    public TypeMirror getJvmType() {
        return isGetter()
                ? ((ExecutableElement) getElement()).getReturnType()
                : getElement().asType();
    }

    /**
     * The field/getter's return type if non-repeated, else the underlying element type.
     *
     * <p>For example, {@code String} for a field {@code String mName} and {@code int} for a field
     * {@code int[] mNums}.
     *
     * <p>The one exception to this is {@code byte[]} where:
     *
     * <pre>
     * {@code
     * @BytesProperty bytes[] mField; // componentType: byte[]
     * @BytesProperty bytes[][] mField; // componentType: byte[]
     * @BytesProperty List<bytes [ ]> mField; // componentType: byte[]
     * }
     * </pre>
     */
    @NonNull
    public abstract TypeMirror getComponentType();

    /**
     * The getter/field's jvm name e.g. {@code mId} or {@code getName}.
     */
    @NonNull
    public String getJvmName() {
        return getElement().getSimpleName().toString();
    }

    /**
     * The normalized/stemmed {@link #getJvmName()}.
     *
     * <p>For example,
     * <pre>
     * {@code
     * getName -> name
     * mName -> name
     * _name -> name
     * name_ -> name
     * isAwesome -> awesome
     * }
     * </pre>
     */
    @NonNull
    public abstract String getNormalizedName();

    /**
     * Whether the {@link #getElement()} is a getter.
     */
    public boolean isGetter() {
        return getElement().getKind() == ElementKind.METHOD;
    }

    /**
     * Whether the {@link #getElement()} is a field.
     */
    public boolean isField() {
        return getElement().getKind() == ElementKind.FIELD;
    }

    private static void requireIsGetterOrField(@NonNull Element element)
            throws ProcessingException {
        switch (element.getKind()) {
            case FIELD:
                return;
            case METHOD:
                ExecutableElement method = (ExecutableElement) element;
                List<ProcessingException> errors = validateIsGetter(method);
                if (!errors.isEmpty()) {
                    ProcessingException err = new ProcessingException(
                            "Failed to find a suitable getter for element \"%s\"".formatted(
                                    method.getSimpleName()),
                            method);
                    err.addWarnings(errors);
                    throw err;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Infers whether the getter/field returns a collection or array or neither.
     *
     * <p>Note: {@code byte[]} are treated specially as documented in {@link ElementTypeCategory}.
     */
    @NonNull
    private static ElementTypeCategory inferTypeCategory(
            @NonNull Element getterOrField,
            @NonNull ProcessingEnvironment env) {
        TypeMirror jvmType = getPropertyType(getterOrField);
        Types typeUtils = env.getTypeUtils();
        IntrospectionHelper helper = new IntrospectionHelper(env);
        if (typeUtils.isAssignable(typeUtils.erasure(jvmType), helper.mCollectionType)) {
            return ElementTypeCategory.COLLECTION;
        } else if (jvmType.getKind() == TypeKind.ARRAY
                && !typeUtils.isSameType(jvmType, helper.mBytePrimitiveArrayType)
                && !typeUtils.isSameType(jvmType, helper.mByteBoxArrayType)) {
            // byte[] has a native representation in Icing and should be considered a
            // primitive by itself.
            //
            // byte[][], however, is considered repeated (ARRAY).
            return ElementTypeCategory.ARRAY;
        } else {
            return ElementTypeCategory.SINGLE;
        }
    }

    /**
     * Infers the getter/field's return type if non-repeated, else the underlying element type.
     *
     * <p>For example, {@code String mField -> String} and {@code List<String> mField -> String}.
     */
    @NonNull
    private static TypeMirror inferComponentType(
            @NonNull Element getterOrField,
            @NonNull ElementTypeCategory typeCategory) throws ProcessingException {
        TypeMirror jvmType = getPropertyType(getterOrField);
        switch (typeCategory) {
            case SINGLE:
                return jvmType;
            case COLLECTION:
                // e.g. List<T>
                //           ^
                List<? extends TypeMirror> typeArguments =
                        ((DeclaredType) jvmType).getTypeArguments();
                if (typeArguments.isEmpty()) {
                    throw new ProcessingException(
                            "Property is repeated but has no generic rawType", getterOrField);
                }
                return typeArguments.get(0);
            case ARRAY:
                return ((ArrayType) jvmType).getComponentType();
            default:
                throw new IllegalStateException("Unhandled type-category: " + typeCategory);
        }
    }

    @NonNull
    private static String inferNormalizedName(
            @NonNull Element element,
            @NonNull ProcessingEnvironment env) {
        return element.getKind() == ElementKind.METHOD
                ? inferNormalizedMethodName(element, env)
                : inferNormalizedFieldName(element);
    }

    /**
     * Makes sure the getter/field's JVM type matches the type expected by the
     * {@link PropertyAnnotation}.
     */
    private static void requireTypeMatchesAnnotation(
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        PropertyAnnotation annotation = getterOrField.getAnnotation();
        switch (annotation.getPropertyKind()) {
            case METADATA_PROPERTY:
                requireTypeMatchesMetadataPropertyAnnotation(
                        getterOrField, (MetadataPropertyAnnotation) annotation, env);
                break;
            case DATA_PROPERTY:
                requireTypeMatchesDataPropertyAnnotation(
                        getterOrField, (DataPropertyAnnotation) annotation, env);
                break;
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
    }

    /**
     * Returns the only {@code @Document.*} annotation on the element e.g.
     * {@code @Document.StringProperty}.
     *
     * <p>Returns null if no such annotation exists on the element.
     *
     * @throws ProcessingException If the element is annotated with more than one of such
     *                             annotations.
     */
    @Nullable
    private static AnnotationMirror getSingleAppSearchAnnotation(
            @NonNull Element element) throws ProcessingException {
        // @Document.* annotation
        List<? extends AnnotationMirror> annotations =
                element.getAnnotationMirrors().stream()
                        .filter(ann -> ann.getAnnotationType().toString().startsWith(
                                DOCUMENT_ANNOTATION_CLASS.canonicalName())).toList();
        if (annotations.isEmpty()) {
            return null;
        }
        if (annotations.size() > 1) {
            throw new ProcessingException("Cannot use multiple @Document.* annotations",
                    element);
        }
        return annotations.get(0);
    }

    @NonNull
    private static String inferNormalizedMethodName(
            @NonNull Element method, @NonNull ProcessingEnvironment env) {
        String methodName = method.getSimpleName().toString();
        IntrospectionHelper helper = new IntrospectionHelper(env);
        // String getName() -> name
        if (methodName.startsWith("get") && methodName.length() > 3
                && Character.isUpperCase(methodName.charAt(3))) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        }

        // String isAwesome() -> awesome
        if (helper.isFieldOfBooleanType(method) && methodName.startsWith("is")
                && methodName.length() > 2) {
            return methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        }

        // Assume the method's name is the normalized name as well: String name() -> name
        return methodName;
    }

    @NonNull
    private static String inferNormalizedFieldName(@NonNull Element field) {
        String fieldName = field.getSimpleName().toString();
        if (fieldName.length() < 2) {
            return fieldName;
        }

        // String mName -> name
        if (fieldName.charAt(0) == 'm' && Character.isUpperCase(fieldName.charAt(1))) {
            return fieldName.substring(1, 2).toLowerCase() + fieldName.substring(2);
        }

        // String _name -> name
        if (fieldName.charAt(0) == '_'
                && fieldName.charAt(1) != '_'
                && Character.isLowerCase(fieldName.charAt(1))) {
            return fieldName.substring(1);
        }

        // String name_ -> name
        if (fieldName.charAt(fieldName.length() - 1) == '_'
                && fieldName.charAt(fieldName.length() - 2) != '_') {
            return fieldName.substring(0, fieldName.length() - 1);
        }

        // Assume the field's name is the normalize name as well: String name -> name
        return fieldName;
    }

    /**
     * Makes sure the getter/field's JVM type matches the type expected by the
     * {@link MetadataPropertyAnnotation}.
     *
     * <p>For example, fields annotated with {@code @Document.Score} must be of type {@code int}
     * or {@link Integer}.
     */
    private static void requireTypeMatchesMetadataPropertyAnnotation(
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull MetadataPropertyAnnotation annotation,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        IntrospectionHelper helper = new IntrospectionHelper(env);
        switch (annotation) {
            case ID: // fall-through
            case NAMESPACE:
                requireTypeIsOneOf(
                        getterOrField, List.of(helper.mStringType), env, /* allowRepeated= */false);
                break;
            case TTL_MILLIS: // fall-through
            case CREATION_TIMESTAMP_MILLIS:
                requireTypeIsOneOf(
                        getterOrField,
                        List.of(helper.mLongPrimitiveType, helper.mIntPrimitiveType,
                                helper.mLongBoxType, helper.mIntegerBoxType),
                        env,
                        /* allowRepeated= */false);
                break;
            case SCORE:
                requireTypeIsOneOf(
                        getterOrField,
                        List.of(helper.mIntPrimitiveType, helper.mIntegerBoxType),
                        env,
                        /* allowRepeated= */false);
                break;
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
    }

    /**
     * Makes sure the getter/field's JVM type matches the type expected by the
     * {@link DataPropertyAnnotation}.
     *
     * <p>For example, fields annotated with {@link StringPropertyAnnotation} must be of type
     * {@link String} or a collection or array of {@link String}s.
     */
    private static void requireTypeMatchesDataPropertyAnnotation(
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull DataPropertyAnnotation annotation,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        IntrospectionHelper helper = new IntrospectionHelper(env);
        switch (annotation.getDataPropertyKind()) {
            case STRING_PROPERTY:
                requireTypeIsOneOf(
                        getterOrField, List.of(helper.mStringType), env, /* allowRepeated= */true);
                break;
            case DOCUMENT_PROPERTY:
                requireTypeIsSomeDocumentClass(getterOrField, env);
                break;
            case LONG_PROPERTY:
                requireTypeIsOneOf(
                        getterOrField,
                        List.of(helper.mLongPrimitiveType, helper.mIntPrimitiveType,
                                helper.mLongBoxType, helper.mIntegerBoxType),
                        env,
                        /* allowRepeated= */true);
                break;
            case DOUBLE_PROPERTY:
                requireTypeIsOneOf(
                        getterOrField,
                        List.of(helper.mDoublePrimitiveType, helper.mFloatPrimitiveType,
                                helper.mDoubleBoxType, helper.mFloatBoxType),
                        env,
                        /* allowRepeated= */true);
                break;
            case BOOLEAN_PROPERTY:
                requireTypeIsOneOf(
                        getterOrField,
                        List.of(helper.mBooleanPrimitiveType, helper.mBooleanBoxType),
                        env,
                        /* allowRepeated= */true);
                break;
            case BYTES_PROPERTY:
                requireTypeIsOneOf(
                        getterOrField,
                        List.of(helper.mBytePrimitiveArrayType),
                        env,
                        /* allowRepeated= */true);
                break;
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
    }

    /**
     * Makes sure the getter/field's type is one of the expected types.
     *
     * <p>If {@code allowRepeated} is true, also allows the getter/field's type to be an array or
     * collection of any of the expected types.
     */
    private static void requireTypeIsOneOf(
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull Collection<TypeMirror> expectedTypes,
            @NonNull ProcessingEnvironment env,
            boolean allowRepeated) throws ProcessingException {
        Types typeUtils = env.getTypeUtils();
        TypeMirror target = allowRepeated
                ? getterOrField.getComponentType() : getterOrField.getJvmType();
        boolean isValid = expectedTypes.stream()
                .anyMatch(expectedType -> typeUtils.isSameType(expectedType, target));
        if (!isValid) {
            String error = "@"
                    + getterOrField.getAnnotation().getClassName().simpleName()
                    + " must only be placed on a getter/field of type "
                    + (allowRepeated ? "or array or collection of " : "")
                    + expectedTypes.stream().map(TypeMirror::toString).collect(joining("|"));
            throw new ProcessingException(error, getterOrField.getElement());
        }
    }

    /**
     * Makes sure the getter/field is assigned a type annotated with {@code @Document}.
     *
     * <p>Allows for arrays and collections of such a type as well.
     */
    private static void requireTypeIsSomeDocumentClass(
            @NonNull AnnotatedGetterOrField annotatedGetterOrField,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        TypeMirror componentType = annotatedGetterOrField.getComponentType();
        if (componentType.getKind() == TypeKind.DECLARED) {
            Element element = env.getTypeUtils().asElement(componentType);
            if (element.getKind() == ElementKind.CLASS && getDocumentAnnotation(element) != null) {
                return;
            }
        }
        throw new ProcessingException(
                "Invalid type for @DocumentProperty. Must be another class "
                        + "annotated with @Document (or collection or array of)",
                annotatedGetterOrField.getElement());
    }
}
