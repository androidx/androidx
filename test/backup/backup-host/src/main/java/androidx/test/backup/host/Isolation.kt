/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

/**
 * Defines the sandbox isolation policy used between individual test methods.
 *
 * Policies can be used to control how the test environment is cleaned up between tests.
 */
public enum class IsolationPolicy {
    /**
     * Automatic isolation policy.
     *
     * This is the default framework behavior which invokes a pm clear between test methods to
     * ensure a clean state.
     */
    AUTOMATIC,

    /**
     * Manual isolation policy.
     *
     * This allows opting out of automatic cleanup to preserve sandbox state across test steps.
     */
    MANUAL,
}

/**
 * Annotation to override the default automatic clear data behavior of the framework.
 *
 * Apply this to a test class or test method to configure the sandbox isolation.
 *
 * @property value The isolation policy to apply. Defaults to [IsolationPolicy.AUTOMATIC].
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class Isolation(public val value: IsolationPolicy = IsolationPolicy.AUTOMATIC)
