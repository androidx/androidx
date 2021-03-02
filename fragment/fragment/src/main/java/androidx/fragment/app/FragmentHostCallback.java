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

package androidx.fragment.app;

import static androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import static androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultRegistryOwner;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Integration points with the Fragment host.
 * <p>
 * Fragments may be hosted by any object; such as an {@link Activity}. In order to
 * host fragments, implement {@link FragmentHostCallback}, overriding the methods
 * applicable to the host.
 * <p>
 * FragmentManager changes its behavior based on what optional interfaces your
 * FragmentHostCallback implements. This includes the following:
 * <ul>
 *     <li><strong>{@link ActivityResultRegistryOwner}</strong>: Removes the need to
 *     override {@link #onStartIntentSenderFromFragment} or
 *     {@link #onRequestPermissionsFromFragment}.</li>
 *     <li><strong>{@link FragmentOnAttachListener}</strong>: Removes the need to
 *     manually call {@link FragmentManager#addFragmentOnAttachListener} from your
 *     host in order to receive {@link FragmentOnAttachListener#onAttachFragment} callbacks
 *     for the {@link FragmentController#getSupportFragmentManager()}.</li>
 *     <li><strong>{@link androidx.activity.OnBackPressedDispatcherOwner}</strong>: Removes
 *     the need to manually call
 *     {@link FragmentManager#popBackStackImmediate()} when handling the system
 *     back button.</li>
 *     <li><strong>{@link androidx.lifecycle.ViewModelStoreOwner}</strong>: Removes the need
 *     for your {@link FragmentController} to call
 *     {@link FragmentController#retainNestedNonConfig()} or
 *     {@link FragmentController#restoreAllState(Parcelable, FragmentManagerNonConfig)}.</li>
 * </ul>
 *
 * @param <E> the type of object that's currently hosting the fragments. An instance of this
 *           class must be returned by {@link #onGetHost()}.
 */
@SuppressWarnings("deprecation")
public abstract class FragmentHostCallback<E> extends FragmentContainer {
    @Nullable private final Activity mActivity;
    @NonNull private final Context mContext;
    @NonNull private final Handler mHandler;
    private final int mWindowAnimations;
    final FragmentManager mFragmentManager = new FragmentManagerImpl();

    public FragmentHostCallback(@NonNull Context context, @NonNull Handler handler,
            int windowAnimations) {
        this(context instanceof Activity ? (Activity) context : null, context, handler,
                windowAnimations);
    }

    @SuppressWarnings("deprecation")
    FragmentHostCallback(@NonNull FragmentActivity activity) {
        this(activity, activity /*context*/, new Handler(), 0 /*windowAnimations*/);
    }

    FragmentHostCallback(@Nullable Activity activity, @NonNull Context context,
            @NonNull Handler handler, int windowAnimations) {
        mActivity = activity;
        mContext = Preconditions.checkNotNull(context, "context == null");
        mHandler = Preconditions.checkNotNull(handler, "handler == null");
        mWindowAnimations = windowAnimations;
    }

    /**
     * Print internal state into the given stream.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state. This will be closed
     *                  for you after you return.
     * @param args additional arguments to the dump request.
     */
    public void onDump(@NonNull String prefix, @Nullable FileDescriptor fd,
            @NonNull PrintWriter writer, @Nullable String[] args) {
    }

    /**
     * Return {@code true} if the fragment's state needs to be saved.
     */
    public boolean onShouldSaveFragmentState(@NonNull Fragment fragment) {
        return true;
    }

    /**
     * Return a {@link LayoutInflater}.
     * See {@link Activity#getLayoutInflater()}.
     */
    @NonNull
    public LayoutInflater onGetLayoutInflater() {
        return LayoutInflater.from(mContext);
    }

    /**
     * Return the object that's currently hosting the fragment. If a {@link Fragment}
     * is hosted by a {@link FragmentActivity}, the object returned here should be
     * the same object returned from {@link Fragment#getActivity()}.
     */
    @Nullable
    public abstract E onGetHost();

    /**
     * Invalidates the activity's options menu.
     * See {@link FragmentActivity#supportInvalidateOptionsMenu()}
     */
    public void onSupportInvalidateOptionsMenu() {
    }

    /**
     * Starts a new {@link Activity} from the given fragment.
     * See {@link FragmentActivity#startActivityForResult(Intent, int)}.
     */
    public void onStartActivityFromFragment(@NonNull Fragment fragment,
            @SuppressLint("UnknownNullness") Intent intent, int requestCode) {
        onStartActivityFromFragment(fragment, intent, requestCode, null);
    }

    /**
     * Starts a new {@link Activity} from the given fragment.
     * See {@link FragmentActivity#startActivityForResult(Intent, int, Bundle)}.
     */
    public void onStartActivityFromFragment(
            @NonNull Fragment fragment, @SuppressLint("UnknownNullness") Intent intent,
            int requestCode, @Nullable Bundle options) {
        if (requestCode != -1) {
            throw new IllegalStateException(
                    "Starting activity with a requestCode requires a FragmentActivity host");
        }
        ContextCompat.startActivity(mContext, intent, options);
    }

    /**
     * Starts a new {@link IntentSender} from the given fragment.
     * See {@link Activity#startIntentSender(IntentSender, Intent, int, int, int, Bundle)}.
     *
     * @deprecated Have your FragmentHostCallback implement {@link ActivityResultRegistryOwner}
     * to allow Fragments to use
     * {@link Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * with {@link StartIntentSenderForResult}. This method will still be called when Fragments
     * call the deprecated <code>startIntentSenderForResult()</code> method.
     */
    @Deprecated
    public void onStartIntentSenderFromFragment(@NonNull Fragment fragment,
            @SuppressLint("UnknownNullness") IntentSender intent, int requestCode,
            @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            @Nullable Bundle options) throws IntentSender.SendIntentException {
        if (requestCode != -1) {
            throw new IllegalStateException(
                    "Starting intent sender with a requestCode requires a FragmentActivity host");
        }
        ActivityCompat.startIntentSenderForResult(mActivity, intent, requestCode, fillInIntent,
                flagsMask, flagsValues, extraFlags, options);
    }

    /**
     * Requests permissions from the given fragment.
     * See {@link FragmentActivity#requestPermissions(String[], int)}
     *
     * @deprecated Have your FragmentHostCallback implement {@link ActivityResultRegistryOwner}
     * to allow Fragments to use
     * {@link Fragment#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
     * with {@link RequestMultiplePermissions}. This method will still be called when Fragments
     * call the deprecated <code>requestPermissions()</code> method.
     */
    @Deprecated
    public void onRequestPermissionsFromFragment(@NonNull Fragment fragment,
            @NonNull String[] permissions, int requestCode) {
    }

    /**
     * Checks whether to show permission rationale UI from a fragment.
     * See {@link FragmentActivity#shouldShowRequestPermissionRationale(String)}
     */
    public boolean onShouldShowRequestPermissionRationale(@NonNull String permission) {
        return false;
    }

    /**
     * Return {@code true} if there are window animations.
     */
    public boolean onHasWindowAnimations() {
        return true;
    }

    /**
     * Return the window animations.
     */
    public int onGetWindowAnimations() {
        return mWindowAnimations;
    }

    @Nullable
    @Override
    public View onFindViewById(int id) {
        return null;
    }

    @Override
    public boolean onHasView() {
        return true;
    }

    @Nullable
    Activity getActivity() {
        return mActivity;
    }

    @NonNull
    Context getContext() {
        return mContext;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public Handler getHandler() {
        return mHandler;
    }
}
