/*
 * Copyright 2025 The Android Open Source Project
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
@file:JvmName("DynamicDataPairUtil")

package androidx.wear.protolayout.expression

import android.graphics.Color
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import java.time.Duration
import java.time.Instant

/**
 * A pair of [DynamicDataKey] and its corresponding [DynamicDataValue]. This is similar to [Pair]
 * but makes sure key and value have the same generic type parameter.
 *
 * @property key represent a key that other APIs can use to reference [value]s value.
 * @property value to use for evaluating references to [key].
 */
public class DynamicDataPair<T : DynamicType>(
    public val key: DynamicDataKey<T>,
    public val value: DynamicDataValue<T>
) {
    public fun asPair(): Pair<DynamicDataKey<T>, DynamicDataValue<T>> = Pair(key, value)
}

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 200)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicBool>.mapTo(value: Boolean): DynamicDataPair<DynamicBool> =
    DynamicDataPair(this, DynamicDataValue.fromBool(value))

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 200)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicColor>.mapTo(value: Color): DynamicDataPair<DynamicColor> =
    DynamicDataPair(this, DynamicDataValue.fromColor(value.toArgb()))

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 300)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicDuration>.mapTo(
    value: Duration
): DynamicDataPair<DynamicDuration> = DynamicDataPair(this, DynamicDataValue.fromDuration(value))

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 200)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicFloat>.mapTo(value: Float): DynamicDataPair<DynamicFloat> =
    DynamicDataPair(this, DynamicDataValue.fromFloat(value))

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 300)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicInstant>.mapTo(
    value: Instant
): DynamicDataPair<DynamicInstant> = DynamicDataPair(this, DynamicDataValue.fromInstant(value))

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 200)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicInt32>.mapTo(value: Int): DynamicDataPair<DynamicInt32> =
    DynamicDataPair(this, DynamicDataValue.fromInt(value))

/** Creates a tuple of type [DynamicDataPair] from this and [value]. */
@RequiresSchemaVersion(major = 1, minor = 200)
@JvmName("dynamicDataPairOf")
public infix fun DynamicDataKey<DynamicString>.mapTo(
    value: String
): DynamicDataPair<DynamicString> = DynamicDataPair(this, DynamicDataValue.fromString(value))
