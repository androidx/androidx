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

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * A constructor or static method used to create a class annotated with {@code @Document} aka
 * document class.
 *
 * <p>Takes in N input params, each corresponding to a value for an
 * {@link AnnotatedGetterOrField}.
 *
 * <p>Moreover, may return the document class itself or a builder. All of the following are
 * examples of valid creation methods:
 *
 * <pre>
 * {@code
 * @Document
 * class MyEntity {
 *     static MyEntity create(String id, String namespace, int someProp);
 * //                  ^^^^^^
 *
 *     MyEntity() {...}
 * //  ^^^^^^^^
 *
 *     MyEntity(String id, String namespace, int someProp) {...}
 * //  ^^^^^^^^
 *
 *     @Document.BuilderProducer
 *     static Builder newBuilder() {...}
 * //                 ^^^^^^^^^^
 *
 *     @Document.BuilderProducer
 *     static class Builder {
 *         Builder() {...}
 * //      ^^^^^^^
 *
 *         Builder(String id, String namespace, int someProp) {...}
 * //      ^^^^^^^
 *     }
 * }
 * }
 * </pre>
 */
@AutoValue
public abstract class CreationMethod {
    /**
     * The constructor/static method element.
     */
    @NonNull
    public abstract ExecutableElement getElement();

    /**
     * Whether the creation method is a constructor.
     */
    public boolean isConstructor() {
        return getElement().getKind() == ElementKind.CONSTRUCTOR;
    }

    /**
     * The {@link AnnotatedGetterOrField}s that each input param corresponds to (order sensitive).
     */
    @NonNull
    public abstract ImmutableList<AnnotatedGetterOrField> getParamAssociations();

    /**
     * Whether the creation method returns the document class itself instead of a builder.
     */
    public abstract boolean returnsDocumentClass();

    /**
     * Whether the creation method returns a builder instead of the document class itself.
     */
    public boolean returnsBuilder() {
        return !returnsDocumentClass();
    }

    /**
     * Infers which annotated getter/field each param corresponds to and creates
     * a {@link CreationMethod}.
     *
     * @param method The creation method element.
     * @param gettersAndFields The annotated getters/fields of the document class.
     * @param returnsDocumentClass Whether the {@code method} returns the document class itself.
     *                             If not, it is assumed that it returns a builder for the
     *                             document class.
     * @throws ProcessingException If the method is not invocable or the association for a param
     *                             could not be deduced.
     */
    @NonNull
    public static CreationMethod inferParamAssociationsAndCreate(
            @NonNull ExecutableElement method,
            @NonNull Collection<AnnotatedGetterOrField> gettersAndFields,
            boolean returnsDocumentClass) throws ProcessingException {
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            throw new ProcessingException(
                    "Method cannot be used to create a "
                            + (returnsDocumentClass ? "document class" : "builder")
                            + ": private visibility",
                    method);
        }

        if (method.getKind() == ElementKind.CONSTRUCTOR
                && method.getEnclosingElement().getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessingException(
                    "Method cannot be used to create a "
                            + (returnsDocumentClass ? "document class" : "builder")
                            + ": abstract constructor", method);
        }

        Map<String, AnnotatedGetterOrField> normalizedNameToGetterOrField = new HashMap<>();
        for (AnnotatedGetterOrField getterOrField : gettersAndFields) {
            normalizedNameToGetterOrField.put(getterOrField.getNormalizedName(), getterOrField);
        }

        ImmutableList.Builder<AnnotatedGetterOrField> paramAssociations = ImmutableList.builder();
        for (VariableElement param : method.getParameters()) {
            String paramName = param.getSimpleName().toString();
            AnnotatedGetterOrField correspondingGetterOrField =
                    normalizedNameToGetterOrField.get(paramName);
            if (correspondingGetterOrField == null) {
                throw new ProcessingException(
                        ("Parameter \"%s\" is not an AppSearch parameter; "
                                + "don't know how to supply it.").formatted(paramName),
                        method);
            }
            paramAssociations.add(correspondingGetterOrField);
        }

        return new AutoValue_CreationMethod(
                method, paramAssociations.build(), returnsDocumentClass);
    }
}
