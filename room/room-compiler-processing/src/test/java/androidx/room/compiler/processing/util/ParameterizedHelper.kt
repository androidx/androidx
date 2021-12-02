/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

/**
 * Used to generate all argument enumerations for Parameterized tests.
 * See [ParameterizedHelperTest] for usage.
 */
fun generateAllEnumerations(vararg args: List<Any?>): List<Array<Any?>> =
    when (args.size) {
        0 -> emptyList()
        1 -> args[0].map {
            arrayOf(it)
        }
        else -> generateAllEnumerations(
            *args.dropLast(1).toTypedArray()
        ).flatMap { prev ->
            args.last().map { arg ->
                prev + arg
            }
        }
    }
