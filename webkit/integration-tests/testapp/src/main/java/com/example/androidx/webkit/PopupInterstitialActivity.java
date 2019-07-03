/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;

/**
 * An {@link Activity} to demonstrate one way to implement a custom Safe Browsing interstitial. This
 * should not be launched by itself (thus, it's not exported), but should be launched in response to
 * a Safe Browsing event in a {@link android.webkit.WebView}.
 *
 * @see CustomInterstitialActivity
 */
@SuppressLint("InlinedApi") // It's OK to inline WebViewClient.* constants.
public class PopupInterstitialActivity extends AppCompatActivity {

    public static final String THREAT_TYPE = "threatType";
    public static final String THREAT_URL = "url";
    public static final String ACTION_RESPONSE = "response";
    public static final String ACTION_RESPONSE_PROCEED = "proceed";
    public static final String ACTION_RESPONSE_BACK_TO_SAFETY = "back";
    public static final String SHOULD_SEND_REPORT = "report";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_interstitial);
        setTitle(R.string.custom_interstitial_title);

        Intent intent = getIntent();
        int threatType = intent.getIntExtra(THREAT_TYPE,
                WebViewClient.SAFE_BROWSING_THREAT_UNKNOWN);
        String url = intent.getStringExtra(THREAT_URL);

        TextView t = findViewById(R.id.warning_message);

        String threatTypeMessage;
        switch (threatType) {
            case WebViewClient.SAFE_BROWSING_THREAT_MALWARE:
                threatTypeMessage = "Malware";
                break;
            case WebViewClient.SAFE_BROWSING_THREAT_PHISHING:
                threatTypeMessage = "Phishing";
                break;
            case WebViewClient.SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE:
                threatTypeMessage = "Harmful unwanted software";
                break;
            case WebViewClient.SAFE_BROWSING_THREAT_BILLING:
                threatTypeMessage = "Trick to bill";
                break;
            default:
                threatTypeMessage = "Unknown";
                break;
        }
        String text = String.format("Threat type: %s!\nURL: %s", threatTypeMessage, url);
        t.setText(text);

        TextView privacyPolicyMessage = findViewById(R.id.privacy_policy);
        String privacyPolicyUrl = WebViewCompat.getSafeBrowsingPrivacyPolicyUrl().toString();
        // Inject the URL into the <a> tag.
        privacyPolicyMessage.setText(Html.fromHtml(getString(R.string.view_privacy_policy_text,
                privacyPolicyUrl)));
        // Open links with an Intent to the browser.
        privacyPolicyMessage.setMovementMethod(LinkMovementMethod.getInstance());

        final Intent returnIntent = new Intent();
        final CheckBox reportingCheckbox = findViewById(R.id.reporting_checkbox);

        // Back to safety
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnIntent.putExtra(ACTION_RESPONSE, ACTION_RESPONSE_BACK_TO_SAFETY);
                returnIntent.putExtra(SHOULD_SEND_REPORT, reportingCheckbox.isChecked());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });

        // Proceed through anyway
        findViewById(R.id.proceed_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnIntent.putExtra(ACTION_RESPONSE, ACTION_RESPONSE_PROCEED);
                returnIntent.putExtra(SHOULD_SEND_REPORT, reportingCheckbox.isChecked());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}
