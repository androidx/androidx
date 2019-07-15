/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.customtabs;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.browser.trusted.TrustedWebActivityBuilder;
import androidx.core.app.BundleCompat;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Class for utilities and convenience calls for opening a qualifying web page as a
 * Trusted Web Activity.
 *
 * Trusted Web Activity is a fullscreen UI with no visible browser controls that hosts web pages
 * meeting certain criteria. The full list of qualifications is at the implementing browser's
 * discretion, but minimum recommended set is for the web page :
 *  <ul>
 *      <li>To have declared delegate_permission/common.handle_all_urls relationship with the
 *      launching client application ensuring 1:1 trust between the Android native and web
 *      components. See https://developers.google.com/digital-asset-links/ for details.</li>
 *      <li>To work as a reliable, fast and engaging standalone component within the launching app's
 *      flow.</li>
 *      <li>To be accessible and operable even when offline.</li>
 *  </ul>
 *
 *  Fallback behaviors may also differ with implementation. Possibilities are launching the page in
 *  a custom tab, or showing it in browser UI. Browsers are encouraged to use
 *  {@link CustomTabsCallback#onRelationshipValidationResult(int, Uri, boolean, Bundle)}
 *  for sending details of the verification results.
 */
public class TrustedWebUtils {
    /**
     * Boolean extra that triggers a {@link CustomTabsIntent} launch to be in a fullscreen UI with
     * no browser controls.
     */
    public static final String EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY =
            "android.support.customtabs.extra.LAUNCH_AS_TRUSTED_WEB_ACTIVITY";

    /**
     * @see #launchBrowserSiteSettings
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA =
            "android.support.customtabs.action.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA";

    private TrustedWebUtils() {}

    /**
     * Open the site settings for given url in the web browser. The url must belong to the origin
     * associated with the calling application via the Digital Asset Links. Prior to calling, one
     * must establish a connection to {@link CustomTabsService} and create a
     * {@link CustomTabsSession}.
     *
     * It is also required to do {@link CustomTabsClient#warmup} and
     * {@link CustomTabsSession#validateRelationship} before calling this method.
     *
     * @param context {@link Context} to use while launching site-settings activity.
     * @param session The {@link CustomTabsSession} used to verify the origin.
     * @param uri The {@link Uri} for which site-settings are to be shown.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void launchBrowserSiteSettings(Context context, CustomTabsSession session,
            Uri uri) {
        Intent intent = new Intent(TrustedWebUtils.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA);
        intent.setPackage(session.getComponentName().getPackageName());
        intent.setData(uri);

        Bundle bundle = new Bundle();
        BundleCompat.putBinder(bundle, CustomTabsIntent.EXTRA_SESSION, session.getBinder());
        intent.putExtras(bundle);
        PendingIntent id = session.getId();
        if (id != null) {
            intent.putExtra(CustomTabsIntent.EXTRA_SESSION_ID, id);
        }
        context.startActivity(intent);
    }

    /**
     * Returns whether the splash screens feature is supported by the given package.
     * Note: you can call this method prior to connecting to a {@link CustomTabsService}. This way,
     * if true is returned, the splash screen can be shown as soon as possible.
     *
     * @param context {@link Context} to use.
     * @param packageName The package name of the Custom Tabs provider to check.
     * @param version The splash screen version/feature you are testing for support. Use a value
     *                from {@link androidx.browser.trusted.splashscreens.SplashScreenVersion}.
     * @return Whether the specified Custom Tabs provider supports the specified splash screen
     *         feature/version.
     */
    public static boolean splashScreensAreSupported(@NonNull Context context,
            @NonNull String packageName, @NonNull String version) {
        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
                .setPackage(packageName);
        ResolveInfo resolveInfo = context.getPackageManager()
                .resolveService(serviceIntent, PackageManager.GET_RESOLVED_FILTER);
        if (resolveInfo == null || resolveInfo.filter == null) return false;
        return resolveInfo.filter.hasCategory(version);
    }

    /**
     * Transfers the splash image to a Custom Tabs provider. The reading and decoding of the image
     * happens synchronously, so it's recommended to call this method on a worker thread.
     *
     * This method should be called prior to {@link TrustedWebActivityBuilder#launchActivity}.
     * Pass additional parameters, such as background color, using
     * {@link TrustedWebActivityBuilder#setSplashScreenParams(Bundle)}.
     *
     * @param context {@link Context} to use.
     * @param file {@link File} with the image.
     * @param fileProviderAuthority authority of {@link FileProvider} used to generate an URI for
     *                              the file.
     * @param packageName Package name of Custom Tabs provider.
     * @param session {@link CustomTabsSession} established with the Custom Tabs provider.
     * @return True if the image was received and processed successfully.
     */
    @WorkerThread
    public static boolean transferSplashImage(@NonNull Context context, @NonNull File file,
            @NonNull String fileProviderAuthority, @NonNull String packageName,
            @NonNull CustomTabsSession session) {
        Uri uri = FileProvider.getUriForFile(context, fileProviderAuthority, file);
        context.grantUriPermission(packageName, uri, FLAG_GRANT_READ_URI_PERMISSION);
        return session.receiveFile(uri,
                CustomTabsService.FILE_PURPOSE_TRUSTED_WEB_ACTIVITY_SPLASH_IMAGE, null);
    }

    /**
     * Launch the given {@link CustomTabsIntent} as a Trusted Web Activity. The given
     * {@link CustomTabsIntent} should have a valid {@link CustomTabsSession} associated with it
     * during construction. Once the Trusted Web Activity is launched, browser side implementations
     * may have their own fallback behavior (e.g. Showing the page in a custom tab UI with toolbar)
     * based on qualifications listed above or more.
     *
     * @param context {@link Context} to use while launching the {@link CustomTabsIntent}.
     * @param customTabsIntent The {@link CustomTabsIntent} to use for launching the
     *                         Trusted Web Activity. Note that all customizations in the given
     *                         associated with browser toolbar controls will be ignored.
     * @param uri The web page to launch as Trusted Web Activity.
     *
     * @deprecated Use {@link TrustedWebActivityBuilder} and
     * {@link TrustedWebActivityBuilder#launchActivity} instead.
     */
    @Deprecated
    public static void launchAsTrustedWebActivity(@NonNull Context context,
            @NonNull CustomTabsIntent customTabsIntent, @NonNull Uri uri) {
        if (BundleCompat.getBinder(
                customTabsIntent.intent.getExtras(), CustomTabsIntent.EXTRA_SESSION) == null) {
            throw new IllegalArgumentException(
                    "Given CustomTabsIntent should be associated with a valid CustomTabsSession");
        }
        customTabsIntent.intent.putExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        customTabsIntent.launchUrl(context, uri);
    }
}
