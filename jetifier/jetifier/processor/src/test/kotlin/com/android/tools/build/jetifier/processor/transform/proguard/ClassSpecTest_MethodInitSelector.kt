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

class ClassSpecTest_MethodInitSelector {

    @Test fun proGuard_methodsInitSelector() {
        ProGuardTester()
            .forGivenPrefixes()
            .forGivenTypesMap()
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  <methods>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  <methods>; \n" +
                "}"
            )
    }

    @Test fun proGuard_methodsInitSelector_modifiers() {
        ProGuardTester()
            .forGivenPrefixes()
            .forGivenTypesMap()
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  public <methods>; \n" +
                "  public static <methods>; \n" +
                "  public !static <methods>; \n" +
                "  !private static <methods>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  public <methods>; \n" +
                "  public static <methods>; \n" +
                "  public !static <methods>; \n" +
                "  !private static <methods>; \n" +
                "}"
            )
    }

    @Test fun proGuard_methodsInitSelector_modifiers_annotation() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Annotation" to "test/Annotation"
            )
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  @support.Annotation public <methods>; \n" +
                "  @support.Annotation public static <methods>; \n" +
                "  @support.Annotation public !static <methods>; \n" +
                "  @support.Annotation !private static <methods>; \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation public <methods>; \n" +
                "  @test.Annotation public static <methods>; \n" +
                "  @test.Annotation public !static <methods>; \n" +
                "  @test.Annotation !private static <methods>; \n" +
                "}"
            )
    }

    @Test fun proGuard_methodInitSelector() {
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
                "  <init>(); \n" +
                "  <init>(*); \n" +
                "  <init>(...); \n" +
                "  <init>(support.Activity); \n" +
                "  <init>(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  <init>(); \n" +
                "  <init>(*); \n" +
                "  <init>(...); \n" +
                "  <init>(test.Activity); \n" +
                "  <init>(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodInitSelector_modifiers() {
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
                "  public <init>(); \n" +
                "  public static <init>(*); \n" +
                "  !public !static <init>(...); \n" +
                "  !private static <init>(support.Activity); \n" +
                "  public !abstract <init>(support.Activity, support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  public <init>(); \n" +
                "  public static <init>(*); \n" +
                "  !public !static <init>(...); \n" +
                "  !private static <init>(test.Activity); \n" +
                "  public !abstract <init>(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodInitSelector_annotation() {
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
                "  @support.Annotation <init>(); \n" +
                "  @support.Annotation <init>(*); \n" +
                "  @support.Annotation <init>(...); \n" +
                "  @keep.Me <init>(support.Activity); \n" +
                "  @support.Annotation <init>(support.Activity, support.Fragment, keep.Please);" +
                " \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation <init>(); \n" +
                "  @test.Annotation <init>(*); \n" +
                "  @test.Annotation <init>(...); \n" +
                "  @keep.Me <init>(test.Activity); \n" +
                "  @test.Annotation <init>(test.Activity, test.Fragment, keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodInitSelector_modifiers_annotation() {
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
                "  @support.Annotation public <init>(); \n" +
                "  @support.Annotation public static <init>(*); \n" +
                "  @support.Annotation !public !static <init>(...); \n" +
                "  @support.Annotation !private static <init>(support.Activity); \n" +
                "  @support.Annotation public !abstract <init>(support.Activity, support.Fragment" +
                ", keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation public <init>(); \n" +
                "  @test.Annotation public static <init>(*); \n" +
                "  @test.Annotation !public !static <init>(...); \n" +
                "  @test.Annotation !private static <init>(test.Activity); \n" +
                "  @test.Annotation public !abstract <init>(test.Activity, test.Fragment, " +
                "keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodInitSelector_modifiers_annotation_test() {
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
                "  @support.Annotation  public  !abstract \t <init> ( support.Activity , " +
                "support.Fragment, keep.Please); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  @test.Annotation  public  !abstract \t <init> (test.Activity, test.Fragment, " +
                "keep.Please); \n" +
                "}"
            )
    }

    @Test fun proGuard_methodInitSelector_multiple() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMapSet("support.**" to setOf("support.**", "androidx.**"))
            .testThatGivenProGuard(
                "-keep public class * { \n" +
                "  <init>(support.**, support.**); \n" +
                "}"
            )
            .rewritesTo(
                "-keep public class * { \n" +
                "  <init>(support.**, support.**); \n" +
                "}\n" +
                "-keep public class * { \n" +
                "  <init>(androidx.**, support.**); \n" +
                "}\n" +
                "-keep public class * { \n" +
                "  <init>(support.**, androidx.**); \n" +
                "}\n" +
                "-keep public class * { \n" +
                "  <init>(androidx.**, androidx.**); \n" +
                "}"
            )
    }
}