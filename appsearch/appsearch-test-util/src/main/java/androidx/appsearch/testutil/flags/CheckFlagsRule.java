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
package androidx.appsearch.testutil.flags;

import static org.junit.Assume.assumeTrue;

import android.annotation.SuppressLint;

import androidx.appsearch.flags.Flags;
import androidx.collection.ArrayMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Shim for real CheckFlagsRule defined in Framework.
 *
 * <p>In Jetpack, this shim only handles invocations for {@link RequiresFlagsEnabled} and
 * {@link RequiresFlagsDisabled}. This rule does two things:
 * <ul>
 *     <li>checks that all {@link RequiresFlagsEnabled} and {@link RequiresFlagsDisabled}
 *     annotations do not conflict.</li>
 *     <li>skips any test/test class with any required flag different from the actual flag.</li>
 * </ul>
 */
public final class CheckFlagsRule implements TestRule {
    @Override
    public @NonNull Statement apply(@NonNull Statement base, @Nullable Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Map<String, Boolean> requiredFlagValues = getMergedFlagValues(description);
                checkFlags(requiredFlagValues);
                base.evaluate();
            }
        };
    }

    /**
     * Checks that the only required flag values specified are from {@link RequiresFlagsEnabled}.
     * The presence of any flag value specific in {@link RequiresFlagsDisabled} will result in the
     * test being skipped.
     */
    @SuppressLint("BanUncheckedReflection") // No public APIs are called, and for test only.
    private static void checkFlags(@NonNull Map<String, Boolean> requiredFlagValues) {
        for (Map.Entry<String, Boolean> required : requiredFlagValues.entrySet()) {
            final String flag = required.getKey();
            final Boolean requiredFlagValue = required.getValue();

            // Parse the flag name, convert to the static method name and invoke the method (of
            // Flags class) to get the actual flag value.
            Boolean actualFlagValue;
            try {
                Method flagMethod = Flags.class.getMethod(getFlagMethodName(flag));
                actualFlagValue = (Boolean) flagMethod.invoke(null);
            } catch (Exception e) {
                // Failed to get the actual flag value. Set it to true.
                // Note: some of flags are missing the static methods. Making it default true will
                // keep the original behavior:
                // - Tests with @RequiresFlagsDisabled will be skipped.
                // - Tests with @RequiresFlagsEnabled will run.
                //
                // TODO(b/491117995): fix missing static methods for all flags and throw
                //   RuntimeException here.
                actualFlagValue = true;
            }

            // Compare the required value and actual value. Skip the test if they are not equal.
            assumeTrue(
                    String.format("Flag %s does not meet the requirement", flag),
                    requiredFlagValue.equals(actualFlagValue));
        }
    }

    /**
     * Retrieves the value of all {@link RequiresFlagsEnabled} and {@link RequiresFlagsDisabled} for
     * both the test class and the test method.
     *
     * @throws AssertionError - if the RequiresFlag annotations conflict with each other.
     * @return a map holding the flag values and whether they are required to be enabled or
     * disabled.
     */
    private static @NonNull Map<String, Boolean> getMergedFlagValues(
            @NonNull Description description) {
        final Map<String, Boolean> flagValues = new ArrayMap<>();
        getFlagValuesFromAnnotations(description.getMethodName(), description.getAnnotations(),
                flagValues);
        Class<?> testClass = description.getTestClass();
        if (testClass != null) {
            getFlagValuesFromAnnotations(testClass.getName(), List.of(testClass.getAnnotations()),
                    flagValues);
        }
        return flagValues;
    }

    private static void getFlagValuesFromAnnotations(
            @NonNull String annotationTarget,
            @NonNull Collection<Annotation> annotations,
            @NonNull Map<String, Boolean> flagValues) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequiresFlagsEnabled) {
                RequiresFlagsEnabled enabled = (RequiresFlagsEnabled) annotation;
                addFlagValues(annotationTarget, enabled.value(), Boolean.TRUE, flagValues);
            } else if (annotation instanceof RequiresFlagsDisabled) {
                RequiresFlagsDisabled disabled = (RequiresFlagsDisabled) annotation;
                addFlagValues(annotationTarget, disabled.value(), Boolean.FALSE, flagValues);
            }
        }
    }

    private static void addFlagValues(@NonNull String annotationTarget, @NonNull String[] flags,
            @NonNull Boolean value, @NonNull Map<String, Boolean> flagValues) {
        for (String flagName : flags) {
            Boolean existingValue = flagValues.get(flagName);
            if (existingValue == null) {
                flagValues.put(flagName, value);
            } else if (!existingValue.equals(value)) {
                throw new AssertionError(
                        "Flag '" + flagName + "' are required by " + annotationTarget
                                + " to be both enabled and disabled.");
            }
        }
    }

    private static String getFlagMethodName(@NonNull String flagName)
            throws IllegalArgumentException {
        if (!flagName.startsWith(Flags.FLAG_PREFIX)) {
            throw new IllegalArgumentException("Invalid flag name");
        }
        StringBuilder methodNameBuilder = new StringBuilder();
        for (int i = Flags.FLAG_PREFIX.length(); i < flagName.length(); ++i) {
            if (flagName.charAt(i) == '_') {
                if (i + 1 >= flagName.length()) {
                    throw new IllegalArgumentException("Invalid flag name ending with underscore");
                }
                methodNameBuilder.append(Character.toUpperCase(flagName.charAt(i + 1)));
                i += 1;
            } else {
                methodNameBuilder.append(flagName.charAt(i));
            }
        }
        return methodNameBuilder.toString();
    }
}
