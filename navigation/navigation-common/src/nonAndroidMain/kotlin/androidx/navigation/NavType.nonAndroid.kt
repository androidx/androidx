/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.savedstate.SavedState
import kotlin.jvm.JvmStatic

public actual abstract class NavType<T>
actual constructor(public actual open val isNullableAllowed: Boolean) {
    public actual abstract fun put(bundle: SavedState, key: String, value: T)

    public actual abstract operator fun get(bundle: SavedState, key: String): T?

    public actual abstract fun parseValue(value: String): T

    public actual open fun parseValue(value: String, previousValue: T): T = parseValue(value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(bundle: SavedState, key: String, value: String): T =
        navTypeParseAndPut(bundle, key, value)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun parseAndPut(
        bundle: SavedState,
        key: String,
        value: String?,
        previousValue: T,
    ): T = navTypeParseAndPut(bundle, key, value, previousValue)

    public actual open fun serializeAsValue(value: T): String = value.toString()

    public actual open val name: String = "nav_type"

    public actual open fun valueEquals(value: T, other: T): Boolean = value == other

    public actual companion object {
        @JvmStatic
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT")
        public actual open fun fromArgType(type: String?, packageName: String?): NavType<*> =
            navTypeFromArgType(type)
                ?: if (!type.isNullOrEmpty()) {
                    throw IllegalArgumentException(
                        "Object of type $type is not supported for navigation arguments."
                    )
                } else {
                    StringType
                }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public actual fun inferFromValue(value: String): NavType<Any> = navTypeInferFromValue(value)

        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public actual fun inferFromValueType(value: Any?): NavType<Any> =
            navTypeInferFromValueType(value)
                ?: if (value is Array<*>) {
                    StringArrayType as NavType<Any>
                } else {
                    throw IllegalArgumentException(
                        "$value is not supported for navigation arguments."
                    )
                }

        public actual val IntType: NavType<Int> = IntNavType()
        public actual val IntArrayType: NavType<IntArray?> = IntArrayNavType()
        public actual val IntListType: NavType<List<Int>?> = IntListNavType()
        public actual val LongType: NavType<Long> = LongNavType()
        public actual val LongArrayType: NavType<LongArray?> = LongArrayNavType()
        public actual val LongListType: NavType<List<Long>?> = LongListNavType()
        public actual val FloatType: NavType<Float> = FloatNavType()
        public actual val FloatArrayType: NavType<FloatArray?> = FloatArrayNavType()
        public actual val FloatListType: NavType<List<Float>?> = FloatListNavType()
        public actual val BoolType: NavType<Boolean> = BoolNavType()
        public actual val BoolArrayType: NavType<BooleanArray?> = BoolArrayNavType()
        public actual val BoolListType: NavType<List<Boolean>?> = BoolListNavType()
        public actual val StringType: NavType<String?> = StringNavType()
        public actual val StringArrayType: NavType<Array<String>?> = StringArrayNavType()
        public actual val StringListType: NavType<List<String>?> = StringListNavType()
    }
}
