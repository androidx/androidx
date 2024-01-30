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

package androidx;

import static androidx.annotation.RestrictTo.Scope.TESTS;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/** @noinspection unused*/
public class RestrictToTestsAnnotationUsageJava {
    @RestrictTo(androidx.annotation.RestrictTo.Scope.TESTS)
    public void testMethodFullyQualified() {}

    @RestrictTo(RestrictTo.Scope.TESTS)
    public void testMethodOuterClass() {}

    @RestrictTo(Scope.TESTS)
    public void testMethodInnerClass() {}

    @RestrictTo(TESTS)
    public void testMethodStaticImport() {}

    @RestrictTo({Scope.TESTS, Scope.LIBRARY})
    public void testMethodVarArg() {}
}
