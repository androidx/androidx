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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A setter/field.
 */
@AutoValue
public abstract class SetterOrField {
    /**
     * The setter/field element.
     */
    @NonNull
    public abstract Element getElement();

    /**
     * Whether it is a setter.
     */
    public boolean isSetter() {
        return getElement().getKind() == ElementKind.METHOD;
    }

    @NonNull
    static SetterOrField create(@NonNull Element element) {
        return new AutoValue_SetterOrField(element);
    }
}
