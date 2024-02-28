/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.wear.protolayout.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrimaryLayoutResponsiveDetectorTest : LintDetectorTest() {
    override fun getDetector() = ResponsiveLayoutDetector()

    override fun getIssues() = mutableListOf(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)

    private val deviceParametersStub =
        java(
            """
                package androidx.wear.protolayout;
                public class DeviceParameters {}
            """
                .trimIndent()
        )

    private val primaryLayoutStub =
        java(
            """
                package androidx.wear.protolayout.material.layouts;

                import androidx.wear.protolayout.DeviceParameters;

                public class PrimaryLayout {
                    public static class Builder {
                        public Builder(DeviceParameters deviceParameters) {}
                        public Builder() {}

                        public Builder setResponsiveContentInsetEnabled(boolean enabled) {
                            return this;
                        }

                        public PrimaryLayout build() {
                            return new PrimaryLayout();
                        }
                    }
                }
            """
                .trimIndent()
        )

    @Test
    fun `primaryLayout with responsiveness doesn't report`() {
        lint()
            .files(
                deviceParametersStub,
                primaryLayoutStub,
                kotlin(
                    """
                        package foo
                        import androidx.wear.protolayout.material.layouts.PrimaryLayout

                        val layout = PrimaryLayout.Builder(null)
                                .setResponsiveContentInsetEnabled(true)
                                .build()

                        class Bar {
                         val layout = PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true)
                                    .build()

                            fun build() {
                                val l = PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true)
                                return l.build()
                            }

                            fun update() {
                                return PrimaryLayout.Builder()
                                .setResponsiveContentInsetEnabled(true)
                            }

                            fun build2() {
                                update().build()
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `primaryLayout without responsiveness requires and fixes setter`() {
        lint()
            .files(
                deviceParametersStub,
                primaryLayoutStub,
                kotlin(
                        """
                        package foo
                        import androidx.wear.protolayout.material.layouts.PrimaryLayout

                        val layout = PrimaryLayout.Builder(null)
                                .setResponsiveContentInsetEnabled(false)
                                .build()

                        class Bar {
                         val layout = PrimaryLayout.Builder(null)
                                .build()

                         val layoutFalse = PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false)
                                .build()

                            fun buildFalse() {
                                val l = PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false)
                                return l.build()
                            }

                            fun update() {
                                val enabled = false
                                PrimaryLayout.Builder().setResponsiveContentInsetEnabled(enabled)
                            }

                            fun build() {
                                update().build()
                            }

                            fun build2() {
                                return PrimaryLayout.Builder().build()
                            }

                            fun doubleFalse() {
                                PrimaryLayout.Builder()
                                    .setResponsiveContentInsetEnabled(true)
                                    .setResponsiveContentInsetEnabled(false)
                            }
                        }
                    """
                    )
                    .indented()
            )
            .issues(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)
            .run()
            .expect(
                """
                    src/foo/Bar.kt:4: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                    val layout = PrimaryLayout.Builder(null)
                                 ~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:9: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                     val layout = PrimaryLayout.Builder(null)
                                  ~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:12: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                     val layoutFalse = PrimaryLayout.Builder(null)
                                       ~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:17: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            val l = PrimaryLayout.Builder(null)
                                    ~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:24: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            PrimaryLayout.Builder().setResponsiveContentInsetEnabled(enabled)
                            ~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:32: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            return PrimaryLayout.Builder().build()
                                   ~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:36: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            PrimaryLayout.Builder()
                            ~~~~~~~~~~~~~~~~~~~~~
                    0 errors, 7 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                    Fix for src/foo/Bar.kt line 4: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -5 +5
                    -         .setResponsiveContentInsetEnabled(false)
                    +         .setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 9: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -9 +9
                    -  val layout = PrimaryLayout.Builder(null)
                    +  val layout = PrimaryLayout.Builder(null).setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 12: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -13 +13
                    -             .setResponsiveContentInsetEnabled(false)
                    +             .setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 17: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -18 +18
                    -             .setResponsiveContentInsetEnabled(false)
                    +             .setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 24: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -24 +24
                    -         PrimaryLayout.Builder().setResponsiveContentInsetEnabled(enabled)
                    +         PrimaryLayout.Builder().setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 32: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -32 +32
                    -         return PrimaryLayout.Builder().build()
                    +         return PrimaryLayout.Builder().setResponsiveContentInsetEnabled(true).build()
                    Fix for src/foo/Bar.kt line 36: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -38 +38
                    -             .setResponsiveContentInsetEnabled(false)
                    +             .setResponsiveContentInsetEnabled(true)
                """
                    .trimIndent()
            )
    }

    @Test
    fun `primaryLayout with responsiveness doesn't (Java) report`() {
        lint()
            .files(
                deviceParametersStub,
                primaryLayoutStub,
                java(
                    """
                        package foo;
                        import androidx.wear.protolayout.material.layouts.PrimaryLayout;

                        class Bar {
                            PrimaryLayout layout = new PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true)
                                    .build();

                            PrimaryLayout build() {
                                PrimaryLayout l = new PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true);
                                return l.build();
                            }

                            PrimaryLayout update() {
                                return new PrimaryLayout.Builder()
                                    .setResponsiveContentInsetEnabled(true);
                            }

                            PrimaryLayout build2() {
                                update().build();
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `primaryLayout without responsiveness requires and fixes setter (Java)`() {
        lint()
            .files(
                deviceParametersStub,
                primaryLayoutStub,
                java(
                    """
                        package foo;
                        import androidx.wear.protolayout.material.layouts.PrimaryLayout;

                        class Bar {
                            PrimaryLayout layout = new PrimaryLayout.Builder(null)
                                .build();

                            PrimaryLayout layoutFalse = new PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false)
                                .build();

                            PrimaryLayout buildFalse() {
                                PrimaryLayout l = new PrimaryLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false);
                                return l.build();
                            }

                            void update() {
                                boolean enabled = false;
                                new PrimaryLayout.Builder().setResponsiveContentInsetEnabled(enabled);
                            }

                            void build() {
                                update().build();
                            }

                            PrimaryLayout build2() {
                                return new PrimaryLayout.Builder().build();
                            }

                            void doubleFalse() {
                                new PrimaryLayout.Builder()
                                    .setResponsiveContentInsetEnabled(true)
                                    .setResponsiveContentInsetEnabled(false);
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)
            .run()
            .expect(
                """
                    src/foo/Bar.java:5: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                        PrimaryLayout layout = new PrimaryLayout.Builder(null)
                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:8: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                        PrimaryLayout layoutFalse = new PrimaryLayout.Builder(null)
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:13: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            PrimaryLayout l = new PrimaryLayout.Builder(null)
                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:20: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            new PrimaryLayout.Builder().setResponsiveContentInsetEnabled(enabled);
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:28: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            return new PrimaryLayout.Builder().build();
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:32: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                            new PrimaryLayout.Builder()
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0 errors, 6 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                    Fix for src/foo/Bar.java line 5: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -5 +5
                    -     PrimaryLayout layout = new PrimaryLayout.Builder(null)
                    +     PrimaryLayout layout = new PrimaryLayout.Builder(null).setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.java line 8: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -9 +9
                    -             .setResponsiveContentInsetEnabled(false)
                    +             .setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.java line 13: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -14 +14
                    -             .setResponsiveContentInsetEnabled(false);
                    +             .setResponsiveContentInsetEnabled(true);
                    Fix for src/foo/Bar.java line 20: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -20 +20
                    -         new PrimaryLayout.Builder().setResponsiveContentInsetEnabled(enabled);
                    +         new PrimaryLayout.Builder().setResponsiveContentInsetEnabled(true);
                    Fix for src/foo/Bar.java line 28: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -28 +28
                    -         return new PrimaryLayout.Builder().build();
                    +         return new PrimaryLayout.Builder().setResponsiveContentInsetEnabled(true).build();
                    Fix for src/foo/Bar.java line 32: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -34 +34
                    -             .setResponsiveContentInsetEnabled(false);
                    +             .setResponsiveContentInsetEnabled(true);
                """
                    .trimIndent()
            )
    }

    @Test
    fun `primaryLayout with responsiveness false positive reports`() {
        lint()
            .files(
                deviceParametersStub,
                primaryLayoutStub,
                kotlin(
                    """
                        package foo
                        import androidx.wear.protolayout.material.layouts.PrimaryLayout

                        class Bar {
                            fun create(): PrimaryLayout.Builder {
                              return PrimaryLayout.Builder()
                            }
                            fun build() {
                              create().setResponsiveContentInsetEnabled(true).build()
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)
            .run()
            .expect(
                """
                    src/foo/Bar.kt:6: Warning: PrimaryLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes. [ProtoLayoutPrimaryLayoutResponsive]
                          return PrimaryLayout.Builder()
                                 ~~~~~~~~~~~~~~~~~~~~~
                    0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                    Fix for src/foo/Bar.kt line 6: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -6 +6
                    -       return PrimaryLayout.Builder()
                    +       return PrimaryLayout.Builder().setResponsiveContentInsetEnabled(true)
                """
                    .trimIndent()
            )
    }

    @Test
    fun `primaryLayout with responsiveness false doesn't report`() {
        lint()
            .files(
                deviceParametersStub,
                primaryLayoutStub,
                kotlin(
                    """
                        package foo
                        import androidx.wear.protolayout.material.layouts.PrimaryLayout

                        class Bar {
                            fun create(): PrimaryLayout.Builder {
                              return PrimaryLayout.Builder().setResponsiveContentInsetEnabled(true)
                            }
                            fun build() {
                              create().setResponsiveContentInsetEnabled(false).build()
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE)
            .run()
            .expectClean()
    }
}
