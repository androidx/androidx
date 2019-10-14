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

class ClassSpecTest_FieldsSelector {

    @Test fun proGuard_fieldsSelector_minimal() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenProGuard(
                "-keep public class * extends support.Activity { \n" +
                "  <fields>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * extends test.Activity { \n" +
                "  <fields>; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldsSelector_modifiers() {
        ProGuardTester()
            .forGivenPrefixes()
            .forGivenTypesMap()
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  public <fields>; \n" +
                "  public static <fields>; \n" +
                "  !private !protected <fields>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  public <fields>; \n" +
                "  public static <fields>; \n" +
                "  !private !protected <fields>; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldsSelector_modifiers_annotation() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Annotation" to "test/Annotation"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  @support.Annotation public <fields>; \n" +
                "  @support.Annotation public static <fields>; \n" +
                "  @support.Annotation !private !protected <fields>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation public <fields>; \n" +
                "  @test.Annotation public static <fields>; \n" +
                "  @test.Annotation !private !protected <fields>; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldsSelector_modifiers_annotation_spaces() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Annotation" to "test/Annotation"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  @support.Annotation  public \t  <fields> ; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation  public \t  <fields> ; \n" +
                "}"
            )
    }

    @Test fun proGuard_fieldsSelector_multiple() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMapSet("support.**" to setOf("support.**", "androidx.**"))
            .testThatGivenProGuard(
                "-keep public class * extends support.** { \n" +
                "  <fields>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * extends support.** { \n" +
                "  <fields>; \n" +
                "}\n" +
                "-keep public class * extends androidx.** { \n" +
                "  <fields>; \n" +
                "}"
            )
    }
}