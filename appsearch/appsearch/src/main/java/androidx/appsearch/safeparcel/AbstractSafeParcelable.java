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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Implements {@link SafeParcelable}.
 *
 * <p>In Jetpack, the annotations from {@SafeParcelable} are moved here so {@code NULL} can be
 * package private.
 *
 * <p>This class is put in androidx.appsearch.app. Thus we can restrict the scope to avoid making it
 * public.
 *
 * <p>DON'T modify this class unless it is necessary. E.g. port new annotations from SafeParcelable.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AbstractSafeParcelable implements SafeParcelable {
    // Note: the field name and value are accessed using reflection for backwards compatibility, and
    // must not be changed.
    static final String NULL = "SAFE_PARCELABLE_NULL_STRING";

    /** Use this annotation on members that you wish to be marshalled in the SafeParcelable. */
    public @interface Field {
        /**
         * Valid values for id are between 1 and 65535. This field id is marshalled into a Parcel
         * . To
         * maintain backwards compatibility, never reuse old id's. It is okay to no longer use
         * old id's
         * and add new ones in subsequent versions of a SafeParcelable.
         */
        int id();

        /**
         * This specifies the name of the getter method for retrieving the value of this field. This
         * must be specified for fields that do not have at least package visibility because the
         * "creator" class will be unable to access the value when attempting to marshall this
         * field.
         * The getter method should take no parameters and return the type of this field (unless
         * overridden by the "type" attribute below).
         */
        String getter() default NULL;

        /**
         * For advanced uses, this specifies the type for the field when marshalling and
         * unmarshalling
         * by the "creator" class to be something different than the declared type of the member
         * variable. This is useful if you want to incorporate an object that is not
         * SafeParcelable (or
         * a system Parcelable object). Be sure to enter the fully qualified name for the class
         * (i.e.,
         * android.os.Bundle and not Bundle). For example,
         *
         * <pre>
         *   &#64;Class(creator="MyAdvancedCreator")
         *   public class MyAdvancedSafeParcelable implements SafeParcelable {
         *       public static final Parcelable.Creator&#60;MyAdvancedSafeParcelable&#62; CREATOR =
         *               new MyAdvancedCreator();
         *
         *       &#64;Field(id=1, getter="getObjectAsBundle", type="android.os.Bundle")
         *       private final MyCustomObject myObject;
         *
         *       &#64;Constructor
         *       MyAdvancedSafeParcelable(
         *               &#64;Param(id=1) Bundle objectAsBundle) {
         *           myObject = myConvertFromBundleToObject(objectAsBundle);
         *       }
         *
         *       Bundle getObjectAsBundle() {
         *           // The code here can convert your custom object to one that can be parcelled.
         *           return myConvertFromObjectToBundle(myObject);
         *       }
         *
         *       ...
         *   }
         * </pre>
         */
        String type() default NULL;

        /**
         * This can be used to specify the default value for primitive types (e.g., boolean, int,
         * long),
         * primitive type object wrappers (e.g., Boolean, Integer, Long) and String in the case a
         * value
         * for a field was not explicitly set in the marshalled Parcel. This performs compile-time
         * checks for the type of the field and inserts the appropriate quotes or double quotes
         * around
         * strings and chars or removes them completely for booleans and numbers. To insert a
         * generic
         * string for initializing field, use {@link #defaultValueUnchecked()}. You can specify
         * at most
         * one of {@link #defaultValue()} or {@link #defaultValueUnchecked()}. For example,
         *
         * <pre>
         *   &#64;Field(id=2, defaultValue="true")
         *   boolean myBoolean;
         *
         *   &#64;Field(id=3, defaultValue="13")
         *   Integer myInteger;
         *
         *   &#64;Field(id=4, defaultValue="foo")
         *   String myString;
         * </pre>
         */
        String defaultValue() default NULL;

        /**
         * This can be used to specify the default value for any object and the string value is
         * literally added to the generated creator class code unchecked. You can specify at most
         * one of
         * {@link #defaultValue()} or {@link #defaultValueUnchecked()}. You must fully qualify any
         * classes you reference within the string. For example,
         *
         * <pre>
         *   &#64;Field(id=2, defaultValueUnchecked="new android.os.Bundle()")
         *   Bundle myBundle;
         * </pre>
         */
        String defaultValueUnchecked() default NULL;
    }

    /**
     * There may be exactly one member annotated with VersionField, which represents the version of
     * this safe parcelable. The attributes are the same as those of {@link Field}. Note you can use
     * any type you want for your version field, although most people use int's.
     */
    public @interface VersionField {
        int id();

        String getter() default NULL;

        String type() default NULL;
    }

    /**
     * Use this to indicate the member field that holds whether a field was set or not. The member
     * field type currently supported is a HashSet&#60;Integer&#62; which is the set of safe
     * parcelable field id's that have been explicitly set.
     *
     * <p>This annotation should also be used to annotate one of the parameters to the constructor
     * annotated with &#64;Constructor. Note that this annotation should either be present on
     * exactly
     * one member field and one constructor parameter or left out completely.
     */
    public @interface Indicator {
        String getter() default NULL;
    }

    /**
     * Use this on a parameter passed in to the Constructor to indicate that a removed field
     * should be
     * read on construction. If the field is not present when read, the default value will be used
     * instead.
     */
    public @interface RemovedParam {
        int id();

        String defaultValue() default NULL;

        String defaultValueUnchecked() default NULL;
    }

    /**
     * Use this to mark tombstones for removed {@link Field Fields} or {@link VersionField
     * VersionFields}.
     */
    public @interface Reserved {
        int[] value();
    }

    /**
     * Use this to indicate the constructor that the creator should use. The constructor annotated
     * with this must be package or public visibility, so that the generated "creator" class can
     * invoke this.
     */
    public @interface Constructor {
    }

    /**
     * Use this on each parameter passed in to the Constructor to indicate to which field id each
     * formal parameter corresponds.
     */
    public @interface Param {
        int id();
    }

    /**
     * To be implemented by child classes.
     *
     * <p>This is purely for code sync purpose. Have {@code writeToParcel} here so we can keep
     * "@Override" in child classes.
     */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}
