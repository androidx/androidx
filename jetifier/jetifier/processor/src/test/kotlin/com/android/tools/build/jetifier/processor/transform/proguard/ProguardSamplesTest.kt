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
                """
                -injars      bin/classes
                -injars      libs
                -outjars     bin/classes-processed.jar
                -libraryjars /usr/local/java/android-sdk/platforms/android-9/android.jar

                -dontpreverify
                -repackageclasses ''
                -allowaccessmodification
                -optimizations !code/simplification/arithmetic
                -keepattributes *Annotation*

                -keep public class * extends android.app.Activity
                -keep public class * extends android.app.Application

                -keep public class * extends android.view.View {
                      public <init>(android.content.Context);
                      public <init>(android.content.Context, android.util.AttributeSet);
                      public <init>(android.content.Context, android.util.AttributeSet, int);
                      public void set*(...);
                }

                -keepclasseswithmembers class * {
                    public <init>(android.content.Context, android.util.AttributeSet);
                }

                -keepclasseswithmembers class * {
                    public <init>(android.content.Context, android.util.AttributeSet, int);
                }

                -keepclassmembers class * extends android.content.Context {
                    public void *(android.view.View);
                    public void *(android.view.MenuItem);
                }

                -keepclassmembers class * implements android.os.Parcelable {
                    static ** CREATOR;
                }

                -keepclassmembers class **.R$* {
                    public static <fields>;
                }

                -keepclassmembers class * {
                    @android.webkit.JavascriptInterface <methods>;
                }
                """.trimIndent()
            )
            .rewritesTo(
                """
                -injars      bin/classes
                -injars      libs
                -outjars     bin/classes-processed.jar
                -libraryjars /usr/local/java/android-sdk/platforms/android-9/android.jar

                -dontpreverify
                -repackageclasses ''
                -allowaccessmodification
                -optimizations !code/simplification/arithmetic
                -keepattributes *Annotation*

                -keep public class * extends test.app.Activity
                -keep public class * extends test.app.Application

                -keep public class * extends test.view.View {
                      public <init>(test.content.Context);
                      public <init>(test.content.Context, android.util.AttributeSet);
                      public <init>(test.content.Context, android.util.AttributeSet, int);
                      public void set*(...);
                }

                -keepclasseswithmembers class * {
                    public <init>(test.content.Context, android.util.AttributeSet);
                }

                -keepclasseswithmembers class * {
                    public <init>(test.content.Context, android.util.AttributeSet, int);
                }

                -keepclassmembers class * extends test.content.Context {
                    public void *(test.view.View);
                    public void *(test.view.MenuItem);
                }

                -keepclassmembers class * implements test.os.Parcelable {
                    static ** CREATOR;
                }

                -keepclassmembers class **.R$* {
                    public static <fields>;
                }

                -keepclassmembers class * {
                    @test.webkit.JavascriptInterface <methods>;
                }
                """.trimIndent()
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
                """
                -keep public class android.support.v7.preference.Preference {
                  public <init>(android.content.Context, android.util.AttributeSet);
                }
                -keep public class * extends android.support.v7.preference.Preference {
                  public <init>(android.content.Context, android.util.AttributeSet);
                }
                """.trimIndent()
            )
            .rewritesTo(
                """
                -keep public class test.Preference {
                  public <init>(android.content.Context, android.util.AttributeSet);
                }
                -keep public class * extends test.Preference {
                  public <init>(android.content.Context, android.util.AttributeSet);
                }
                """.trimIndent()
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
                """
                -dontwarn android.support.design.**
                -keep class android.support.design.** { *; }
                -keep interface android.support.design.** { *; }
                -keep public class android.support.design.R$* { *; }
                -keep class android.support.v7.widget.RoundRectDrawable { *; }
                """.trimIndent()
            )
            .rewritesTo(
                """
                -dontwarn test.design.**
                -keep class test.design.** { *; }
                -keep interface test.design.** { *; }
                -keep public class test.design.R$* { *; }
                -keep class test.RoundRectDrawable { *; }
                """.trimIndent()
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
                """
                -dontwarn android.support.v7.**
                -keep public class android.support.v7.widget.** { *; }
                -keep public class android.support.v7.internal.widget.** { *; }
                -keep class android.support.v7.widget.LinearLayoutManager { *; }
                -keep class android.support.v7.internal.** { *; }
                -keep interface android.support.v7.internal.** { *; }

                -keep class android.support.v7.** { *; }
                -keep interface android.support.v7.** { *; }

                -keep public class * extends android.support.v4.view.ActionProvider {
                    public <init>(android.content.Context);
                """.trimIndent()
            )
            .rewritesTo(
                """
                -dontwarn test.v7.**
                -keep public class test.v7.widget.** { *; }
                -keep public class test.v7.internal.widget.** { *; }
                -keep class test.LinearLayoutManager { *; }
                -keep class test.v7.internal.** { *; }
                -keep interface test.v7.internal.** { *; }

                -keep class test.v7.** { *; }
                -keep interface test.v7.** { *; }

                -keep public class * extends test.ActionProvider {
                    public <init>(android.content.Context);
                """.trimIndent()
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
                """
                -keep public class * extends support.Activity {
                  public static <fields>;
                  public !static <methods>;
                  public support.Fragment height;
                  public static <fields>;
                  public not.related.Type width; public support.Fragment width;
                  ignoreMe;
                  @support.Annotation public support.Fragment get();
                }
                -dontwarn support.**
                """.trimIndent()
            )
            .rewritesTo(
                """
                -keep public class * extends test.Activity {
                  public static <fields>;
                  public !static <methods>;
                  public test.Fragment height;
                  public static <fields>;
                  public not.related.Type width; public test.Fragment width;
                  ignoreMe;
                  @test.Annotation public test.Fragment get();
                }
                -dontwarn test.**
                """.trimIndent()
            )
    }

    @Test fun proGuard_sample6() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenProGuard(
                "# Comment\n" +
                    "-keep class * extends com.google.Class { *; }\n" +
                    "-dontwarn com.google.**\n" +
                    // Comment should not break the transformer
                    "# Note: comment 'com.google.android.Class\$Nested {\n" +
                    "-keep class com.google.android.Class\$*\n"
            )
            .rewritesTo(
                "# Comment\n" +
                    "-keep class * extends com.google.Class { *; }\n" +
                    "-dontwarn com.google.**\n" +
                    "# Note: comment 'com.google.android.Class\$Nested {\n" +
                    "-keep class com.google.android.Class\$*\n"
            )
    }

    @Test fun proGuard_sample7() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenProGuard(
                "-dontwarn support.Activity,\n" +
                    "support.Activity\n" + // New line should not be rewritten
                    "-dontwarn support.Activity\n" +
                    "support.Activity" // New line should not be rewritten
            )
            .rewritesTo(
                """
                -dontwarn test.Activity
                support.Activity
                -dontwarn test.Activity
                support.Activity
                """.trimIndent()
            )
    }

    @Test fun proGuard_sample8() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                """
                -keep public class * {
                  void get(*);
                  void get(support.Activity);
                #  void get(support.Activity, support.Fragment, keep.Please);
                }
                """.trimIndent()
            )
            .rewritesTo(
                """
                -keep public class * {
                  void get(*);
                  void get(support.Activity);
                #  void get(support.Activity, support.Fragment, keep.Please);
                }
                """.trimIndent()
            )
    }

    @Test fun proGuard_sample9() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity",
                "support/Fragment" to "test/Fragment"
            )
            .testThatGivenProGuard(
                """
                -keep public class * {
                  void get(*);
                  void get(#support.Activity);
                  void get(support.Activity, support.Fragment, keep.Please);
                }
                """.trimIndent()
            )
            .rewritesTo(
                """
                -keep public class * {
                  void get(*);
                  void get(#support.Activity);
                  void get(support.Activity, support.Fragment, keep.Please);
                }
                """.trimIndent()
            )
    }
}
