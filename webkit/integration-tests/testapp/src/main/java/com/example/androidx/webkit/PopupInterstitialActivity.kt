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

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.webkit.WebViewCompat

/**
 * An {@link Activity} to demonstrate one way to implement a custom Safe Browsing interstitial. This
 * should not be launched by itself (thus, it's not exported), but should be launched in response to
 * a Safe Browsing event in a {@link android.webkit.WebView}.
 *
 * @see CustomInterstitialActivity
 */
class PopupInterstitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_popup_interstitial)
        setTitle(R.string.custom_interstitial_title)
        enableEdgeToEdge()

        val threatTypeMessage =
            when (intent.getIntExtra(THREAT_TYPE, WebViewClient.SAFE_BROWSING_THREAT_UNKNOWN)) {
                WebViewClient.SAFE_BROWSING_THREAT_MALWARE -> "Malware"
                WebViewClient.SAFE_BROWSING_THREAT_PHISHING -> "Phishing"
                WebViewClient.SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE -> "Harmful unwanted software"
                WebViewClient.SAFE_BROWSING_THREAT_BILLING -> "Trick to bill"
                else -> "Unknown"
            }
        findViewById<TextView>(R.id.warning_message).text =
            "Threat type: $threatTypeMessage!\nURL: ${intent.getStringExtra(THREAT_URL)}"

        val privacyPolicyUrl = WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString()
        val privacyPolicyMessage =
            findViewById<TextView>(R.id.privacy_policy).apply {
                // Inject the URL into the <a> tag. Use FROM_HTML_MODE_LEGACY for consistency across
                // OS levels (the exact HTML doesn't matter much, so long as it renders).
                text =
                    HtmlCompat.fromHtml(
                        getString(R.string.view_privacy_policy_text, privacyPolicyUrl),
                        HtmlCompat.FROM_HTML_MODE_COMPACT,
                    )
            }
        // Open links with an Intent to the browser.
        privacyPolicyMessage.movementMethod = LinkMovementMethod.getInstance()

        val returnIntent = Intent()
        val reportingCheckbox = findViewById<CheckBox>(R.id.reporting_checkbox)

        // Back to safety
        findViewById<Button>(R.id.back_button).setOnClickListener {
            returnIntent.putExtra(ACTION_RESPONSE, ACTION_RESPONSE_BACK_TO_SAFETY)
            returnIntent.putExtra(SHOULD_SEND_REPORT, reportingCheckbox.isChecked)
            setResult(RESULT_OK, returnIntent)
            finish()
        }

        // Proceed through anyway
        findViewById<Button>(R.id.proceed_button).setOnClickListener {
            returnIntent.putExtra(ACTION_RESPONSE, ACTION_RESPONSE_PROCEED)
            returnIntent.putExtra(SHOULD_SEND_REPORT, reportingCheckbox.isChecked)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    companion object {
        const val THREAT_TYPE = "threatType"
        const val THREAT_URL = "url"
        const val ACTION_RESPONSE = "response"
        const val SHOULD_SEND_REPORT = "report"
        const val ACTION_RESPONSE_BACK_TO_SAFETY = "back"
        const val ACTION_RESPONSE_PROCEED = "proceed"
    }
}
