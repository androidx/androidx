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
package androidx.navigation

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * NavArgument denotes an argument that is supported by a [NavDestination].
 *
 * A NavArgument has a type and optionally a default value, that are used to read/write
 * it in a Bundle. It can also be nullable if the type supports it.
 */
public class NavArgument internal constructor(
    type: NavType<Any?>,
    isNullable: Boolean,
    defaultValue: Any?,
    defaultValuePresent: Boolean
) {
    /**
     * The type of this NavArgument.
     * @return the NavType object denoting the type that can be help in this argument.
     */
    public val type: NavType<Any?>

    /**
     * Whether this argument allows passing a `null` value.
     * @return true if `null` is allowed, false otherwise
     */
    public val isNullable: Boolean

    /**
     * Used to distinguish between a default value of `null` and an argument without an explicit
     * default value.
     * @return true if this argument has a default value (even if that value is set to null),
     * false otherwise
     */
    public val isDefaultValuePresent: Boolean

    /**
     * The default value of this argument or `null` if it doesn't have a default value.
     * Use [isDefaultValuePresent] to distinguish between `null` and absence of a value.
     * @return The default value assigned to this argument.
     */
    public val defaultValue: Any?

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun putDefaultValue(name: String, bundle: Bundle) {
        if (isDefaultValuePresent) {
            type.put(bundle, name, defaultValue)
        }
    }

    /** @suppress */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("DEPRECATION")
    public fun verify(name: String, bundle: Bundle): Boolean {
        if (!isNullable && bundle.containsKey(name) && bundle[name] == null) {
            return false
        }
        try {
            type[bundle, name]
        } catch (e: ClassCastException) {
            return false
        }
        return true
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(javaClass.simpleName)
        sb.append(" Type: $type")
        sb.append(" Nullable: $isNullable")
        if (isDefaultValuePresent) {
            sb.append(" DefaultValue: $defaultValue")
        }
        return sb.toString()
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NavArgument
        if (isNullable != that.isNullable) return false
        if (isDefaultValuePresent != that.isDefaultValuePresent) return false
        if (type != that.type) return false
        return if (defaultValue != null) {
            defaultValue == that.defaultValue
        } else {
            that.defaultValue == null
        }
    }

    public override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + if (isNullable) 1 else 0
        result = 31 * result + if (isDefaultValuePresent) 1 else 0
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }

    /**
     * A builder for constructing [NavArgument] instances.
     */
    @Suppress("UNCHECKED_CAST")
    public class Builder {
        private var type: NavType<Any?>? = null
        private var isNullable = false
        private var defaultValue: Any? = null
        private var defaultValuePresent = false

        /**
         * Set the type of the argument.
         * @param type Type of the argument.
         * @return This builder.
         */
        public fun <T> setType(type: NavType<T>): Builder {
            this.type = type as NavType<Any?>
            return this
        }

        /**
         * Specify if the argument is nullable.
         * The NavType you set for this argument must allow nullable values.
         * @param isNullable Argument will be nullable if true.
         * @return This builder.
         * @see NavType.isNullableAllowed
         */
        public fun setIsNullable(isNullable: Boolean): Builder {
            this.isNullable = isNullable
            return this
        }

        /**
         * Specify the default value for an argument. Calling this at least once will cause the
         * argument to have a default value, even if it is set to null.
         * @param defaultValue Default value for this argument.
         * Must match NavType if it is specified.
         * @return This builder.
         */
        public fun setDefaultValue(defaultValue: Any?): Builder {
            this.defaultValue = defaultValue
            defaultValuePresent = true
            return this
        }

        /**
         * Build the NavArgument specified by this builder.
         * If the type is not set, the builder will infer the type from the default argument value.
         * If there is no default value, the type will be unspecified.
         * @return the newly constructed NavArgument.
         */
        public fun build(): NavArgument {
            val finalType = type ?: NavType.inferFromValueType(defaultValue) as NavType<Any?>
            return NavArgument(finalType, isNullable, defaultValue, defaultValuePresent)
        }
    }

    init {
        require(!(!type.isNullableAllowed && isNullable)) {
            "${type.name} does not allow nullable values"
        }
        require(!(!isNullable && defaultValuePresent && defaultValue == null)) {
            "Argument with type ${type.name} has null value but is not nullable."
        }
        this.type = type
        this.isNullable = isNullable
        this.defaultValue = defaultValue
        isDefaultValuePresent = defaultValuePresent
    }
}