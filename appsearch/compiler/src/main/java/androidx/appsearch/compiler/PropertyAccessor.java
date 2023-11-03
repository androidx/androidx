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

import static java.util.stream.Collectors.joining;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.AnnotatedGetterOrField.ElementTypeCategory;

import com.google.auto.value.AutoValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * The public/package-private accessor for an {@link AnnotatedGetterOrField}.
 *
 * <p>The accessor itself may be a getter or a field.
 *
 * <p>May be the {@link AnnotatedGetterOrField} itself or some completely different method in
 * case the {@link AnnotatedGetterOrField} is private. For example:
 *
 * <pre>
 * {@code
 * @Document("MyEntity")
 * class Entity {
 *     @Document.StringProperty
 *     private String mName;
 *
 *     public String getName();
 *     //            ^^^^^^^
 * }
 * }
 * </pre>
 */
@AutoValue
public abstract class PropertyAccessor {

    /**
     * The getter/field element.
     */
    @NonNull
    public abstract Element getElement();


    /**
     * Whether the accessor is a getter.
     */
    public boolean isGetter() {
        return getElement().getKind() == ElementKind.METHOD;
    }

    /**
     * Whether the accessor is a field.
     */
    public boolean isField() {
        return getElement().getKind() == ElementKind.FIELD;
    }

    /**
     * Infers the {@link PropertyAccessor} for a given {@link AnnotatedGetterOrField}.
     *
     * @param neighboringMethods The surrounding methods in the same class as the field. In case
     *                           the field is private, an appropriate non-private getter can be
     *                           picked from this list.
     */
    @NonNull
    public static PropertyAccessor infer(
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull Collection<ExecutableElement> neighboringMethods,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        if (!getterOrField.getElement().getModifiers().contains(Modifier.PRIVATE)) {
            // Accessible as-is
            return new AutoValue_PropertyAccessor(getterOrField.getElement());
        }

        if (getterOrField.isGetter()) {
            throw new ProcessingException(
                    "Annotated getter must not be private", getterOrField.getElement());
        }

        return new AutoValue_PropertyAccessor(
                findCorrespondingGetter(getterOrField, neighboringMethods, helper));
    }

    @NonNull
    private static ExecutableElement findCorrespondingGetter(
            @NonNull AnnotatedGetterOrField privateField,
            @NonNull Collection<ExecutableElement> neighboringMethods,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        Set<String> getterNames = getAcceptableGetterNames(privateField, helper);
        List<ExecutableElement> potentialGetters =
                neighboringMethods.stream()
                        .filter(method -> getterNames.contains(method.getSimpleName().toString()))
                        .toList();

        // Start building the exception for the case where we don't find a suitable getter
        String potentialSignatures = getterNames.stream()
                .map(name -> "[public] " + privateField.getJvmType() + " " + name + "()")
                .collect(joining(" OR "));
        ProcessingException processingException = new ProcessingException(
                "Field '%s' cannot be read: it is private and has no suitable getters %s"
                        .formatted(privateField.getJvmName(), potentialSignatures),
                privateField.getElement());

        for (ExecutableElement method : potentialGetters) {
            List<ProcessingException> errors =
                    helper.validateIsGetterThatReturns(method, privateField.getJvmType());
            if (!errors.isEmpty()) {
                processingException.addWarnings(errors);
                continue;
            }
            // found one!
            return method;
        }

        throw processingException;
    }

    @NonNull
    private static Set<String> getAcceptableGetterNames(
            @NonNull AnnotatedGetterOrField privateField,
            @NonNull IntrospectionHelper helper) {
        // String mMyField -> {myField, getMyField}
        // boolean mMyField -> {myField, getMyField, isMyField}
        String normalizedName = privateField.getNormalizedName();
        Set<String> getterNames = new HashSet<>();
        getterNames.add(normalizedName);
        String upperCamelCase = normalizedName.substring(0, 1).toUpperCase()
                + normalizedName.substring(1);
        getterNames.add("get" + upperCamelCase);
        boolean isBooleanField = helper.isFieldOfExactType(
                privateField.getElement(),
                helper.mBooleanPrimitiveType,
                helper.mBooleanBoxType);
        if (isBooleanField && privateField.getElementTypeCategory() == ElementTypeCategory.SINGLE) {
            getterNames.add("is" + upperCamelCase);
        }
        return getterNames;
    }
}
