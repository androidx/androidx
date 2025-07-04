/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.sdkproviderutils.fullscreen

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.ui.integration.sdkproviderutils.R
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.BackNavigation
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ScreenOrientation

class FullscreenActivityHandler(
    private val sdkContext: Context,
    private val activityHolder: ActivityHolder,
) {

    private val activity = activityHolder.getActivity()
    private val handler = Handler(Looper.getMainLooper())
    private val handlerCallback: () -> Unit = {
        destroyActivityButton.isEnabled = true
        destroyActivityButton.setOnClickListener { activity.finish() }
        backPressDispatcherCallback.remove()
    }
    private val backPressDispatcherCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                makeToast("Can not go back!")
            }
        }
    private lateinit var destroyActivityButton: Button
    private lateinit var openLandingPage: Button

    fun buildLayout(
        @ScreenOrientation screenOrientation: Int,
        @BackNavigation backNavigation: Int,
    ) {
        initUI(screenOrientation)
        registerDestroyActivityButton(backNavigation)
        registerOpenLandingPageButton()
        registerLifecycleListener()
    }

    private fun initUI(screenOrientation: Int) {
        val mainLayout =
            LayoutInflater.from(sdkContext).inflate(R.layout.layout_fullscreen_ad, null)

        destroyActivityButton = mainLayout.findViewById(R.id.btn_destroy)
        openLandingPage = mainLayout.findViewById(R.id.btn_open_landing_page)

        val webView = mainLayout.findViewById<WebView>(R.id.webview)
        initializeSettings(webView.settings)
        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    return false
                }
            }
        webView.loadUrl(WEB_VIEW_LINK)

        activity.requestedOrientation =
            convertOrientationToActivityInfoOrientation(screenOrientation)
        activity.setContentView(mainLayout)
    }

    private fun registerDestroyActivityButton(@BackNavigation backNavigation: Int) {
        when (backNavigation) {
            BackNavigation.ENABLED -> {
                destroyActivityButton.isEnabled = true
                destroyActivityButton.setOnClickListener { activity.finish() }
            }
            BackNavigation.ENABLED_AFTER_5_SECONDS -> {
                destroyActivityButton.isEnabled = false
                activityHolder.getOnBackPressedDispatcher().addCallback(backPressDispatcherCallback)
                handler.postDelayed(handlerCallback, BACK_CONTROL_DISABLE_TIME)
            }
        }
    }

    private fun convertOrientationToActivityInfoOrientation(
        @ScreenOrientation screenOrientation: Int
    ): Int {
        return when (screenOrientation) {
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.USER -> ActivityInfo.SCREEN_ORIENTATION_USER
            else -> ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    private fun registerOpenLandingPageButton() {
        openLandingPage.setOnClickListener {
            val visitUrl = Intent(Intent.ACTION_VIEW)
            visitUrl.setData(Uri.parse(LANDING_PAGE_URL))
            visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(visitUrl)
        }
    }

    private fun registerLifecycleListener() {
        activityHolder.lifecycle.addObserver(LocalLifecycleObserver())
    }

    private fun makeToast(message: String) {
        activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
    }

    private inner class LocalLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            makeToast("Current activity state is: $event")
            if (event == Lifecycle.Event.ON_DESTROY) {
                handler.removeCallbacks(handlerCallback)
            }
        }
    }

    private fun initializeSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(true)
        settings.setSupportZoom(true)
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
    }

    private companion object {
        private const val BACK_CONTROL_DISABLE_TIME = 5000L
        private const val LANDING_PAGE_URL = "https://www.google.com"
        private const val WEB_VIEW_LINK = "https://developer.android.com/"
    }
}
