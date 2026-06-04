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

package com.example.androidx.webkit

import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Process
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executors

class DefaultTrafficStatsTaggingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default_traffic_tagging)
        setTitle(R.string.default_trafficstats_tagging_activity)
        setUpDemoAppActivity()
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DEFAULT_TRAFFICSTATS_TAGGING)) {
            showMessage(R.string.default_trafficstats_tagging_unsupported)
            return
        }

        WebViewCompat.setDefaultTrafficStatsTag(TRAFFIC_TAG)
        findViewById<WebView>(R.id.default_trafficstats_tagging_webview).run {
            loadUrl(TRAFFIC_URL)
        }

        findViewById<Button>(R.id.fetch_traffic_stats).setOnClickListener(this::fetchTrafficStats)
    }

    private fun fetchTrafficStats(v: View) {
        Executors.newSingleThreadExecutor().execute {
            val statsManager = this.getSystemService(NetworkStatsManager::class.java)
            var transportType = TRANSPORT_CELLULAR
            val bucket =
                fetchTrafficStatsBucket(NetworkCapabilities.TRANSPORT_CELLULAR, statsManager)
                    ?: run {
                        transportType = TRANSPORT_WIFI
                        fetchTrafficStatsBucket(NetworkCapabilities.TRANSPORT_WIFI, statsManager)
                    }

            runOnUiThread { showToast(makeBytesTransferredText(transportType, bucket)) }
        }
    }

    @SuppressLint("NewApi") // minimum sdk version is > 24
    private fun fetchTrafficStatsBucket(
        transportType: Int,
        statsManager: NetworkStatsManager,
    ): NetworkStats.Bucket? {
        statsManager
            .queryDetailsForUidTag(
                transportType,
                null,
                Long.MIN_VALUE,
                Long.MAX_VALUE,
                Process.myUid(),
                TRAFFIC_TAG,
            )
            .use {
                if (it.hasNextBucket()) {
                    return NetworkStats.Bucket().apply { it.getNextBucket(this) }
                }
            }
        return null
    }

    private fun makeBytesTransferredText(transportType: String, bucket: NetworkStats.Bucket?) =
        if (bucket == null) {
            "No tagged bytes transferred. This may be due to caching."
        } else {
            "The total bytes transferred via $transportType for tag $TRAFFIC_TAG is ${bucket.txBytes + bucket.rxBytes}."
        }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TRANSPORT_CELLULAR = "Cellular"
        private const val TRANSPORT_WIFI = "WiFi"
        private const val TRAFFIC_TAG = 1234567
        private const val TRAFFIC_URL = "https://example.com"
    }
}
