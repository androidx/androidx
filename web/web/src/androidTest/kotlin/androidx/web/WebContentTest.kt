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

package androidx.web

import android.view.ContextThemeWrapper
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WebContentTest {

    @Before
    fun setUp() {
        assumeTrue(WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT))
    }

    @Test
    fun testRetainsUrlOnReattach() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val detachedView = webContent.attach(activity, ::WebContentView)
                    val testUrl = "about:blank"
                    assertNotEquals(testUrl, detachedView.url)
                    detachedView.loadUrl(testUrl)

                    webContent.detach()

                    detachedView.loadUrl("https://www.google.com")

                    val reattachedView = webContent.attach(activity, ::WebContentView)
                    assertEquals(testUrl, reattachedView.url)
                }
            }
        }
    }

    @Test
    fun testPreservesWebViewClientOnReattachAndAllowsOverride() {
        var client1Count = 0
        var client2Count = 0
        var onPageFinishedCallback: (() -> Unit)? = null

        val client1 = OnPageFinishedClient {
            client1Count++
            onPageFinishedCallback?.invoke()
        }
        val client2 = OnPageFinishedClient {
            client2Count++
            onPageFinishedCallback?.invoke()
        }

        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.runOnActivityAndWait { activity, done ->
                    onPageFinishedCallback = done
                    val view =
                        webContent.attach(activity, ::WebContentView).apply {
                            webViewClient = client1
                        }
                    view.loadUrl("about:blank")
                }
                assertEquals(1, client1Count)

                scenario.onActivity { webContent.detach() }

                scenario.runOnActivityAndWait { activity, done ->
                    onPageFinishedCallback = done
                    val view = webContent.attach(activity, ::WebContentView)
                    view.loadUrl("data:text/html,<html><body>ClientTest1</body></html>")
                }
                assertEquals(2, client1Count)
                assertEquals(0, client2Count)

                scenario.runOnActivityAndWait { activity, done ->
                    onPageFinishedCallback = done
                    val view = webContent.attach(activity, ::WebContentView)
                    view.webViewClient = client2
                    view.loadUrl("data:text/html,<html><body>ClientTest2</body></html>")
                }
                assertEquals(2, client1Count)
                assertEquals(1, client2Count)
            }
        }
    }

    @Test
    fun testRestoresScrollPositionOnReattach() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.webViewClient = OnPageFinishedClient(done)
                    val html =
                        "<html><body style='width:2000px; height:2000px;'><p>Scroll Test</p></body></html>"
                    view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }

                scenario.onActivity { activity ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.scrollTo(100, 200)
                    assertEquals(100, view.scrollX)
                    assertEquals(200, view.scrollY)
                    webContent.detach()
                }

                scenario.onActivity { activity ->
                    val view = webContent.attach(activity, ::WebContentView)
                    assertEquals(100, view.scrollX)
                    assertEquals(200, view.scrollY)
                }
            }
        }
    }

    @Test
    fun testPreservesJavaScriptStateOnReattach() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    view.webViewClient = OnPageFinishedClient(done)
                    view.loadUrl("about:blank")
                }

                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    view.evaluateJavascript("window.myTestVar = 'persisted_js_data';") { done() }
                }

                scenario.onActivity { webContent.detach() }

                lateinit var evaluatedValue: String
                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    view.evaluateJavascript("window.myTestVar") { result ->
                        evaluatedValue = result
                        done()
                    }
                }

                assertEquals("\"persisted_js_data\"", evaluatedValue)
            }
        }
    }

    @Test
    fun testTransfersToNewActivity() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    view.webViewClient = OnPageFinishedClient(done)
                    view.loadUrl("about:blank")
                }

                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    view.evaluateJavascript(
                        "window.transferTestVar = 'transferred_across_activities';"
                    ) {
                        done()
                    }
                }

                scenario.onActivity { webContent.detach() }
            }

            lateinit var evaluatedValue: String
            ActivityScenario.launch(TestActivity2::class.java).use { scenario ->
                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    view.evaluateJavascript("window.transferTestVar") { result ->
                        evaluatedValue = result
                        done()
                    }
                }

                scenario.onActivity { webContent.detach() }
            }

            assertEquals("\"transferred_across_activities\"", evaluatedValue)
        }
    }

    @Test
    fun testAttachesWithContextThemeWrapper() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val themedContext =
                        ContextThemeWrapper(activity, android.R.style.Theme_Material)
                    val view = webContent.attach(themedContext, ::WebContentView)
                    assertNotNull(view)
                    webContent.detach()
                }
            }
        }
    }

    @Test
    fun testAttach_afterClose_throwsIllegalStateException() {
        val webContent = WebContent()
        webContent.close()

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertThrows(IllegalStateException::class.java) {
                    webContent.attach(activity, ::WebContentView)
                }
            }
        }
    }

    @Test
    fun testPreservesWebSettingsOnReattach() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val view1 = webContent.attach(activity, ::WebContentView)
                    view1.settings.javaScriptEnabled = true
                    view1.settings.domStorageEnabled = true
                    view1.settings.userAgentString = "CustomTestUserAgent/1.0"
                    webContent.detach()
                }

                scenario.onActivity { activity ->
                    val view2 = webContent.attach(activity, ::WebContentView)
                    assertTrue(view2.settings.javaScriptEnabled)
                    assertTrue(view2.settings.domStorageEnabled)
                    assertEquals("CustomTestUserAgent/1.0", view2.settings.userAgentString)
                    webContent.detach()
                }
            }
        }
    }

    @Test
    fun testPreservesWebChromeClientOnReattach() {
        var chromeClientCalled = false
        val chromeClient =
            object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    chromeClientCalled = true
                }
            }

        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    webContent.attach(activity, ::WebContentView).apply {
                        webChromeClient = chromeClient
                    }
                    webContent.detach()
                }

                scenario.runOnActivityAndWait { activity, done ->
                    val view2 = webContent.attach(activity, ::WebContentView)
                    view2.webViewClient = OnPageFinishedClient(done)
                    val html =
                        "<html><head><title>TitleTest</title></head><body>Hello</body></html>"
                    view2.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }

                assertTrue(chromeClientCalled)
                scenario.onActivity { webContent.detach() }
            }
        }
    }

    @Test
    fun testSupportsMultipleAttachesWithoutDetach() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val view1 = webContent.attach(activity, ::WebContentView)
                    view1.loadUrl("about:blank")
                    val view2 = webContent.attach(activity, ::WebContentView)
                    assertNotNull(view2)
                    assertEquals("about:blank", view2.url)
                    val view3 = webContent.attach(activity, ::WebContentView)
                    assertNotNull(view3)
                    assertEquals("about:blank", view3.url)
                    webContent.detach()
                }
            }
        }
    }

    @Test
    fun testDetach_whileViewAttachedToWindow_throwsIllegalStateException() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity2::class.java).use { scenario ->
                scenario.runOnActivityAndWait { activity, done ->
                    val view = webContent.attachToActivity(activity)
                    assertThrows(IllegalStateException::class.java) { webContent.detach() }
                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                    webContent.detach()
                    done()
                }
            }
        }
    }

    @Test
    fun testOperationsOnDetachedView_doNotCorruptState() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                lateinit var oldView: WebContentView
                scenario.onActivity { activity ->
                    oldView = webContent.attach(activity, ::WebContentView)
                    oldView.loadUrl("about:blank")
                    webContent.detach()
                }

                scenario.onActivity {
                    oldView.loadUrl("https://www.google.com")
                    oldView.evaluateJavascript("1+1") {}
                    oldView.goBack()
                    oldView.reload()
                    oldView.stopLoading()
                }

                scenario.onActivity { activity ->
                    val newView = webContent.attach(activity, ::WebContentView)
                    assertEquals("about:blank", newView.url)
                    webContent.detach()
                }
            }
        }
    }

    @Test
    fun testHandlesRapidAttachDetach() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    for (i in 1..50) {
                        val view = webContent.attach(activity, ::WebContentView)
                        if (i % 2 == 0) {
                            view.scrollTo(i * 10, i * 10)
                        }
                        webContent.detach()
                    }
                }
            }
        }
    }

    @Test
    fun testDetachedView_handlesJavascriptExecution() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                lateinit var view: WebContentView
                scenario.onActivity { activity ->
                    view = webContent.attach(activity, ::WebContentView)
                    view.settings.javaScriptEnabled = true
                    webContent.detach()
                }

                scenario.onActivity {
                    view.evaluateJavascript("console.log('Detached context test');") {}
                    view.evaluateJavascript("alert('Test Alert');") {}
                }
            }
        }
    }

    @Test
    fun testPreservesTypedInputAcrossReattach() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        WebContent().useOnMain { webContent ->
            ActivityScenario.launch(TestActivity2::class.java).use { scenario ->
                lateinit var view: WebContentView
                scenario.runOnActivityAndWait { activity, done ->
                    view = webContent.attachToActivity(activity)
                    view.settings.javaScriptEnabled = true
                    view.webViewClient = OnPageFinishedClient(done)
                    val html =
                        """
                        <html>
                        <head>
                        <style>
                        html, body { width: 100%; height: 100%; margin: 0; padding: 0; }
                        input { width: 100%; height: 100%; box-sizing: border-box; }
                        </style>
                        </head>
                        <body>
                        <input type='text' id='input' value='' />
                        </body>
                        </html>
                        """
                            .trimIndent()
                    view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }

                scenario.onActivity { webContent.detachFromView(view) }

                val inputConnectionLatch = CountDownLatch(1)
                scenario.runOnActivityAndWait { activity, done ->
                    view =
                        webContent.attachToActivity(activity) { ctx ->
                            TestWebContentView(ctx).apply {
                                onCreateInputConnectionCallback = {
                                    inputConnectionLatch.countDown()
                                }
                            }
                        }
                    view.requestFocus()
                    view.evaluateJavascript("document.getElementById('input').focus();") { done() }
                }
                instrumentation.waitForIdleSync()

                assertTrue(
                    "Timed out waiting for input connection",
                    inputConnectionLatch.await(2, TimeUnit.SECONDS),
                )

                val testView = view as TestWebContentView
                scenario.runOnActivityAndWait { activity, done ->
                    val ic = testView.inputConnection
                    assertNotNull("InputConnection should not be null", ic)
                    ic!!.commitText("hello", 1)
                    done()
                }
                instrumentation.waitForIdleSync()

                scenario.onActivity { webContent.detachFromView(view) }

                var result = ""
                scenario.runOnActivityAndWait { activity, done ->
                    view = webContent.attachToActivity(activity)
                    view.evaluateJavascript("document.getElementById('input').value") {
                        result = it
                        done()
                    }
                }
                assertEquals("\"hello\"", result)
            }
        }
    }

    @Test
    fun testAttach_withViewNotCreatedFromProvidedContext_throwsException() {
        WebContent().use { webContent ->
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val externalView = WebContentView(activity)
                    assertThrows(IllegalStateException::class.java) {
                        webContent.attach(activity) { _ -> externalView }
                    }
                }
            }
        }
    }
}
