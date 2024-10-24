/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.notification;

import static androidx.car.app.utils.CommonUtils.isAutomotiveOS;

import static java.util.Objects.requireNonNull;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;

/**
 * A class which creates {@link PendingIntent}s that will start a car app, to be used in a
 * notification action.
 */
public final class CarPendingIntent {
    @VisibleForTesting
    static final String CAR_APP_ACTIVITY_CLASSNAME = "androidx.car.app.activity.CarAppActivity";

    /**
     * The key for retrieving the original {@link Intent} form the one the OS sent from the user
     * click.
     */
    static final String COMPONENT_EXTRA_KEY =
            "androidx.car.app.notification.COMPONENT_EXTRA_KEY";

    private static final String NAVIGATION_URI_PREFIX = "geo:";
    private static final String PHONE_URI_PREFIX = "tel:";
    private static final String SEARCH_QUERY_PARAMETER = "q";
    private static final String SEARCH_QUERY_PARAMETER_SPLITTER = SEARCH_QUERY_PARAMETER + "=";

    // TODO(b/185173683): Update to PendingIntent.FLAG_MUTABLE once available (Android S)
    private static final int FLAG_MUTABLE = 1 << 25;

    /**
     * Creates a {@link PendingIntent} that can be sent in a notification action which will allow
     * the targeted car app to be started when the user clicks on the action.
     *
     * <p>See {@link CarContext#startCarApp} for the supported intents that can be passed to this
     * method.
     *
     * <p>Here is an example of usage of this method when setting a notification's intent:
     *
     * <pre>
     *     NotificationCompat.Builder builder;
     *     ...
     *     builder.setContentIntent(CarPendingIntent.getCarApp(getCarContext(), 0,
     *             new Intent(Intent.ACTION_VIEW).setComponent(
     *                     new ComponentName(getCarContext(), MyCarAppService.class)), 0));
     * </pre>
     *
     * @param context     the context in which this PendingIntent should use to start the car app
     * @param requestCode private request code for the sender
     * @param intent      the intent that will be sent to the car app
     * @param flags       may be any of the flags allowed by
     *                    {@link PendingIntent#getBroadcast(Context, int, Intent, int)} except for
     *                    {@link PendingIntent#FLAG_IMMUTABLE} as the {@link PendingIntent} needs
     *                    to be mutable to allow the host to add the necessary extras for
     *                    starting the car app. If {@link PendingIntent#FLAG_IMMUTABLE} is set,
     *                    it will be unset before creating the {@link PendingIntent}
     * @throws NullPointerException      if either {@code context} or {@code intent} are null
     * @throws InvalidParameterException if the {@code intent} is not for starting a navigation
     *                                   or a phone call and does not have the target car app's
     *                                   component name
     * @throws SecurityException         if the {@code intent} is for a different component than the
     *                                   one associated with the input {@code context}
     *
     * @return an existing or new PendingIntent matching the given parameters. May return {@code
     * null} only if {@link PendingIntent#FLAG_NO_CREATE} has been supplied.
     */
    public static @NonNull PendingIntent getCarApp(@NonNull Context context, int requestCode,
            @NonNull Intent intent, int flags) {
        requireNonNull(context);
        requireNonNull(intent);

        validateIntent(context, intent);

        flags &= ~PendingIntent.FLAG_IMMUTABLE;
        flags |= FLAG_MUTABLE;

        if (isAutomotiveOS(context)) {
            return createForAutomotive(context, requestCode, intent, flags);
        } else {
            return createForProjected(context, requestCode, intent, flags);
        }
    }

    /**
     * Ensures that the {@link Intent} provided is valid for starting a car app.
     *
     * @see CarContext#startCarApp(Intent)
     */
    @VisibleForTesting
    @SuppressWarnings("deprecation")
    static void validateIntent(Context context, Intent intent) {
        String packageName = context.getPackageName();
        String action = intent.getAction();
        ComponentName intentComponent = intent.getComponent();
        if (intentComponent != null && Objects.equals(intentComponent.getPackageName(),
                packageName)) {
            try {
                context.getPackageManager().getServiceInfo(intentComponent,
                        PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new InvalidParameterException("Intent does not have the CarAppService's "
                        + "ComponentName as its target" + intent);
            }
        } else if (Objects.equals(action, CarContext.ACTION_NAVIGATE)) {
            validateNavigationIntentIsValid(intent);
        } else if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_CALL.equals(action)) {
            validatePhoneIntentIsValid(intent);
        } else if (intentComponent == null) {
            throw new InvalidParameterException("The intent is not for a supported action");
        } else {
            throw new SecurityException("Explicitly starting a separate app is not supported");
        }
    }

    private static PendingIntent createForProjected(Context context, int requestCode, Intent intent,
            int flags) {
        intent.putExtra(COMPONENT_EXTRA_KEY, intent.getComponent());
        intent.setClass(context, CarAppNotificationBroadcastReceiver.class);

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private static PendingIntent createForAutomotive(Context context, int requestCode,
            Intent intent, int flags) {
        String packageName = context.getPackageName();
        ComponentName intentComponent = intent.getComponent();
        if (intentComponent != null && Objects.equals(intentComponent.getPackageName(),
                packageName)) {
            intent.setClassName(packageName, CAR_APP_ACTIVITY_CLASSNAME);
        }

        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    /**
     * Checks that the {@link Intent} is for a phone call by validating it meets the following:
     *
     * <ul>
     *   <li>The data is correctly formatted starting with 'tel:'
     *   <li>Has no component name set
     * </ul>
     */
    private static void validatePhoneIntentIsValid(Intent intent) {
        String data = intent.getDataString() == null ? "" : intent.getDataString();
        if (!data.startsWith(PHONE_URI_PREFIX)) {
            throw new InvalidParameterException("Phone intent data is not properly formatted");
        }

        if (intent.getComponent() != null) {
            throw new SecurityException("Phone intent cannot have a component");
        }
    }

    /**
     * Checks that the {@link Intent} is for navigation by validating it meets the following:
     *
     * <ul>
     *   <li>The data is formatted as described in {@link CarContext#startCarApp(Intent)}
     *   <li>Has no component name set
     * </ul>
     */
    private static void validateNavigationIntentIsValid(Intent intent) {
        String data = intent.getDataString() == null ? "" : intent.getDataString();
        if (!data.startsWith(NAVIGATION_URI_PREFIX)) {
            throw new InvalidParameterException("Navigation intent has a malformed uri");
        }

        Uri uri = intent.getData();
        if (getQueryString(uri) == null) {
            if (!isLatitudeLongitude(uri.getEncodedSchemeSpecificPart())) {
                throw new InvalidParameterException(
                        "Navigation intent has neither a location nor a query string");
            }
        }
    }

    /**
     * Returns whether the {@code possibleLatitudeLongitude} has a latitude longitude.
     */
    @SuppressWarnings("StringSplitter")
    private static boolean isLatitudeLongitude(String possibleLatitudeLongitude) {
        String[] parts = possibleLatitudeLongitude.split(",");
        if (parts.length == 2) {
            try {
                // Ensure both parts are doubles.
                Double.parseDouble(parts[0]);
                Double.parseDouble(parts[1]);
                return true;
            } catch (NumberFormatException e) {
                // Values are not Doubles.
            }
        }
        return false;
    }

    /**
     * Returns the actual query from the {@link Uri}, or {@code null} if none exists.
     *
     * <p>The query will be after 'q='.
     *
     * <p>For example if Uri string is 'geo:0,0?q=124+Foo+St', return value will be '124+Foo+St'.
     */
    @SuppressWarnings("StringSplitter")
    private static @Nullable String getQueryString(Uri uri) {
        if (uri.isHierarchical()) {
            List<String> queries = uri.getQueryParameters(SEARCH_QUERY_PARAMETER);
            return queries.isEmpty() ? null : queries.get(0);
        }

        String schemeSpecificPart = uri.getEncodedSchemeSpecificPart();
        String[] parts = schemeSpecificPart.split(SEARCH_QUERY_PARAMETER_SPLITTER);

        // If we have a valid split on "q=" split on "&" to only get the one parameter.
        return parts.length < 2 ? null : parts[1].split("&")[0];
    }

    private CarPendingIntent() {
    }
}
