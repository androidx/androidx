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

class ClassSpecTest_MethodSelectorWithReturnType {

    @Test fun proGuard_methodReturnTypeSelector() {
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
                "  void get*(); \n" +
                "  void get*(...); \n" +
                "  void get*(*); \n" +
                "  void get*(support.Activity); \n" +
                "  void get?(support.Activity); \n" +
                "  void get(support.Activity); \n" +
                "  void *(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  void get*(); \n" +
                "  void get*(...); \n" +
                "  void get*(*); \n" +
                "  void get*(test.Activity); \n" +
                "  void get?(test.Activity); \n" +
                "  void get(test.Activity); \n" +
                "  void *(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_voidResult() {
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
                "  void get(); \n" +
                "  void get(...); \n" +
                "  void get(*); \n" +
                "  void get(support.Activity); \n" +
                "  void get(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  void get(); \n" +
                "  void get(...); \n" +
                "  void get(*); \n" +
                "  void get(test.Activity); \n" +
                "  void get(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_starResult() {
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
                "  * get(); \n" +
                "  * get(...); \n" +
                "  * get(*); \n" +
                "  * get(support.Activity); \n" +
                "  * get(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  * get(); \n" +
                "  * get(...); \n" +
                "  * get(*); \n" +
                "  * get(test.Activity); \n" +
                "  * get(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_typeResult() {
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
                "  support.Fragment get(); \n" +
                "  support.Fragment get(...); \n" +
                "  support.Fragment get(*); \n" +
                "  support.Fragment get(support.Activity); \n" +
                "  support.Fragment get(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  test.Fragment get(); \n" +
                "  test.Fragment get(...); \n" +
                "  test.Fragment get(*); \n" +
                "  test.Fragment get(test.Activity); \n" +
                "  test.Fragment get(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_typeResult_wildcards() {
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
                "  support.Fragment get*(); \n" +
                "  support.Fragment get?(...); \n" +
                "  support.Fragment *(*); \n" +
                "  support.Fragment *(support.Activity); \n" +
                "  support.Fragment *(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  test.Fragment get*(); \n" +
                "  test.Fragment get?(...); \n" +
                "  test.Fragment *(*); \n" +
                "  test.Fragment *(test.Activity); \n" +
                "  test.Fragment *(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_typeResult_modifiers() {
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
                "  public support.Fragment get(); \n" +
                "  public static support.Fragment get(...); \n" +
                "  !public !static support.Fragment get(*); \n" +
                "  private support.Fragment get(support.Activity); \n" +
                "  public abstract support.Fragment get(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  public test.Fragment get(); \n" +
                "  public static test.Fragment get(...); \n" +
                "  !public !static test.Fragment get(*); \n" +
                "  private test.Fragment get(test.Activity); \n" +
                "  public abstract test.Fragment get(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_typeResult_annotation() {
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
                "  @support.Annotation support.Fragment get(); \n" +
                "  @support.Annotation support.Fragment get(...); \n" +
                "  @support.Annotation support.Fragment get(*); \n" +
                "  @keep.Me support.Fragment get(support.Activity); \n" +
                "  @support.Annotation support.Fragment get(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation test.Fragment get(); \n" +
                "  @test.Annotation test.Fragment get(...); \n" +
                "  @test.Annotation test.Fragment get(*); \n" +
                "  @keep.Me test.Fragment get(test.Activity); \n" +
                "  @test.Annotation test.Fragment get(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_typeResult_modifiers_annotation() {
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
                "  @support.Annotation public support.Fragment get(); \n" +
                "  @support.Annotation public static support.Fragment get(...); \n" +
                "  @support.Annotation !public !static support.Fragment get(*); \n" +
                "  @support.Annotation private support.Fragment get(support.Activity); \n" +
                "  @support.Annotation public abstract support.Fragment get(support.Activity, support.Fragment,  keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation public test.Fragment get(); \n" +
                "  @test.Annotation public static test.Fragment get(...); \n" +
                "  @test.Annotation !public !static test.Fragment get(*); \n" +
                "  @test.Annotation private test.Fragment get(test.Activity); \n" +
                "  @test.Annotation public abstract test.Fragment get(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodReturnTypeSelector_typeResult_modifiers_annotation_spaces() {
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
                "  @support.Annotation  support.Fragment \t get(support.Activity ,  support.Fragment ,  keep.Please) ; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation  test.Fragment \t get(test.Activity, test.Fragment, keep.Please) ; \n" +
                "}"
            )
    }
}