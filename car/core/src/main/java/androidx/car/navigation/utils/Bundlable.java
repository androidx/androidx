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

package androidx.car.navigation.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface for classes whose instances can be written to and restored from a {@link Bundle}.
 * Classes implementing the {@link Bundlable} interface must also provide a public default
 * constructor, or a constructor that accepts {@link BundleMarshaller} as its only parameter.
 * <p>
 * This serialization protocol is designed to:
 * <ul>
 * <li>provide backward/forward compatibility between producers and consumers (following the
 * Protocol Buffers pattern).
 * <li>minimize the number of objects being allocated during both serialization and deserialization.
 * </ul>
 * <p>
 * Implementations of this interface should comply to the following rules:
 * <ul>
 * <li>Fields should be serialized and deserialized using {@link BundleMarshaller} "put" and
 * "get" methods.
 * <li>Marshalling keys must be lower camel case alphanumerical identifiers (i.e.: "distanceUnit").
 * (symbols such as "." and "_" are reserved by the system).
 * <li>Field types should not be modified between versions of {@link Bundlable} objects to provide
 * backward and forward compatibility. Only deprecations and additions are allowed.
 * <li>When a field is deprecated, its marshalling key shouldn't be reused by any new field.
 * <li>Enums are marshalled using {@link Enum#name()}. Because of this, enum values should not be
 * renamed. Because enum values could be added or deprecated, clients must be prepared to accept
 * null or a default value in case the server sends a value it doesn't know.
 * <li>Fields annotated with {@link androidx.annotation.NonNull} should not be deprecated
 * (as clients might not be prepared for their absence). Implementations of this interface should
 * enforce this constraint, i.e. by initializing these fields at class instantiation, and using
 * {@link androidx.core.util.Preconditions#checkNotNull(Object)} to prevent null values. If a new
 * {@link androidx.annotation.NonNull} field is added on an existing {@link Bundlable}, the
 * deserialization must provide a default value for it (as existing services won't provide values
 * for it until they are updated).
 * </ul>
 * The following is an example of the suggested implementation:
 * <pre>
 * public class MyClass implements Bundlable {
 *     private static final String FOO_VALUE_KEY = "fooValue";
 *     private static final String BAR_VALUE_KEY = "barValue";
 *
 *     public enum MyEnum {
 *         VALUE_1,
 *         VALUE_2
 *     }
 *
 *     public String mFooValue;
 *     public MyEnum mBarValue;
 *
 *     &#064;Override
 *     public void toBundle(@NonNull BundleMarshaller out) {
 *         out.putString(FOO_VALUE_KEY, mFooValue);
 *         out.putEnum(BAR_VALUE_KEY, mBarValue);
 *     }
 *
 *     &#064;Override
 *     public void fromBundle(@NonNull BundleMarshaller in) {
 *         mFooValue = in.getString(FOO_VALUE_KEY);
 *         mBarValue = in.getEnum(BAR_VALUE_KEY, MyEnum.class);
 *     }
 * }
 * </pre>
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface Bundlable {
    /**
     * Serializes this object into a {@link BundleMarshaller} by writing all its fields to it.
     */
    void toBundle(@NonNull BundleMarshaller out);

    /**
     * Deserializes this object from a {@link BundleMarshaller} by reading all its fields from it.
     */
    void fromBundle(@NonNull BundleMarshaller in);
}
