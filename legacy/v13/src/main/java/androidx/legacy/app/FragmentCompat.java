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

package androidx.legacy.app;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.Arrays;

/**
 * Helper for accessing features in {@link Fragment} in a backwards compatible fashion.
 *
 * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework fragment.
 */
@Deprecated
public class FragmentCompat {

    /**
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework fragment.
     */
    @Deprecated
    public FragmentCompat() {
    }

    interface FragmentCompatImpl {
        void setUserVisibleHint(Fragment f, boolean deferStart);
        void requestPermissions(Fragment fragment, String[] permissions, int requestCode);
        boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission);
    }

    /**
     * Customizable delegate that allows delegating permission related compatibility methods
     * to a custom implementation.
     *
     * <p>
     *     To delegate fragment compatibility methods to a custom class, implement this interface,
     *     and call {@code FragmentCompat.setPermissionCompatDelegate(delegate);}. All future calls
     *     to the compatibility methods in this class will first check whether the delegate can
     *     handle the method call, and invoke the corresponding method if it can.
     * </p>
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public interface PermissionCompatDelegate {

        /**
         * Determines whether the delegate should handle
         * {@link FragmentCompat#requestPermissions(Fragment, String[], int)}, and request
         * permissions if applicable. If this method returns true, it means that permission
         * request is successfully handled by the delegate, and platform should not perform any
         * further requests for permission.
         *
         * @param fragment The target fragment.
         * @param permissions The requested permissions.
         * @param requestCode Application specific request code to match with a result
         *    reported to {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(
         *    int, String[], int[])}.
         *
         * @return Whether the delegate has handled the permission request.
         * @see FragmentCompat#requestPermissions(Fragment, String[], int)
         *
         * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
         * {@link Fragment}.
         */
        @Deprecated
        boolean requestPermissions(Fragment fragment, String[] permissions, int requestCode);
    }

    static class FragmentCompatBaseImpl implements FragmentCompatImpl {
        @Override
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
        }
        @Override
        public void requestPermissions(final Fragment fragment, final String[] permissions,
                final int requestCode) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final int[] grantResults = new int[permissions.length];

                    Context context = fragment.getActivity();
                    if (context != null) {
                        PackageManager packageManager = context.getPackageManager();
                        String packageName = context.getPackageName();

                        final int permissionCount = permissions.length;
                        for (int i = 0; i < permissionCount; i++) {
                            grantResults[i] = packageManager.checkPermission(
                                    permissions[i], packageName);
                        }
                    } else {
                        Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
                    }

                    ((OnRequestPermissionsResultCallback) fragment).onRequestPermissionsResult(
                            requestCode, permissions, grantResults);
                }
            });
        }
        @Override
        public boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission) {
            return false;
        }
    }

    @RequiresApi(15)
    static class FragmentCompatApi15Impl extends FragmentCompatBaseImpl {
        @Override
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            f.setUserVisibleHint(deferStart);
        }
    }

    @RequiresApi(23)
    static class FragmentCompatApi23Impl extends FragmentCompatApi15Impl {
        @Override
        public void requestPermissions(Fragment fragment, String[] permissions, int requestCode) {
            fragment.requestPermissions(permissions, requestCode);
        }

        @Override
        public boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission) {
            return fragment.shouldShowRequestPermissionRationale(permission);
        }
    }

    @RequiresApi(24)
    static class FragmentCompatApi24Impl extends FragmentCompatApi23Impl {
        @Override
        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            f.setUserVisibleHint(deferStart);
        }
    }

    static final FragmentCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 24) {
            IMPL = new FragmentCompatApi24Impl();
        } else if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new FragmentCompatApi23Impl();
        } else if (android.os.Build.VERSION.SDK_INT >= 15) {
            IMPL = new FragmentCompatApi15Impl();
        } else {
            IMPL = new FragmentCompatBaseImpl();
        }
    }

    private static PermissionCompatDelegate sDelegate;

    /**
     * Sets the permission delegate for {@code FragmentCompat}. Replaces the previously set
     * delegate.
     *
     * @param delegate The delegate to be set. {@code null} to clear the set delegate.
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public static void setPermissionCompatDelegate(PermissionCompatDelegate delegate) {
        sDelegate = delegate;
    }

    /**
     * @hide
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated
    public static PermissionCompatDelegate getPermissionCompatDelegate() {
        return sDelegate;
    }

    /**
     * This interface is the contract for receiving the results for permission requests.
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public interface OnRequestPermissionsResultCallback {

        /**
         * Callback for the result from requesting permissions. This method
         * is invoked for every call on {@link #requestPermissions(android.app.Fragment,
         * String[], int)}
         *
         * @param requestCode The request code passed in {@link #requestPermissions(
         *     android.app.Fragment, String[], int)}
         * @param permissions The requested permissions. Never null.
         * @param grantResults The grant results for the corresponding permissions
         *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
         *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
         *
         * @see #requestPermissions(android.app.Fragment, String[], int)
         *
         * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
         * {@link Fragment}.
         */
        @Deprecated
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                @NonNull int[] grantResults);
    }

    /**
     * Call {@link Fragment#setMenuVisibility(boolean) Fragment.setMenuVisibility(boolean)}
     * if running on an appropriate version of the platform.
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public static void setMenuVisibility(Fragment f, boolean visible) {
        f.setMenuVisibility(visible);
    }

    /**
     * Call {@link Fragment#setUserVisibleHint(boolean) setUserVisibleHint(boolean)}
     * if running on an appropriate version of the platform.
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public static void setUserVisibleHint(Fragment f, boolean deferStart) {
        IMPL.setUserVisibleHint(f, deferStart);
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
     * permissions were granted or not. Your fragment has to implement {@link
     * OnRequestPermissionsResultCallback}
     * and the results of permission requests will be delivered to its
     * {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(
     * int, String[], int[])}.
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
     * androidx.core.content.ContextCompat#checkSelfPermission(
     * android.content.Context, String)}.
     * </p>
     *
     * @param fragment The target fragment.
     * @param permissions The requested permissions.
     * @param requestCode Application specific request code to match with a result
     *    reported to {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(
     *    int, String[], int[])}.
     *
     * @see androidx.core.content.ContextCompat#checkSelfPermission(
     *     android.content.Context, String)
     * @see #shouldShowRequestPermissionRationale(android.app.Fragment, String)
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public static void requestPermissions(@NonNull Fragment fragment,
            @NonNull String[] permissions, int requestCode) {
        if (sDelegate != null && sDelegate.requestPermissions(fragment, permissions, requestCode)) {
            // Delegate has handled the request.
            return;
        }

        IMPL.requestPermissions(fragment, permissions, requestCode);
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
     * @param fragment The target fragment.
     * @param permission A permission your app wants to request.
     * @return Whether you can show permission rationale UI.
     *
     * @see androidx.core.content.ContextCompat#checkSelfPermission(
     *     android.content.Context, String)
     * @see #requestPermissions(android.app.Fragment, String[], int)
     *
     * @deprecated Use {@link androidx.fragment.app.Fragment} instead of the framework
     * {@link Fragment}.
     */
    @Deprecated
    public static boolean shouldShowRequestPermissionRationale(@NonNull Fragment fragment,
            @NonNull String permission) {
        return IMPL.shouldShowRequestPermissionRationale(fragment, permission);
    }
}
