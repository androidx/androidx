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

import static androidx.appsearch.compiler.IntrospectionHelper.BUILDER_PRODUCER_CLASS;

import static java.util.stream.Collectors.joining;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Info about how to construct a class annotated with {@code @Document}, aka the document class.
 *
 * <p>This has two components:
 * <ol>
 *     <li>
 *         A constructor/static {@link CreationMethod} that takes in <b>N</b> params, each
 *         corresponding to an {@link AnnotatedGetterOrField} and returns either the document
 *         class or a builder.
 *     </li>
 *     <li>
 *         A set of <b>M</b> setters/fields on the object returned by the {@link CreationMethod}.
 *
 *         <p>Note: Fields only apply if {@link CreationMethod#returnsDocumentClass}
 *         since it is assumed that builders cannot have fields.
 *         When {@link CreationMethod#returnsBuilder}, this only contains setters.
 *     </li>
 * </ol>
 *
 * <p><b>N + M</b> collectively encompass all of the annotated getters/fields in the document class.
 *
 * <p>For example:
 *
 * <pre>
 * {@code
 * @Document
 * class DocumentClass {
 *     public DocumentClass(String id, String namespace, int someProp) {...}
 * //         ^^^^^^^^^^^^^
 * //       Creation method
 *
 *     @Document.Id
 *     public String getId() {...}
 *
 *     @Document.Namespace
 *     public String getNamespace() {...}
 *
 *     @Document.LongProperty
 *     public int getSomeProp() {...}
 *
 *     @Document.StringProperty
 *     public String getOtherProp() {...}
 *     public void setOtherProp(String otherProp) {...}
 * //              ^^^^^^^^^^^^
 * //                 setter
 *
 *     @Document.BooleanProperty
 *     public boolean mYetAnotherProp;
 * //                 ^^^^^^^^^^^^^^^
 * //                      field
 * }
 * }
 * </pre>
 */
@AutoValue
public abstract class DocumentClassCreationInfo {

    /**
     * The creation method.
     */
    @NonNull
    public abstract CreationMethod getCreationMethod();

    /**
     * Maps an annotated getter/field to the corresponding setter/field on the object returned by
     * the {@link CreationMethod}.
     */
    @NonNull
    public abstract ImmutableMap<AnnotatedGetterOrField, SetterOrField> getSettersAndFields();

    /**
     * Infers the {@link DocumentClassCreationInfo} for a specified document class.
     */
    @NonNull
    public static DocumentClassCreationInfo infer(
            @NonNull TypeElement documentClass,
            @NonNull Set<AnnotatedGetterOrField> annotatedGettersAndFields,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        BuilderProducer builderProducer = BuilderProducer.tryCreate(documentClass, helper);

        Map<AnnotatedGetterOrField, SetterOrField> settersAndFields = new LinkedHashMap<>();
        List<ProcessingException> setterNotFoundErrors = new ArrayList<>();
        for (AnnotatedGetterOrField getterOrField : annotatedGettersAndFields) {
            if (builderProducer == null && getterOrField.isField()
                    && fieldCanBeSetDirectly(getterOrField.getElement())) {
                // annotated field on the document class itself
                settersAndFields.put(
                        getterOrField, SetterOrField.create(getterOrField.getElement()));
            } else {
                // Annotated getter|annotated private field|must use builder pattern
                try {
                    TypeElement targetClass = builderProducer != null
                            ? (TypeElement) builderProducer.getBuilderType().asElement()
                            : documentClass;
                    ExecutableElement setter = findSetter(targetClass, getterOrField, helper);
                    settersAndFields.put(getterOrField, SetterOrField.create(setter));
                } catch (ProcessingException e) {
                    setterNotFoundErrors.add(e);
                }
            }
        }

        List<CreationMethod> potentialCreationMethods = extractPotentialCreationMethods(
                documentClass, annotatedGettersAndFields, builderProducer, helper);

        // Start building the exception in case we don't find a suitable creation method
        Set<AnnotatedGetterOrField> remainingGettersAndFields =
                subtract(annotatedGettersAndFields, settersAndFields.keySet());
        ProcessingException exception = new ProcessingException(
                ("Could not find a suitable %s for \"%s\" that covers properties: [%s]. "
                        + "See the warnings for more details.").formatted(
                        builderProducer != null ? "builder producer" : "constructor/factory method",
                        documentClass.getQualifiedName(),
                        getCommaSeparatedJvmNames(remainingGettersAndFields)),
                documentClass);
        exception.addWarnings(setterNotFoundErrors);

        // Pick the first creation method that covers the annotated getters/fields that we don't
        // already have setters/fields for
        for (CreationMethod creationMethod : potentialCreationMethods) {
            Set<AnnotatedGetterOrField> missingParams =
                    subtract(remainingGettersAndFields, creationMethod.getParamAssociations());
            if (!missingParams.isEmpty()) {
                exception.addWarning(new ProcessingException(
                        ("Cannot use this %s to construct the class: \"%s\". "
                                + "No parameters for the properties: [%s]")
                                .formatted(
                                        creationMethod.isConstructor()
                                                ? "constructor" : "creation method",
                                        documentClass.getQualifiedName(),
                                        getCommaSeparatedJvmNames(missingParams)),
                        creationMethod.getElement()));
                continue;
            }
            // found one!
            // This creation method may cover properties that we already have setters for.
            // If so, forget those setters.
            for (AnnotatedGetterOrField getterOrField : creationMethod.getParamAssociations()) {
                settersAndFields.remove(getterOrField);
            }
            return new AutoValue_DocumentClassCreationInfo(
                    creationMethod, ImmutableMap.copyOf(settersAndFields));
        }

        throw exception;
    }

    /**
     * Finds a setter corresponding to the getter/field within the specified class.
     *
     * @throws ProcessingException if no suitable setter was found within the specified class.
     */
    @NonNull
    private static ExecutableElement findSetter(
            @NonNull TypeElement clazz,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        Set<String> setterNames = getAcceptableSetterNames(getterOrField);
        // Start building the exception in case we don't find a suitable setter
        String setterSignatures = setterNames.stream()
                .map(setterName ->
                        "[public] void %s(%s)".formatted(setterName, getterOrField.getJvmType()))
                .collect(joining("|"));
        ProcessingException exception = new ProcessingException(
                "Could not find any of the setter(s): " + setterSignatures,
                getterOrField.getElement());

        List<ExecutableElement> potentialSetters = helper.getAllMethods(clazz).stream()
                .filter(method -> setterNames.contains(method.getSimpleName().toString()))
                .toList();
        for (ExecutableElement method : potentialSetters) {
            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                exception.addWarning(new ProcessingException(
                        "Setter cannot be used: private visibility", method));
                continue;
            }
            if (method.getModifiers().contains(Modifier.STATIC)) {
                exception.addWarning(new ProcessingException(
                        "Setter cannot be used: static method", method));
                continue;
            }
            if (method.getParameters().size() != 1) {
                exception.addWarning(new ProcessingException(
                        "Setter cannot be used: takes %d parameters instead of 1"
                                .formatted(method.getParameters().size()),
                        method));
                continue;
            }
            // found one!
            return method;
        }

        throw exception;
    }

    @NonNull
    private static Set<String> getAcceptableSetterNames(
            @NonNull AnnotatedGetterOrField getterOrField) {
        // String mField -> {field(String), setField(String)}
        // String getProp() -> {prop(String), setProp(String)}
        // List<String> getProps() -> {props(List), setProps(List), addProps(List)}
        Set<String> setterNames = new HashSet<>();
        String normalizedName = getterOrField.getNormalizedName();
        setterNames.add(normalizedName);
        String pascalCase =
                normalizedName.substring(0, 1).toUpperCase() + normalizedName.substring(1);
        setterNames.add("set" + pascalCase);
        AnnotatedGetterOrField.ElementTypeCategory typeCategory =
                getterOrField.getElementTypeCategory();
        switch (typeCategory) {
            case SINGLE:
                break;
            case COLLECTION: // fall-through
            case ARRAY:
                setterNames.add("add" + pascalCase);
                break;
            default:
                throw new IllegalStateException("Unhandled type-category: " + typeCategory);
        }
        return setterNames;
    }

    private static boolean fieldCanBeSetDirectly(@NonNull Element field) {
        Set<Modifier> modifiers = field.getModifiers();
        return !modifiers.contains(Modifier.PRIVATE) && !modifiers.contains(Modifier.FINAL);
    }

    /**
     * Extracts potential creation methods for the document class.
     *
     * <p>Returns creation methods corresponding to the {@link BuilderProducer}, when it is not
     * null.
     *
     * @throws ProcessingException if no viable creation methods could be extracted.
     */
    private static List<CreationMethod> extractPotentialCreationMethods(
            @NonNull TypeElement documentClass,
            @NonNull Set<AnnotatedGetterOrField> annotatedGettersAndFields,
            @Nullable BuilderProducer builderProducer,
            @NonNull IntrospectionHelper helper) throws ProcessingException {
        List<ExecutableElement> potentialMethods;
        if (builderProducer != null && builderProducer.isStaticMethod()) {
            potentialMethods = List.of((ExecutableElement) builderProducer.getElement());
        } else {
            // Use the constructors & factory methods on the document class or builder class itself
            TypeElement targetClass = builderProducer != null
                    ? (TypeElement) builderProducer.getElement() : documentClass;
            potentialMethods = targetClass.getEnclosedElements().stream()
                    .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR
                            || helper.isStaticFactoryMethod(element))
                    .map(element -> (ExecutableElement) element)
                    .toList();
        }

        // Start building an exception in case none of the candidates are suitable
        ProcessingException exception = new ProcessingException(
                "Could not find a suitable creation method", documentClass);

        List<CreationMethod> creationMethods = new ArrayList<>();
        for (ExecutableElement candidate : potentialMethods) {
            try {
                creationMethods.add(CreationMethod.inferParamAssociationsAndCreate(
                        candidate,
                        annotatedGettersAndFields,
                        /* returnsDocumentClass= */builderProducer == null));
            } catch (ProcessingException e) {
                exception.addWarning(e);
            }
        }

        if (creationMethods.isEmpty()) {
            throw exception;
        }
        return creationMethods;
    }

    /**
     * Returns a new set with all the elements of lhs that don't exist in rhs i.e. set difference.
     */
    private static <T> Set<T> subtract(Set<T> lhs, Collection<T> rhs) {
        Set<T> copy = new LinkedHashSet<>(lhs);
        copy.removeAll(rhs);
        return copy;
    }

    private static String getCommaSeparatedJvmNames(
            @NonNull Collection<AnnotatedGetterOrField> gettersAndFields) {
        return gettersAndFields.stream()
                .map(AnnotatedGetterOrField::getJvmName)
                .collect(joining(", "));
    }

    /**
     * Represents a static method/nested class within a document class annotated with
     * {@code @Document.BuilderProducer}. For example:
     *
     * <pre>
     * {@code
     * @Document
     * public class MyEntity {
     *     @Document.BuilderProducer
     *     public static Builder newBuilder();
     *
     *     // This class may directly be annotated with @Document.BuilderProducer instead
     *     public static class Builder {...}
     * }
     * }
     * </pre>
     */
    private static final class BuilderProducer {
        private final Element mElement;
        private final DeclaredType mBuilderType;

        private BuilderProducer(@NonNull Element element, @NonNull DeclaredType builderType) {
            mElement = element;
            mBuilderType = builderType;
        }

        @Nullable
        static BuilderProducer tryCreate(
                @NonNull TypeElement documentClass,
                @NonNull IntrospectionHelper helper) throws ProcessingException {
            List<? extends Element> annotatedElements = documentClass.getEnclosedElements().stream()
                    .filter(BuilderProducer::isAnnotatedWithBuilderProducer)
                    .toList();
            if (annotatedElements.isEmpty()) {
                return null;
            } else if (annotatedElements.size() > 1) {
                throw new ProcessingException("Found duplicated builder producer", documentClass);
            }

            Element annotatedElement = annotatedElements.get(0);
            requireBuilderProducerAccessible(annotatedElement);
            // Since @Document.BuilderProducer is configured with
            // @Target({ElementType.METHOD, ElementType.TYPE}), this should never throw in practice.
            requireBuilderProducerIsMethodOrClass(annotatedElement);

            DeclaredType builderType;
            if (annotatedElement.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) annotatedElement;
                requireIsDeclaredTypeWithBuildMethod(
                        method.getReturnType(), documentClass, annotatedElement, helper);
                builderType = (DeclaredType) method.getReturnType();
            } else {
                // A class is annotated with @Document.BuilderProducer. Use its constructors as
                // the creation methods.
                TypeElement builderClass = (TypeElement) annotatedElement;
                requireIsDeclaredTypeWithBuildMethod(
                        builderClass.asType(), documentClass, annotatedElement, helper);
                builderType = (DeclaredType) annotatedElement.asType();
            }

            return new BuilderProducer(annotatedElement, builderType);
        }

        /**
         * The static method/nested class annotated with {@code @Document.BuilderProducer}.
         */
        @NonNull
        Element getElement() {
            return mElement;
        }

        boolean isStaticMethod() {
            return mElement.getKind() == ElementKind.METHOD;
        }

        /**
         * The return type of the annotated method or the annotated builder class.
         */
        @NonNull
        DeclaredType getBuilderType() {
            return mBuilderType;
        }

        private static boolean isAnnotatedWithBuilderProducer(@NonNull Element element) {
            return element.getAnnotationMirrors().stream()
                    .anyMatch(annotation -> annotation.getAnnotationType().toString()
                            .equals(BUILDER_PRODUCER_CLASS.canonicalName()));
        }

        /**
         * Makes sure the annotated element is a builder/class.
         */
        private static void requireBuilderProducerIsMethodOrClass(
                @NonNull Element annotatedElement) throws ProcessingException {
            if (annotatedElement.getKind() != ElementKind.METHOD
                    && annotatedElement.getKind() != ElementKind.CLASS) {
                throw new ProcessingException(
                        "Builder producer must be a method or a class", annotatedElement);
            }
        }

        /**
         * Makes sure the annotated element is static and not private.
         */
        private static void requireBuilderProducerAccessible(
                @NonNull Element annotatedElement) throws ProcessingException {
            if (!annotatedElement.getModifiers().contains(Modifier.STATIC)) {
                throw new ProcessingException("Builder producer must be static", annotatedElement);
            }
            if (annotatedElement.getModifiers().contains(Modifier.PRIVATE)) {
                throw new ProcessingException("Builder producer cannot be private",
                        annotatedElement);
            }
        }

        /**
         * Makes sure the type is a {@link DeclaredType} with a non-private & non-static method
         * of the form {@code DocumentClass build()}.
         *
         * @param annotatedElement The method/class annotated with
         *                         {@code @Document.BuilderProducer}.
         * @throws ProcessingException on the annotated element if the conditions are not met.
         */
        private static void requireIsDeclaredTypeWithBuildMethod(
                @NonNull TypeMirror builderType,
                @NonNull TypeElement documentClass,
                @NonNull Element annotatedElement,
                @NonNull IntrospectionHelper helper) throws ProcessingException {
            ProcessingException exception = new ProcessingException(
                    "Invalid builder producer: %s does not have a method %s build()"
                            .formatted(builderType, documentClass),
                    annotatedElement);
            if (builderType.getKind() != TypeKind.DECLARED) {
                throw exception;
            }
            TypeElement builderClass = (TypeElement) ((DeclaredType) builderType).asElement();
            boolean hasBuildMethod = helper.getAllMethods(builderClass).stream()
                    .anyMatch(method -> !method.getModifiers().contains(Modifier.STATIC)
                            && !method.getModifiers().contains(Modifier.PRIVATE)
                            && helper.isReturnTypeMatching(method, documentClass.asType())
                            && method.getParameters().isEmpty());
            if (!hasBuildMethod) {
                throw exception;
            }
        }
    }
}
