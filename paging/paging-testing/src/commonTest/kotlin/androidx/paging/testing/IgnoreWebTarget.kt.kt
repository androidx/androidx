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

package androidx.paging.testing

/**
 * Ignores a test on Kotlin/JS and Kotlin/Wasm targets.
 *
 * This annotation is a temporary measure to exclude tests in `commonTest` that currently fail on
 * Web-based platforms (JS and Wasm). Many of these tests were written before official support for
 * these targets and rely on assumptions that do not hold true in a web environment.
 *
 * TODO: Tests involving coroutines are timing out in JS, diagnosing the reason for timeout is
 *   difficult in JS where debugging and logs don't work by default. Remove this when we can figure
 *   out the reason and refactor the tests accordingly.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation // Allows the annotation to be absent on some platforms [1]
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
) // Apply to test classes or individual test functions [2]
internal expect annotation class IgnoreWebTarget()
