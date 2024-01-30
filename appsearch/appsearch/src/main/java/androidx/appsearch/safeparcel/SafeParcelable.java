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

package androidx.appsearch.safeparcel;

import android.os.Parcel;

import androidx.annotation.RestrictTo;

/**
 * AppSearch's own version of SafeParcelable so we don't need to include a complete version
 * here, which is not needed inside Jetpack.
 *
 * <p>In Jetpack, annotation processor is not run, but we still need them so the classes, e.g.
 * {@link androidx.appsearch.app.StorageInfo}, can mostly share the same code.
 *
 * <p>Most of its original annotations are moved to
 * {@link AbstractSafeParcelable} so {@code AbstractSafeParcelable#NULL} can be package private.
 *
 * <p>This class is put in androidx.appsearch.app so we can restrict the scope to avoid making it
 * public.
 *
 * <p>DON'T modify this class unless it is necessary. E.g. port new annotations from SafeParcelable.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SafeParcelable {
    /**
     * This annotates your class and specifies the name of the generated "creator" class for
     * marshalling/unmarshalling a SafeParcelable to/from a {@link Parcel}. The "creator" class is
     * generated in the same package as the SafeParcelable class. You can also set "validate" to
     * true,
     * which will cause the "creator" to invoke the method validateContents() on your class after
     * constructing an instance.
     */
    @SuppressWarnings("JavaLangClash")
    @interface Class {
        /**
         * Simple name of the generated "creator" class generated in the same package as the
         * SafeParceable.
         */
        String creator();

        /**
         * When set to true, invokes the validateContents() method in this SafeParcelable object
         * after
         * constructing a new instance.
         */
        boolean validate() default false;

        /**
         * When set to true, it will not write type default values to the Parcel.
         *
         * boolean: false
         * byte/char/short/int/long: 0
         * float: 0.0f
         * double: 0.0
         * Objects/arrays: null
         *
         * <p>Cannot be used with Field(defaultValue)
         */
        boolean doNotParcelTypeDefaultValues() default false;
    }
}
