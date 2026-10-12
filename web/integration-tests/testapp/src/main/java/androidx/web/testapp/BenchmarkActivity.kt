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

package androidx.web.testapp

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.web.compose.WebSurface
import androidx.web.compose.WebViewBridge
import androidx.web.compose.retainWebContent

/** Benchmark Activity comparing [WebSurface] and [AndroidView]-hosted [WebView]. */
class BenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val useWebSurface = intent.getBooleanExtra(EXTRA_USE_WEB_SURFACE, true)
        setContent {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp))
                BenchmarkWebContent(
                    useWebSurface = useWebSurface,
                    modifier = Modifier.fillMaxWidth().height(600.dp),
                )
                Box(modifier = Modifier.fillMaxWidth().height(1200.dp))
            }
        }
    }

    companion object {
        const val EXTRA_USE_WEB_SURFACE = "use_web_surface"
    }
}

@Composable
@Suppress("RestrictedApiAndroidX")
private fun BenchmarkWebContent(useWebSurface: Boolean, modifier: Modifier = Modifier) {
    if (useWebSurface) {
        WebSurface(
            content = retainWebContent(),
            modifier = modifier,
            bridgeFactory = { context -> WebViewBridge(context).apply { loadBenchmarkPage() } },
        )
    } else {
        AndroidView(
            factory = { context -> WebView(context).apply { loadBenchmarkPage() } },
            modifier = modifier,
        )
    }
}

private fun WebView.loadBenchmarkPage() {
    settings.javaScriptEnabled = true
    loadUrl("file:///android_asset/benchmark.html")
}
