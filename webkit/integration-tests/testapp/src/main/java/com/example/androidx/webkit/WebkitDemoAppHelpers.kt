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
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewCompat

/**
 * Inserts the {@link android.webkit.WebView} version in the current Activity title. This assumes
 * the title has already been set to something interesting, and we want to append the WebView
 * version to the end of the title.
 */
fun Activity.appendWebViewVersionToTitle() {
    val versionName =
        WebViewCompat.getCurrentWebViewPackage(this)?.versionName
            ?: this.resources.getString(R.string.not_updateable_webview)

    val oldTitle = this.title
    this.title = "$oldTitle ($versionName)"
}

/**
 * Replaces the entire view hierarchy of this {@link Activity} to show an error message.
 *
 * <p>Returns the {@link TextView} holding the error message, so callers can optionally add more
 * functionality (ex. {@code setOnClickListener()}).
 *
 * @param activity the Activity to show the message in.
 * @param messageResourceId the resource ID of the message to show.
 * @return the {@link TextView} holding the error message.
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
 * <p>
 * Must be called after {@link Activity#setContentView(View)}
 */
fun AppCompatActivity.enableEdgeToEdge() {
    ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(android.R.id.content)) {
        v: View,
        insets: WindowInsetsCompat ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.top, systemBars.bottom)
        insets
    }
}

/**
 * Sets up the {@link Activity} to be used in the Webkit demo app
 *
 * <p>Appends the {@link android.webkit.WebView} version into the current title Enables edge to edge
 * rendering and handle insets
 *
 * @param activity the demo app Activity to be set up
 */
fun AppCompatActivity.setUpDemoAppActivity() {
    this.appendWebViewVersionToTitle()
    this.enableEdgeToEdge()
}
