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

import org.junit.Test;

public class VisibilityUtilTest {
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
}
