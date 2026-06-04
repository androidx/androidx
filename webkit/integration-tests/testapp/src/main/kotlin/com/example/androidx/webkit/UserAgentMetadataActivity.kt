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
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewFeature

/** Demo activity to demonstrate the behavior of overriding user-agent metadata APIs. */
class UserAgentMetadataActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_agent_metadata)
        setTitle(R.string.user_agent_metadata_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        webView =
            findViewById<WebView>(R.id.user_agent_metadata_webview).apply {
                with(settings) {
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    javaScriptEnabled = true
                }
            }

        findViewById<RadioGroup>(R.id.user_agent_metadata_radio_group).apply {
            check(R.id.user_agent_metadata_without_override_mode)
            setOnCheckedChangeListener { _, checkedId -> refreshView(checkedId) }

            refreshView(this.checkedRadioButtonId)
        }
    }

    private fun refreshView(checkedId: Int) {
        val resourcePath =
            when (checkedId) {
                R.id.user_agent_metadata_with_override_form_factors_mode -> {
                    if (
                        WebViewFeature.isFeatureSupported(
                            WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS
                        )
                    ) {
                        FORM_FACTORS_USER_AGENT_RESOURCE
                    } else {
                        NO_FORM_FACTORS_USER_AGENT_RESOURCE
                    }
                }
                else -> MAIN_USER_AGENT_RESOURCE
            }
        val overrideSetting: UserAgentMetadata =
            when (checkedId) {
                R.id.user_agent_metadata_with_override_mode -> {
                    buildUserAgentMetadata(hasFormFactors = false)
                }
                R.id.user_agent_metadata_with_override_form_factors_mode -> {
                    if (
                        WebViewFeature.isFeatureSupported(
                            WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS
                        )
                    ) {
                        buildUserAgentMetadata(hasFormFactors = true)
                    } else {
                        UserAgentMetadata.Builder().build()
                    }
                }
                else -> {
                    UserAgentMetadata.Builder().build()
                }
            }
        WebSettingsCompat.setUserAgentMetadata(webView.settings, overrideSetting)

        val assetLoader =
            WebViewAssetLoader.Builder()
                .setDomain(ASSET_LOADER_DOMAIN)
                .addPathHandler(ASSET_LOADER_PATH, AssetsPathHandler(this))
                .build()
        with(webView) {
            webViewClient = AssetLoaderWebViewClient(assetLoader)
            webView.loadUrl(FULL_ASSET_URL + resourcePath)
        }
    }

    private fun buildUserAgentMetadata(hasFormFactors: Boolean): UserAgentMetadata {
        val brandVersion =
            UserAgentMetadata.BrandVersion.Builder()
                .apply {
                    setBrand("myBrand")
                    setMajorVersion("1")
                    setFullVersion("1.1.1.1")
                }
                .build()

        return UserAgentMetadata.Builder()
            .apply {
                setBrandVersionList(listOf(brandVersion))
                setFullVersion("1.1.1.1")
                setPlatform("myPlatform")
                setPlatformVersion("2.2.2.2")
                setArchitecture("myArch")
                setMobile(true)
                setModel("myModel")
                setBitness(32)
                setWow64(false)
                if (hasFormFactors) {
                    setFormFactors(listOf(UserAgentMetadata.FORM_FACTOR_XR))
                }
            }
            .build()
    }

    companion object {
        private const val ASSET_LOADER_DOMAIN = "example.com"
        private const val ASSET_LOADER_PATH = "/androidx_webkit/example/assets/"
        private const val FULL_ASSET_URL = "https://$ASSET_LOADER_DOMAIN$ASSET_LOADER_PATH"
        private const val MAIN_USER_AGENT_RESOURCE = "www/user_agent_metadata_main.html"
        private const val FORM_FACTORS_USER_AGENT_RESOURCE =
            "www/user_agent_metadata_form_factors.html"
        private const val NO_FORM_FACTORS_USER_AGENT_RESOURCE =
            "www/user_agent_metadata_form_factors_not_supported.html"
    }
}
