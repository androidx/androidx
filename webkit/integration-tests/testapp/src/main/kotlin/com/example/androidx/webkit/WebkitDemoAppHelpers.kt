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

import android.app.Activity
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Inserts the [WebView] version in the current Activity title. This assumes the title has already
 * been set to something interesting, and we want to append the WebView version to the end of the
 * title.
 */
fun Activity.appendWebViewVersionToTitle() {
    val versionName =
        WebViewCompat.getCurrentWebViewPackage(this)?.versionName
            ?: this.resources.getString(R.string.not_updateable_webview)

    val oldTitle = this.title
    this.title = "$oldTitle ($versionName)"
}

/**
 * Replaces the entire view hierarchy of this [Activity] to show an error message.
 *
 * Returns the [TextView] holding the error message, so callers can optionally add more
 * functionality (ex. `setOnClickListener()`).
 *
 * @param activity the Activity to show the message in.
 * @param messageResourceId the resource ID of the message to show.
 * @return the [TextView] holding the error message.
 */
fun Activity.showMessage(@StringRes messageResourceId: Int): TextView {
    val errorMessage = TextView(this)
    errorMessage.setText(messageResourceId)
    this.setContentView(errorMessage)
    return errorMessage
}

/**
 * Enable edge to edge rendering and handle insets.
 *
 * Must be called after [Activity.setContentView]
 */
fun AppCompatActivity.enableEdgeToEdge() {
    ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(android.R.id.content)) {
        v: View,
        insets: WindowInsetsCompat ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }
}

/**
 * Sets up the [Activity] to be used in the Webkit demo app
 *
 * Appends the [WebView] version into the current title Enables edge to edge rendering and handle
 * insets
 *
 * @param activity the demo app Activity to be set up
 */
fun AppCompatActivity.setUpDemoAppActivity() {
    this.appendWebViewVersionToTitle()
    this.enableEdgeToEdge()
}

/**
 * Helper function to check if the features are supported.
 *
 * @param features the list of WebView features to check.
 * @return `true` if all features are supported, `false` otherwise.
 */
fun areAllFeaturesSupported(vararg features: String) =
    features.all { feature -> WebViewFeature.isFeatureSupported(feature) }

/** Reads text from an [InputStream] and returns the content as a [String]. */
fun InputStream.readText(charSet: Charset = UTF_8): String {
    return BufferedReader(InputStreamReader(this, charSet)).use { it.readText() }
}
