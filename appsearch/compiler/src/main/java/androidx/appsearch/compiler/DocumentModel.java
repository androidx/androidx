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
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Processes @Document annotations.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DocumentModel {

    /** Enumeration of fields that must be handled specially (i.e. are not properties) */
    enum SpecialField { ID, NAMESPACE, CREATION_TIMESTAMP_MILLIS, TTL_MILLIS, SCORE }
    /** Determines how the annotation processor has decided to read the value of a field. */
    enum ReadKind { FIELD, GETTER }
    /** Determines how the annotation processor has decided to write the value of a field. */
    enum WriteKind { FIELD, SETTER, CONSTRUCTOR }

    private final IntrospectionHelper mHelper;
    private final TypeElement mClass;
    private final AnnotationMirror mDocumentAnnotation;
    private final Set<ExecutableElement> mConstructors = new LinkedHashSet<>();
    private final Set<ExecutableElement> mMethods = new LinkedHashSet<>();
    // Key: Name of the field which is accessed through the getter method.
    // Value: ExecutableElement of the getter method.
    private final Map<String, ExecutableElement> mGetterMethods = new HashMap<>();
    // Key: Name of the field whose value is set through the setter method.
    // Value: ExecutableElement of the setter method.
    private final Map<String, ExecutableElement> mSetterMethods = new HashMap<>();
    private final Map<String, VariableElement> mAllAppSearchFields = new LinkedHashMap<>();
    private final Map<String, VariableElement> mPropertyFields = new LinkedHashMap<>();
    private final Map<SpecialField, String> mSpecialFieldNames = new EnumMap<>(SpecialField.class);
    private final Map<VariableElement, ReadKind> mReadKinds = new HashMap<>();
    private final Map<VariableElement, WriteKind> mWriteKinds = new HashMap<>();
    private final Map<VariableElement, ProcessingException> mWriteWhyConstructor = new HashMap<>();
    private List<String> mChosenConstructorParams = null;

    private DocumentModel(
            @NonNull ProcessingEnvironment env,
            @NonNull TypeElement clazz)
            throws ProcessingException {
        mHelper = new IntrospectionHelper(env);
        mClass = clazz;
        if (mClass.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ProcessingException("@Document annotated class is private", mClass);
        }

        mDocumentAnnotation = getDocumentAnnotation(mClass);

        // Scan methods and constructors. AppSearch doesn't define any annotations that apply to
        // these, but we will need this info when processing fields to make sure the fields can
        // be get and set.
        for (Element child : mClass.getEnclosedElements()) {
            if (child.getKind() == ElementKind.CONSTRUCTOR) {
                mConstructors.add((ExecutableElement) child);
            } else if (child.getKind() == ElementKind.METHOD) {
                mMethods.add((ExecutableElement) child);
            }
        }

        scanFields();
        scanConstructors();
    }

    /**
     * Tries to create an {@link DocumentModel} from the given {@link Element}.
     *
     * @throws ProcessingException if the @{@code Document}-annotated class is invalid.
     */
    public static DocumentModel create(
            @NonNull ProcessingEnvironment env, @NonNull TypeElement clazz)
            throws ProcessingException {
        return new DocumentModel(env, clazz);
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
        AnnotationMirror annotation = IntrospectionHelper.getPropertyAnnotation(property);
        Map<String, Object> params = mHelper.getAnnotationParams(annotation);
        String propertyName = params.get("name").toString();
        if (propertyName.isEmpty()) {
            propertyName = property.getSimpleName().toString();
        }
        return propertyName;
    }

    @NonNull
    public List<String> getChosenConstructorParams() {
        return Collections.unmodifiableList(mChosenConstructorParams);
    }

    private void scanFields() throws ProcessingException {
        Element namespaceField = null;
        Element idField = null;
        Element creationTimestampField = null;
        Element ttlField = null;
        Element scoreField = null;
        for (Element childElement : mClass.getEnclosedElements()) {
            if (!childElement.getKind().isField()) continue;
            VariableElement child = (VariableElement) childElement;
            String fieldName = child.getSimpleName().toString();
            for (AnnotationMirror annotation : child.getAnnotationMirrors()) {
                String annotationFq = annotation.getAnnotationType().toString();
                boolean isAppSearchField = true;
                if (IntrospectionHelper.ID_CLASS.equals(annotationFq)) {
                    if (idField != null) {
                        throw new ProcessingException(
                                "Class contains multiple fields annotated @Id", child);
                    }
                    idField = child;
                    mSpecialFieldNames.put(SpecialField.ID, fieldName);

                } else if (IntrospectionHelper.NAMESPACE_CLASS.equals(annotationFq)) {
                    if (namespaceField != null) {
                        throw new ProcessingException(
                                "Class contains multiple fields annotated @Namespace", child);
                    }
                    namespaceField = child;
                    mSpecialFieldNames.put(SpecialField.NAMESPACE, fieldName);

                } else if (
                        IntrospectionHelper.CREATION_TIMESTAMP_MILLIS_CLASS.equals(annotationFq)) {
                    if (creationTimestampField != null) {
                        throw new ProcessingException(
                                "Class contains multiple fields annotated @CreationTimestampMillis",
                                child);
                    }
                    creationTimestampField = child;
                    mSpecialFieldNames.put(SpecialField.CREATION_TIMESTAMP_MILLIS, fieldName);

                } else if (IntrospectionHelper.TTL_MILLIS_CLASS.equals(annotationFq)) {
                    if (ttlField != null) {
                        throw new ProcessingException(
                                "Class contains multiple fields annotated @TtlMillis", child);
                    }
                    ttlField = child;
                    mSpecialFieldNames.put(SpecialField.TTL_MILLIS, fieldName);

                } else if (IntrospectionHelper.SCORE_CLASS.equals(annotationFq)) {
                    if (scoreField != null) {
                        throw new ProcessingException(
                                "Class contains multiple fields annotated @Score", child);
                    }
                    scoreField = child;
                    mSpecialFieldNames.put(SpecialField.SCORE, fieldName);

                } else {
                    PropertyClass propertyClass = getPropertyClass(annotationFq);
                    if (propertyClass != null) {
                        checkFieldTypeForPropertyAnnotation(child, propertyClass);
                        mPropertyFields.put(fieldName, child);
                    } else {
                        isAppSearchField = false;
                    }
                }

                if (isAppSearchField) {
                    mAllAppSearchFields.put(fieldName, child);
                }
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
            case INT64_PROPERTY_CLASS:
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
    private @Nullable PropertyClass getPropertyClass(@Nullable String annotationFq) {
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
     * <p>For write: visible mutable field, or visible setter, or visible constructor accepting at
     * minimum all fields that aren't mutable and have no visible setter.
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
            // Try to find a setter. If we can't find one, mark the WriteKind as CONSTRUCTOR. We
            // don't know if this is true yet, the constructors will be inspected in a subsequent
            // pass.
            try {
                findSetter(fieldName);
                mWriteKinds.put(field, WriteKind.SETTER);
            } catch (ProcessingException e) {
                // We'll look for a constructor, so we may still be able to set this field,
                // but it's more likely the developer configured the setter incorrectly. Keep
                // the exception around to include it in the report if no constructor is found.
                mWriteWhyConstructor.put(field, e);
                mWriteKinds.put(field, WriteKind.CONSTRUCTOR);
            }
        } else {
            mWriteKinds.put(field, WriteKind.FIELD);
        }
    }

    private void scanConstructors() throws ProcessingException {
        // Maps name to Element
        Map<String, VariableElement> constructorWrittenFields = new LinkedHashMap<>();
        for (Map.Entry<VariableElement, WriteKind> it : mWriteKinds.entrySet()) {
            if (it.getValue() == WriteKind.CONSTRUCTOR) {
                String name = it.getKey().getSimpleName().toString();
                constructorWrittenFields.put(name, it.getKey());
            }
        }

        Map<ExecutableElement, String> whyNotConstructor = new HashMap<>();
        constructorSearch:
        for (ExecutableElement constructor : mConstructors) {
            if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
                whyNotConstructor.put(constructor, "Constructor is private");
                continue constructorSearch;
            }
            List<String> constructorParamFields = new ArrayList<>();
            Set<String> remainingFields = new HashSet<>(constructorWrittenFields.keySet());
            for (VariableElement parameter : constructor.getParameters()) {
                String name = parameter.getSimpleName().toString();
                if (!mAllAppSearchFields.containsKey(name)) {
                    whyNotConstructor.put(
                            constructor,
                            "Parameter \"" + name + "\" is not an AppSearch parameter; don't know "
                                    + "how to supply it.");
                    continue constructorSearch;
                }
                remainingFields.remove(name);
                constructorParamFields.add(name);
            }
            if (!remainingFields.isEmpty()) {
                whyNotConstructor.put(
                        constructor,
                        "This constructor doesn't have parameters for the following fields: "
                                + remainingFields);
                continue constructorSearch;
            }
            // Found one!
            mChosenConstructorParams = constructorParamFields;
            return;
        }

        // If we got here, we couldn't find any constructors.
        ProcessingException e =
                new ProcessingException(
                        "Failed to find any suitable constructors to build this class. See "
                                + "warnings for details.", mClass);

        // Inform the developer why we started looking for constructors in the first place
        for (VariableElement field : constructorWrittenFields.values()) {
            ProcessingException warning = mWriteWhyConstructor.get(field);
            if (warning != null) {
                e.addWarning(warning);
            }
        }

        // Inform the developer about why each constructor we considered was rejected
        for (Map.Entry<ExecutableElement, String> it : whyNotConstructor.entrySet()) {
            ProcessingException warning = new ProcessingException(
                    "Cannot use this constructor to construct the class: " + it.getValue(),
                    it.getKey());
            e.addWarning(warning);
        }

        throw e;
    }

    /** Finds getter function for a private field. */
    private void findGetter(String fieldName) throws ProcessingException {
        ProcessingException e = new ProcessingException(
                "Field cannot be read: it is private and we failed to find a suitable getter "
                        + "for field \"" + fieldName + "\"",
                mAllAppSearchFields.get(fieldName));
        String getFieldName = "get" + fieldName.substring(0, 1).toUpperCase()
                + fieldName.substring(1);

        for (ExecutableElement method : mMethods) {
            String methodName = method.getSimpleName().toString();
            if (methodName.equals(getFieldName) || methodName.equals(fieldName)) {
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
    private void findSetter(String fieldName) throws ProcessingException {
        // We can't report setter failure until we've searched the constructors, so this message is
        // anticipatory and should be buffered by the caller.
        ProcessingException e = new ProcessingException(
                "Field cannot be written directly or via setter because it is private, final, or "
                        + "static, and we failed to find a suitable setter for field \""
                        + fieldName
                        + "\". Trying to find a suitable constructor.",
                mAllAppSearchFields.get(fieldName));
        String setFieldName = "set" + fieldName.substring(0, 1).toUpperCase()
                + fieldName.substring(1);

        for (ExecutableElement method : mMethods) {
            String methodName = method.getSimpleName().toString();
            if (methodName.equals(setFieldName) || methodName.equals(fieldName)) {
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
}
