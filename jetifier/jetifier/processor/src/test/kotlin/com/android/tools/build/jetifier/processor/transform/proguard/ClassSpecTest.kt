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

class ClassSpecTest {

    @Test fun proGuard_classSpec_simple() {
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
                "-keep class support.Activity"
            )
            .rewritesTo(
                "-keep class test.Activity"
            )
    }

    @Test fun proGuard_classSpec_allExistingRules() {
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
                "-keep class support.Activity \n" +
                "-keepclassmembers class support.Activity \n" +
                "-keepclasseswithmembers class support.Activity \n" +
                "-keepnames class support.Activity \n" +
                "-keepclassmembernames class support.Activity \n" +
                "-keepclasseswithmembernames class support.Activity \n" +
                "-whyareyoukeeping class support.Activity \n" +
                "-assumenosideeffects class support.Activity"
            )
            .rewritesTo(
                "-keep class test.Activity \n" +
                "-keepclassmembers class test.Activity \n" +
                "-keepclasseswithmembers class test.Activity \n" +
                "-keepnames class test.Activity \n" +
                "-keepclassmembernames class test.Activity \n" +
                "-keepclasseswithmembernames class test.Activity \n" +
                "-whyareyoukeeping class test.Activity \n" +
                "-assumenosideeffects class test.Activity"
            )
    }

    @Test fun proGuard_classSpec_rulesModifiers() {
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
                "-keep includedescriptorclasses class support.Activity \n" +
                "-keep allowshrinking class support.Activity \n" +
                "-keep allowoptimization class support.Activity \n" +
                "-keep allowobfuscation class support.Activity \n" +
                "-keep allowshrinking allowoptimization allowobfuscation class support.Activity" +
                " \n" +
                "-keep allowshrinking   allowoptimization   allowobfuscation  class " +
                "support.Activity"
            )
            .rewritesTo(
                "-keep includedescriptorclasses class test.Activity \n" +
                "-keep allowshrinking class test.Activity \n" +
                "-keep allowoptimization class test.Activity \n" +
                "-keep allowobfuscation class test.Activity \n" +
                "-keep allowshrinking allowoptimization allowobfuscation class test.Activity \n" +
                "-keep allowshrinking   allowoptimization   allowobfuscation  class test.Activity"
            )
    }

    @Test fun proGuard_classSpec_extends() {
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
                "-keep class * extends support.Activity \n" +
                "-keep class support.Fragment extends support.Activity"
            )
            .rewritesTo(
                "-keep class * extends test.Activity \n" +
                "-keep class test.Fragment extends test.Activity"
            )
    }

    @Test fun proGuard_classSpec_modifiers_extends() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenTypesMap(
                "support/Activity" to "test/Activity"
            )
            .testThatGivenProGuard(
                "-keep !public enum * extends support.Activity \n" +
                "-keep public !final enum * extends support.Activity"
            )
            .rewritesTo(
                "-keep !public enum * extends test.Activity \n" +
                "-keep public !final enum * extends test.Activity"
            )
    }

    @Test fun proGuard_classSpec_annotation() {
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
                "-keep @support.Annotation public class support.Activity \n" +
                "-keep @some.Annotation public class support.Activity"
            )
            .rewritesTo(
                "-keep @test.Annotation public class test.Activity \n" +
                "-keep @some.Annotation public class test.Activity"
            )
    }

    @Test fun proGuard_classSpec_annotation_extends() {
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
                "-keep @support.Annotation public class * extends support.Activity\n" +
                "-keep @some.Annotation !public class * extends support.Activity"
            )
            .rewritesTo(
                "-keep @test.Annotation public class * extends test.Activity\n" +
                "-keep @some.Annotation !public class * extends test.Activity"
            )
    }

    @Test fun proGuard_classSpec_annotation_extends_spaces() {
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
                "-keep \t @support.Annotation \t public  class  *  extends support.Activity"
            )
            .rewritesTo(
                "-keep \t @test.Annotation \t public  class  *  extends test.Activity"
            )
    }

    @Test fun proGuard_classSpec_multipleResults() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMapSet(
                "support.**" to setOf("support.**", "androidx.**"))
            .testThatGivenProGuard(
                "-keep class support.**"
            )
            .rewritesTo(
                "-keep class support.**\n" +
                "-keep class androidx.**"
            )
    }

    @Test fun proGuard_classSpec_annotation_multipleResults() {
        ProGuardTester()
            .forGivenPrefixes(
                "support/"
            )
            .forGivenProGuardMapSet(
                "support.Activity*" to setOf("test.Activity1*", "test.Activity2*"),
                "support.Annotation*" to setOf("test.Annotation1*", "test.Annotation2*"))
            .testThatGivenProGuard(
                "-keep @support.Annotation* public class * extends support.Activity*"
            )
            .rewritesTo(
                "-keep @test.Annotation1* public class * extends test.Activity1*\n" +
                "-keep @test.Annotation2* public class * extends test.Activity1*\n" +
                "-keep @test.Annotation1* public class * extends test.Activity2*\n" +
                "-keep @test.Annotation2* public class * extends test.Activity2*"
            )
    }
}