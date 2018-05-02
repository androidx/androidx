/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.proguard

import org.junit.Test

class ClassSpecTest_FieldTypeSelector {

    @Test fun proGuard_fieldTypeSelector() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  support.Activity height; \n" +
                "  support.Fragment *; \n" +
                "  keep.Me width; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  test.Activity height; \n" +
                "  test.Fragment *; \n" +
                "  keep.Me width; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldTypeSelector_modifiers() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  public support.Fragment height; \n" +
                "  !public !static support.Fragment height; \n" +
                "  !protected support.Fragment height; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  public test.Fragment height; \n" +
                "  !public !static test.Fragment height; \n" +
                "  !protected test.Fragment height; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldTypeSelector_annotation() {
        ProGuardTester()
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
                "  @support.Annotation support.Fragment height; \n" +
                "  @some.Annotation support.Fragment height; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation test.Fragment height; \n" +
                "  @some.Annotation test.Fragment height; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldTypeSelector_modifiers_annotation() {
        ProGuardTester()
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
                "  @support.Annotation public support.Fragment height; \n" +
                "  @support.Annotation !public !static support.Fragment height; \n" +
                "  @support.Annotation !protected volatile support.Fragment height; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation public test.Fragment height; \n" +
                "  @test.Annotation !public !static test.Fragment height; \n" +
                "  @test.Annotation !protected volatile test.Fragment height; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldTypeSelector_modifiers_annotation_spaces() {
        ProGuardTester()
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
                "  @support.Annotation  public  static \t support.Fragment  height ; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation  public  static \t test.Fragment  height ; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldTypeSelector_multiple() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMapSet("support.**" to setOf("support.**", "androidx.**"))
            .testThatGivenProGuard(
                "-keep public class support.** { \n" +
                "  public support.** height; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class support.** { \n" +
                "  public support.** height; \n" +
                "}\n" +
                "-keep public class androidx.** { \n" +
                "  public support.** height; \n" +
                "}\n" +
                "-keep public class support.** { \n" +
                "  public androidx.** height; \n" +
                "}\n" +
                "-keep public class androidx.** { \n" +
                "  public androidx.** height; \n" +
                "}"
            )
    }
}