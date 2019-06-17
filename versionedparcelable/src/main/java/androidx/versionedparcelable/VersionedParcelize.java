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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tags implementations of {@link VersionedParcelable} for code generation.
 *
 * @hide
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@RestrictTo(LIBRARY_GROUP_PREFIX)
public @interface VersionedParcelize {
    /**
     * Whether or not to allow this VersionedParcelable to be used with
     * {@link ParcelUtils#toOutputStream(VersionedParcelable, OutputStream)} and
     * {@link ParcelUtils#fromInputStream(InputStream)}.
     */
    boolean allowSerialization() default false;

    /**
     * Whether or not to allow calls to serialize {@link android.os.Parcelable}
     * or {@link android.os.IBinder} in
     * {@link ParcelUtils#toOutputStream(VersionedParcelable, OutputStream)} and
     * {@link ParcelUtils#fromInputStream(InputStream)}.
     * <p>
     * If this flag is false and a call to {@link VersionedParcel#writeParcelable(Parcelable, int)},
     * {@link VersionedParcel#writeStrongBinder(IBinder, int)}, or
     * {@link VersionedParcel#writeStrongInterface(IInterface, int)} is made, then a runtime
     * exception
     * is thrown. If the flag is true, then the object will simply be skipped and initalized
     * to its default value upon deserialization.
     * <p>
     * If {@link #allowSerialization()} is false, this flag has no effect.
     */
    boolean ignoreParcelables() default false;

    /**
     * Whether or not this class implements {@link CustomVersionedParcelable} and those callbacks
     * should be called during serialization.
     */
    boolean isCustom() default false;

    /**
     * This can be filled with any ids that used to be contained within this VersionedParcelable,
     * but are no longer present. Ids listed here cannot be used by any fields within this class.
     */
    int[] deprecatedIds() default {};

    /**
     * An alternate classname to also generate serialization for to support jetifier androidx
     * migration.
     */
    String jetifyAs() default "";

    /**
     * Specifies a class to use to get objects for instantiation rather than creating them
     * directly. The class must have an accessible empty constructor, and a get() method that
     * returns an instance of the class this annotation is on.
     */
    Class<?> factory() default void.class;
}
