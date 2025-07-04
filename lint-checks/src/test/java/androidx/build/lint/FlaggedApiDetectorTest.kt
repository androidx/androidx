/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class FlaggedApiDetectorTest : LintDetectorTest() {
    override fun getIssues(): List<Issue> = listOf(FlaggedApiDetector.ISSUE)

    override fun getDetector(): Detector = FlaggedApiDetector()

    override fun lint(): TestLintTask {
        return super.lint().allowMissingSdk()
    }

    fun testBasic() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi("test.pkg.myFlag")
                class Foo {
                    public void someMethod() { }
                }

                public void testValid1() {
                    if (Flags.myFlag()) {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testApiGating() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                interface MyInterface {
                    void bar();
                }

                static class OldImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                }

                @FlaggedApi("test.pkg.myFlag")
                static class NewImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                 }

                 void test(MyInterface f) {
                     MyInterface obj = null;
                     if (Flags.myFlag()) {
                         obj = new NewImpl();
                     } else {
                         obj = new OldImpl();
                     }
                     f.bar();
                 }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testApiGating_fromFiles() {
        lint()
            .files(
                javaSample("android.flagging.FlaggedApiContainer"),
                ktSample("flaggedapi.FlaggedUsageInOutline"),
                Stubs.FlaggedApi,
                Stubs.RequiresAconfigFlag,
                Stubs.Flags,
            )
            .run()
            .expect(
                """
                src/flaggedapi/FlaggedUsageInOutline.kt:32: Error: Class FlagFlaggedApiImpl is a flagged API and must be inside a flag check for "flaggedapi.myFlag" [AndroidXFlaggedApi]
                        FlagFlaggedApiImpl.innerApi()
                        ~~~~~~~~~~~~~~~~~~
                src/flaggedapi/FlaggedUsageInOutline.kt:32: Error: Method innerApi() is a flagged API and must be inside a flag check for "flaggedapi.myFlag" [AndroidXFlaggedApi]
                        FlagFlaggedApiImpl.innerApi()
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors
                """
                    .trimIndent()
            )
    }

    fun testApiGating_withOutlining_isClean() {
        lint()
            .files(
                java(
                    """
            package android.os;

            import android.annotation.FlaggedApi;

            public class Build {
                @FlaggedApi("android.os.flaggedApi")
                public static boolean flaggedApi() { return true; }
            }
            """
                ),
                java(
                        """
            package test.pkg;

            import android.os.Build;
            import androidx.core.flagging.Flags;
            import androidx.annotation.RequiresAconfigFlag;

            public class JavaTest {
                void test() {
                    if (Flags.getBooleanFlagValue("android.os", "flaggedApi")) {
                        FlagFlaggedApiImpl.flaggedApi();
                    }
                }

                @RequiresAconfigFlag("android.os.flaggedApi")
                static class FlagFlaggedApiImpl {
                    static boolean flaggedApi() {
                        return Build.flaggedApi();
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.RequiresAconfigFlag,
                Stubs.Flags,
            )
            .run()
            .expectClean()
    }

    fun testApiGating_withOutliningNoRequires_raisesError() {
        lint()
            .files(
                java(
                    """
            package android.os;

            import android.annotation.FlaggedApi;

            public class Build {
                @FlaggedApi("android.os.flaggedApi")
                public static boolean flaggedApi() { return true; }
            }
            """
                ),
                java(
                        """
            package test.pkg;

            import android.os.Build;
            import androidx.core.flagging.Flags;
            import androidx.annotation.RequiresAconfigFlag;

            public class JavaTest {
                void test() {
                    if (Flags.getBooleanFlagValue("android.os", "flaggedApi")) {
                        FlagFlaggedApiImpl.flaggedApi();
                    }
                }

                static class FlagFlaggedApiImpl {
                    static boolean flaggedApi() {
                        return Build.flaggedApi();
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.RequiresAconfigFlag,
                Stubs.Flags,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaTest.java:16: Error: Method flaggedApi() is a flagged API and must be inside a flag check for "android.os.flaggedApi" [AndroidXFlaggedApi]
                            return Build.flaggedApi();
                                   ~~~~~~~~~~~~~~~~~~
                1 error
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/test/pkg/JavaTest.java line 16: Wrap with flag check:
                @@ -16 +16
                -             return Build.flaggedApi();
                +             if (androidx.core.flagging.Flags.getBooleanFlagValue("android.os", "flaggedApi")) { return Build.flaggedApi(); } else { throw new RuntimeException("TODO: Implement fallback behavior"); }
                """
                    .trimIndent()
            )
    }

    fun testApiGating_withOutliningWrongRequires_raisesError() {
        lint()
            .files(
                java(
                    """
            package android.os;

            import android.annotation.FlaggedApi;

            public class Build {
                @FlaggedApi("android.os.flaggedApi")
                public static boolean flaggedApi() { return true; }
            }
            """
                ),
                java(
                        """
            package test.pkg;

            import android.os.Build;
            import androidx.core.flagging.Flags;
            import androidx.annotation.RequiresAconfigFlag;

            public class JavaTest {
                void test() {
                    if (Flags.getBooleanFlagValue("android.os", "flaggedApi")) {
                        FlagFlaggedApiImpl.flaggedApi();
                    }
                }

                @RequiresAconfigFlag("android.os.otherFlaggedApi")
                static class FlagFlaggedApiImpl {
                    static boolean flaggedApi() {
                        return Build.flaggedApi();
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.RequiresAconfigFlag,
                Stubs.Flags,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaTest.java:10: Error: Method flaggedApi() is a flagged API and must be inside a flag check for "android.os.otherFlaggedApi" [AndroidXFlaggedApi]
                            FlagFlaggedApiImpl.flaggedApi();
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/JavaTest.java:17: Error: Method flaggedApi() is a flagged API and must be inside a flag check for "android.os.flaggedApi" [AndroidXFlaggedApi]
                            return Build.flaggedApi();
                                   ~~~~~~~~~~~~~~~~~~
                2 errors
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/test/pkg/JavaTest.java line 10: Wrap with flag check:
                @@ -10 +10
                -             FlagFlaggedApiImpl.flaggedApi();
                +             if (androidx.core.flagging.Flags.getBooleanFlagValue("android.os", "otherFlaggedApi")) { FlagFlaggedApiImpl.flaggedApi(); } else { throw new RuntimeException("TODO: Implement fallback behavior"); }
                Fix for src/test/pkg/JavaTest.java line 17: Wrap with flag check:
                @@ -17 +17
                -             return Build.flaggedApi();
                +             if (androidx.core.flagging.Flags.getBooleanFlagValue("android.os", "flaggedApi")) { return Build.flaggedApi(); } else { throw new RuntimeException("TODO: Implement fallback behavior"); }
                """
                    .trimIndent()
            )
    }

    fun testChecksAconfigFlagGating_javaIfCheck_isClean() {
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class FlaggedApiContainer {
                    @FlaggedApi("test.pkg.myFlag")
                    public static void flaggedApi() {
                    }
                }

                void callFlaggedApi(MyInterface f) {
                   if (FlagsCompat.myFlag()) {
                       FlaggedApiContainer.flaggedApi();
                   }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testChecksAconfigFlagGating_kotlinIfCheck_isClean() {
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                kotlin(
                        """
                    package test.pkg
        
                    import android.annotation.FlaggedApi
        
                    class KotlinTest {
                        object FlaggedApiContainer {
                            @FlaggedApi("test.pkg.myFlag")
                            fun flaggedApi() {
                            }
                        }
        
                        fun callFlaggedApi() =
                            if (FlagsCompat.myFlag()) {
                                FlaggedApiContainer.flaggedApi()
                            }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testChecksAconfigFlagGating_kotlinIncorrectWhenCheck_raisesError() {
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                kotlin(
                        """
                    package test.pkg
        
                    import android.annotation.FlaggedApi
        
                    class KotlinTest {
                        object FlaggedApiContainer {
                            @FlaggedApi("test.pkg.myFlag")
                            fun flaggedApi() {
                            }
                        }
        
                        fun callFlaggedApi() =
                            when {
                                FlagsCompat.myFlag() ->
                                    println("")
                                else ->
                                    FlaggedApiContainer.flaggedApi()
                            }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expect(
                """
        src/test/pkg/KotlinTest.kt:17: Error: Method flaggedApi() is a flagged API and must be inside a flag check for "test.pkg.myFlag" [AndroidXFlaggedApi]
                        FlaggedApiContainer.flaggedApi()
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
            )
            .expectFixDiffs(
                """
                Fix for src/test/pkg/KotlinTest.kt line 17: Wrap with flag check:
                @@ -17 +17
                -                 FlaggedApiContainer.flaggedApi()
                +                 if (androidx.core.flagging.Flags.getBooleanFlagValue("test.pkg", "myFlag")) { FlaggedApiContainer.flaggedApi() } else { TODO("Implement fallback behavior") }
                """
                    .trimIndent()
            )
    }

    fun testChecksAconfigFlagGating_javaCheckForWrongFlag_raisesError() {
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myOtherFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class FlaggedApiContainer {
                    @FlaggedApi("test.pkg.myFlag")
                    public static void flaggedApi() {
                    }
                }

                void callFlaggedApi(MyInterface f) {
                    if (FlagsCompat.INSTANCE.flaggedApi()) {
                       FlaggedApiContainer.flaggedApi();
                   }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaTest.java:14: Error: Method flaggedApi() is a flagged API and must be inside a flag check for "test.pkg.myFlag" [AndroidXFlaggedApi]
                   FlaggedApiContainer.flaggedApi();
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
            )
            .expectFixDiffs(
                """
                Fix for src/test/pkg/JavaTest.java line 14: Wrap with flag check:
                @@ -14 +14
                -            FlaggedApiContainer.flaggedApi();
                +            if (androidx.core.flagging.Flags.getBooleanFlagValue("test.pkg", "myFlag")) { FlaggedApiContainer.flaggedApi(); } else { throw new RuntimeException("TODO: Implement fallback behavior"); }
                """
                    .trimIndent()
            )
    }

    fun testChecksAconfigFlagGating_kotlinCheckForWrongFlag_raisesError() {
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myOtherFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                kotlin(
                        """
                    package test.pkg

                    import android.annotation.FlaggedApi

                    class KotlinTest {
                        object FlaggedApiContainer {
                            @FlaggedApi("test.pkg.myFlag")
                            fun flaggedApi() {
                            }
                        }

                        fun callFlaggedApi() =
                            if (FlagsCompat.myFlag()) {
                                FlaggedApiContainer.flaggedApi()
                            }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expect(
                """
        src/test/pkg/KotlinTest.kt:14: Error: Method flaggedApi() is a flagged API and must be inside a flag check for "test.pkg.myFlag" [AndroidXFlaggedApi]
                    FlaggedApiContainer.flaggedApi()
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
            )
            .expectFixDiffs(
                """
                Fix for src/test/pkg/KotlinTest.kt line 14: Wrap with flag check:
                @@ -14 +14
                -             FlaggedApiContainer.flaggedApi()
                +             if (androidx.core.flagging.Flags.getBooleanFlagValue("test.pkg", "myFlag")) { FlaggedApiContainer.flaggedApi() } else { TODO("Implement fallback behavior") }
                """
                    .trimIndent()
            )
    }

    fun testChecksAconfigFlagGating_javaWithFlaggedDeprecation_isClean() {
        lint()
            .files(
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class FlaggedApiContainer {
                    @FlaggedApi("test.pkg.myFlag")
                    @Deprecated
                    public static void flaggedApi() {
                    }
                 }

                 void callFlaggedApi(MyInterface f) {
                     FlaggedApiContainer.flaggedApi();
                 }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expectClean()
    }

    fun testChecksAconfigFlagGating_kotlinWithFlaggedDeprecation_isClean() {
        lint()
            .files(
                kotlin(
                        """
                    package test.pkg

                    import android.annotation.FlaggedApi

                    class KotlinTest {
                        object FlaggedApiContainer {
                            @FlaggedApi("test.pkg.myFlag")
                            @Deprecated
                            fun flaggedApi() {
                            }
                        }

                        fun callFlaggedApi() =
                            FlaggedApiContainer.flaggedApi()
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expectClean()
    }

    fun testFinalFields() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;

          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class Bar {
                    @FlaggedApi("test.pkg.myFlag")
                    public void bar() { }
                }
                static class Foo {
                    private static final boolean useNewStuff = Flags.myFlag();
                    private final Bar mBar = new Bar();

                    void someMethod() {
                        if (useNewStuff) {
                            // OK because flags can't change value without a reboot, though this might change in
                            // the future and in that case caching the flag value would be an error. We can restart
                            // apps due to a server push of new flag values but restarting the framework would be
                            // too disruptive
                            mBar.bar(); // OK
                        }
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testInverseLogic() {
        lint()
            .files(
                java(
                    """
          package test.pkg;

          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi("test.pkg.myFlag")
                class Foo {
                    public void someMethod() { }
                }

                public void testInverse() {
                    if (!Flags.myFlag()) {
                        // ...
                    } else {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testAnded() {
        lint()
            .files(
                java(
                    """
          package test.pkg;

          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;
            import static test.pkg.Flags.myFlag;

            /** @noinspection InstantiationOfUtilityClass, AccessStaticViaInstance , ResultOfMethodCallIgnored , StatementWithEmptyBody */
            public class JavaTest {
                @FlaggedApi("test.pkg.myFlag")
                public static class Foo {
                    public static boolean someMethod() { return true; }
                }

                public void testValid1(boolean something) {
                    if (true && something && Flags.myFlag()) {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }

                public void testValid2(boolean something) {
                    if (something || !Flags.myFlag()) {
                    } else {
                        Foo f = new Foo(); // OK 3
                        f.someMethod();    // OK 4
                    }
                }

                public void testValid3(Foo f, boolean something) {
                    // b/b/383061307
                    if (Flags.myFlag() && f.someMethod()) { // OK 5
                    }
                    if (myFlag() && f.someMethod()) { // OK 6
                    }
                    if (Flags.myFlag() && something && f.someMethod()) { // OK 7
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testChecksAconfigFlagGating_notInAllowlist_raisesError() {
        val project =
            project()
                .name("notallowedArtifactId")
                .type(ProjectDescription.Type.LIBRARY)
                .report(false)
                .files(
                    gradle(
                            """
                    apply plugin: 'com.android.library'
                    group=notallowedGroupId
                    version=1.0.0-alpha01
                    """
                        )
                        .indented(),
                    kotlin(
                        """
                  package test.pkg

                  import androidx.annotation.ChecksAconfigFlag

                  object FlagsCompat {
                      @ChecksAconfigFlag("test.pkg.myFlag")
                      fun myFlag() { return true; }
                  }
                  """
                    ),
                    kotlin(
                            """
                    package test.pkg

                    import android.annotation.FlaggedApi

                    class KotlinTest {
                        object FlaggedApiContainer {
                            @FlaggedApi("test.pkg.myFlag")
                            fun flaggedApi() {
                            }
                        }

                        fun callFlaggedApi() =
                            if (FlagsCompat.myFlag()) {
                                FlaggedApiContainer.flaggedApi()
                            }
                    }
                    """
                        )
                        .indented(),
                    Stubs.FlaggedApi,
                    Stubs.ChecksAconfigFlag,
                )

        lint()
            .projects(project)
            .run()
            .expect(
                """
        src/main/kotlin/test/pkg/KotlinTest.kt:14: Error: Flagged APIs are subject to additional policies and may only be called by libraries that have been allowlisted by Jetpack Working Group [AndroidXFlaggedApi]
                    FlaggedApiContainer.flaggedApi()
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
            )
    }

    fun testChecksAconfigFlagGating_withBetaVersion_raisesError() {
        val project =
            project()
                .name("allowedArtifactId")
                .type(ProjectDescription.Type.LIBRARY)
                .report(false)
                .files(
                    gradle(
                            """
                    apply plugin: 'com.android.library'
                    group=test
                    version=1.0.0-beta01
                    """
                        )
                        .indented(),
                    kotlin(
                        """
                  package test.pkg

                  import androidx.annotation.ChecksAconfigFlag

                  object FlagsCompat {
                      @ChecksAconfigFlag("test.pkg.myFlag")
                      fun myFlag() { return true; }
                  }
                  """
                    ),
                    kotlin(
                            """
                    package test.pkg

                    import android.annotation.FlaggedApi

                    class KotlinTest {
                        object FlaggedApiContainer {
                            @FlaggedApi("test.pkg.myFlag")
                            fun flaggedApi() {
                            }
                        }

                        fun callFlaggedApi() =
                            if (FlagsCompat.myFlag()) {
                                FlaggedApiContainer.flaggedApi()
                            }
                    }
                    """
                        )
                        .indented(),
                    Stubs.FlaggedApi,
                    Stubs.ChecksAconfigFlag,
                )

        lint()
            .configureOption("allowlist", "test")
            .projects(project)
            .run()
            .expect(
                """
        src/main/kotlin/test/pkg/KotlinTest.kt:14: Error: Flagged APIs may only be called during alpha and must be removed before moving to beta [AndroidXFlaggedApi]
                    FlaggedApiContainer.flaggedApi()
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 error
        """
            )
    }
}
