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

import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.getDocumentAnnotation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.compiler.IntrospectionHelper.PropertyClass;

import java.util.ArrayList;
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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

/**
 * Processes @Document annotations.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DocumentModel {

    /** Enumeration of fields that must be handled specially (i.e. are not properties) */
    enum SpecialField {ID, NAMESPACE, CREATION_TIMESTAMP_MILLIS, TTL_MILLIS, SCORE}

    /** Determines how the annotation processor has decided to read the value of a field. */
    enum ReadKind {FIELD, GETTER}

    /** Determines how the annotation processor has decided to write the value of a field. */
    enum WriteKind {FIELD, SETTER, CREATION_METHOD}

    private final IntrospectionHelper mHelper;
    private final TypeElement mClass;
    private final AnnotationMirror mDocumentAnnotation;
    // Warning: if you change this to a HashSet, we may choose different getters or setters from
    // run to run, causing the generated code to bounce.
    private final Set<ExecutableElement> mAllMethods = new LinkedHashSet<>();
    private final boolean mIsAutoValueDocument;
    // Key: Name of the field which is accessed through the getter method.
    // Value: ExecutableElement of the getter method.
    private final Map<String, ExecutableElement> mGetterMethods = new HashMap<>();
    // Key: Name of the field whose value is set through the setter method.
    // Value: ExecutableElement of the setter method.
    private final Map<String, ExecutableElement> mSetterMethods = new HashMap<>();
    // Warning: if you change this to a HashMap, we may assign fields in a different order from run
    // to run, causing the generated code to bounce.
    private final Map<String, VariableElement> mAllAppSearchFields = new LinkedHashMap<>();
    // Warning: if you change this to a HashMap, we may assign fields in a different order from run
    // to run, causing the generated code to bounce.
    private final Map<String, VariableElement> mPropertyFields = new LinkedHashMap<>();
    private final Map<SpecialField, String> mSpecialFieldNames = new EnumMap<>(SpecialField.class);
    private final Map<VariableElement, ReadKind> mReadKinds = new HashMap<>();
    private final Map<VariableElement, WriteKind> mWriteKinds = new HashMap<>();
    // Contains the reason why that field couldn't be written either by field or by setter.
    private final Map<VariableElement, ProcessingException> mWriteWhyCreationMethod =
            new HashMap<>();
    private ExecutableElement mChosenCreationMethod = null;
    private List<String> mChosenCreationMethodParams = null;

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
        mDocumentAnnotation = getDocumentAnnotation(mClass);

        if (generatedAutoValueElement != null) {
            mIsAutoValueDocument = true;
            // Scan factory methods from AutoValue class.
            Set<ExecutableElement> creationMethods = new LinkedHashSet<>();
            for (Element child : ElementFilter.methodsIn(mClass.getEnclosedElements())) {
                ExecutableElement method = (ExecutableElement) child;
                if (isFactoryMethod(method)) {
                    creationMethods.add(method);
                }
            }
            mAllMethods.addAll(
                    ElementFilter.methodsIn(generatedAutoValueElement.getEnclosedElements()));

            scanFields(generatedAutoValueElement);
            scanCreationMethods(creationMethods);
        } else {
            mIsAutoValueDocument = false;
            // Scan methods and constructors. We will need this info when processing fields to
            // make sure the fields can be get and set.
            Set<ExecutableElement> creationMethods = new LinkedHashSet<>();
            for (Element child : mClass.getEnclosedElements()) {
                if (child.getKind() == ElementKind.CONSTRUCTOR) {
                    creationMethods.add((ExecutableElement) child);
                } else if (child.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) child;
                    mAllMethods.add(method);
                    if (isFactoryMethod(method)) {
                        creationMethods.add(method);
                    }
                }
            }

            scanFields(mClass);
            scanCreationMethods(creationMethods);
        }
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

    @NonNull
    public String getSchemaName() {
        Map<String, Object> params =
                mHelper.getAnnotationParams(mDocumentAnnotation);
        String name = params.get("name").toString();
        if (name.isEmpty()) {
            return mClass.getSimpleName().toString();
        }
        return name;
    }

    @NonNull
    public Map<String, VariableElement> getAllFields() {
        return Collections.unmodifiableMap(mAllAppSearchFields);
    }

    @NonNull
    public Map<String, VariableElement> getPropertyFields() {
        return Collections.unmodifiableMap(mPropertyFields);
    }

    @Nullable
    public String getSpecialFieldName(SpecialField field) {
        return mSpecialFieldNames.get(field);
    }

    @Nullable
    public ReadKind getFieldReadKind(String fieldName) {
        VariableElement element = mAllAppSearchFields.get(fieldName);
        return mReadKinds.get(element);
    }

    @Nullable
    public WriteKind getFieldWriteKind(String fieldName) {
        VariableElement element = mAllAppSearchFields.get(fieldName);
        return mWriteKinds.get(element);
    }

    @Nullable
    public ExecutableElement getGetterForField(String fieldName) {
        return mGetterMethods.get(fieldName);
    }

    @Nullable
    public ExecutableElement getSetterForField(String fieldName) {
        return mSetterMethods.get(fieldName);
    }

    /**
     * Finds the AppSearch name for the given property.
     *
     * <p>This is usually the name of the field in Java, but may be changed if the developer
     * specifies a different 'name' parameter in the annotation.
     */
    @NonNull
    public String getPropertyName(@NonNull VariableElement property) throws ProcessingException {
        AnnotationMirror annotation = getPropertyAnnotation(property);
        Map<String, Object> params = mHelper.getAnnotationParams(annotation);
        String propertyName = params.get("name").toString();
        if (propertyName.isEmpty()) {
            propertyName = getNormalizedFieldName(property.getSimpleName().toString());
        }
        return propertyName;
    }

    /**
     * Returns the first found AppSearch property annotation element from the input element's
     * annotations.
     *
     * @throws ProcessingException if no AppSearch property annotation is found.
     */
    @NonNull
    public AnnotationMirror getPropertyAnnotation(@NonNull Element element)
            throws ProcessingException {
        Objects.requireNonNull(element);
        if (mIsAutoValueDocument) {
            element = getGetterForField(element.getSimpleName().toString());
        }
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

    private boolean isFactoryMethod(ExecutableElement method) {
        Set<Modifier> methodModifiers = method.getModifiers();
        return methodModifiers.contains(Modifier.STATIC)
                && !methodModifiers.contains(Modifier.PRIVATE)
                && method.getReturnType() == mClass.asType();
    }

    private void scanFields(TypeElement element) throws ProcessingException {
        Element namespaceField = null;
        Element idField = null;
        Element creationTimestampField = null;
        Element ttlField = null;
        Element scoreField = null;
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (int i = 0; i < enclosedElements.size(); i++) {
            Element childElement = enclosedElements.get(i);
            if (mIsAutoValueDocument && childElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            String fieldName = childElement.getSimpleName().toString();
            for (AnnotationMirror annotation : childElement.getAnnotationMirrors()) {
                String annotationFq = annotation.getAnnotationType().toString();
                if (!annotationFq.startsWith(DOCUMENT_ANNOTATION_CLASS)) {
                    continue;
                }
                VariableElement child;
                if (mIsAutoValueDocument) {
                    child = findFieldForFunctionWithSameName(enclosedElements, childElement);
                } else {
                    if (childElement.getKind() == ElementKind.METHOD) {
                        throw new ProcessingException(
                                "AppSearch annotation is not applicable to methods for "
                                        + "Non-AutoValue class",
                                childElement);
                    } else {
                        child = (VariableElement) childElement;
                    }
                }
                switch (annotationFq) {
                    case IntrospectionHelper.ID_CLASS:
                        if (idField != null) {
                            throw new ProcessingException(
                                    "Class contains multiple fields annotated @Id", child);
                        }
                        idField = child;
                        mSpecialFieldNames.put(SpecialField.ID, fieldName);
                        break;
                    case IntrospectionHelper.NAMESPACE_CLASS:
                        if (namespaceField != null) {
                            throw new ProcessingException(
                                    "Class contains multiple fields annotated @Namespace",
                                    child);
                        }
                        namespaceField = child;
                        mSpecialFieldNames.put(SpecialField.NAMESPACE, fieldName);
                        break;
                    case IntrospectionHelper.CREATION_TIMESTAMP_MILLIS_CLASS:
                        if (creationTimestampField != null) {
                            throw new ProcessingException(
                                    "Class contains multiple fields annotated "
                                            + "@CreationTimestampMillis",
                                    child);
                        }
                        creationTimestampField = child;
                        mSpecialFieldNames.put(SpecialField.CREATION_TIMESTAMP_MILLIS, fieldName);
                        break;
                    case IntrospectionHelper.TTL_MILLIS_CLASS:
                        if (ttlField != null) {
                            throw new ProcessingException(
                                    "Class contains multiple fields annotated @TtlMillis",
                                    child);
                        }
                        ttlField = child;
                        mSpecialFieldNames.put(SpecialField.TTL_MILLIS, fieldName);
                        break;
                    case IntrospectionHelper.SCORE_CLASS:
                        if (scoreField != null) {
                            throw new ProcessingException(
                                    "Class contains multiple fields annotated @Score", child);
                        }
                        scoreField = child;
                        mSpecialFieldNames.put(SpecialField.SCORE, fieldName);
                        break;
                    default:
                        PropertyClass propertyClass = getPropertyClass(annotationFq);
                        if (propertyClass != null) {
                            checkFieldTypeForPropertyAnnotation(child, propertyClass);
                            mPropertyFields.put(fieldName, child);
                        }
                }
                mAllAppSearchFields.put(fieldName, child);
            }
        }

        // Every document must always have a namespace
        if (namespaceField == null) {
            throw new ProcessingException(
                    "All @Document classes must have exactly one field annotated with @Namespace",
                    mClass);
        }

        // Every document must always have an ID
        if (idField == null) {
            throw new ProcessingException(
                    "All @Document classes must have exactly one field annotated with @Id",
                    mClass);
        }

        for (VariableElement appSearchField : mAllAppSearchFields.values()) {
            chooseAccessKinds(appSearchField);
        }
    }

    @NonNull
    private VariableElement findFieldForFunctionWithSameName(
            @NonNull List<? extends Element> elements,
            @NonNull Element functionElement) throws ProcessingException {
        String fieldName = functionElement.getSimpleName().toString();
        for (VariableElement field : ElementFilter.fieldsIn(elements)) {
            if (fieldName.equals(field.getSimpleName().toString())) {
                return field;
            }
        }
        throw new ProcessingException(
                "Cannot find the corresponding field for the annotated function",
                functionElement);
    }

    /**
     * Checks whether property's data type matches the {@code androidx.appsearch.annotation
     * .Document} property annotation's requirement.
     *
     * @throws ProcessingException if data type doesn't match property annotation's requirement.
     */
    void checkFieldTypeForPropertyAnnotation(@NonNull VariableElement property,
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
     * Chooses how to access the given field for read and write, subject to our requirements for all
     * AppSearch-managed class fields:
     *
     * <p>For read: visible field, or visible getter
     *
     * <p>For write: visible mutable field, or visible setter, or visible creation method
     * accepting at minimum all fields that aren't mutable and have no visible setter.
     *
     * @throws ProcessingException if no access type is possible for the given field
     */
    private void chooseAccessKinds(@NonNull VariableElement field)
            throws ProcessingException {
        // Choose get access
        String fieldName = field.getSimpleName().toString();
        Set<Modifier> modifiers = field.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            findGetter(fieldName);
            mReadKinds.put(field, ReadKind.GETTER);
        } else {
            mReadKinds.put(field, ReadKind.FIELD);
        }

        // Choose set access
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL)
                || modifiers.contains(Modifier.STATIC)) {
            // Try to find a setter. If we can't find one, mark the WriteKind as {@code
            // CREATION_METHOD}. We don't know if this is true yet, the creation methods will be
            // inspected in a subsequent pass.
            try {
                findSetter(fieldName);
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

    private void scanCreationMethods(Set<ExecutableElement> creationMethods)
            throws ProcessingException {
        // Maps field name to Element.
        // If this is changed to a HashSet, we might report errors to the developer in a different
        // order about why a field was written via creation method.
        Map<String, VariableElement> creationMethodWrittenFields = new LinkedHashMap<>();
        for (Map.Entry<VariableElement, WriteKind> it : mWriteKinds.entrySet()) {
            if (it.getValue() == WriteKind.CREATION_METHOD) {
                String name = it.getKey().getSimpleName().toString();
                creationMethodWrittenFields.put(name, it.getKey());
            }
        }

        // Maps normalized field name to real field name.
        Map<String, String> normalizedToRawFieldName = new HashMap<>();
        for (String fieldName : mAllAppSearchFields.keySet()) {
            normalizedToRawFieldName.put(getNormalizedFieldName(fieldName), fieldName);
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
            // Found one!
            mChosenCreationMethod = method;
            mChosenCreationMethodParams = creationMethodParamFields;
            return;
        }

        // If we got here, we couldn't find any creation methods.
        ProcessingException e =
                new ProcessingException(
                        "Failed to find any suitable creation methods to build this class. See "
                                + "warnings for details.", mClass);

        // Inform the developer why we started looking for creation methods in the first place.
        for (VariableElement field : creationMethodWrittenFields.values()) {
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

    /** Finds getter function for a private field. */
    private void findGetter(@NonNull String fieldName) throws ProcessingException {
        ProcessingException e = new ProcessingException(
                "Field cannot be read: it is private and we failed to find a suitable getter "
                        + "for field \"" + fieldName + "\"",
                mAllAppSearchFields.get(fieldName));

        for (ExecutableElement method : mAllMethods) {
            String methodName = method.getSimpleName().toString();
            String normalizedFieldName = getNormalizedFieldName(fieldName);
            if (methodName.equals(normalizedFieldName)
                    || methodName.equals("get"
                    + normalizedFieldName.substring(0, 1).toUpperCase()
                    + normalizedFieldName.substring(1))) {
                if (method.getModifiers().contains(Modifier.PRIVATE)) {
                    e.addWarning(new ProcessingException(
                            "Getter cannot be used: private visibility", method));
                    continue;
                }
                if (!method.getParameters().isEmpty()) {
                    e.addWarning(new ProcessingException(
                            "Getter cannot be used: should take no parameters", method));
                    continue;
                }
                // Found one!
                mGetterMethods.put(fieldName, method);
                return;
            }
        }

        // Broke out of the loop without finding anything.
        throw e;
    }

    /** Finds setter function for a private field. */
    private void findSetter(@NonNull String fieldName) throws ProcessingException {
        // We can't report setter failure until we've searched the creation methods, so this
        // message is anticipatory and should be buffered by the caller.
        ProcessingException e = new ProcessingException(
                "Field cannot be written directly or via setter because it is private, final, or "
                        + "static, and we failed to find a suitable setter for field \""
                        + fieldName
                        + "\". Trying to find a suitable creation method.",
                mAllAppSearchFields.get(fieldName));

        for (ExecutableElement method : mAllMethods) {
            String methodName = method.getSimpleName().toString();
            String normalizedFieldName = getNormalizedFieldName(fieldName);
            if (methodName.equals(normalizedFieldName)
                    || methodName.equals("set"
                    + normalizedFieldName.substring(0, 1).toUpperCase()
                    + normalizedFieldName.substring(1))) {
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
                mSetterMethods.put(fieldName, method);
                return;
            }
        }

        // Broke out of the loop without finding anything.
        throw e;
    }

    /**
     * Produces the canonical name of a field (which is used as the default property name as well as
     * to find accessors) by removing prefixes and suffixes of common conventions.
     */
    private String getNormalizedFieldName(String fieldName) {
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
}
