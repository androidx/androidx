/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.appsearch.compiler.IntrospectionHelper.generateClassHierarchy;
import static androidx.appsearch.compiler.IntrospectionHelper.getDocumentAnnotation;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Processes @Document annotations.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DocumentModel {
    private static final String CLASS_SUFFIX = ".class";

    private final IntrospectionHelper mHelper;

    private final Elements mElementUtil;

    private final TypeElement mClass;

    // The name of the original class annotated with @Document
    private final String mQualifiedDocumentClassName;

    private final String mSchemaName;

    private final LinkedHashSet<TypeElement> mParentTypes;

    private final LinkedHashSet<AnnotatedGetterOrField> mAnnotatedGettersAndFields;

    @NonNull
    private final AnnotatedGetterOrField mIdAnnotatedGetterOrField;

    @NonNull
    private final AnnotatedGetterOrField mNamespaceAnnotatedGetterOrField;

    @NonNull
    private final Map<AnnotatedGetterOrField, PropertyAccessor> mAccessors;

    @NonNull
    private final DocumentClassCreationInfo mDocumentClassCreationInfo;

    private DocumentModel(
            @NonNull ProcessingEnvironment env,
            @NonNull TypeElement clazz,
            @Nullable TypeElement generatedAutoValueElement)
            throws ProcessingException {
        if (clazz.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ProcessingException("@Document annotated class is private", clazz);
        }

        mHelper = new IntrospectionHelper(env);
        mElementUtil = env.getElementUtils();
        mClass = clazz;
        mQualifiedDocumentClassName = generatedAutoValueElement != null
                ? generatedAutoValueElement.getQualifiedName().toString()
                : clazz.getQualifiedName().toString();
        mParentTypes = getParentSchemaTypes(clazz);

        List<TypeElement> classHierarchy = generateClassHierarchy(clazz);
        mSchemaName = computeSchemaName(classHierarchy);
        mAnnotatedGettersAndFields = scanAnnotatedGettersAndFields(classHierarchy, env);

        requireNoDuplicateMetadataProperties();
        mIdAnnotatedGetterOrField = requireGetterOrFieldMatchingPredicate(
                getterOrField -> getterOrField.getAnnotation() == MetadataPropertyAnnotation.ID,
                /* errorMessage= */"All @Document classes must have exactly one field annotated "
                        + "with @Id");
        mNamespaceAnnotatedGetterOrField = requireGetterOrFieldMatchingPredicate(
                getterOrField ->
                        getterOrField.getAnnotation() == MetadataPropertyAnnotation.NAMESPACE,
                /* errorMessage= */"All @Document classes must have exactly one field annotated "
                        + "with @Namespace");

        LinkedHashSet<ExecutableElement> allMethods = mHelper.getAllMethods(clazz);
        mAccessors = inferPropertyAccessors(mAnnotatedGettersAndFields, allMethods, mHelper);
        mDocumentClassCreationInfo =
                DocumentClassCreationInfo.infer(clazz, mAnnotatedGettersAndFields, mHelper);
    }

    private static LinkedHashSet<AnnotatedGetterOrField> scanAnnotatedGettersAndFields(
            @NonNull List<TypeElement> hierarchy,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        AnnotatedGetterAndFieldAccumulator accumulator = new AnnotatedGetterAndFieldAccumulator();
        for (TypeElement type : hierarchy) {
            for (Element enclosedElement : type.getEnclosedElements()) {
                AnnotatedGetterOrField getterOrField =
                        AnnotatedGetterOrField.tryCreateFor(enclosedElement, env);
                if (getterOrField == null) {
                    continue;
                }
                accumulator.add(getterOrField);
            }
        }
        return accumulator.getAccumulatedGettersAndFields();
    }

    /**
     * Makes sure {@link #mAnnotatedGettersAndFields} does not contain two getters/fields
     * annotated with the same metadata annotation e.g. it doesn't make sense for a document to
     * have two {@code @Document.Id}s.
     */
    private void requireNoDuplicateMetadataProperties() throws ProcessingException {
        Map<MetadataPropertyAnnotation, List<AnnotatedGetterOrField>> annotationToGettersAndFields =
                mAnnotatedGettersAndFields.stream()
                        .filter(getterOrField ->
                                getterOrField.getAnnotation().getPropertyKind()
                                        == PropertyAnnotation.Kind.METADATA_PROPERTY)
                        .collect(groupingBy((getterOrField) ->
                                (MetadataPropertyAnnotation) getterOrField.getAnnotation()));
        for (Map.Entry<MetadataPropertyAnnotation, List<AnnotatedGetterOrField>> entry :
                annotationToGettersAndFields.entrySet()) {
            MetadataPropertyAnnotation annotation = entry.getKey();
            List<AnnotatedGetterOrField> gettersAndFields = entry.getValue();
            if (gettersAndFields.size() > 1) {
                // Can show the error on any of the duplicates. Just pick the first first.
                throw new ProcessingException(
                        "Duplicate member annotated with @"
                                + annotation.getClassName().simpleName(),
                        gettersAndFields.get(0).getElement());
            }
        }
    }

    /**
     * Makes sure {@link #mAnnotatedGettersAndFields} contains a getter/field that matches the
     * predicate.
     *
     * @return The matched getter/field.
     * @throws ProcessingException with the error message if no match.
     */
    @NonNull
    private AnnotatedGetterOrField requireGetterOrFieldMatchingPredicate(
            @NonNull Predicate<AnnotatedGetterOrField> predicate,
            @NonNull String errorMessage) throws ProcessingException {
        return mAnnotatedGettersAndFields.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new ProcessingException(errorMessage, mClass));
    }

    /**
     * Tries to create an {@link DocumentModel} from the given {@link Element}.
     *
     * @throws ProcessingException if the @{@code Document}-annotated class is invalid.
     */
    public static DocumentModel createPojoModel(
            @NonNull ProcessingEnvironment env, @NonNull TypeElement clazz)
            throws ProcessingException {
        return new DocumentModel(env, clazz, null);
    }

    /**
     * Tries to create an {@link DocumentModel} from the given AutoValue {@link Element} and
     * corresponding generated class.
     *
     * @throws ProcessingException if the @{@code Document}-annotated class is invalid.
     */
    public static DocumentModel createAutoValueModel(
            @NonNull ProcessingEnvironment env, @NonNull TypeElement clazz,
            @NonNull TypeElement generatedAutoValueElement)
            throws ProcessingException {
        return new DocumentModel(env, clazz, generatedAutoValueElement);
    }

    @NonNull
    public TypeElement getClassElement() {
        return mClass;
    }

    /**
     * The name of the original class annotated with @Document
     *
     * @return the class name
     */
    @NonNull
    public String getQualifiedDocumentClassName() {
        return mQualifiedDocumentClassName;
    }

    @NonNull
    public String getSchemaName() {
        return mSchemaName;
    }

    /**
     * Returns the set of parent classes specified in @Document via the "parent" parameter.
     */
    @NonNull
    public Set<TypeElement> getParentTypes() {
        return mParentTypes;
    }

    /**
     * Returns all getters/fields (declared or inherited) annotated with some
     * {@link PropertyAnnotation}.
     */
    @NonNull
    public Set<AnnotatedGetterOrField> getAnnotatedGettersAndFields() {
        return mAnnotatedGettersAndFields;
    }

    /**
     * Returns the getter/field annotated with {@code @Document.Id}.
     */
    @NonNull
    public AnnotatedGetterOrField getIdAnnotatedGetterOrField() {
        return mIdAnnotatedGetterOrField;
    }

    /**
     * Returns the getter/field annotated with {@code @Document.Namespace}.
     */
    @NonNull
    public AnnotatedGetterOrField getNamespaceAnnotatedGetterOrField() {
        return mNamespaceAnnotatedGetterOrField;
    }

    /**
     * Returns the public/package-private accessor for an annotated getter/field (may be private).
     */
    @NonNull
    public PropertyAccessor getAccessor(@NonNull AnnotatedGetterOrField getterOrField) {
        PropertyAccessor accessor = mAccessors.get(getterOrField);
        if (accessor == null) {
            throw new IllegalArgumentException(
                    "No such getter/field belongs to this DocumentModel: " + getterOrField);
        }
        return accessor;
    }

    @NonNull
    public DocumentClassCreationInfo getDocumentClassCreationInfo() {
        return mDocumentClassCreationInfo;
    }

    /**
     * Infers the {@link PropertyAccessor} for each of the {@link AnnotatedGetterOrField}.
     *
     * <p>Each accessor may be the {@link AnnotatedGetterOrField} itself or some other non-private
     * getter.
     */
    @NonNull
    private static Map<AnnotatedGetterOrField, PropertyAccessor> inferPropertyAccessors(
            @NonNull Collection<AnnotatedGetterOrField> annotatedGettersAndFields,
            @NonNull Collection<ExecutableElement> allMethods,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        Map<AnnotatedGetterOrField, PropertyAccessor> accessors = new HashMap<>();
        for (AnnotatedGetterOrField getterOrField : annotatedGettersAndFields) {
            accessors.put(
                    getterOrField,
                    PropertyAccessor.infer(getterOrField, allMethods, helper));
        }
        return accessors;
    }

    /**
     * Returns the parent types mentioned within the {@code @Document} annotation.
     */
    @NonNull
    private LinkedHashSet<TypeElement> getParentSchemaTypes(
            @NonNull TypeElement documentClass) throws ProcessingException {
        AnnotationMirror documentAnnotation = requireNonNull(getDocumentAnnotation(documentClass));
        Map<String, Object> params = mHelper.getAnnotationParams(documentAnnotation);
        LinkedHashSet<TypeElement> parentsSchemaTypes = new LinkedHashSet<>();
        Object parentsParam = params.get("parent");
        if (parentsParam instanceof List) {
            for (Object parent : (List<?>) parentsParam) {
                String parentClassName = parent.toString();
                parentClassName = parentClassName.substring(0,
                        parentClassName.length() - CLASS_SUFFIX.length());
                parentsSchemaTypes.add(mElementUtil.getTypeElement(parentClassName));
            }
        }
        if (!parentsSchemaTypes.isEmpty() && params.get("name").toString().isEmpty()) {
            throw new ProcessingException(
                    "All @Document classes with a parent must explicitly provide a name",
                    mClass);
        }
        return parentsSchemaTypes;
    }

    /**
     * Computes the schema name for a Document class given its hierarchy of parent @Document
     * classes.
     *
     * <p>The schema name will be the most specific Document class that has an explicit schema name,
     * to allow the schema name to be manually set with the "name" annotation. If no such Document
     * class exists, use the name of the root Document class, so that performing a query on the base
     * \@Document class will also return child @Document classes.
     *
     * @param hierarchy List of classes annotated with \@Document, with the root class at the
     *                  beginning and the final class at the end
     * @return the final schema name for the class at the end of the hierarchy
     */
    @NonNull
    private String computeSchemaName(List<TypeElement> hierarchy) {
        for (int i = hierarchy.size() - 1; i >= 0; i--) {
            AnnotationMirror documentAnnotation = getDocumentAnnotation(hierarchy.get(i));
            if (documentAnnotation == null) {
                continue;
            }
            Map<String, Object> params = mHelper.getAnnotationParams(documentAnnotation);
            String name = params.get("name").toString();
            if (!name.isEmpty()) {
                return name;
            }
        }
        // Nobody had a name annotation -- use the class name of the root document in the hierarchy
        TypeElement rootDocumentClass = hierarchy.get(0);
        AnnotationMirror rootDocumentAnnotation = getDocumentAnnotation(rootDocumentClass);
        if (rootDocumentAnnotation == null) {
            return mClass.getSimpleName().toString();
        }
        // Documents don't need an explicit name annotation, can use the class name
        return rootDocumentClass.getSimpleName().toString();
    }

    /**
     * Accumulates and de-duplicates {@link AnnotatedGetterOrField}s within a class hierarchy and
     * ensures all of the following:
     *
     * <ol>
     *     <li>
     *         The same getter/field doesn't appear in the class hierarchy with different
     *         annotation types e.g.
     *
     *         <pre>
     *         {@code
     *         @Document
     *         class Parent {
     *             @Document.StringProperty
     *             public String getProp();
     *         }
     *
     *         @Document
     *         class Child extends Parent {
     *             @Document.Id
     *             public String getProp();
     *         }
     *         }
     *         </pre>
     *     </li>
     *     <li>
     *         The same getter/field doesn't appear twice with different serialized names e.g.
     *
     *         <pre>
     *         {@code
     *         @Document
     *         class Parent {
     *             @Document.StringProperty("foo")
     *             public String getProp();
     *         }
     *
     *         @Document
     *         class Child extends Parent {
     *             @Document.StringProperty("bar")
     *             public String getProp();
     *         }
     *         }
     *         </pre>
     *     </li>
     *     <li>
     *         The same serialized name doesn't appear on two separate getters/fields e.g.
     *
     *         <pre>
     *         {@code
     *         @Document
     *         class Gift {
     *             @Document.StringProperty("foo")
     *             String mField;
     *
     *             @Document.LongProperty("foo")
     *             Long getProp();
     *         }
     *         }
     *         </pre>
     *     </li>
     *     <li>
     *         Two annotated element do not have the same normalized name because this hinders with
     *         downstream logic that tries to infer {@link CreationMethod}s e.g.
     *
     *         <pre>
     *         {@code
     *         @Document
     *         class Gift {
     *             @Document.StringProperty
     *             String mFoo;
     *
     *             @Document.StringProperty
     *             String getFoo() {...}
     *             void setFoo(String value) {...}
     *         }
     *         }
     *         </pre>
     *     </li>
     * </ol>
     *
     * @see CreationMethod#inferParamAssociationsAndCreate
     */
    private static final class AnnotatedGetterAndFieldAccumulator {
        private final Map<String, AnnotatedGetterOrField> mJvmNameToGetterOrField =
                new LinkedHashMap<>();
        private final Map<String, AnnotatedGetterOrField> mSerializedNameToGetterOrField =
                new HashMap<>();
        private final Map<String, AnnotatedGetterOrField> mNormalizedNameToGetterOrField =
                new HashMap<>();

        AnnotatedGetterAndFieldAccumulator() {
        }

        /**
         * Adds the {@link AnnotatedGetterOrField} to the accumulator.
         *
         * <p>{@link AnnotatedGetterOrField} that appear again are considered to be overridden
         * versions and replace the older ones.
         *
         * <p>Hence, this method should be called with {@link AnnotatedGetterOrField}s from the
         * least specific types to the most specific type.
         */
        void add(@NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
            String jvmName = getterOrField.getJvmName();
            AnnotatedGetterOrField existingGetterOrField = mJvmNameToGetterOrField.get(jvmName);

            if (existingGetterOrField == null) {
                // First time we're seeing this getter or field
                mJvmNameToGetterOrField.put(jvmName, getterOrField);

                requireUniqueNormalizedName(getterOrField);
                mNormalizedNameToGetterOrField.put(
                        getterOrField.getNormalizedName(), getterOrField);

                if (hasDataPropertyAnnotation(getterOrField)) {
                    requireSerializedNameNeverSeenBefore(getterOrField);
                    mSerializedNameToGetterOrField.put(
                            getSerializedName(getterOrField), getterOrField);
                }
            } else {
                // Seen this getter or field before. It showed up again because of overriding.
                requireAnnotationTypeIsConsistent(existingGetterOrField, getterOrField);
                // Replace the old entries
                mJvmNameToGetterOrField.put(jvmName, getterOrField);
                mNormalizedNameToGetterOrField.put(
                        getterOrField.getNormalizedName(), getterOrField);

                if (hasDataPropertyAnnotation(getterOrField)) {
                    requireSerializedNameIsConsistent(existingGetterOrField, getterOrField);
                    // Replace the old entry
                    mSerializedNameToGetterOrField.put(
                            getSerializedName(getterOrField), getterOrField);
                }
            }
        }

        @NonNull
        LinkedHashSet<AnnotatedGetterOrField> getAccumulatedGettersAndFields() {
            return new LinkedHashSet<>(mJvmNameToGetterOrField.values());
        }

        /**
         * Makes sure the getter/field's normalized name either never appeared before, or if it did,
         * did so for the same getter/field and re-appeared likely because of overriding.
         */
        private void requireUniqueNormalizedName(
                @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
            AnnotatedGetterOrField existingGetterOrField =
                    mNormalizedNameToGetterOrField.get(getterOrField.getNormalizedName());
            if (existingGetterOrField == null) {
                // Never seen this normalized name before
                return;
            }
            if (existingGetterOrField.getJvmName().equals(getterOrField.getJvmName())) {
                // Same getter/field appeared again (likely because of overriding). Ok.
                return;
            }
            throw new ProcessingException(
                    ("Normalized name \"%s\" is already taken up by pre-existing %s. "
                            + "Please rename this getter/field to something else.").formatted(
                            getterOrField.getNormalizedName(),
                            createSignatureString(existingGetterOrField)),
                    getterOrField.getElement());
        }

        /**
         * Makes sure a new getter/field is never annotated with a serialized name that is
         * already given to some other getter/field.
         *
         * <p>Assumes the getter/field is annotated with a {@link DataPropertyAnnotation}.
         */
        private void requireSerializedNameNeverSeenBefore(
                @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
            String serializedName = getSerializedName(getterOrField);
            AnnotatedGetterOrField existingGetterOrField =
                    mSerializedNameToGetterOrField.get(serializedName);
            if (existingGetterOrField != null) {
                throw new ProcessingException(
                        "Cannot give property the name '%s' because it is already used for %s"
                                .formatted(serializedName, existingGetterOrField.getJvmName()),
                        getterOrField.getElement());
            }
        }

        /**
         * Returns the serialized name that should be used for the property in the database.
         *
         * <p>Assumes the getter/field is annotated with a {@link DataPropertyAnnotation}.
         */
        @NonNull
        private static String getSerializedName(@NonNull AnnotatedGetterOrField getterOrField) {
            DataPropertyAnnotation annotation =
                    (DataPropertyAnnotation) getterOrField.getAnnotation();
            return annotation.getName();
        }

        private static boolean hasDataPropertyAnnotation(
                @NonNull AnnotatedGetterOrField getterOrField) {
            PropertyAnnotation annotation = getterOrField.getAnnotation();
            return annotation.getPropertyKind() == PropertyAnnotation.Kind.DATA_PROPERTY;
        }

        /**
         * Makes sure the annotation type didn't change when overriding e.g.
         * {@code @StringProperty -> @Id}.
         */
        private static void requireAnnotationTypeIsConsistent(
                @NonNull AnnotatedGetterOrField existingGetterOrField,
                @NonNull AnnotatedGetterOrField overriddenGetterOfField)
                throws ProcessingException {
            PropertyAnnotation existingAnnotation = existingGetterOrField.getAnnotation();
            PropertyAnnotation overriddenAnnotation = overriddenGetterOfField.getAnnotation();
            if (!existingAnnotation.getClassName().equals(overriddenAnnotation.getClassName())) {
                throw new ProcessingException(
                        ("Property type must stay consistent when overriding annotated members "
                                + "but changed from @%s -> @%s").formatted(
                                existingAnnotation.getClassName().simpleName(),
                                overriddenAnnotation.getClassName().simpleName()),
                        overriddenGetterOfField.getElement());
            }
        }

        /**
         * Makes sure the serialized name didn't change when overriding.
         *
         * <p>Assumes the getter/field is annotated with a {@link DataPropertyAnnotation}.
         */
        private static void requireSerializedNameIsConsistent(
                @NonNull AnnotatedGetterOrField existingGetterOrField,
                @NonNull AnnotatedGetterOrField overriddenGetterOrField)
                throws ProcessingException {
            String existingSerializedName = getSerializedName(existingGetterOrField);
            String overriddenSerializedName = getSerializedName(overriddenGetterOrField);
            if (!existingSerializedName.equals(overriddenSerializedName)) {
                throw new ProcessingException(
                        ("Property name within the annotation must stay consistent when overriding "
                                + "annotated members but changed from '%s' -> '%s'".formatted(
                                existingSerializedName, overriddenSerializedName)),
                        overriddenGetterOrField.getElement());
            }
        }

        @NonNull
        private static String createSignatureString(@NonNull AnnotatedGetterOrField getterOrField) {
            return getterOrField.getJvmType()
                    + " "
                    + getterOrField.getElement().getEnclosingElement().getSimpleName()
                    + "#"
                    + getterOrField.getJvmName()
                    + (getterOrField.isGetter() ? "()" : "");
        }
    }
}
