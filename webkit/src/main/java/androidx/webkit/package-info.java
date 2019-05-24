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
 * <p>Inside your app's build.gradle file, include this line in dependencies:
 * <pre class="prettyprint">
 *dependencies {
 *    ...
 *    implementation 'androidx.webkit:webkit:1.0.0'
 *}</pre>
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
