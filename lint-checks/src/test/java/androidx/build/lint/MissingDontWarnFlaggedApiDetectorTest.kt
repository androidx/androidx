/*
 * Copyright 2026 The Android Open Source Project
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
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MissingDontWarnFlaggedApiDetectorTest : LintDetectorTest() {

    // kotlinc version info: kotlinc-jvm 2.1.20 (JRE 21.0.4+-12414455)
    private val ENCODED_FLAGGED_CLASS_JAR =
        "H4sIAAAAAAAA/wvwZmYRYeDg4GBgYFBkQAYiDLwMvq4hjrqefm76vo5+nm6u" +
            "wSF6vm6hIawMjAzLzgn9O8XA8Nn3zGkfb129i7zeulrnzpzfHGRwxfjB0yI9" +
            "L18dT9+Lpau2BH3w0i3U8jpzRjvswzn9kyfPPH766CkTQ4A3O8d6Yc31lkCb" +
            "zIE4AKc7lBk4GRLzUoryM1P0E/Py8ksSSzLz8/TdchLT01NTHAsy9ZJzEouL" +
            "4c5qDYrNFwoUsC3/GbXyUFOo5tZt17yifVmk739pVTriuy1nc46vlNwZ3Wrn" +
            "bTq7eXcvqH181v6B/of0Hwke8n8Dj+1ccIB5pX56Wvq5MzOtrffX/f/B+YA3" +
            "ziin8L4ky7rgZ/vkXWyKmPff+VCxXXvOq6mzmZIv6P3smG77rvz0s6NLX7ho" +
            "HpHYJn5x6aVXd3wVD06QKMzXkLjGz9fLul+UN2b/72lHeuMEKtftv3NQaOKH" +
            "pe4//4r+bD1kZFy5xnz1NUvnQtfOs+yxgoLah231f+jMiVRIubE7NCFu+grJ" +
            "q2HdjrlzTLO35ZlpXrnw+lh129ZZN3NuXos8a6YbFbr1psnbVUtqfkVvTl4T" +
            "c+XTh6X/A/yYz7+N8f++dZLF/OLy0n3vOu1KN61fzzLx0befmXPNyoK228/4" +
            "0vTm0pnNIe/uh/m5HJGVK1wV9GuxxN0epUZPBubXgX+6tfdrPbFcL3nP91hU" +
            "lKZGputtH/9jKUYpV2av90o0sgvSehNxVt106YqQpLCytPx96pOP7liVUXjz" +
            "cYfe6R1f1TIlLu3bzrHnlSvPyzADmRKXw19f3DFbo6i7Tt9u18Lqh51H5Xm9" +
            "lzRaXFV/yu1hyBpqx90ebHOAe9P9CQdkHy1huRRxOFbzupuTQvMaFl8nP6e4" +
            "Q7Vrzp/gn3DU+aXfyf6ACRMmb1BU1F/Y4fvq347eGX26sv4lOcntOvNe3Ti6" +
            "2Syiw9ZmOZ+il+I35yXLVvxjBCU0gQrNIn4mBoatzPgSmhIwoZWkFpfoF2Sn" +
            "w5KXMyhpuRXl53olFqGls9qg03mXHURsjfvePhB5JRJ1iT038WqTyHJRa7mg" +
            "lSInjzawaMxeo7Zxql9ytdZ5labYfa32Ca1HmFx1fmocmSugxcLhbfe9su/5" +
            "vY/b38/fz1DRUc1jrXZZbZdM/6I8PbfFOrZnW6/d9RJNX/TpoMQhbkPRQL1T" +
            "gV+yCiby9vK3H6zW8c6cZ2TB3r5XdMcHpisPjK+c2XmFZ/fJ/y3yhixRoje7" +
            "z8xZPSVRXzA1SnOm2yKdqYJuqz6Jv38SFLTbWvSnSnLE9k25k3LF38/Xnnz6" +
            "0jZDk8kflbo8gpYc4nj0IenVhTl3mDYzsJkt+2OduZiv8qWgs8n8Vsb8jw3t" +
            "nyMPvOaz3PNu+7PNyfcSz/9ZL78nam77uZni17ZVHN+6JTntqNk/w6jqnAzO" +
            "5VpLD69T23Qmzq/LMWyvaPMco+xOqa496lqZj1ONKharXTV47SWds2/eFDex" +
            "SP8sz6uTV9deCNo+feuOOf9Cjwpa3G2otZDt1WspaF90ws/32t7/nF/7uz/J" +
            "Or1PF9rdou39X4ppk6/JuhP8Yt6Lv/sv+i89UbeT75tY7O0b6r9bPq1Wkfr2" +
            "8EjGsYjuQuHtJ5Jsj7HlfT9oEnXjzeHLX6wTvA9fvZY/oX+3ykUFzt+dr6XM" +
            "68BFUc13IeOTjMAUwoQvhUgDUwi8SMxNzMzTy84vycnMi8/NTynNSYUnjeSE" +
            "hIQ0IGZKcmPTCEi6kMQAtkRmleoWYaAxEuDyjpFJhAFhDXJZCCp5UQHBchjd" +
            "OOQUDypAEaADiEkpTtFNRg4pJRSTPwJFSMg/6AYjh440isHCrAxEBXuANysb" +
            "SD0LEIYAnVjBCuIBANyyFLXiBgAA"

    override fun getIssues(): List<Issue> = listOf(FlaggedApiDetector.MISSING_DONTWARN_ISSUE)

    override fun getDetector(): Detector = FlaggedApiDetector()

    override fun lint(): com.android.tools.lint.checks.infrastructure.TestLintTask {
        return super.lint().allowMissingSdk()
    }

    @Test
    fun missingProguardRule_java_reportsError() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                        public void someMethod() {}
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            FlaggedClass obj = new FlaggedClass();
                            obj.someMethod();
                        }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaUsage.java:5: Error: Usage of Flagged API class 'test.pkg.FlaggedClass' requires a matching '-dontwarn test.pkg.FlaggedClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        FlaggedClass obj = new FlaggedClass();
                                           ~~~~~~~~~~~~~~~~~~
                src/test/pkg/JavaUsage.java:6: Error: Usage of Flagged API class 'test.pkg.FlaggedClass' requires a matching '-dontwarn test.pkg.FlaggedClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        obj.someMethod();
                        ~~~~~~~~~~~~~~~~
                2 errors
                """
                    .trimIndent()
            )
    }

    @Test
    fun missingProguardRule_kotlin_reportsError() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                        public void someMethod() {}
                    }
                    """
                    )
                    .indented(),
                kotlin(
                        """
                    package test.pkg

                    class KotlinUsage {
                        fun use() {
                            val obj = FlaggedClass()
                            obj.someMethod()
                        }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/KotlinUsage.kt:5: Error: Usage of Flagged API class 'test.pkg.FlaggedClass' requires a matching '-dontwarn test.pkg.FlaggedClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        val obj = FlaggedClass()
                                  ~~~~~~~~~~~~~~
                src/test/pkg/KotlinUsage.kt:6: Error: Usage of Flagged API class 'test.pkg.FlaggedClass' requires a matching '-dontwarn test.pkg.FlaggedClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        obj.someMethod()
                        ~~~~~~~~~~~~~~~~
                2 errors
                """
                    .trimIndent()
            )
    }

    @Test
    fun withProguardRule_java_isClean() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                        public void someMethod() {}
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            FlaggedClass obj = new FlaggedClass();
                            obj.someMethod();
                        }
                    }
                    """
                    )
                    .indented(),
                TestFiles.source("consumer-rules.pro", "-dontwarn test.pkg.FlaggedClass"),
                Stubs.FlaggedApi,
            )
            .run()
            .expectClean()
    }

    @Test
    fun withProguardRuleInSrcMain_java_isClean() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                        public void someMethod() {}
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            FlaggedClass obj = new FlaggedClass();
                            obj.someMethod();
                        }
                    }
                    """
                    )
                    .indented(),
                TestFiles.source("src/main/consumer-rules.pro", "-dontwarn test.pkg.FlaggedClass"),
                Stubs.FlaggedApi,
            )
            .run()
            .expectClean()
    }

    @Test
    fun missingProguardRule_innerClass_reportsError() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                        public static class InnerClass {
                            public void someMethod() {}
                        }
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            FlaggedClass.InnerClass obj = new FlaggedClass.InnerClass();
                            obj.someMethod();
                        }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaUsage.java:5: Error: Usage of Flagged API class 'test.pkg.FlaggedClass${"$"}InnerClass' requires a matching '-dontwarn test.pkg.FlaggedClass${"$"}InnerClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        FlaggedClass.InnerClass obj = new FlaggedClass.InnerClass();
                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/JavaUsage.java:6: Error: Usage of Flagged API class 'test.pkg.FlaggedClass${"$"}InnerClass' requires a matching '-dontwarn test.pkg.FlaggedClass${"$"}InnerClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        obj.someMethod();
                        ~~~~~~~~~~~~~~~~
                2 errors
                """
                    .trimIndent()
            )
    }

    @Test
    fun withProguardRule_innerClass_isClean() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                        public static class InnerClass {
                            public void someMethod() {}
                        }
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            FlaggedClass.InnerClass obj = new FlaggedClass.InnerClass();
                            obj.someMethod();
                        }
                    }
                    """
                    )
                    .indented(),
                TestFiles.source(
                    "consumer-rules.pro",
                    "-dontwarn test.pkg.FlaggedClass${"$"}InnerClass",
                ),
                Stubs.FlaggedApi,
            )
            .run()
            .expectClean()
    }

    @Test
    fun missingProguardRule_flaggedClassReturningFlaggedType() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;
                    import java.util.List;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClassA {
                        public FlaggedClassB getB() { return null; }
                        public FlaggedClassB[] getBArray() { return null; }
                        public List<FlaggedClassB> getBList() { return null; }
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClassB {
                        public void methodB() {}
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    import java.util.List;

                    public class JavaUsage {
                        public void use(FlaggedClassA a) {
                            a.getB();
                            a.getBArray();
                            a.getBList();
                            a.getB().methodB();
                        }
                    }
                    """
                    )
                    .indented(),
                TestFiles.source("consumer-rules.pro", "-dontwarn test.pkg.FlaggedClassA"),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaUsage.java:10: Error: Usage of Flagged API class 'test.pkg.FlaggedClassB' requires a matching '-dontwarn test.pkg.FlaggedClassB' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        a.getB().methodB();
                        ~~~~~~~~~~~~~~~~~~
                1 error
                """
                    .trimIndent()
            )
    }

    @Test
    fun missingProguardRule_flaggedClassReturningFlaggedType_noRules() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;
                    import java.util.List;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClassA {
                        public FlaggedClassB getB() { return null; }
                        public FlaggedClassB[] getBArray() { return null; }
                        public List<FlaggedClassB> getBList() { return null; }
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClassB {
                        public void methodB() {}
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    import java.util.List;

                    public class JavaUsage {
                        public void use(FlaggedClassA a) {
                            a.getB();
                            a.getBArray();
                            a.getBList();
                            a.getB().methodB();
                        }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaUsage.java:7: Error: Usage of Flagged API class 'test.pkg.FlaggedClassA' requires a matching '-dontwarn test.pkg.FlaggedClassA' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        a.getB();
                        ~~~~~~~~
                src/test/pkg/JavaUsage.java:8: Error: Usage of Flagged API class 'test.pkg.FlaggedClassA' requires a matching '-dontwarn test.pkg.FlaggedClassA' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        a.getBArray();
                        ~~~~~~~~~~~~~
                src/test/pkg/JavaUsage.java:9: Error: Usage of Flagged API class 'test.pkg.FlaggedClassA' requires a matching '-dontwarn test.pkg.FlaggedClassA' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        a.getBList();
                        ~~~~~~~~~~~~
                src/test/pkg/JavaUsage.java:10: Error: Usage of Flagged API class 'test.pkg.FlaggedClassA' requires a matching '-dontwarn test.pkg.FlaggedClassA' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        a.getB().methodB();
                        ~~~~~~~~
                src/test/pkg/JavaUsage.java:10: Error: Usage of Flagged API class 'test.pkg.FlaggedClassB' requires a matching '-dontwarn test.pkg.FlaggedClassB' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        a.getB().methodB();
                        ~~~~~~~~~~~~~~~~~~
                5 errors
                """
                    .trimIndent()
            )
    }

    @Test
    fun missingProguardRule_instantiationOnly_reportsError() {
        lint()
            .files(
                java(
                        """
                    package test.pkg;

                    import android.annotation.FlaggedApi;

                    @FlaggedApi("test.pkg.myFlag")
                    public class FlaggedClass {
                    }
                    """
                    )
                    .indented(),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            new FlaggedClass();
                        }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaUsage.java:5: Error: Usage of Flagged API class 'test.pkg.FlaggedClass' requires a matching '-dontwarn test.pkg.FlaggedClass' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        new FlaggedClass();
                        ~~~~~~~~~~~~~~~~~~
                1 error
                """
                    .trimIndent()
            )
    }

    @Test
    fun missingProguardRule_fromJar_reportsError() {
        lint()
            .files(
                base64gzip("libs/flagged.jar", ENCODED_FLAGGED_CLASS_JAR),
                java(
                        """
                    package test.pkg;

                    public class JavaUsage {
                        public void use() {
                            FlaggedClassFromJar obj = new FlaggedClassFromJar();
                            obj.someMethod();
                        }
                    }
                    """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expect(
                """
                src/test/pkg/JavaUsage.java:5: Error: Usage of Flagged API class 'test.pkg.FlaggedClassFromJar' requires a matching '-dontwarn test.pkg.FlaggedClassFromJar' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        FlaggedClassFromJar obj = new FlaggedClassFromJar();
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/JavaUsage.java:6: Error: Usage of Flagged API class 'test.pkg.FlaggedClassFromJar' requires a matching '-dontwarn test.pkg.FlaggedClassFromJar' rule in this module's consumer ProGuard file. [AndroidXMissingDontWarnRule]
                        obj.someMethod();
                        ~~~~~~~~~~~~~~~~
                2 errors
                """
                    .trimIndent()
            )
    }
}
