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

class ProguardSamplesTest {

    @Test fun proGuard_sample() {
        ProGuardTester()
            .forGivenPrefixes(
                "android/app/",
                "android/view/",
                "android/content/",
                "android/os/",
                "android/webkit/"
            )
            .forGivenTypesMap(
                "android/app/Activity" to "test/app/Activity",
                "android/app/Application" to "test/app/Application",
                "android/view/View" to "test/view/View",
                "android/view/MenuItem" to "test/view/MenuItem",
                "android/content/Context" to "test/content/Context",
                "android/os/Parcelable" to "test/os/Parcelable",
                "android/webkit/JavascriptInterface" to "test/webkit/JavascriptInterface"
            )
            .testThatGivenProGuard(
                "-injars      bin/classes \n" +
                "-injars      libs \n" +
                "-outjars     bin/classes-processed.jar \n" +
                "-libraryjars /usr/local/java/android-sdk/platforms/android-9/android.jar \n" +
                "\n" +
                "-dontpreverify \n" +
                "-repackageclasses '' \n" +
                "-allowaccessmodification \n" +
                "-optimizations !code/simplification/arithmetic \n" +
                "-keepattributes *Annotation* \n" +
                "\n" +
                "-keep public class * extends android.app.Activity \n" +
                "-keep public class * extends android.app.Application \n" +
                " \n" +
                "-keep public class * extends android.view.View { \n" +
                "      public <init>(android.content.Context); \n" +
                "      public <init>(android.content.Context, android.util.AttributeSet); \n" +
                "      public <init>(android.content.Context, android.util.AttributeSet, int); \n" +
                "      public void set*(...); \n" +
                "} \n" +
                "\n" +
                "-keepclasseswithmembers class * { \n" +
                "    public <init>(android.content.Context, android.util.AttributeSet); \n" +
                "} \n" +
                "\n" +
                "-keepclasseswithmembers class * { \n" +
                "    public <init>(android.content.Context, android.util.AttributeSet, int); \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class * extends android.content.Context { \n" +
                "    public void *(android.view.View); \n" +
                "    public void *(android.view.MenuItem); \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class * implements android.os.Parcelable { \n" +
                "    static ** CREATOR; \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class **.R\$* { \n" +
                "    public static <fields>; \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class * { \n" +
                "    @android.webkit.JavascriptInterface <methods>; \n" +
                "} "
            )
            .rewritesTo(
                "-injars      bin/classes \n" +
                "-injars      libs \n" +
                "-outjars     bin/classes-processed.jar \n" +
                "-libraryjars /usr/local/java/android-sdk/platforms/android-9/android.jar \n" +
                "\n" +
                "-dontpreverify \n" +
                "-repackageclasses '' \n" +
                "-allowaccessmodification \n" +
                "-optimizations !code/simplification/arithmetic \n" +
                "-keepattributes *Annotation* \n" +
                "\n" +
                "-keep public class * extends test.app.Activity \n" +
                "-keep public class * extends test.app.Application \n" +
                " \n" +
                "-keep public class * extends test.view.View { \n" +
                "      public <init>(test.content.Context); \n" +
                "      public <init>(test.content.Context, android.util.AttributeSet); \n" +
                "      public <init>(test.content.Context, android.util.AttributeSet, int); \n" +
                "      public void set*(...); \n" +
                "} \n" +
                "\n" +
                "-keepclasseswithmembers class * { \n" +
                "    public <init>(test.content.Context, android.util.AttributeSet); \n" +
                "} \n" +
                "\n" +
                "-keepclasseswithmembers class * { \n" +
                "    public <init>(test.content.Context, android.util.AttributeSet, int); \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class * extends test.content.Context { \n" +
                "    public void *(test.view.View); \n" +
                "    public void *(test.view.MenuItem); \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class * implements test.os.Parcelable { \n" +
                "    static ** CREATOR; \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class **.R\$* { \n" +
                "    public static <fields>; \n" +
                "} \n" +
                "\n" +
                "-keepclassmembers class * { \n" +
                "    @test.webkit.JavascriptInterface <methods>; \n" +
                "} "
            )
    }

    @Test fun proGuard_sample2() {
        ProGuardTester()
            .forGivenPrefixes(
                "android/support/v7/"
            )
            .forGivenTypesMap(
                "android/support/v7/preference/Preference" to "test/Preference"
            )
            .testThatGivenProGuard(
                "-keep public class android.support.v7.preference.Preference {\n" +
                "  public <init>(android.content.Context, android.util.AttributeSet);\n" +
                "}\n" +
                "-keep public class * extends android.support.v7.preference.Preference {\n" +
                "  public <init>(android.content.Context, android.util.AttributeSet);\n" +
                "}"
            )
            .rewritesTo(
                "-keep public class test.Preference {\n" +
                "  public <init>(android.content.Context, android.util.AttributeSet);\n" +
                "}\n" +
                "-keep public class * extends test.Preference {\n" +
                "  public <init>(android.content.Context, android.util.AttributeSet);\n" +
                "}"
            )
    }

    @Test fun proGuard_sample3() {
        ProGuardTester()
            .forGivenPrefixes(
                "android/support/design/",
                "android/support/v7/"
            )
            .forGivenTypesMap(
                "support/Fragment" to "test/Fragment",
                "android/support/v7/widget/RoundRectDrawable" to "test/RoundRectDrawable"
            )
            .forGivenProGuardMap(
                "android/support/design.**" to "test/design.**",
                "android/support/design/R\$*" to "test/design/R\$*"
            )
            .testThatGivenProGuard(
                "-dontwarn android.support.design.**\n" +
                "-keep class android.support.design.** { *; }\n" +
                "-keep interface android.support.design.** { *; }\n" +
                "-keep public class android.support.design.R\$* { *; }\n" +
                "-keep class android.support.v7.widget.RoundRectDrawable { *; }"
            )
            .rewritesTo(
                "-dontwarn test.design.**\n" +
                "-keep class test.design.** { *; }\n" +
                "-keep interface test.design.** { *; }\n" +
                "-keep public class test.design.R\$* { *; }\n" +
                "-keep class test.RoundRectDrawable { *; }"
            )
    }

    @Test fun proGuard_sample4() {
        ProGuardTester()
            .forGivenPrefixes(
                "android/support/design/",
                "android/support/v7/",
                "android/support/v4/"
            )
            .forGivenTypesMap(
                "android/support/v7/widget/LinearLayoutManager" to "test/LinearLayoutManager",
                "android/support/v4/view/ActionProvider" to "test/ActionProvider"
            )
            .forGivenProGuardMap(
                "android/support/v7/**" to "test/v7/**",
                "android/support/v7/widget/**" to "test/v7/widget/**",
                "android/support/v7/internal/widget/**" to "test/v7/internal/widget/**",
                "android/support/v7/internal/**" to "test/v7/internal/**"
            )
            .testThatGivenProGuard(
                "-dontwarn android.support.v7.**\n" +
                "-keep public class android.support.v7.widget.** { *; }\n" +
                "-keep public class android.support.v7.internal.widget.** { *; }\n" +
                "-keep class android.support.v7.widget.LinearLayoutManager { *; }\n" +
                "-keep class android.support.v7.internal.** { *; }\n" +
                "-keep interface android.support.v7.internal.** { *; }\n" +
                "\n" +
                "-keep class android.support.v7.** { *; }\n" +
                "-keep interface android.support.v7.** { *; }\n" +
                "\n" +
                "-keep public class * extends android.support.v4.view.ActionProvider {\n" +
                "    public <init>(android.content.Context);"
            )
            .rewritesTo(
                "-dontwarn test.v7.**\n" +
                "-keep public class test.v7.widget.** { *; }\n" +
                "-keep public class test.v7.internal.widget.** { *; }\n" +
                "-keep class test.LinearLayoutManager { *; }\n" +
                "-keep class test.v7.internal.** { *; }\n" +
                "-keep interface test.v7.internal.** { *; }\n" +
                "\n" +
                "-keep class test.v7.** { *; }\n" +
                "-keep interface test.v7.** { *; }\n" +
                "\n" +
                "-keep public class * extends test.ActionProvider {\n" +
                "    public <init>(android.content.Context);"
            )
    }

    @Test fun proGuard_sample5() {
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
                "-keep public class * extends support.Activity { \n" +
                "  public static <fields>; \n" +
                "  public !static <methods>; \n" +
                "  public support.Fragment height; \n" +
                "  public static <fields>; \n" +
                "  public not.related.Type width; public support.Fragment width; \n" +
                "  ignoreMe; \n" +
                "  @support.Annotation public support.Fragment get(); \n" +
                "}\n" +
                "-dontwarn support.**"
            )
            .rewritesTo(
                "-keep public class * extends test.Activity { \n" +
                "  public static <fields>; \n" +
                "  public !static <methods>; \n" +
                "  public test.Fragment height; \n" +
                "  public static <fields>; \n" +
                "  public not.related.Type width; public test.Fragment width; \n" +
                "  ignoreMe; \n" +
                "  @test.Annotation public test.Fragment get(); \n" +
                "}\n" +
                "-dontwarn test.**"
            )
    }
}