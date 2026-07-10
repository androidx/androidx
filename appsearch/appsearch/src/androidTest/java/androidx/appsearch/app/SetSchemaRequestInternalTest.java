/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.app;

import static androidx.appsearch.app.SetSchemaRequest.EXECUTE_APP_FUNCTIONS_SYSTEM;
import static androidx.appsearch.testutil.FrameworkFlagUtils.assumeFlagIsDisabled;
import static androidx.appsearch.testutil.FrameworkFlagUtils.assumeFlagIsEnabled;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsDisabled;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class SetSchemaRequestInternalTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testAddRequiredPermissionsForSchemaTypeVisibility_emptyPermissions() {
        // This test should only run in non-framework environments. The check for an empty
        // permission set is a client-side validation.
        // skip this validation when running in the framework environment to avoid
        // breaking compatibility.
        assumeTrue(AppSearchEnvironmentFactory.getEnvironmentInstance()
                .getEnvironment()
                != AppSearchEnvironment.FRAMEWORK_ENVIRONMENT);
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();
        SetSchemaRequest.Builder setSchemaRequestBuilder = new SetSchemaRequest.Builder()
                .addSchemas(schema);

        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
                () -> setSchemaRequestBuilder.addRequiredPermissionsForSchemaTypeVisibility(
                        "Schema", ImmutableSet.of()));
        assertThat(expected).hasMessageThat().contains(
                "The set of required permissions cannot be empty");
    }

    @Test
    @RequiresFlagsEnabled(
            androidx.appsearch.flags.appfunctions.Flags.FLAG_ENABLE_APP_FUNCTION_PERMISSION_V2)
    public void testSetExecuteAppFunctionsSystemPermissions() {
        assumeFlagIsEnabled(
                androidx.appsearch.flags.appfunctions.Flags.FLAG_ENABLE_APP_FUNCTION_PERMISSION_V2);
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("Schema").build())
                .addRequiredPermissionsForSchemaTypeVisibility(
                        "Schema",
                        ImmutableSet.of(EXECUTE_APP_FUNCTIONS_SYSTEM))
                .build();
        assertThat(request.getRequiredPermissionsForSchemaTypeVisibility())
                .containsExactly("Schema",
                        ImmutableSet.of(ImmutableSet.of(EXECUTE_APP_FUNCTIONS_SYSTEM)));
    }

    @Test
    @RequiresFlagsDisabled(
            androidx.appsearch.flags.appfunctions.Flags.FLAG_ENABLE_APP_FUNCTION_PERMISSION_V2)
    public void testSetExecuteAppFunctionsSystemPermissions_disabled() {
        assumeFlagIsDisabled(
                androidx.appsearch.flags.appfunctions.Flags.FLAG_ENABLE_APP_FUNCTION_PERMISSION_V2);
        assertThrows(IllegalArgumentException.class,
                () -> new SetSchemaRequest.Builder().addRequiredPermissionsForSchemaTypeVisibility(
                        "Schema", ImmutableSet.of(EXECUTE_APP_FUNCTIONS_SYSTEM)));
    }
}
