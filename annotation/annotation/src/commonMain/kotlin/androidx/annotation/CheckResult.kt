/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.annotation

/**
 * Denotes that the annotated method returns a result that it typically is an error to ignore. This
 * is usually used for methods that have no side effect, so calling it without actually looking at
 * the result usually means the developer has misunderstood what the method does.
 *
 * Example:
 * ```
 * public @CheckResult String trim(String s) { return s.trim(); }
 * ...
 * trim(s); // this is probably an error
 * s = trim(s); // ok
 * ```
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
public annotation class CheckResult(
    /**
     * Defines the name of the suggested method to use instead, if applicable (using the same
     * signature format as javadoc.) If there is more than one possibility, list them all separated
     * by commas.
     *
     * For example, ProcessBuilder has a method named `redirectErrorStream()` which sounds like it
     * might redirect the error stream. It does not. It's just a getter which returns whether the
     * process builder will redirect the error stream, and to actually set it, you must call
     * `redirectErrorStream(boolean)`. In that case, the method should be defined like this:
     * ```
     * @CheckResult(suggest="#redirectErrorStream(boolean)")
     * public boolean redirectErrorStream() { ... }
     * ```
     */
    val suggest: String = ""
)
