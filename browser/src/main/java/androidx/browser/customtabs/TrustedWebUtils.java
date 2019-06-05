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
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.core.app.BundleCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String EXTRA_ADDITIONAL_TRUSTED_ORIGINS =
            "android.support.customtabs.extra.ADDITIONAL_TRUSTED_ORIGINS";

    /**
     * @see #launchBrowserSiteSettings
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA =
            "android.support.customtabs.action.ACTION_MANAGE_TRUSTED_WEB_ACTIVITY_DATA";

    /**
     * Extra that stores the {@link Bundle} of splash screen parameters, see
     * {@link SplashScreenParamKey}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String EXTRA_SPLASH_SCREEN_PARAMS =
            "androidx.browser.trusted.EXTRA_SPLASH_SCREEN_PARAMS";


    /**
     * The keys of the entries in the {@link Bundle} passed in {@link #EXTRA_SPLASH_SCREEN_PARAMS}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface SplashScreenParamKey {
        /**
         * The version of splash screens to use.
         * The value must be one of {@link SplashScreenVersion}.
         */
        String VERSION = "androidx.browser.trusted.KEY_SPLASH_SCREEN_VERSION";

        /**
         * The background color of the splash screen.
         * The value must be an integer representing the color in RGB (alpha channel is ignored if
         * provided). The default is white.
         */
        String BACKGROUND_COLOR =
                "androidx.browser.trusted.trusted.KEY_SPLASH_SCREEN_BACKGROUND_COLOR";

        /**
         * The {@link android.widget.ImageView.ScaleType} to apply to the image on the splash
         * screen.
         * The value must be an integer - the ordinal of the ScaleType.
         * The default is {@link android.widget.ImageView.ScaleType#CENTER}.
         */
        String SCALE_TYPE = "androidx.browser.trusted.KEY_SPLASH_SCREEN_SCALE_TYPE";

        /**
         * The transformation matrix to apply to the image on the splash screen. See
         * {@link android.widget.ImageView#setImageMatrix}. Only needs to be provided if the scale
         * type is {@link android.widget.ImageView.ScaleType#MATRIX}.
         * The value must be an array of 9 floats or null. This array can be retrieved from
         * {@link Matrix#getValues)}. The default is null.
         */
        String IMAGE_TRANSFORMATION_MATRIX =
                "androidx.browser.trusted.KEY_SPLASH_SCREEN_TRANSFORMATION_MATRIX";

        /**
         * The duration of fade out animation in milliseconds to be played when removing splash
         * screen.
         * The value must be provided as an int. The default is 0 (no animation).
         */
        String FADE_OUT_DURATION_MS =
                "androidx.browser.trusted.KEY_SPLASH_SCREEN_FADE_OUT_DURATION";
    }




    /**
     * These constants are the categories the providers add to the intent filter of
     * CustomTabService implementation to declare the support of a particular version of splash
     * screens. The are also passed by the client as the value for the key
     * {@link SplashScreenParamKey#VERSION} when launching a Trusted Web Activity.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef({SplashScreenVersion.V1})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SplashScreenVersion {
        /**
         * The splash screen is transferred via {@link CustomTabsSession#receiveFile},
         * and then used by Trusted Web Activity when it is launched.
         *
         * The passed image is shown in a full-screen ImageView.
         * The following parameters are supported:
         * - {@link SplashScreenParamKey#BACKGROUND_COLOR},
         * - {@link SplashScreenParamKey#SCALE_TYPE},
         * - {@link SplashScreenParamKey#IMAGE_TRANSFORMATION_MATRIX}
         * - {@link SplashScreenParamKey#FADE_OUT_DURATION_MS}.
         */
        String V1 = "androidx.browser.trusted.category.TrustedWebActivitySplashScreensV1";
    }

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
     * TODO(pshmakov): make TwaProviderPicker gather supported features, including splash screens,
     * to avoid extra PackageManager queries.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean splashScreensAreSupported(Context context, String packageName,
            @SplashScreenVersion String version) {
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
     *
     * @param context {@link Context} to use.
     * @param file {@link File} with the image.
     * @param fileProviderAuthority authority of {@link FileProvider} used to generate an URI for
     *                              the file.
     * @param packageName Package name of Custom Tabs provider.
     * @param session {@link CustomTabsSession} established with the Custom Tabs provider.
     * @return True if the image was received and processed successfully.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean transferSplashImage(Context context, File file,
            String fileProviderAuthority, String packageName, CustomTabsSession session) {
        // TODO(peconn): Return this comment to the javadoc once TWABuilder is not hidden.
        // This method should be called prior to {@link TrustedWebActivityBuilder#launchActivity}.
        // Pass additional parameters, such as background color, using
        // {@link TrustedWebActivityBuilder#setSplashScreenParams(Bundle)}.

        Uri uri = FileProvider.getUriForFile(context, fileProviderAuthority, file);
        context.grantUriPermission(packageName, uri, FLAG_GRANT_READ_URI_PERMISSION);
        return session.receiveFile(uri, CustomTabsService.FILE_PURPOSE_TWA_SPLASH_IMAGE, null);
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
     * TODO(peconn): Deprecate with API change.
     */
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
