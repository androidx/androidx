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

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

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
}
