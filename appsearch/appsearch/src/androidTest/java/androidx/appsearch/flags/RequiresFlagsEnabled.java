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
// @exportToFramework:skipFile()
package androidx.appsearch.flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shim for real RequiresFlagsEnabled defined in Framework.
 *
 * <p>In Jetpack, this shim does nothing and exists only for code sync purpose.
 *
 * <p>In Framework, indicates that a specific test or class should be run only if all of the given
 * feature flags are enabled in the device's current state. Enforced by the {@code CheckFlagsRule}.
 *
 * <p>This annotation works together with RequiresFlagsDisabled to define the value that is
 * required of the flag by the test for the test to run. It is an error for either a method or class
 * to require that a particular flag be both enabled and disabled.
 *
 * <p>If the value of a particular flag is required (by either {@code RequiresFlagsEnabled} or
 * {@code RequiresFlagsDisabled}) by both the class and test method, then the values must be
 * consistent.
 *
 * <p>If the value of a one flag is required by an annotation on the class, and the value of a
 * different flag is required by an annotation of the method, then both requirements apply.
 *
 * <p>With {@code CheckFlagsRule}, test(s) will be skipped with 'assumption failed' when any of the
 * required flag on the target Android platform is disabled.
 *
 * <p>Both {@code SetFlagsRule} and {@code CheckFlagsRule} will fail the test if a particular flag
 * is both set (with {@code EnableFlags} or {@code DisableFlags}) and required (with {@code
 * RequiresFlagsEnabled} or {@code RequiresFlagsDisabled}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresFlagsEnabled {
    /**
     * The list of the feature flags that require to be enabled. Each item is the full flag name
     * with the format {package_name}.{flag_name}.
     */
    String[] value();
}
