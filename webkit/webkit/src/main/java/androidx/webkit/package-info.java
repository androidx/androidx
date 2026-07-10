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
 * The Jetpack Webkit library is a static library you can add to your Android application
 * in order to use android.webkit APIs that are not available for older platform versions.
 *
 * <h3>How to use this library in your app</h3>
 * <p>Add this to your {@code build.gradle} file:
 *
 * <pre class="prettyprint">
 * dependencies {
 *     implementation "androidx.webkit:webkit:1.14.0"
 * }
 * </pre>
 *
 * <p><b>Important:</b> replace {@code 1.14.0} with the latest version from
 * <a href="https://developer.android.com/jetpack/androidx/releases/webkit">https://developer.android.com/jetpack/androidx/releases/webkit</a>.
 *
 * <h3>Jetpack Webkit and Android System WebView</h3>
 *
 * <p>The Jetpack Webkit library enables developers to access new features that are
 * available in the installed version of
 * <a href="https://play.google.com/store/apps/details?id=com.google.android.webview">Android System WebView</a>, even if those
 * features are not exposed through the
 * <a href="https://developer.android.com/reference/android/webkit/package-summary">android.webkit framework API</a>.
 * It does this by dynamically checking the set of available
 * features through the
 * <a href="http://go/android-dev/reference/androidx/webkit/WebViewFeature#isFeatureSupported(java.lang.String)">WebViewFeature</a>
 * class.
 *
 * <p>You should take care to always check feature availability before calling an
 * API, as you otherwise risk a runtime crash if the WebView provider installed on
 * a users device doesn't support the feature in question. This is most likely to
 * happen if the user in question has not yet updated to a version of
 * <a href="https://play.google.com/store/apps/details?id=com.google.android.webview">Android
 * System WebView</a> that supports the feature, but in rare cases WebView may also
 * stop supporting a previously supported feature as part of an API deprecation.
 *
 * <h3>Sample apps</h3>
 *
 * <p>Please check out the WebView samples <a
 * href="https://github.com/android/views-widgets-samples/tree/main/WebView">on GitHub</a> for a
 * showcase of a handful of Jetpack Webkit APIs.
 *
 * <p>For more APIs, check out the sample app in the <a
 * href="https://android.googlesource.com/platform/frameworks/support/+/androidx-main/webkit
 * /integration-tests/testapp/README.md">AndroidX
 * repo</a>.
 *
 * <h3>Public bug tracker</h3>
 *
 * <p>If you find bugs in the Jetpack Webkit library or want to request new features, please
 * <a href="https://issuetracker.google.com/issues/new?component=460423">file a ticket</a>.
 *
 * <h3>Migrating to Jetpack Webkit</h3>
 *
 * <p>For static methods:
 *
 * <p>Old code:
 * <pre class="prettyprint">
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
 *    WebView.startSafeBrowsing(appContext, callback);
 * }</pre>
 *
 * <p>New code:
 * <pre class="prettyprint">
 * if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
 *    WebViewCompat.startSafeBrowsing(appContext, callback);
 * }</pre>
 *
 * <p>Or, if you are using a non-static method:
 *
 * <p>Old code:
 * <pre class="prettyprint">
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
 *    myWebView.postVisualStateCallback(requestId, callback);
 * }</pre>
 *
 * <p>New code:
 * <pre class="prettyprint">
 * if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
 *    WebViewCompat.postVisualStateCallback(myWebView, requestId, callback);
 * }</pre>
 *
 * <h3>Experimental APIs</h3>
 *
 * <p>The Jetpack Webkit library contains a number of experimental APIs. These APIs
 * will be marked with annotations that use the
 * <a href="https://developer.android.com/reference/kotlin/androidx/annotation/RequiresOptIn"><code>@RequiresOptIn</code></a>
 * annotation. We encourage you to try out these APIs in your application and file
 * feedback on the public bug tracker if you encounter any issues with the API in the
 * current shape.
 *
 * <p>When using an experimental API, you should keep the following points in mind:
 * <ul>
 * <li>New features or changes may be introduced to experimental APIs. If you are
 * using experimental APIs, we recommend that you always use the latest version
 * of the library in your dependencies, and that you adopt changes in the API as
 * soon as possible. For example, if you are building against the stable version
 * of the library, you should make sure to upgrade to the next stable version as
 * soon as it becomes available.</li>
 * <li>Experimental APIs may be removed from future versions of the library. If this
 * happens, you will not be able to continue using the API by relying on an older
 * version of the library, since the support for the feature will also have been
 * removed from new versions of WebView.</li>
 * <li>The number of WebView versions that support a given experimental API depends
 * on the release status of a given experimental API. For stable library
 * releases, you can expect that WebView versions will continue to offer support
 * until the next stable release of the library. For example, if an experimental
 * API is available in <code>androidx.webkit:webkit:1.14.0</code>, then the stable version
 * of WebView will continue to support the API until
 * <code>androidx.webkit:webkit:1.15.0</code> has been released, but after the next release
 * the support may then be dropped in WebView versions, depending on the
 * evolution of the API.</li>
 * </ul>
 */

package androidx.webkit;
