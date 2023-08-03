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

import static androidx.appsearch.compiler.IntrospectionHelper.BUILDER_PRODUCER_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.generateClassHierarchy;
import static androidx.appsearch.compiler.IntrospectionHelper.getDocumentAnnotation;

import static java.util.stream.Collectors.groupingBy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.compiler.IntrospectionHelper.PropertyClass;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Processes @Document annotations.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DocumentModel {

    /** Enumeration of fields that must be handled specially (i.e. are not properties) */
    enum SpecialField {NAMESPACE, ID, CREATION_TIMESTAMP_MILLIS, TTL_MILLIS, SCORE}

    /** Determines how the annotation processor has decided to write the value of a field. */
    enum WriteKind {FIELD, SETTER, CREATION_METHOD}

    private static final String CLASS_SUFFIX = ".class";

    private final IntrospectionHelper mHelper;
    private final TypeElement mClass;
    private final Types mTypeUtil;
    private final Elements mElementUtil;
    // The name of the original class annotated with @Document
    private final String mQualifiedDocumentClassName;
    private String mSchemaName;
    private final Set<TypeElement> mParentTypes = new LinkedHashSet<>();
    // All methods in the current @Document annotated class/interface, or in the generated class
    // for AutoValue document.
    // Warning: if you change this to a HashSet, we may choose different getters or setters from
    // run to run, causing the generated code to bounce.
    private final LinkedHashSet<ExecutableElement> mAllMethods;
    // All methods in the builder class, if a builder producer is provided.
    private final LinkedHashSet<ExecutableElement> mAllBuilderMethods;
    // Key: Name of the element whose value is set through the setter method.
    // Value: ExecutableElement of the setter method.
    private final Map<String, ExecutableElement> mSetterMethods = new HashMap<>();
    // Warning: if you change this to a HashMap, we may assign elements in a different order from
    // run to run, causing the generated code to bounce.
    // Keeps tracks of all AppSearch elements so we can find creation and access methods for them
    // all
    private final Map<String, Element> mAllAppSearchElements = new LinkedHashMap<>();
    // Warning: if you change this to a HashMap, we may assign elements in a different order from
    // run to run, causing the generated code to bounce.
    // Keeps track of property elements so we don't allow multiple annotated elements of the same
    // name
    private final Map<String, Element> mPropertyElements = new LinkedHashMap<>();
    private final Map<SpecialField, String> mSpecialFieldNames = new EnumMap<>(SpecialField.class);
    private final Map<Element, WriteKind> mWriteKinds = new HashMap<>();
    // Contains the reason why that element couldn't be written either by field or by setter.
    private final Map<Element, ProcessingException> mWriteWhyCreationMethod =
            new HashMap<>();
    private ExecutableElement mChosenCreationMethod = null;
    private List<String> mChosenCreationMethodParams = null;
    private TypeElement mBuilderClass = null;
    private Set<ExecutableElement> mBuilderProducers = new LinkedHashSet<>();

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
        mClass = clazz;
        mTypeUtil = env.getTypeUtils();
        mElementUtil = env.getElementUtils();
        mQualifiedDocumentClassName = generatedAutoValueElement != null
                ? generatedAutoValueElement.getQualifiedName().toString()
                : clazz.getQualifiedName().toString();
        mAnnotatedGettersAndFields = scanAnnotatedGettersAndFields(clazz, env);

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

        mAllMethods = mHelper.getAllMethods(clazz);
        mAccessors = inferPropertyAccessors(mAnnotatedGettersAndFields, mAllMethods, mHelper);
        mDocumentClassCreationInfo =
                DocumentClassCreationInfo.infer(clazz, mAnnotatedGettersAndFields, mHelper);

        // Scan methods and constructors. We will need this info when processing fields to
        // make sure the fields can be get and set.
        Set<ExecutableElement> potentialCreationMethods = extractCreationMethods(clazz);
        mAllBuilderMethods = mBuilderClass != null
                ? mHelper.getAllMethods(mBuilderClass) : new LinkedHashSet<>();
        scanFields(mClass);
        chooseCreationMethod(potentialCreationMethods);
    }

    /**
     * Scans all the elements in typeElement to find a builder producer. If found, set
     * mBuilderProducers and mBuilderClass to the builder producer candidates and the builder class
     * respectively.
     *
     * @throws ProcessingException if there are more than one elements annotated with
     *                             {@code @Document.BuilderProducer}, or if the builder producer
     *                             element is not a visible static
     *                             method or a class.
     */
    private void extractBuilderProducer(TypeElement typeElement)
            throws ProcessingException {
        for (Element child : typeElement.getEnclosedElements()) {
            boolean isAnnotated = false;
            for (AnnotationMirror annotation : child.getAnnotationMirrors()) {
                if (annotation.getAnnotationType().toString().equals(
                        BUILDER_PRODUCER_CLASS.canonicalName())) {
                    isAnnotated = true;
                    break;
                }
            }
            if (!isAnnotated) {
                continue;
            }
            if (child.getKind() != ElementKind.METHOD && child.getKind() != ElementKind.CLASS) {
                // Since @Document.BuilderProducer is configured with
                // @Target({ElementType.METHOD, ElementType.TYPE}), it's not possible to reach here.
                throw new ProcessingException("Builder producer must be a method or a class",
                        child);
            }
            if (mBuilderClass != null) {
                throw new ProcessingException("Found duplicated builder producer", typeElement);
            }
            Set<Modifier> methodModifiers = child.getModifiers();
            if (!methodModifiers.contains(Modifier.STATIC)) {
                throw new ProcessingException("Builder producer must be static", child);
            }
            if (methodModifiers.contains(Modifier.PRIVATE)) {
                throw new ProcessingException("Builder producer cannot be private", child);
            }
            if (child.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) child;
                mBuilderProducers.add(method);
                mBuilderClass = (TypeElement) mTypeUtil.asElement(method.getReturnType());
            } else {
                // child is a class, so extract all of its constructors as builder producer
                // candidates. The validity of the constructor will be checked later when we
                // choose the right creation method.
                mBuilderClass = (TypeElement) child;
                for (Element builderProducer : mBuilderClass.getEnclosedElements()) {
                    if (builderProducer.getKind() == ElementKind.CONSTRUCTOR) {
                        mBuilderProducers.add((ExecutableElement) builderProducer);
                    }
                }
            }
        }
    }

    private static LinkedHashSet<AnnotatedGetterOrField> scanAnnotatedGettersAndFields(
            @NonNull TypeElement clazz,
            @NonNull ProcessingEnvironment env) throws ProcessingException {
        AnnotatedGetterAndFieldAccumulator accumulator = new AnnotatedGetterAndFieldAccumulator();
        for (TypeElement type : generateClassHierarchy(clazz)) {
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

    private Set<ExecutableElement> extractCreationMethods(TypeElement typeElement)
            throws ProcessingException {
        extractBuilderProducer(typeElement);
        // If a builder producer is provided, then only the builder can be used as a creation
        // method.
        if (mBuilderClass != null) {
            return Collections.unmodifiableSet(mBuilderProducers);
        }

        Set<ExecutableElement> creationMethods = new LinkedHashSet<>();
        for (Element child : typeElement.getEnclosedElements()) {
            if (child.getKind() == ElementKind.CONSTRUCTOR) {
                creationMethods.add((ExecutableElement) child);
            } else if (child.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) child;
                if (isFactoryMethod(method)) {
                    creationMethods.add(method);
                }
            }
        }
        return Collections.unmodifiableSet(creationMethods);
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

    @NonNull
    public Map<String, Element> getAllElements() {
        return Collections.unmodifiableMap(mAllAppSearchElements);
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
     * @deprecated Use {@link #getAnnotatedGettersAndFields()} instead.
     */
    @Deprecated
    @NonNull
    public Map<String, Element> getPropertyElements() {
        return Collections.unmodifiableMap(mPropertyElements);
    }

    @Nullable
    public String getSpecialFieldName(SpecialField field) {
        return mSpecialFieldNames.get(field);
    }

    @Nullable
    public WriteKind getElementWriteKind(String elementName) {
        Element element = mAllAppSearchElements.get(elementName);
        return mWriteKinds.get(element);
    }

    @Nullable
    public ExecutableElement getSetterForElement(String elementName) {
        return mSetterMethods.get(elementName);
    }

    /**
     * Finds the AppSearch name for the given property.
     *
     * <p>This is usually the name of the field in Java, but may be changed if the developer
     * specifies a different 'name' parameter in the annotation.
     *
     * @deprecated Use {@link #getAnnotatedGettersAndFields()} and
     * {@link DataPropertyAnnotation#getName()} ()} instead.
     */
    @Deprecated
    @NonNull
    public String getPropertyName(@NonNull Element property) throws ProcessingException {
        AnnotationMirror annotation = getPropertyAnnotation(property);
        Map<String, Object> params = mHelper.getAnnotationParams(annotation);
        String propertyName = params.get("name").toString();
        if (propertyName.isEmpty()) {
            propertyName = getNormalizedElementName(property);
        }
        return propertyName;
    }

    /**
     * Returns the first found AppSearch property annotation element from the input element's
     * annotations.
     *
     * @throws ProcessingException if no AppSearch property annotation is found.
     * @deprecated Use {@link #getAnnotatedGettersAndFields()} and
     * {@link AnnotatedGetterOrField#getAnnotation()} instead.
     */
    @Deprecated
    @NonNull
    public AnnotationMirror getPropertyAnnotation(@NonNull Element element)
            throws ProcessingException {
        Objects.requireNonNull(element);
        Set<String> propertyClassPaths = new HashSet<>();
        for (PropertyClass propertyClass : PropertyClass.values()) {
            propertyClassPaths.add(propertyClass.getClassFullPath());
        }
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            String annotationFq = annotation.getAnnotationType().toString();
            if (propertyClassPaths.contains(annotationFq)) {
                return annotation;
            }
        }
        throw new ProcessingException("Missing AppSearch property annotation.", element);
    }

    @NonNull
    public ExecutableElement getChosenCreationMethod() {
        return mChosenCreationMethod;
    }

    @NonNull
    public List<String> getChosenCreationMethodParams() {
        return Collections.unmodifiableList(mChosenCreationMethodParams);
    }

    @Nullable
    public TypeElement getBuilderClass() {
        return mBuilderClass;
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

    private boolean isFactoryMethod(ExecutableElement method) {
        Set<Modifier> methodModifiers = method.getModifiers();
        return methodModifiers.contains(Modifier.STATIC)
                && !methodModifiers.contains(Modifier.PRIVATE)
                && mTypeUtil.isSameType(method.getReturnType(), mClass.asType());
    }

    /**
     * Scan the annotations of a field to determine the fields type and handle it accordingly
     *
     * @param childElement the member of class elements currently being scanned
     * @deprecated Rely on {@link #mAnnotatedGettersAndFields} instead of
     * {@link #mAllAppSearchElements} and {@link #mSpecialFieldNames}.
     */
    @Deprecated
    private void scanAnnotatedField(@NonNull Element childElement) throws ProcessingException {
        String fieldName = childElement.getSimpleName().toString();

        // a property field shouldn't be able to override a special field
        if (mSpecialFieldNames.containsValue(fieldName)) {
            throw new ProcessingException(
                    "Non-annotated field overriding special annotated fields named: "
                            + fieldName, mAllAppSearchElements.get(fieldName));
        }

        // no annotation mirrors -> non-indexable field
        for (AnnotationMirror annotation : childElement.getAnnotationMirrors()) {
            String annotationFq = annotation.getAnnotationType().toString();
            if (!annotationFq.startsWith(DOCUMENT_ANNOTATION_CLASS.canonicalName())
                    || annotationFq.equals(BUILDER_PRODUCER_CLASS.canonicalName())) {
                continue;
            }
            if (childElement.getKind() == ElementKind.CLASS) {
                continue;
            }

            if (annotationFq.equals(MetadataPropertyAnnotation.ID.getClassName().canonicalName())) {
                if (mSpecialFieldNames.containsKey(SpecialField.ID)) {
                    throw new ProcessingException(
                            "Class hierarchy contains multiple fields annotated @Id",
                            childElement);
                }
                mSpecialFieldNames.put(SpecialField.ID, fieldName);
            } else if (annotationFq.equals(
                    MetadataPropertyAnnotation.NAMESPACE.getClassName().canonicalName())) {
                if (mSpecialFieldNames.containsKey(SpecialField.NAMESPACE)) {
                    throw new ProcessingException(
                            "Class hierarchy contains multiple fields annotated @Namespace",
                            childElement);
                }
                mSpecialFieldNames.put(SpecialField.NAMESPACE, fieldName);
            } else if (annotationFq.equals(
                    MetadataPropertyAnnotation.CREATION_TIMESTAMP_MILLIS
                            .getClassName()
                            .canonicalName())) {
                if (mSpecialFieldNames.containsKey(SpecialField.CREATION_TIMESTAMP_MILLIS)) {
                    throw new ProcessingException("Class hierarchy contains multiple fields "
                            + "annotated @CreationTimestampMillis", childElement);
                }
                mSpecialFieldNames.put(
                        SpecialField.CREATION_TIMESTAMP_MILLIS, fieldName);
            } else if (annotationFq.equals(
                    MetadataPropertyAnnotation.TTL_MILLIS.getClassName().canonicalName())) {
                if (mSpecialFieldNames.containsKey(SpecialField.TTL_MILLIS)) {
                    throw new ProcessingException(
                            "Class hierarchy contains multiple fields annotated @TtlMillis",
                            childElement);
                }
                mSpecialFieldNames.put(SpecialField.TTL_MILLIS, fieldName);
            } else if (annotationFq.equals(
                    MetadataPropertyAnnotation.SCORE.getClassName().canonicalName())) {
                if (mSpecialFieldNames.containsKey(SpecialField.SCORE)) {
                    throw new ProcessingException(
                            "Class hierarchy contains multiple fields annotated @Score",
                            childElement);
                }
                mSpecialFieldNames.put(SpecialField.SCORE, fieldName);
            } else {
                PropertyClass propertyClass = getPropertyClass(annotationFq);
                if (propertyClass != null) {
                    // A property must either:
                    //   1. be unique
                    //   2. override a property from the Java parent while maintaining the same
                    //      AppSearch property name
                    checkFieldTypeForPropertyAnnotation(childElement, propertyClass);
                    // It's assumed that parent types, in the context of Java's type system,
                    // are always visited before child types, so existingProperty must come
                    // from the parent type. To make this assumption valid, the result
                    // returned by generateClassHierarchy must put parent types before child
                    // types.
                    Element existingProperty = mPropertyElements.get(fieldName);
                    if (existingProperty != null) {
                        if (!mTypeUtil.isSameType(
                                existingProperty.asType(), childElement.asType())) {
                            throw new ProcessingException(
                                    "Cannot override a property with a different type",
                                    childElement);
                        }
                        if (!getPropertyName(existingProperty).equals(getPropertyName(
                                childElement))) {
                            throw new ProcessingException(
                                    "Cannot override a property with a different name",
                                    childElement);
                        }
                    }
                    mPropertyElements.put(fieldName, childElement);
                }
            }

            mAllAppSearchElements.put(fieldName, childElement);
        }
    }

    /**
     * Scans all the fields of the class, as well as superclasses annotated with @Document,
     * to get AppSearch fields such as id
     *
     * @param element the class to scan
     */
    private void scanFields(@NonNull TypeElement element) throws ProcessingException {
        AnnotationMirror documentAnnotation = getDocumentAnnotation(element);
        if (documentAnnotation != null) {
            Map<String, Object> params = mHelper.getAnnotationParams(documentAnnotation);
            Object parents = params.get("parent");
            if (parents instanceof List) {
                for (Object parent : (List<?>) parents) {
                    String parentClassName = parent.toString();
                    parentClassName = parentClassName.substring(0,
                            parentClassName.length() - CLASS_SUFFIX.length());
                    mParentTypes.add(mElementUtil.getTypeElement(parentClassName));
                }
            }
            if (!mParentTypes.isEmpty() && params.get("name").toString().isEmpty()) {
                throw new ProcessingException(
                        "All @Document classes with a parent must explicitly provide a name",
                        mClass);
            }
        }

        List<TypeElement> hierarchy = generateClassHierarchy(element);

        for (TypeElement clazz : hierarchy) {
            List<? extends Element> enclosedElements = clazz.getEnclosedElements();
            for (Element childElement : enclosedElements) {
                scanAnnotatedField(childElement);
            }
        }

        // Every document must always have a namespace
        if (!mSpecialFieldNames.containsKey(SpecialField.NAMESPACE)) {
            throw new ProcessingException(
                    "All @Document classes must have exactly one field annotated with @Namespace",
                    mClass);
        }

        // Every document must always have an ID
        if (!mSpecialFieldNames.containsKey(SpecialField.ID)) {
            throw new ProcessingException(
                    "All @Document classes must have exactly one field annotated with @Id",
                    mClass);
        }

        mSchemaName = computeSchemaName(hierarchy);

        for (Element appSearchField : mAllAppSearchElements.values()) {
            chooseWriteKind(appSearchField);
        }
    }

    /**
     * Checks whether property's data type matches the {@code androidx.appsearch.annotation
     * .Document} property annotation's requirement.
     *
     * @throws ProcessingException if data type doesn't match property annotation's requirement.
     */
    void checkFieldTypeForPropertyAnnotation(@NonNull Element property,
            PropertyClass propertyClass) throws ProcessingException {
        switch (propertyClass) {
            case BOOLEAN_PROPERTY_CLASS:
                if (mHelper.isFieldOfExactType(property, mHelper.mBooleanBoxType,
                        mHelper.mBooleanPrimitiveType)) {
                    return;
                }
                break;
            case BYTES_PROPERTY_CLASS:
                if (mHelper.isFieldOfExactType(property, mHelper.mByteBoxType,
                        mHelper.mBytePrimitiveType, mHelper.mByteBoxArrayType,
                        mHelper.mBytePrimitiveArrayType)) {
                    return;
                }
                break;
            case DOCUMENT_PROPERTY_CLASS:
                if (mHelper.isFieldOfDocumentType(property)) {
                    return;
                }
                break;
            case DOUBLE_PROPERTY_CLASS:
                if (mHelper.isFieldOfExactType(property, mHelper.mDoubleBoxType,
                        mHelper.mDoublePrimitiveType, mHelper.mFloatBoxType,
                        mHelper.mFloatPrimitiveType)) {
                    return;
                }
                break;
            case LONG_PROPERTY_CLASS:
                if (mHelper.isFieldOfExactType(property, mHelper.mIntegerBoxType,
                        mHelper.mIntPrimitiveType, mHelper.mLongBoxType,
                        mHelper.mLongPrimitiveType)) {
                    return;
                }
                break;
            case STRING_PROPERTY_CLASS:
                if (mHelper.isFieldOfExactType(property, mHelper.mStringType)) {
                    return;
                }
                break;
            default:
                // do nothing
        }
        throw new ProcessingException(
                "Property Annotation " + propertyClass.getClassFullPath() + " doesn't accept the "
                        + "data type of property field " + property.getSimpleName(), property);
    }

    /**
     * Returns the {@link PropertyClass} with {@code annotationFq} as full class path, and {@code
     * null} if failed to find such a {@link PropertyClass}.
     */
    @Nullable
    private PropertyClass getPropertyClass(@Nullable String annotationFq) {
        for (PropertyClass propertyClass : PropertyClass.values()) {
            if (propertyClass.isPropertyClass(annotationFq)) {
                return propertyClass;
            }
        }
        return null;
    }

    /**
     * Chooses how to write a given field.
     *
     * <p>The writing strategy can be one of: visible mutable field, or visible setter, or visible
     * creation method accepting at minimum all fields that aren't mutable and have no visible
     * setter.
     */
    private void chooseWriteKind(@NonNull Element field) {
        // TODO(b/300114568): Carve out better distinction b/w the different write strategies
        Set<Modifier> modifiers = field.getModifiers();
        // Choose set access
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL)
                || modifiers.contains(Modifier.STATIC) || field.getKind() == ElementKind.METHOD
                || mBuilderClass != null) {
            // Try to find a setter. If we can't find one, mark the WriteKind as {@code
            // CREATION_METHOD}. We don't know if this is true yet, the creation methods will be
            // inspected in a subsequent pass.
            try {
                findSetter(field);
                mWriteKinds.put(field, WriteKind.SETTER);
            } catch (ProcessingException e) {
                // We'll look for a creation method, so we may still be able to set this field,
                // but it's more likely the developer configured the setter incorrectly. Keep
                // the exception around to include it in the report if no creation method is found.
                mWriteWhyCreationMethod.put(field, e);
                mWriteKinds.put(field, WriteKind.CREATION_METHOD);
            }
        } else {
            mWriteKinds.put(field, WriteKind.FIELD);
        }
    }

    private void chooseCreationMethod(Set<ExecutableElement> creationMethods)
            throws ProcessingException {
        // Maps field name to Element.
        // If this is changed to a HashSet, we might report errors to the developer in a different
        // order about why a field was written via creation method.
        Map<String, Element> creationMethodWrittenFields = new LinkedHashMap<>();
        for (Map.Entry<Element, WriteKind> it : mWriteKinds.entrySet()) {
            if (it.getValue() == WriteKind.CREATION_METHOD) {
                String name = it.getKey().getSimpleName().toString();
                creationMethodWrittenFields.put(name, it.getKey());
            }
        }

        // Maps normalized field name to real field name.
        Map<String, String> normalizedToRawFieldName = new HashMap<>();
        for (Element field : mAllAppSearchElements.values()) {
            normalizedToRawFieldName.put(getNormalizedElementName(field),
                    field.getSimpleName().toString());
        }

        Map<ExecutableElement, String> whyNotCreationMethod = new HashMap<>();
        creationMethodSearch:
        for (ExecutableElement method : creationMethods) {
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                whyNotCreationMethod.put(method, "Creation method is private");
                continue creationMethodSearch;
            }
            // The field name of each field that goes into the creation method, in the order they
            // are declared in the creation method signature.
            List<String> creationMethodParamFields = new ArrayList<>();
            Set<String> remainingFields = new HashSet<>(creationMethodWrittenFields.keySet());
            for (VariableElement parameter : method.getParameters()) {
                String paramName = parameter.getSimpleName().toString();
                String fieldName = normalizedToRawFieldName.get(paramName);
                if (fieldName == null) {
                    whyNotCreationMethod.put(
                            method,
                            "Parameter \"" + paramName + "\" is not an AppSearch parameter; don't "
                                    + "know how to supply it.");
                    continue creationMethodSearch;
                }
                remainingFields.remove(fieldName);
                creationMethodParamFields.add(fieldName);
            }
            if (!remainingFields.isEmpty()) {
                whyNotCreationMethod.put(
                        method,
                        "This method doesn't have parameters for the following fields: "
                                + remainingFields);
                continue creationMethodSearch;
            }

            // If the field is set in the constructor, choose creation method for the write kind
            for (String param : creationMethodParamFields) {
                for (Element appSearchField : mAllAppSearchElements.values()) {
                    if (appSearchField.getSimpleName().toString().equals(param)) {
                        mWriteKinds.put(appSearchField, WriteKind.CREATION_METHOD);
                        break;
                    }
                }
            }

            // Found one!
            mChosenCreationMethod = method;
            mChosenCreationMethodParams = creationMethodParamFields;
            return;
        }

        // If we got here, we couldn't find any creation methods.
        ProcessingException e =
                new ProcessingException(
                        "Failed to find any suitable creation methods to build class \""
                                + mClass.getQualifiedName()
                                + "\". See warnings for details.",
                        mClass);

        // Inform the developer why we started looking for creation methods in the first place.
        for (Element field : creationMethodWrittenFields.values()) {
            ProcessingException warning = mWriteWhyCreationMethod.get(field);
            if (warning != null) {
                e.addWarning(warning);
            }
        }

        // Inform the developer about why each creation method we considered was rejected.
        for (Map.Entry<ExecutableElement, String> it : whyNotCreationMethod.entrySet()) {
            ProcessingException warning = new ProcessingException(
                    "Cannot use this creation method to construct the class: " + it.getValue(),
                    it.getKey());
            e.addWarning(warning);
        }

        throw e;
    }

    /**
     * Finds setter function for a private field, or for a property defined by a annotated getter
     * method.
     */
    private void findSetter(@NonNull Element element) throws ProcessingException {
        String elementName = element.getSimpleName().toString();
        // We can't report setter failure until we've searched the creation methods, so this
        // message is anticipatory and should be buffered by the caller.
        String error;
        if (mBuilderClass != null) {
            error = "Element cannot be written directly because a builder producer is provided";
        } else if (element.getKind() == ElementKind.METHOD) {
            error = "Element cannot be written directly because it is an annotated getter";
        } else {
            error = "Field cannot be written directly because it is private, final, or static";
        }
        error += ", and we failed to find a suitable setter for \"" + elementName + "\". "
                + "Trying to find a suitable creation method.";
        ProcessingException e = new ProcessingException(error,
                mAllAppSearchElements.get(elementName));

        // When using the builder pattern, setters can only come from the builder.
        Set<ExecutableElement> methods;
        if (mBuilderClass != null) {
            methods = mAllBuilderMethods;
        } else {
            methods = mAllMethods;
        }
        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();
            String normalizedElementName = getNormalizedElementName(element);
            if (methodName.equals(normalizedElementName)
                    || methodName.equals("set"
                    + normalizedElementName.substring(0, 1).toUpperCase()
                    + normalizedElementName.substring(1))) {
                if (method.getModifiers().contains(Modifier.PRIVATE)) {
                    e.addWarning(new ProcessingException(
                            "Setter cannot be used: private visibility", method));
                    continue;
                }
                if (method.getParameters().size() != 1) {
                    e.addWarning(new ProcessingException(
                            "Setter cannot be used: takes " + method.getParameters().size()
                                    + " parameters instead of 1",
                            method));
                    continue;
                }
                // Found one!
                mSetterMethods.put(elementName, method);
                return;
            }
        }

        // Broke out of the loop without finding anything.
        throw e;
    }

    /**
     * Produces the canonical name of a field element.
     *
     * @see #getNormalizedElementName(Element)
     */
    private String getNormalizedFieldElementName(Element fieldElement) {
        String fieldName = fieldElement.getSimpleName().toString();

        if (fieldName.length() < 2) {
            return fieldName;
        }

        // Handle convention of having field names start with m
        // (e.g. String mName; public String getName())
        if (fieldName.charAt(0) == 'm' && Character.isUpperCase(fieldName.charAt(1))) {
            return fieldName.substring(1, 2).toLowerCase() + fieldName.substring(2);
        }

        // Handle convention of having field names start with _
        // (e.g. String _name; public String getName())
        if (fieldName.charAt(0) == '_'
                && fieldName.charAt(1) != '_'
                && Character.isLowerCase(fieldName.charAt(1))) {
            return fieldName.substring(1);
        }

        // Handle convention of having field names end with _
        // (e.g. String name_; public String getName())
        if (fieldName.charAt(fieldName.length() - 1) == '_'
                && fieldName.charAt(fieldName.length() - 2) != '_') {
            return fieldName.substring(0, fieldName.length() - 1);
        }

        return fieldName;
    }

    /**
     * Produces the canonical name of a method element.
     *
     * @see #getNormalizedElementName(Element)
     */
    private String getNormalizedMethodElementName(Element methodElement) {
        String methodName = methodElement.getSimpleName().toString();

        // If this property is defined by an annotated getter, then we can remove the prefix
        // "get" or "is" if possible.
        if (methodName.startsWith("get") && methodName.length() > 3) {
            methodName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (mHelper.isFieldOfBooleanType(methodElement) && methodName.startsWith("is")
                && methodName.length() > 2) {
            // "is" is a valid getter prefix for boolean property.
            methodName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        }
        // Return early because the rest normalization procedures do not apply to getters.
        return methodName;
    }

    /**
     * Produces the canonical name of a element (which is used as the default property name as
     * well as to find accessors) by removing prefixes and suffixes of common conventions.
     */
    private String getNormalizedElementName(Element property) {
        if (property.getKind() == ElementKind.METHOD) {
            return getNormalizedMethodElementName(property);
        }
        return getNormalizedFieldElementName(property);
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
