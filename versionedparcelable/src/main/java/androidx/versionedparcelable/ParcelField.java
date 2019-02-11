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

package androidx.versionedparcelable;

import androidx.annotation.RestrictTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tags a field that should be serialized as part of SafeParcelization.
 * @hide
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public @interface ParcelField {
    int value();

    /**
     * Specifies the default value of this field.
     */
    String defaultValue() default "";
}
