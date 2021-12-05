/*
 * Copyright (C) 2011 The Android Open Source Project
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

package androidx.core.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Display;
import android.view.DragEvent;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.content.LocusIdCompat;
import androidx.core.view.DragAndDropPermissionsCompat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Helper for accessing features in {@link android.app.Activity}.
 */
public class ActivityCompat extends ContextCompat {

    /**
     * This interface is the contract for receiving the results for permission requests.
     */
    public interface OnRequestPermissionsResultCallback {

        /**
         * Callback for the result from requesting permissions. This method
         * is invoked for every call on {@link #requestPermissions(android.app.Activity,
         * String[], int)}.
         * <p>
         * <strong>Note:</strong> It is possible that the permissions request interaction
         * with the user is interrupted. In this case you will receive empty permissions
         * and results arrays which should be treated as a cancellation.
         * </p>
         *
         * @param requestCode The request code passed in {@link #requestPermissions(
         * android.app.Activity, String[], int)}
         * @param permissions The requested permissions. Never null.
         * @param grantResults The grant results for the corresponding permissions
         *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
         *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
         *
         * @see #requestPermissions(android.app.Activity, String[], int)
         */
        void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                @NonNull int[] grantResults);
    }

    /**
     * Customizable delegate that allows delegating permission compatibility methods to a custom
     * implementation.
     *
     * <p>
     *     To delegate permission compatibility methods to a custom class, implement this interface,
     *     and call {@code ActivityCompat.setPermissionCompatDelegate(delegate);}. All future calls
     *     to the permission compatibility methods in this class will first check whether the
     *     delegate can handle the method call, and invoke the corresponding method if it can.
     * </p>
     */
    public interface PermissionCompatDelegate {

        /**
         * Determines whether the delegate should handle
         * {@link ActivityCompat#requestPermissions(Activity, String[], int)}, and request
         * permissions if applicable. If this method returns true, it means that permission
         * request is successfully handled by the delegate, and platform should not perform any
         * further requests for permission.
         *
         * @param activity The target activity.
         * @param permissions The requested permissions. Must be non-null and not empty.
         * @param requestCode Application specific request code to match with a result reported to
         *    {@link
         *    OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
         *    Should be >= 0.
         *
         * @return Whether the delegate has handled the permission request.
         * @see ActivityCompat#requestPermissions(Activity, String[], int)
         */
        boolean requestPermissions(@NonNull Activity activity,
                @NonNull String[] permissions, @IntRange(from = 0) int requestCode);

        /**
         * Determines whether the delegate should handle the permission request as part of
         * {@code FragmentActivity#onActivityResult(int, int, Intent)}. If this method returns true,
         * it means that activity result is successfully handled by the delegate, and no further
         * action is needed on this activity result.
         *
         * @param activity    The target Activity.
         * @param requestCode The integer request code originally supplied to
         *                    {@code startActivityForResult()}, allowing you to identify who this
         *                    result came from.
         * @param resultCode  The integer result code returned by the child activity
         *                    through its {@code }setResult()}.
         * @param data        An Intent, which can return result data to the caller
         *                    (various data can be attached to Intent "extras").
         *
         * @return Whether the delegate has handled the activity result.
         * @see ActivityCompat#requestPermissions(Activity, String[], int)
         */
        boolean onActivityResult(@NonNull Activity activity,
                @IntRange(from = 0) int requestCode, int resultCode, @Nullable Intent data);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public interface RequestPermissionsRequestCodeValidator {
        void validateRequestPermissionsRequestCode(int requestCode);
    }

    private static PermissionCompatDelegate sDelegate;

    /**
     * This class should not be instantiated, but the constructor must be
     * visible for the class to be extended (as in support-v13).
     */
    protected ActivityCompat() {
        // Not publicly instantiable, but may be extended.
    }

    /**
     * Sets the permission delegate for {@code ActivityCompat}. Replaces the previously set
     * delegate.
     *
     * @param delegate The delegate to be set. {@code null} to clear the set delegate.
     */
    public static void setPermissionCompatDelegate(
            @Nullable PermissionCompatDelegate delegate) {
        sDelegate = delegate;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static PermissionCompatDelegate getPermissionCompatDelegate() {
        return sDelegate;
    }

    /**
     * Invalidate the activity's options menu, if able.
     *
     * <p>Before API level 11 (Android 3.0/Honeycomb) the lifecycle of the
     * options menu was controlled primarily by the user's operation of
     * the hardware menu key. When the user presses down on the menu key
     * for the first time the menu was created and prepared by calls
     * to {@link Activity#onCreateOptionsMenu(android.view.Menu)} and
     * {@link Activity#onPrepareOptionsMenu(android.view.Menu)} respectively.
     * Subsequent presses of the menu key kept the existing instance of the
     * Menu itself and called {@link Activity#onPrepareOptionsMenu(android.view.Menu)}
     * to give the activity an opportunity to contextually alter the menu
     * before the menu panel was shown.</p>
     *
     * <p>In Android 3.0+ the Action Bar forces the options menu to be built early
     * so that items chosen to show as actions may be displayed when the activity
     * first becomes visible. The Activity method invalidateOptionsMenu forces
     * the entire menu to be destroyed and recreated from
     * {@link Activity#onCreateOptionsMenu(android.view.Menu)}, offering a similar
     * though heavier-weight opportunity to change the menu's contents. Normally
     * this functionality is used to support a changing configuration of Fragments.</p>
     *
     * <p>Applications may use this support helper to signal a significant change in
     * activity state that should cause the options menu to be rebuilt. If the app
     * is running on an older platform version that does not support menu invalidation
     * the app will still receive {@link Activity#onPrepareOptionsMenu(android.view.Menu)}
     * the next time the user presses the menu key and this method will return false.
     * If this method returns true the options menu was successfully invalidated.</p>
     *
     * @param activity Invalidate the options menu of this activity
     * @return true if this operation was supported and it completed; false if it was not available.
     * @deprecated Use {@link Activity#invalidateOptionsMenu()} directly.
     */
    @Deprecated
    public static boolean invalidateOptionsMenu(Activity activity) {
        activity.invalidateOptionsMenu();
        return true;
    }

    /**
     * Start new activity with options, if able, for which you would like a
     * result when it finished.
     *
     * <p>In Android 4.1+ additional options were introduced to allow for more
     * control on activity launch animations. Applications can use this method
     * along with {@link ActivityOptionsCompat} to use these animations when
     * available. When run on versions of the platform where this feature does
     * not exist the activity will be launched normally.</p>
     *
     * @param activity Origin activity to launch from.
     * @param intent The description of the activity to start.
     * @param requestCode If >= 0, this code will be returned in
     *                   onActivityResult() when the activity exits.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     */
    public static void startActivityForResult(@NonNull Activity activity, @NonNull Intent intent,
            int requestCode, @Nullable Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            activity.startActivityForResult(intent, requestCode, options);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * Start new IntentSender with options, if able, for which you would like a
     * result when it finished.
     *
     * <p>In Android 4.1+ additional options were introduced to allow for more
     * control on activity launch animations. Applications can use this method
     * along with {@link ActivityOptionsCompat} to use these animations when
     * available. When run on versions of the platform where this feature does
     * not exist the activity will be launched normally.</p>
     *
     * @param activity Origin activity to launch from.
     * @param intent The IntentSender to launch.
     * @param requestCode If >= 0, this code will be returned in
     *                   onActivityResult() when the activity exits.
     * @param fillInIntent If non-null, this will be provided as the
     *                     intent parameter to {@link IntentSender#sendIntent}.
     * @param flagsMask Intent flags in the original IntentSender that you
     *                  would like to change.
     * @param flagsValues Desired values for any bits set in <var>flagsMask</var>
     * @param extraFlags Always set to 0.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     */
    public static void startIntentSenderForResult(@NonNull Activity activity,
            @NonNull IntentSender intent, int requestCode, @Nullable Intent fillInIntent,
            int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options)
            throws IntentSender.SendIntentException {
        if (Build.VERSION.SDK_INT >= 16) {
            activity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask,
                    flagsValues, extraFlags, options);
        } else {
            activity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask,
                    flagsValues, extraFlags);
        }
    }

    /**
     * Finish this activity, and tries to finish all activities immediately below it
     * in the current task that have the same affinity.
     *
     * <p>On Android 4.1+ calling this method will call through to the native version of this
     * method. For other platforms {@link Activity#finish()} will be called instead.</p>
     */
    public static void finishAffinity(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= 16) {
            activity.finishAffinity();
        } else {
            activity.finish();
        }
    }

    /**
     * Reverses the Activity Scene entry Transition and triggers the calling Activity
     * to reverse its exit Transition. When the exit Transition completes,
     * {@link Activity#finish()} is called. If no entry Transition was used, finish() is called
     * immediately and the Activity exit Transition is run.
     *
     * <p>On Android 4.4 or lower, this method only finishes the Activity with no
     * special exit transition.</p>
     */
    public static void finishAfterTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            activity.finishAfterTransition();
        } else {
            activity.finish();
        }
    }

    /**
     * Return information about who launched this activity.  If the launching Intent
     * contains an {@link Intent#EXTRA_REFERRER Intent.EXTRA_REFERRER},
     * that will be returned as-is; otherwise, if known, an
     * {@link Intent#URI_ANDROID_APP_SCHEME android-app:} referrer URI containing the
     * package name that started the Intent will be returned.  This may return null if no
     * referrer can be identified -- it is neither explicitly specified, nor is it known which
     * application package was involved.
     *
     * <p>If called while inside the handling of {@link Activity#onNewIntent}, this function will
     * return the referrer that submitted that new intent to the activity.  Otherwise, it
     * always returns the referrer of the original Intent.</p>
     *
     * <p>Note that this is <em>not</em> a security feature -- you can not trust the
     * referrer information, applications can spoof it.</p>
     */
    @Nullable
    public static Uri getReferrer(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= 22) {
            return activity.getReferrer();
        }
        Intent intent = activity.getIntent();
        Uri referrer = intent.getParcelableExtra("android.intent.extra.REFERRER");
        if (referrer != null) {
            return referrer;
        }
        String referrerName = intent.getStringExtra("android.intent.extra.REFERRER_NAME");
        if (referrerName != null) {
            return Uri.parse(referrerName);
        }
        return null;
    }

    /**
     * Finds a view that was identified by the {@code android:id} XML attribute that was processed
     * in {@link Activity#onCreate}, or throws an IllegalArgumentException if the ID is invalid, or
     * there is no matching view in the hierarchy.
     * <p>
     * <strong>Note:</strong> In most cases -- depending on compiler support --
     * the resulting view is automatically cast to the target class type. If
     * the target class type is unconstrained, an explicit cast may be
     * necessary.
     *
     * @param id the ID to search for
     * @return a view with given ID
     * @see Activity#findViewById(int)
     * @see androidx.core.view.ViewCompat#requireViewById(View, int)
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @NonNull
    public static <T extends View> T requireViewById(@NonNull Activity activity, @IdRes int id) {
        if (Build.VERSION.SDK_INT >= 28) {
            return activity.requireViewById(id);
        }

        T view = activity.findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Activity");
        }
        return view;
    }

    /**
     * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
     * android.view.View, String)} was used to start an Activity, <var>callback</var>
     * will be called to handle shared elements on the <i>launched</i> Activity. This requires
     * {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS}.
     *
     * @param callback Used to manipulate shared element transitions on the launched Activity.
     */
    public static void setEnterSharedElementCallback(@NonNull Activity activity,
            @Nullable SharedElementCallback callback) {
        if (Build.VERSION.SDK_INT >= 21) {
            android.app.SharedElementCallback frameworkCallback = callback != null
                    ? new SharedElementCallback21Impl(callback)
                    : null;
            activity.setEnterSharedElementCallback(frameworkCallback);
        }
    }

    /**
     * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
     * android.view.View, String)} was used to start an Activity, <var>callback</var>
     * will be called to handle shared elements on the <i>launching</i> Activity. Most
     * calls will only come when returning from the started Activity.
     * This requires {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS}.
     *
     * @param callback Used to manipulate shared element transitions on the launching Activity.
     */
    public static void setExitSharedElementCallback(@NonNull Activity activity,
            @Nullable SharedElementCallback callback) {
        if (Build.VERSION.SDK_INT >= 21) {
            android.app.SharedElementCallback frameworkCallback = callback != null
                    ? new SharedElementCallback21Impl(callback)
                    : null;
            activity.setExitSharedElementCallback(frameworkCallback);
        }
    }

    public static void postponeEnterTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            activity.postponeEnterTransition();
        }
    }

    public static void startPostponedEnterTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            activity.startPostponedEnterTransition();
        }
    }

    /**
     * Requests permissions to be granted to this application. These permissions
     * must be requested in your manifest, they should not be granted to your app,
     * and they should have protection level {@link
     * android.content.pm.PermissionInfo#PROTECTION_DANGEROUS dangerous}, regardless
     * whether they are declared by the platform or a third-party app.
     * <p>
     * Normal permissions {@link android.content.pm.PermissionInfo#PROTECTION_NORMAL}
     * are granted at install time if requested in the manifest. Signature permissions
     * {@link android.content.pm.PermissionInfo#PROTECTION_SIGNATURE} are granted at
     * install time if requested in the manifest and the signature of your app matches
     * the signature of the app declaring the permissions.
     * </p>
     * <p>
     * Call {@link #shouldShowRequestPermissionRationale(Activity, String)} before calling this API
     * to check if the system recommends to show a rationale dialog before asking for a permission.
     * </p>
     * <p>
     * If your app does not have the requested permissions the user will be presented
     * with UI for accepting them. After the user has accepted or rejected the
     * requested permissions you will receive a callback reporting whether the
     * permissions were granted or not. Your activity has to implement {@link
     * androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback}
     * and the results of permission requests will be delivered to its {@link
     * androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(
     * int, String[], int[])} method.
     * </p>
     * <p>
     * Note that requesting a permission does not guarantee it will be granted and
     * your app should be able to run without having this permission.
     * </p>
     * <p>
     * This method may start an activity allowing the user to choose which permissions
     * to grant and which to reject. Hence, you should be prepared that your activity
     * may be paused and resumed. Further, granting some permissions may require
     * a restart of you application. In such a case, the system will recreate the
     * activity stack before delivering the result to your
     * {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
     * </p>
     * <p>
     * When checking whether you have a permission you should use {@link
     * #checkSelfPermission(android.content.Context, String)}.
     * </p>
     * <p>
     * Calling this API for permissions already granted to your app would show UI
     * to the user to decided whether the app can still hold these permissions. This
     * can be useful if the way your app uses the data guarded by the permissions
     * changes significantly.
     * </p>
     * <p>
     * You cannot request a permission if your activity sets {@link
     * android.R.attr#noHistory noHistory} to <code>true</code> in the manifest
     * because in this case the activity would not receive result callbacks including
     * {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
     * </p>
     * <p>
     * The <a href="https://github.com/googlesamples/android-RuntimePermissions">
     * RuntimePermissions</a> sample app demonstrates how to use this method to
     * request permissions at run time.
     * </p>
     *
     * @param activity The target activity.
     * @param permissions The requested permissions. Must be non-null and not empty.
     * @param requestCode Application specific request code to match with a result
     *    reported to {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
     *    Should be >= 0.
     *
     * @see OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])
     * @see #checkSelfPermission(android.content.Context, String)
     * @see #shouldShowRequestPermissionRationale(android.app.Activity, String)
     */
    public static void requestPermissions(final @NonNull Activity activity,
            final @NonNull String[] permissions, final @IntRange(from = 0) int requestCode) {
        if (sDelegate != null
                && sDelegate.requestPermissions(activity, permissions, requestCode)) {
            // Delegate has handled the permission request.
            return;
        }

        for (String permission : permissions) {
            if (TextUtils.isEmpty(permission)) {
                throw new IllegalArgumentException("Permission request for permissions "
                        + Arrays.toString(permissions) + " must not contain null or empty values");
            }
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (activity instanceof RequestPermissionsRequestCodeValidator) {
                ((RequestPermissionsRequestCodeValidator) activity)
                        .validateRequestPermissionsRequestCode(requestCode);
            }
            activity.requestPermissions(permissions, requestCode);
        } else if (activity instanceof OnRequestPermissionsResultCallback) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final int[] grantResults = new int[permissions.length];

                    PackageManager packageManager = activity.getPackageManager();
                    String packageName = activity.getPackageName();

                    final int permissionCount = permissions.length;
                    for (int i = 0; i < permissionCount; i++) {
                        grantResults[i] = packageManager.checkPermission(
                                permissions[i], packageName);
                    }

                    ((OnRequestPermissionsResultCallback) activity).onRequestPermissionsResult(
                            requestCode, permissions, grantResults);
                }
            });
        }
    }

    /**
     * Gets whether you should show UI with rationale before requesting a permission.
     *
     * @param activity The target activity.
     * @param permission A permission your app wants to request.
     * @return Whether you should show permission rationale UI.
     *
     * @see #checkSelfPermission(android.content.Context, String)
     * @see #requestPermissions(android.app.Activity, String[], int)
     */
    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,
            @NonNull String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return activity.shouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    /**
     * Indicates whether this activity is launched from a bubble. A bubble is a floating shortcut
     * on the screen that expands to show an activity.
     *
     * If your activity can be used normally or as a bubble, you might use this method to check
     * if the activity is bubbled to modify any behaviour that might be different between the
     * normal activity and the bubbled activity. For example, if you normally cancel the
     * notification associated with the activity when you open the activity, you might not want to
     * do that when you're bubbled as that would remove the bubble.
     *
     * @return {@code true} if the activity is launched from a bubble.
     *
     * @see NotificationCompat.Builder#setBubbleMetadata(NotificationCompat.BubbleMetadata)
     * @see NotificationCompat.BubbleMetadata.Builder#Builder(String)
     *
     * Compatibility behavior:
     * <ul>
     *     <li>API 31 and above, this method matches platform behavior</li>
     *     <li>API 29, 30, this method checks the window display ID</li>
     *     <li>API 28 and earlier, this method is a no-op</li>
     * </ul>
     */
    public static boolean isLaunchedFromBubble(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= 31) {
            return Api31Impl.isLaunchedFromBubble(activity);
        } else if (Build.VERSION.SDK_INT == 30) {
            return activity.getDisplay() != null
                    && activity.getDisplay().getDisplayId() != Display.DEFAULT_DISPLAY;
        } else if (Build.VERSION.SDK_INT == 29) {
            return activity.getWindowManager().getDefaultDisplay() != null
                    && activity.getWindowManager().getDefaultDisplay().getDisplayId()
                    != Display.DEFAULT_DISPLAY;
        }
        return false;
    }

    /**
     * Create {@link DragAndDropPermissionsCompat} object bound to this activity and controlling
     * the access permissions for content URIs associated with the {@link android.view.DragEvent}.
     * @param dragEvent Drag event to request permission for
     * @return The {@link DragAndDropPermissionsCompat} object used to control access to the content
     * URIs. {@code null} if no content URIs are associated with the event or if permissions could
     * not be granted.
     */
    @Nullable
    public static DragAndDropPermissionsCompat requestDragAndDropPermissions(Activity activity,
            DragEvent dragEvent) {
        return DragAndDropPermissionsCompat.request(activity, dragEvent);
    }

    /**
     * Cause the given Activity to be recreated with a new instance. This version of the method
     * allows a consistent behavior across API levels, emulating what happens on Android Pie (and
     * newer) when running on older platforms.
     *
     * @param activity The activity to recreate
     */
    public static void recreate(@NonNull final Activity activity) {
        if (Build.VERSION.SDK_INT >= 28) {
            // On Android P and later, we can safely rely on the platform recreate()
            activity.recreate();
        } else if (Build.VERSION.SDK_INT <= 23) {
            // Prior to Android M, we can't call recreate() before the Activity has fully resumed,
            // but we also can't inspect its current lifecycle state, so we'll just schedule the
            // recreate for later.
            Handler handler = new Handler(activity.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!activity.isFinishing()) {
                        if (!ActivityRecreator.recreate(activity)) {
                            // Fall back to the platform method if ActivityRecreator failed for any
                            // reason.
                            activity.recreate();
                        }
                    }
                }
            });
        } else {
            if (!ActivityRecreator.recreate(activity)) {
                // Fall back to the platform method if ActivityRecreator failed for any reason.
                activity.recreate();
            }
        }
    }

    /**
     * Sets the {@link LocusIdCompat} for this activity. The locus id helps identify different
     * instances of the same {@code Activity} class.
     * <p> For example, a locus id based on a specific conversation could be set on a
     * conversation app's chat {@code Activity}. The system can then use this locus id
     * along with app's contents to provide ranking signals in various UI surfaces
     * including sharing, notifications, shortcuts and so on.
     * <p> It is recommended to set the same locus id in the shortcut's locus id using
     * {@link androidx.core.content.pm.ShortcutInfoCompat.Builder#setLocusId(LocusIdCompat)}
     * so that the system can learn appropriate ranking signals linking the activity's
     * locus id with the matching shortcut.
     *
     * @param locusId  a unique, stable id that identifies this {@code Activity} instance. LocusId
     *      is an opaque ID that links this Activity's state to different Android concepts:
     *      {@link androidx.core.content.pm.ShortcutInfoCompat.Builder#setLocusId(LocusIdCompat)}.
     *      LocusID is null by default or if you explicitly reset it.
     * @param bundle extras set or updated as part of this locus context. This may help provide
     *      additional metadata such as URLs, conversation participants specific to this
     *      {@code Activity}'s context. Bundle can be null if additional metadata is not needed.
     *      Bundle should always be null for null locusId.
     *
     * @see android.view.contentcapture.ContentCaptureManager
     * @see android.view.contentcapture.ContentCaptureContext
     *
     * Compatibility behavior:
     * <ul>
     *      <li>API 30 and above, this method matches platform behavior.
     *      <li>API 29 and earlier, this method is no-op.
     * </ul>
     */
    public static void setLocusContext(@NonNull final Activity activity,
            @Nullable final LocusIdCompat locusId, @Nullable final Bundle bundle) {
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.setLocusContext(activity, locusId, bundle);
        }
    }

    @RequiresApi(21)
    private static class SharedElementCallback21Impl extends android.app.SharedElementCallback {
        private final SharedElementCallback mCallback;

        SharedElementCallback21Impl(SharedElementCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onSharedElementStart(List<String> sharedElementNames,
                List<View> sharedElements, List<View> sharedElementSnapshots) {
            mCallback.onSharedElementStart(sharedElementNames, sharedElements,
                    sharedElementSnapshots);
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements,
                List<View> sharedElementSnapshots) {
            mCallback.onSharedElementEnd(sharedElementNames, sharedElements,
                    sharedElementSnapshots);
        }

        @Override
        public void onRejectSharedElements(List<View> rejectedSharedElements) {
            mCallback.onRejectSharedElements(rejectedSharedElements);
        }

        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            mCallback.onMapSharedElements(names, sharedElements);
        }

        @Override
        public Parcelable onCaptureSharedElementSnapshot(View sharedElement,
                Matrix viewToGlobalMatrix, RectF screenBounds) {
            return mCallback.onCaptureSharedElementSnapshot(sharedElement, viewToGlobalMatrix,
                    screenBounds);
        }

        @Override
        public View onCreateSnapshotView(Context context, Parcelable snapshot) {
            return mCallback.onCreateSnapshotView(context, snapshot);
        }

        @Override
        @RequiresApi(23) // Callback added on 23.
        public void onSharedElementsArrived(List<String> sharedElementNames,
                List<View> sharedElements, final OnSharedElementsReadyListener listener) {
            mCallback.onSharedElementsArrived(sharedElementNames, sharedElements,
                    new SharedElementCallback.OnSharedElementsReadyListener() {
                        @Override
                        public void onSharedElementsReady() {
                            listener.onSharedElementsReady();
                        }
                    });
        }
    }

    @RequiresApi(30)
    static class Api30Impl {

        /**
         * This class should not be instantiated.
         */
        private Api30Impl() {
            // Not intented for instantiation.
        }

        static void setLocusContext(@NonNull final Activity activity,
                @Nullable final LocusIdCompat locusId, @Nullable final Bundle bundle) {
            activity.setLocusContext(locusId == null ? null : locusId.toLocusId(), bundle);
        }
    }

    @RequiresApi(31)
    static class Api31Impl  {

      /**
       * This class should not be instantiated.
       */
        private Api31Impl() {
            // Not intended for instantiation.
        }

        static boolean isLaunchedFromBubble(@NonNull final Activity activity)  {
            return activity.isLaunchedFromBubble();
        }
    }
}
