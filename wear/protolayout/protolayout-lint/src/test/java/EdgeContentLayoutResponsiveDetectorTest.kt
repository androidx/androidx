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

import androidx.wear.protolayout.lint.ResponsiveLayoutDetector.Companion.EDGE_CONTENT_LAYOUT_ISSUE
import androidx.wear.protolayout.lint.ResponsiveLayoutDetector.Companion.PRIMARY_LAYOUT_ISSUE
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EdgeContentLayoutResponsiveDetectorTest : LintDetectorTest() {
    override fun getDetector() = ResponsiveLayoutDetector()

    override fun getIssues() = mutableListOf(PRIMARY_LAYOUT_ISSUE, EDGE_CONTENT_LAYOUT_ISSUE)

    private val deviceParametersStub =
        java(
            """
                package androidx.wear.protolayout;
                public class DeviceParameters {}
            """
                .trimIndent()
        )

    private val edgeContentLayoutStub =
        java(
            """
                package androidx.wear.protolayout.material.layouts;

                import androidx.wear.protolayout.DeviceParameters;

                public class EdgeContentLayout {
                    public static class Builder {
                        public Builder(DeviceParameters deviceParameters) {}
                        public Builder() {}

                        public Builder setResponsiveContentInsetEnabled(boolean enabled) {
                            return this;
                        }

                        public EdgeContentLayout build() {
                            return new EdgeContentLayout();
                        }
                    }
                }
            """
                .trimIndent()
        )

    @Test
    fun `edgeContentLayout with responsiveness doesn't report`() {
        lint()
            .files(
                deviceParametersStub,
                edgeContentLayoutStub,
                kotlin(
                    """
                        package foo
                        import androidx.wear.protolayout.material.layouts.EdgeContentLayout

                        val layout = EdgeContentLayout.Builder(null)
                                .setResponsiveContentInsetEnabled(true)
                                .build()

                        class Bar {
                         val layout = EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true)
                                    .build()

                            fun build() {
                                val l = EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true)
                                return l.build()
                            }

                            fun update() {
                                return EdgeContentLayout.Builder()
                                .setResponsiveContentInsetEnabled(true)
                            }

                            fun build2() {
                                update().build()
                            }

                            fun callRandom() {
                              random(EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(true))
                            }
                            fun random(val l: EdgeContentLayout.Builder) {}

                        }
                    """
                        .trimIndent()
                )
            )
            .issues(EDGE_CONTENT_LAYOUT_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `edgeContentLayout without responsiveness requires and fixes setter`() {
        lint()
            .files(
                deviceParametersStub,
                edgeContentLayoutStub,
                kotlin(
                        """
                        package foo
                        import androidx.wear.protolayout.material.layouts.EdgeContentLayout

                        val layout = EdgeContentLayout.Builder(null)
                                .setResponsiveContentInsetEnabled(false)
                                .build()

                        class Bar {
                         val layout = EdgeContentLayout.Builder(null)
                                .build()

                         val layoutFalse = EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false)
                                .build()

                            fun buildFalse() {
                                val l = EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false)
                                return l.build()
                            }

                            fun update() {
                                val enabled = false
                                EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(enabled)
                            }

                            fun build() {
                                update().build()
                            }

                            fun build2() {
                                return EdgeContentLayout.Builder().build()
                            }

                            fun doubleFalse() {
                                EdgeContentLayout.Builder()
                                    .setResponsiveContentInsetEnabled(true)
                                    .setResponsiveContentInsetEnabled(false)
                            }

                            fun callRandom() {
                              random(EdgeContentLayout.Builder())
                            }
                            fun random(val l: EdgeContentLayout.Builder) {}

                            fun condition(val cond: Boolean) {
                                val e = EdgeContentLayout.Builder()
                                if (cond) {
                                  e.setResponsiveContentInsetEnabled(false)
                                } else {
                                  e.setResponsiveContentInsetEnabled(true)
                                }
                            }
                        }
                    """
                    )
                    .indented()
            )
            // To confirm they are not mixed up.
            .issues(EDGE_CONTENT_LAYOUT_ISSUE, PRIMARY_LAYOUT_ISSUE)
            .run()
            .expect(
                """
                    src/foo/Bar.kt:4: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                    val layout = EdgeContentLayout.Builder(null)
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:9: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                     val layout = EdgeContentLayout.Builder(null)
                                  ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:12: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                     val layoutFalse = EdgeContentLayout.Builder(null)
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:17: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            val l = EdgeContentLayout.Builder(null)
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:24: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(enabled)
                            ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:32: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            return EdgeContentLayout.Builder().build()
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:36: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            EdgeContentLayout.Builder()
                            ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:42: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                          random(EdgeContentLayout.Builder())
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.kt:47: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            val e = EdgeContentLayout.Builder()
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~
                    0 errors, 9 warnings
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
                    -  val layout = EdgeContentLayout.Builder(null)
                    +  val layout = EdgeContentLayout.Builder(null).setResponsiveContentInsetEnabled(true)
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
                    -         EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(enabled)
                    +         EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 32: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -32 +32
                    -         return EdgeContentLayout.Builder().build()
                    +         return EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(true).build()
                    Fix for src/foo/Bar.kt line 36: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -38 +38
                    -             .setResponsiveContentInsetEnabled(false)
                    +             .setResponsiveContentInsetEnabled(true)
                    Fix for src/foo/Bar.kt line 42: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -42 +42
                    -       random(EdgeContentLayout.Builder())
                    +       random(EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(true))
                    Fix for src/foo/Bar.kt line 47: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -49 +49
                    -           e.setResponsiveContentInsetEnabled(false)
                    +           e.setResponsiveContentInsetEnabled(true)
                """
                    .trimIndent()
            )
    }

    @Test
    fun `edgeContentLayout with responsiveness doesn't report (Java)`() {
        lint()
            .files(
                deviceParametersStub,
                edgeContentLayoutStub,
                java(
                    """
                        package foo;
                        import androidx.wear.protolayout.material.layouts.EdgeContentLayout;

                        class Bar {
                            EdgeContentLayout layout = new EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true)
                                    .build();

                            EdgeContentLayout build() {
                                EdgeContentLayout l = new EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(true);
                                return l.build();
                            }

                            EdgeContentLayout update() {
                                return new EdgeContentLayout.Builder()
                                    .setResponsiveContentInsetEnabled(true);
                            }

                            EdgeContentLayout build2() {
                                update().build();
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(EDGE_CONTENT_LAYOUT_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun `edgeContentLayout without responsiveness requires and fixes setter (Java)`() {
        lint()
            .files(
                deviceParametersStub,
                edgeContentLayoutStub,
                java(
                    """
                        package foo;
                        import androidx.wear.protolayout.material.layouts.EdgeContentLayout;

                        class Bar {
                            EdgeContentLayout layout = new EdgeContentLayout.Builder(null)
                                .build();

                            EdgeContentLayout layoutFalse = new EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false)
                                .build();

                            EdgeContentLayout buildFalse() {
                                EdgeContentLayout l = new EdgeContentLayout.Builder(null)
                                    .setResponsiveContentInsetEnabled(false);
                                return l.build();
                            }

                            void update() {
                                boolean enabled = false;
                                new EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(enabled);
                            }

                            void build() {
                                update().build();
                            }

                            EdgeContentLayout build2() {
                                return new EdgeContentLayout.Builder().build();
                            }

                            void doubleFalse() {
                                new EdgeContentLayout.Builder()
                                    .setResponsiveContentInsetEnabled(true)
                                    .setResponsiveContentInsetEnabled(false);
                            }
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(EDGE_CONTENT_LAYOUT_ISSUE)
            .run()
            .expect(
                """
                    src/foo/Bar.java:5: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                        EdgeContentLayout layout = new EdgeContentLayout.Builder(null)
                                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:8: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                        EdgeContentLayout layoutFalse = new EdgeContentLayout.Builder(null)
                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:13: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            EdgeContentLayout l = new EdgeContentLayout.Builder(null)
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:20: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            new EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(enabled);
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:28: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            return new EdgeContentLayout.Builder().build();
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/foo/Bar.java:32: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                            new EdgeContentLayout.Builder()
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0 errors, 6 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                    Fix for src/foo/Bar.java line 5: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -5 +5
                    -     EdgeContentLayout layout = new EdgeContentLayout.Builder(null)
                    +     EdgeContentLayout layout = new EdgeContentLayout.Builder(null).setResponsiveContentInsetEnabled(true)
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
                    -         new EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(enabled);
                    +         new EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(true);
                    Fix for src/foo/Bar.java line 28: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -28 +28
                    -         return new EdgeContentLayout.Builder().build();
                    +         return new EdgeContentLayout.Builder().setResponsiveContentInsetEnabled(true).build();
                    Fix for src/foo/Bar.java line 32: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -34 +34
                    -             .setResponsiveContentInsetEnabled(false);
                    +             .setResponsiveContentInsetEnabled(true);
                """
                    .trimIndent()
            )
    }

    @Test
    fun `edgeContentLayout false report`() {
        lint()
            .files(
                deviceParametersStub,
                edgeContentLayoutStub,
                kotlin(
                    """
                        package foo
                        import androidx.wear.protolayout.material.layouts.EdgeContentLayout


                        fun doubleTrue() {
                            EdgeContentLayout.Builder()
                                .setResponsiveContentInsetEnabled(false)
                                .setResponsiveContentInsetEnabled(true)
                        }
                    """
                        .trimIndent()
                )
            )
            .issues(EDGE_CONTENT_LAYOUT_ISSUE)
            .run()
            .expect(
                """
                    src/foo/test.kt:6: Warning: EdgeContentLayout used, but responsiveness isn't set: Please call
                    setResponsiveContentInsetEnabled(true) for the best results across
                    different screen sizes and update to the looks of layout. [ProtoLayoutEdgeContentLayoutResponsive]
                        EdgeContentLayout.Builder()
                        ~~~~~~~~~~~~~~~~~~~~~~~~~
                    0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                    Fix for src/foo/test.kt line 6: Call setResponsiveContentInsetEnabled(true) on layouts:
                    @@ -7 +7
                    -         .setResponsiveContentInsetEnabled(false)
                    @@ -9 +8
                    +         .setResponsiveContentInsetEnabled(true)
                """
                    .trimIndent()
            )
    }
}
