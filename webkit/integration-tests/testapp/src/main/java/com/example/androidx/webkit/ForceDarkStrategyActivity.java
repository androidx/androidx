/*
 * Copyright 2020 The Android Open Source Project
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

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} to exercise Force Dark Strategy functionality.
 * It loads two web-pages in WebViews, one of which supports dark-theme while another does not
 * and shows them side by side.
 * Activity allows setting WebViews to use UA darkening, Web theme darkening (media query vs
 * meta-tag) or both.
 */
public class ForceDarkStrategyActivity extends AppCompatActivity {
    private final String mNoDarkThemeSupport = Base64.encodeToString((
                      "<html>"
                    + "  <head><style>"
                    + "    table, th, td {"
                    + "      border: 2px solid black;"
                    + "      border-collapse: collapse;"
                    + "      margin-top: 10px;"
                    + "    }"
                    + "  </style></head>"
                    + "  <body>"
                    + "    <h2> No support for dark theme </h2>"
                    + "    <table>"
                    + "      <tr>"
                    + "        <th>UA only</th>"
                    + "        <th>Web theme only</th>"
                    + "        <th>Prefer web theme</th>"
                    + "      </tr>"
                    + "      <tr>"
                    + "        <td>Black</td>"
                    + "        <td>White</td>"
                    + "        <td>Black</td>"
                    + "      </tr>"
                    + "    </table>"
                    + "  </body>"
                    + "</html>"
    ).getBytes(), Base64.NO_PADDING);

    private final String mDarkThemeSupport = Base64.encodeToString((
                      "<html>"
                    + "  <head>"
                    + "    <meta name=\"color-scheme\" content=\"dark light\">"
                    + "    <style>"
                    + "      table, th, td {"
                    + "        border: 2px solid black;"
                    + "        border-collapse: collapse;"
                    + "        margin-top: 10px;"
                    + "      }"
                    + "      @media (prefers-color-scheme: dark) {"
                    + "        body {background-color: green;} "
                    + "      }"
                    + "    </style>"
                    + "  </head>"
                    + "  <body>"
                    + "    <h2>Support dark theme: </h2>"
                    + "    @media (prefers-color-scheme: dark) { <br>"
                    + "      body {background-color: green;} <br> "
                    + "    }"
                    + "    <table>"
                    + "      <tr>"
                    + "        <th>UA only</th>"
                    + "        <th>Web theme only</th>"
                    + "        <th>Prefer web theme</th>"
                    + "      </tr>"
                    + "      <tr>"
                    + "        <td>Black</td>"
                    + "        <td>Green</td>"
                    + "        <td>Green</td>"
                    + "      </tr>"
                    + "    </table>"
                    + "  </body>"
                    + "</html>"
    ).getBytes(), Base64.NO_PADDING);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_force_dark_strategy);
        setTitle(R.string.force_dark_strategy_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebkitHelpers.showMessageInActivity(ForceDarkStrategyActivity.this,
                    R.string.webkit_api_not_available);
            return;
        }

        final WebView darkThemeWebView = findViewById(R.id.webview_dark_theme);
        final WebView noDarkThemeWebView = findViewById(R.id.webview_no_dark_theme);
        final Spinner darkStrategySpinner = findViewById(R.id.spinner_force_dark_strategy);
        Switch forceDarkSwitch = findViewById(R.id.switch_force_dark_strategy);

        darkThemeWebView.loadData(mDarkThemeSupport,
                "text/html", "base64");

        noDarkThemeWebView.loadData(mNoDarkThemeSupport,
                "text/html", "base64");

        forceDarkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int forceDark =
                    isChecked ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF;

            WebSettingsCompat.setForceDark(darkThemeWebView.getSettings(), forceDark);
            WebSettingsCompat.setForceDark(noDarkThemeWebView.getSettings(), forceDark);
        });
        darkStrategySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int forceDarkStrategy =
                        WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING;
                switch (darkStrategySpinner.getSelectedItemPosition()) {
                    case 0:
                        forceDarkStrategy =
                                WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY;
                        break;
                    case 1:
                        forceDarkStrategy =
                                WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY;
                        break;
                }
                WebSettingsCompat.setForceDarkStrategy(darkThemeWebView.getSettings(),
                        forceDarkStrategy);
                WebSettingsCompat.setForceDarkStrategy(darkThemeWebView.getSettings(),
                        forceDarkStrategy);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

}
