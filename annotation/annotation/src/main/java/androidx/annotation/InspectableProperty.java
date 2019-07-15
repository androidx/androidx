/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.annotation;


import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method is the getter for a resources-backed property that should be
 * shown in Android Studio's inspection tools.
 */
@Target({METHOD})
@Retention(SOURCE)
public @interface InspectableProperty {
    /**
     * The name of the property.
     *
     * If left empty (the default), the property name will be inferred from the name of the getter
     * method.
     *
     * @return The name of the property.
     */
    String name() default "";

    /**
     * If the property is inflated from XML, the resource ID of its XML attribute.
     *
     * If left as the default, and {@link #hasAttributeId()} is true, the attribute ID will be
     * inferred from {@link #name()}.
     *
     * @return The attribute ID of the property or the default null resource ID
     */
    int attributeId() default 0;

    /**
     * If this property has an attribute ID.
     *
     * Set to false if the annotated property does not have an attribute ID, that is, it is not
     * inflated from an XML attribute. This will prevent the automatic inference of the attribute.
     *
     * @return Whether to infer an attribute ID if not supplied
     */
    boolean hasAttributeId() default true;

    /**
     * Specify how to interpret a value type packed into a primitive integer.
     *
     * @return A {@link ValueType}
     */
    ValueType valueType() default ValueType.INFERRED;

    /**
     * For enumerations packed into primitive {int} properties, map the values to string names.
     *
     * Note that {@code #enumMapping()} cannot be used simultaneously with {@link #flagMapping()}.
     *
     * @return An array of {@link EnumEntry}, empty if not applicable
     */
    EnumEntry[] enumMapping() default {};

    /**
     * For flags packed into primitive {int} properties, model the string names of the flags.
     *
     * Note that {@code #flagMapping()} cannot be used simultaneously with {@link #enumMapping()}.
     *
     * @return An array of {@link FlagEntry}, empty if not applicable
     */
    FlagEntry[] flagMapping() default {};


    /**
     * One entry in an enumeration packed into a primitive {int}.
     */
    @Target({TYPE})
    @Retention(SOURCE)
    @interface EnumEntry {
        /**
         * The string name of this enumeration value.
         *
         * @return A string name
         */
        String name();

        /**
         * The integer value of this enumeration value.
         *
         * @return An integer value
         */
        int value();
    }

    /**
     * One flag value of many that may be packed into a primitive {int}.
     */
    @Target({TYPE})
    @Retention(SOURCE)
    @interface FlagEntry {
        /**
         * The string name of this flag.
         *
         * @return A string name
         */
        String name();

        /**
         * A target value that the property's value must equal after masking.
         *
         * If a mask is not supplied (i.e., {@link #mask()} is 0), the target will be reused as the
         * mask. This handles the common case where no flags mutually exclude each other.
         *
         * @return The target value to compare against
         */
        int target();

        /**
         * A mask that the property will be bitwise anded with before comparing to the target.
         *
         * If set to 0 (the default), the value of {@link #target()} will be used as a mask. Zero
         * was chosen as the default since bitwise and with zero is always zero.
         *
         * @return A mask, or 0 to use the target as a mask
         */
        int mask() default 0;
    }

    /**
     * The type of value packed into a primitive {int}.
     */
    enum ValueType {
        /**
         * No special handling, property is considered to be a numeric value.
         */
        NONE,

        /**
         * The default the annotation processor infers the value type from context.
         */
        INFERRED,

        /**
         * Value packs an enumeration.
         *
         * This is inferred if {@link #enumMapping()} is specified.
         *
         * @see EnumEntry
         */
        INT_ENUM,

        /**
         * Value packs flags, of which many may be enabled at once.
         *
         * This is inferred if {@link #flagMapping()} is specified.
         *
         * @see FlagEntry
         */
        INT_FLAG,

        /**
         * Value packs color information.
         *
         * This is inferred from {@link ColorInt}, or {@link ColorLong} on the getter method.
         */
        COLOR,

        /**
         * Value packs gravity information.
         *
         * This type is not inferred and is non-trivial to represent using {@link FlagEntry}.
         */
        GRAVITY,

        /**
         * Value is a resource ID
         *
         * This type is inferred from the presence of a resource ID annotation such as
         * {@link AnyRes}.
         */
        RESOURCE_ID
    }
}
