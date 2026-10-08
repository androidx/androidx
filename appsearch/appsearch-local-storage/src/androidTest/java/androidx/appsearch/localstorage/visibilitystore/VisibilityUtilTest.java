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

package androidx.appsearch.localstorage.visibilitystore;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class VisibilityUtilTest {
    @Rule public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testIsSchemaSearchableByCaller_selfAccessDefaultAllowed() {
        CallerAccess callerAccess = new CallerAccess("package1");
        assertThat(VisibilityUtil.isSchemaSearchableByCaller(callerAccess,
                /*targetPackageName=*/ "package1",
                /*prefixedSchema=*/ "schema",
                /*visibilityStore=*/ null,
                /*visibilityChecker=*/ null)).isTrue();
        assertThat(VisibilityUtil.isSchemaSearchableByCaller(callerAccess,
                /*targetPackageName=*/ "package2",
                /*prefixedSchema=*/ "schema",
                /*visibilityStore=*/ null,
                /*visibilityChecker=*/ null)).isFalse();
    }

    @Test
    public void testIsSchemaSearchableByCaller_selfAccessNotAllowed() {
        CallerAccess callerAccess = new CallerAccess("package1") {
            @Override
            public boolean doesCallerHaveSelfAccess() {
                return false;
            }
        };
        assertThat(VisibilityUtil.isSchemaSearchableByCaller(callerAccess,
                /*targetPackageName=*/ "package1",
                /*prefixedSchema=*/ "schema",
                /*visibilityStore=*/ null,
                /*visibilityChecker=*/ null)).isFalse();
        assertThat(VisibilityUtil.isSchemaSearchableByCaller(callerAccess,
                /*targetPackageName=*/ "package2",
                /*prefixedSchema=*/ "schema",
                /*visibilityStore=*/ null,
                /*visibilityChecker=*/ null)).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PCC_DATA_ENCAPSULATION)
    public void testIsSchemaSearchableByCaller_delegatesToVisibilityChecker() {
        CallerAccess callerAccess = new CallerAccess("package");
        VisibilityStore mockVisibilityStore = mock(VisibilityStore.class);
        VisibilityChecker mockVisibilityChecker = mock(VisibilityChecker.class);

        when(mockVisibilityChecker.isSchemaSearchableByCaller(
                        eq(callerAccess),
                        eq("package"),
                        eq("schema"),
                        eq(mockVisibilityStore)))
                .thenReturn(false);

        // Even with same package, when visibilityChecker is provided, it delegates to checker.
        assertThat(
                        VisibilityUtil.isSchemaSearchableByCaller(
                                callerAccess,
                                /* targetPackageName= */ "package",
                                /* prefixedSchema= */ "schema",
                                mockVisibilityStore,
                                mockVisibilityChecker))
                .isFalse();
        verify(mockVisibilityChecker)
                .isSchemaSearchableByCaller(
                        eq(callerAccess),
                        eq("package"),
                        eq("schema"),
                        eq(mockVisibilityStore));
    }
}
