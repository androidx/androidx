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

import android.os.Bundle
import android.webkit.WebView
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * An Activity to test and demonstrate the functionality of the
 * [WebSettingsCompat.setHyperlinkContextMenuItems] API. It provides a checkbox UI to configure
 * which menu items should be included in the hyperlink context menu within a WebView.
 */
class HyperlinkContextMenuActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var checkboxCopyAddress: CheckBox
    private lateinit var checkboxCopyText: CheckBox
    private lateinit var checkboxOpenLink: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_hyperlink_context_menu)
        setTitle(R.string.hyperlink_context_menu_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.HYPERLINK_CONTEXT_MENU_ITEMS)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        checkboxCopyAddress = findViewById(R.id.checkbox_copy_address)
        checkboxCopyText = findViewById(R.id.checkbox_copy_text)
        checkboxOpenLink = findViewById(R.id.checkbox_open_link)
        webView = findViewById(R.id.hyperlink_webview)

        // Enable JavaScript and load HTML with links and text/paste box
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(
            "https://example.com",
            resources.openRawResource(R.raw.hyperlink_context_menu_template).readText(),
            "text/html",
            "UTF-8",
            null,
        )

        checkboxCopyAddress.setOnCheckedChangeListener { _, _ -> applyContextMenuSettings() }
        checkboxCopyText.setOnCheckedChangeListener { _, _ -> applyContextMenuSettings() }
        checkboxOpenLink.setOnCheckedChangeListener { _, _ -> applyContextMenuSettings() }

        applyContextMenuSettings()
    }

    private fun applyContextMenuSettings() {
        var mask = 0
        if (checkboxCopyAddress.isChecked) {
            mask = mask or WebSettingsCompat.HYPERLINK_CONTEXT_MENU_ITEM_COPY_LINK_ADDRESS
        }
        if (checkboxCopyText.isChecked) {
            mask = mask or WebSettingsCompat.HYPERLINK_CONTEXT_MENU_ITEM_COPY_LINK_TEXT
        }
        if (checkboxOpenLink.isChecked) {
            mask = mask or WebSettingsCompat.HYPERLINK_CONTEXT_MENU_ITEM_OPEN_LINK
        }

        WebSettingsCompat.setHyperlinkContextMenuItems(webView.settings, mask)
        Toast.makeText(this, "Hyperlink context menu items updated", Toast.LENGTH_SHORT).show()
    }
}
