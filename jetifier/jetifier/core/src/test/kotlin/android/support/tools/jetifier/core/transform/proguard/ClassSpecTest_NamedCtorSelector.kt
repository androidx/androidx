/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.proguard

import org.junit.Test

class ClassSpecTest_NamedCtorSelector {

    @Test fun proGuard_ctorSelector() {
        ProGuardTester
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  support.Activity(); \n" +
                "  support.Activity(...); \n" +
                "  support.Activity(*); \n" +
                "  support.Activity(support.Activity); \n" +
                "  support.Activity(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  test.Activity(); \n" +
                "  test.Activity(...); \n" +
                "  test.Activity(*); \n" +
                "  test.Activity(test.Activity); \n" +
                "  test.Activity(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_ctorSelector_modifiers() {
        ProGuardTester
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  public support.Activity(); \n" +
                "  public static support.Activity(...); \n" +
                "  !private support.Activity(*); \n" +
                "  !public !static support.Activity(support.Activity); \n" +
                "  !protected support.Activity(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  public test.Activity(); \n" +
                "  public static test.Activity(...); \n" +
                "  !private test.Activity(*); \n" +
                "  !public !static test.Activity(test.Activity); \n" +
                "  !protected test.Activity(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_ctorSelector_annotation() {
        ProGuardTester
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment",
                "support/Annotation" to "test/Annotation"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  @support.Annotation support.Activity(); \n" +
                "  @support.Annotation support.Activity(...); \n" +
                "  @support.Annotation support.Activity(*); \n" +
                "  @support.Annotation support.Activity(support.Activity); \n" +
                "  @support.Annotation support.Activity(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation test.Activity(); \n" +
                "  @test.Annotation test.Activity(...); \n" +
                "  @test.Annotation test.Activity(*); \n" +
                "  @test.Annotation test.Activity(test.Activity); \n" +
                "  @test.Annotation test.Activity(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_ctorSelector_modifiers_annotation() {
        ProGuardTester
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment",
                "support/Annotation" to "test/Annotation"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  @support.Annotation public support.Activity(); \n" +
                "  @support.Annotation public static support.Activity(...); \n" +
                "  @support.Annotation !private support.Activity(*); \n" +
                "  @support.Annotation !public !static support.Activity(support.Activity); \n" +
                "  @support.Annotation !protected support.Activity(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation public test.Activity(); \n" +
                "  @test.Annotation public static test.Activity(...); \n" +
                "  @test.Annotation !private test.Activity(*); \n" +
                "  @test.Annotation !public !static test.Activity(test.Activity); \n" +
                "  @test.Annotation !protected test.Activity(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_ctorSelector_modifiers_annotation_spaces() {
        ProGuardTester
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment",
                "support/Annotation" to "test/Annotation"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  @support.Annotation  !protected \t support.Activity( support.Activity ); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation  !protected \t test.Activity(test.Activity); \n" +
                "}"
            )
    }
}