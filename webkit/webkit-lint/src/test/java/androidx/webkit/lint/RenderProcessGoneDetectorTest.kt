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

package androidx.webkit.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class RenderProcessGoneDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = RenderProcessGoneDetector()

    override fun getIssues(): List<Issue> = listOf(RenderProcessGoneDetector.ISSUE)

    @Test
    fun testNoWarningIfMethodImplemented() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import android.webkit.WebViewClient;
                import android.webkit.WebView;
                import android.webkit.RenderProcessGoneDetail;

                public class MyWebViewClient extends WebViewClient {
                    @Override
                    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                        return true;
                    }
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testWarningIfMethodMissing() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import android.webkit.WebViewClient;

                public class MyWebViewClient extends WebViewClient {
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expect(
                """
                src/com/example/MyWebViewClient.java:4: Warning: WebViewClient should implement onRenderProcessGone to handle render process crashes or out-of-memory errors. Otherwise, the app will crash when the render process is shut down. [MissingOnRenderProcessGone]
                public class MyWebViewClient extends WebViewClient {
                             ~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testNoWarningForAbstractClass() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import android.webkit.WebViewClient;

                public abstract class MyWebViewClient extends WebViewClient {
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testDirectWebViewClientInstantiation() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import android.webkit.WebViewClient;
                import android.webkit.WebView;

                public class MyClass {
                    public void doSomething(WebView webView) {
                        webView.setWebViewClient(new WebViewClient());
                    }
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expect(
                """
                src/com/example/MyClass.java:7: Warning: WebViewClient should implement onRenderProcessGone to handle render process crashes or out-of-memory errors. Otherwise, the app will crash when the render process is shut down. [MissingOnRenderProcessGone]
                        webView.setWebViewClient(new WebViewClient());
                                                 ~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testAnonymousWebViewClientInstantiation() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import android.webkit.WebViewClient;
                import android.webkit.WebView;
                import android.webkit.RenderProcessGoneDetail;

                public class MyClass {
                    public void doSomething(WebView webView) {
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                                return true;
                            }
                        });
                    }
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expectClean()
    }

    private val stubs =
        arrayOf(
            java(
                    """
            package android.webkit;
            public class WebViewClient {
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    return false;
                }
            }
            """
                )
                .indented(),
            java(
                    """
            package android.webkit;
            public class WebView {}
            """
                )
                .indented(),
            java(
                    """
            package android.webkit;
            public abstract class RenderProcessGoneDetail {}
            """
                )
                .indented(),
        )
}
