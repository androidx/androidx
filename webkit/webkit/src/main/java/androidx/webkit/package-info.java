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

/**
 * The androidx.webkit library is a static library you can add to your Android application
 * in order to use android.webkit APIs that are not available for older platform versions.
 *
 * <p><b>Requirements</b>
 * <p>The minimum sdk version to use this library is 14.
 *
 * <p><b>How to declare the dependencies to use the library</b>
 *
 * <p>Please check the <a
 * href="https://developer.android.com/jetpack/androidx/releases/webkit#declaring_dependencies">
 * release notes</a> for instructions to add the latest release to your {@code build.gradle} file.
 *
 * <p><b>Public bug tracker</b>
 *
 * <p>If you find bugs in the androidx.webkit library or want to request new features, please
 * <a href="https://issuetracker.google.com/issues/new?component=460423">do so here</a>.
 *
 * <p><b>Sample apps</b>
 *
 * <p>Please check out the WebView samples <a
 * href="https://github.com/android/views-widgets-samples/tree/main/WebView">on GitHub</a> for a
 * showcase of a handful of androidx.webkit APIs.
 *
 * <p>For more APIs, check out the sample app in the <a
 * href="https://android.googlesource.com/platform/frameworks/support/+/androidx-main/webkit/integration-tests/testapp/README.md">AndroidX
 * repo</a>.
 *
 * <p><b>Migrating to androidx.webkit</b>
 *
 * <p>For static methods:
 *
 * <p>Old code:
 * <pre class="prettyprint">
 *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
 *    WebView.startSafeBrowsing(appContext, callback);
 *}</pre>
 *
 * <p>New code:
 * <pre class="prettyprint">
 *if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
 *    WebViewCompat.startSafeBrowsing(appContext, callback);
 *}</pre>
 *
 * <p>Or, if you are using a non-static method:
 *
 * <p>Old code:
 * <pre class="prettyprint">
 *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
 *    myWebView.postVisualStateCallback(requestId, callback);
 *}</pre>
 *
 * <p>New code:
 * <pre class="prettyprint">
 *if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
 *    WebViewCompat.postVisualStateCallback(myWebView, requestId, callback);
 *}</pre>
 */

package androidx.webkit;
