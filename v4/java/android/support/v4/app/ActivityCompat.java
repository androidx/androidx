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

package android.support.v4.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Helper for accessing features in {@link android.app.Activity}
 * introduced after API level 4 in a backwards compatible fashion.
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
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                @NonNull int[] grantResults);
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
     */
    public static boolean invalidateOptionsMenu(Activity activity) {
        if (Build.VERSION.SDK_INT >= 11) {
            ActivityCompatHoneycomb.invalidateOptionsMenu(activity);
            return true;
        }
        return false;
    }

    /**
     * Start an activity with additional launch information, if able.
     *
     * <p>In Android 4.1+ additional options were introduced to allow for more
     * control on activity launch animations. Applications can use this method
     * along with {@link ActivityOptionsCompat} to use these animations when
     * available. When run on versions of the platform where this feature does
     * not exist the activity will be launched normally.</p>
     *
     * @param activity Context to launch activity from.
     * @param intent The description of the activity to start.
     * @param options Additional options for how the Activity should be started.
     *                May be null if there are no options. See
     *                {@link ActivityOptionsCompat} for how to build the Bundle
     *                supplied here; there are no supported definitions for
     *                building it manually.
     */
    public static void startActivity(Activity activity, Intent intent, @Nullable Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityCompatJB.startActivity(activity, intent, options);
        } else {
            activity.startActivity(intent);
        }
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
    public static void startActivityForResult(Activity activity, Intent intent, int requestCode,
            @Nullable Bundle options) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityCompatJB.startActivityForResult(activity, intent, requestCode, options);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * Finish this activity, and tries to finish all activities immediately below it
     * in the current task that have the same affinity.
     *
     * <p>On Android 4.1+ calling this method will call through to the native version of this
     * method. For other platforms {@link Activity#finish()} will be called instead.</p>
     */
    public static void finishAffinity(Activity activity) {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityCompatJB.finishAffinity(activity);
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
    public static void finishAfterTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat21.finishAfterTransition(activity);
        } else {
            activity.finish();
        }
    }

    /**
     * Backwards compatible implementation of {@link android.app.Activity#getReferrer()
     * Activity.getReferrer}.  Uses the platform's implementation if available, otherwise
     * only falls back to digging any explicitly specified referrer from the activity's intent.
     */
    public Uri getReferrer(Activity activity) {
        if (Build.VERSION.SDK_INT >= 22) {
            return ActivityCompat22.getReferrer(activity);
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
     * When {@link android.app.ActivityOptions#makeSceneTransitionAnimation(Activity,
     * android.view.View, String)} was used to start an Activity, <var>callback</var>
     * will be called to handle shared elements on the <i>launched</i> Activity. This requires
     * {@link android.view.Window#FEATURE_CONTENT_TRANSITIONS}.
     *
     * @param callback Used to manipulate shared element transitions on the launched Activity.
     */
    public static void setEnterSharedElementCallback(Activity activity,
            SharedElementCallback callback) {
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat21.setEnterSharedElementCallback(activity, createCallback(callback));
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
    public static void setExitSharedElementCallback(Activity activity,
            SharedElementCallback callback) {
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat21.setExitSharedElementCallback(activity, createCallback(callback));
        }
    }

    public static void postponeEnterTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat21.postponeEnterTransition(activity);
        }
    }

    public static void startPostponedEnterTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            ActivityCompat21.startPostponedEnterTransition(activity);
        }
    }

    /**
     * Requests permissions to be granted to this application. These permissions
     * must be requested in your manifest, they should not be granted to your app,
     * and they should have protection level {@link android.content.pm.PermissionInfo
     * #PROTECTION_DANGEROUS dangerous}, regardless whether they are declared by
     * the platform or a third-party app.
     * <p>
     * Normal permissions {@link android.content.pm.PermissionInfo#PROTECTION_NORMAL}
     * are granted at install time if requested in the manifest. Signature permissions
     * {@link android.content.pm.PermissionInfo#PROTECTION_SIGNATURE} are granted at
     * install time if requested in the manifest and the signature of your app matches
     * the signature of the app declaring the permissions.
     * </p>
     * <p>
     * If your app does not have the requested permissions the user will be presented
     * with UI for accepting them. After the user has accepted or rejected the
     * requested permissions you will receive a callback reporting whether the
     * permissions were granted or not. Your activity has to implement {@link
     * android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback}
     * and the results of permission requests will be delivered to its {@link
     * android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(
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
     * activity stack before delivering the result to your onRequestPermissionsResult(
     * int, String[], int[]).
     * </p>
     * <p>
     * When checking whether you have a permission you should use {@link
     * #checkSelfPermission(android.content.Context, String)}.
     * </p>
     *
     * @param activity The target activity.
     * @param permissions The requested permissions.
     * @param requestCode Application specific request code to match with a result
     *    reported to {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(
     *    int, String[], int[])}.
     *
     * @see #checkSelfPermission(android.content.Context, String)
     * @see #shouldShowRequestPermissionRationale(android.app.Activity, String)
     */
    public static void requestPermissions(final @NonNull Activity activity,
            final @NonNull String[] permissions, final int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompatApi23.requestPermissions(activity, permissions, requestCode);
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
     * Gets whether you should show UI with rationale for requesting a permission.
     * You should do this only if you do not have the permission and the context in
     * which the permission is requested does not clearly communicate to the user
     * what would be the benefit from granting this permission.
     * <p>
     * For example, if you write a camera app, requesting the camera permission
     * would be expected by the user and no rationale for why it is requested is
     * needed. If however, the app needs location for tagging photos then a non-tech
     * savvy user may wonder how location is related to taking photos. In this case
     * you may choose to show UI with rationale of requesting this permission.
     * </p>
     *
     * @param activity The target activity.
     * @param permission A permission your app wants to request.
     * @return Whether you can show permission rationale UI.
     *
     * @see #checkSelfPermission(android.content.Context, String)
     * @see #requestPermissions(android.app.Activity, String[], int)
     */
    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity,
            @NonNull String permission) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ActivityCompatApi23.shouldShowRequestPermissionRationale(activity, permission);
        }
        return false;
    }

    private static ActivityCompat21.SharedElementCallback21 createCallback(
            SharedElementCallback callback) {
        ActivityCompat21.SharedElementCallback21 newCallback = null;
        if (callback != null) {
            newCallback = new ActivityCompat.SharedElementCallback21Impl(callback);
        }
        return newCallback;
    }

    private static class SharedElementCallback21Impl
            extends ActivityCompat21.SharedElementCallback21 {

        private SharedElementCallback mCallback;

        public SharedElementCallback21Impl(SharedElementCallback callback) {
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
    }
}
