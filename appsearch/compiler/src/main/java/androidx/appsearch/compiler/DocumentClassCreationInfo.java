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
import com.google.common.collect.ImmutableMap;

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
}
