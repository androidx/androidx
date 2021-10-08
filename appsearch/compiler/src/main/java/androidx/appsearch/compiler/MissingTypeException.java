/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.annotation.RestrictTo;

import javax.lang.model.element.Element;

/**
 * An exception thrown from the appsearch annotation processor to indicate a type element is not
 * found due to it being possibly generated at a later annotation processing round.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
final class MissingTypeException extends Exception {
    @NonNull
    private final Element mTypeElement;

    MissingTypeException(@NonNull Element typeElement) {
        super("Type " + typeElement.getSimpleName() + " is not present");
        mTypeElement = typeElement;
    }

    @NonNull
    Element getTypeName() {
        return mTypeElement;
    }
}
