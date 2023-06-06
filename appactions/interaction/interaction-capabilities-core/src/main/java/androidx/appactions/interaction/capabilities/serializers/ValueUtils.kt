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

package androidx.appactions.interaction.capabilities.serializers

import androidx.appactions.interaction.protobuf.ListValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value

fun stringValue(string: String): Value {
  return Value.newBuilder().setStringValue(string).build()
}

fun numberValue(number: Double): Value {
  return Value.newBuilder().setNumberValue(number).build()
}

fun boolValue(bool: Boolean): Value {
  return Value.newBuilder().setBoolValue(bool).build()
}

fun structValue(jsonObj: Struct): Value {
  return Value.newBuilder().setStructValue(jsonObj).build()
}

fun listValue(elements: Iterable<Value>): Value {
  return Value.newBuilder()
      .setListValue(ListValue.newBuilder().addAllValues(elements).build())
      .build()
}

/**
 * Returns the string value if the [Value] holds a string or the first element in the list if
 * the [Value] holds a list with a single string element.
 *
 * ```
 * "Hello" -> "Hello"
 * ["Hello"] -> "Hello"
 * ["Hello", "World"] -> null
 * {...} -> null
 * ```
 *
 * This behavior can provide a bit of extra forward-compat where the JSON value originated from
 * a newer process where it has been promoted to be repeated.
 */
val Value.singleStringValue: String?
  get() {
    return when {
      hasStringValue() -> stringValue
      hasListValue() ->
          listValue.valuesList.onlyElementOrNull
              ?.let { if (it.hasStringValue()) it.stringValue else null }
      else -> null
    }
  }

/**
 * Returns the number value if the [Value] holds a number or the first element in the list if
 * the [Value] holds a list with a single number element.
 *
 * ```
 * 123 -> 123
 * [123] -> 123
 * [123, 456] -> null
 * {...} -> null
 * ```
 *
 * This behavior can provide a bit of extra forward-compat where the JSON value originated from
 * a newer process where it has been promoted to be repeated.
 */
val Value.singleNumberValue: Double?
    get() {
        return when {
            hasNumberValue() -> numberValue
            hasListValue() ->
                listValue.valuesList.onlyElementOrNull
                    ?.let { if (it.hasNumberValue()) it.numberValue else null }
            else -> null
        }
    }

/**
 * Returns the bool value if the [Value] holds a bool or the first element in the list if
 * the [Value] holds a list with a single bool element.
 *
 * ```
 * true -> true
 * [true] -> true
 * [true, false] -> null
 * {...} -> null
 * ```
 *
 * This behavior can provide a bit of extra forward-compat where the JSON value originated from
 * a newer process where it has been promoted to be repeated.
 */
val Value.singleBoolValue: Boolean?
    get() {
        return when {
            hasBoolValue() -> boolValue
            hasListValue() ->
                listValue.valuesList.onlyElementOrNull
                    ?.let { if (it.hasBoolValue()) it.boolValue else null }
            else -> null
        }
    }

private val <T> List<T>.onlyElementOrNull: T?
    get() = if (size == 1) first() else null
