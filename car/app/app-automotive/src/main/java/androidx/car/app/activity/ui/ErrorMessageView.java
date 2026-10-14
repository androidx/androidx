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

package androidx.car.app.activity.ui;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.activity.ErrorHandler.ActionType.UPDATE_HOST;
import static androidx.car.app.activity.LogTags.TAG_ERROR;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.car.app.activity.CarAppActivity;
import androidx.car.app.activity.CarAppViewModel;
import androidx.car.app.activity.ErrorHandler;
import androidx.car.app.automotive.R;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} to show error message on {@link CarAppActivity}
 *
 */
@RestrictTo(LIBRARY)
public final class ErrorMessageView extends LinearLayout {

    private static final String VENDING_PACKAGE = "com.android.vending";
    private static final String MARKET_DETAILS_URI = "market://details?id=";
    private static final String ACTION_RENDER = "android.car.template.host.RendererService";

    // TODO(b/194324567): Remove the hard coded Google templates host package name
    // This is only used as a Play Store specific fallback when no host is installed, as the Google
    // templates host is the only host known to be distributed through the Play Store.
    private static final String GAS_HOST_PACKAGE =
            "com.google.android.apps.automotive.templates.host";

    private TextView mErrorMessage;
    private Button mActionButton;
    private ErrorHandler.@Nullable ErrorType mErrorType;
    private final List<Intent> mStoreIntents = new ArrayList<>();

    public ErrorMessageView(@NonNull Context context) {
        super(context);
    }

    public ErrorMessageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ErrorMessageView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ErrorMessageView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mErrorMessage = requireViewById(R.id.error_message);
        mActionButton = requireViewById(R.id.action_button);
        mActionButton.setOnClickListener(v -> onClick());
    }

    /**
     * Updates the error displayed by this view
     */
    public void setError(ErrorHandler.@Nullable ErrorType errorType) {
        mErrorType = errorType;
        mStoreIntents.clear();
        mErrorMessage.setText(mErrorType != null
                ? getContext().getString(mErrorType.getMessageResId())
                : null);
        mActionButton.setText(mErrorType != null
                ? getContext().getString(mErrorType.getActionType().getActionResId())
                : null);
        mActionButton.setVisibility(mErrorType != null ? View.VISIBLE : View.GONE);

        // If no app store can be used to install or update the host, hide the button and update
        // the message.
        if (mErrorType != null && mErrorType.getActionType() == UPDATE_HOST) {
            mStoreIntents.addAll(resolveStoreIntents());
            if (mStoreIntents.isEmpty()) {
                showNoStoreAvailable();
            }
        }
    }

    private void onClick() {
        if (mErrorType == null) {
            return;
        }
        switch (mErrorType.getActionType()) {
            case UPDATE_HOST:
                if (startFirstAvailableStoreIntent()) {
                    requireActivity().finish();
                } else {
                    showNoStoreAvailable();
                }
                return;
            case FINISH:
                requireActivity().finish();
                return;
            case RETRY:
                new ViewModelProvider(requireActivity())
                        .get(CarAppViewModel.class)
                        .retryBinding();
                return;
        }
        throw new IllegalArgumentException("Unknown action type: " + mErrorType.getActionType());
    }

    private void showNoStoreAvailable() {
        mActionButton.setVisibility(INVISIBLE);
        mErrorMessage.setText(R.string.error_message_no_vending);
    }

    /**
     * Launches the first store intent that can be started.
     *
     * <p>The store state might have changed since the intents were resolved (e.g.: the store was
     * disabled), so a failure to launch an intent falls back to the next one.
     *
     * @return whether an intent was launched
     */
    private boolean startFirstAvailableStoreIntent() {
        for (Intent intent : mStoreIntents) {
            try {
                getContext().startActivity(intent);
                return true;
            } catch (ActivityNotFoundException | SecurityException e) {
                Log.w(TAG_ERROR, "Unable to launch app store intent: " + intent, e);
            }
        }
        return false;
    }

    /**
     * Returns the intents that can be used to send the user to an app store where the host can be
     * installed or updated, in order of preference. Only intents that resolve to an activity are
     * returned.
     *
     * <p>It is not possible to know whether an app store actually handled an intent once it is
     * launched (e.g.: whether it knows about the requested package), so the intent to use is
     * decided before launching it.
     */
    @SuppressWarnings("deprecation")
    private List<Intent> resolveStoreIntents() {
        PackageManager packageManager = getContext().getPackageManager();
        List<Intent> storeIntents = new ArrayList<>();
        for (Intent candidate : getStoreIntentCandidates(packageManager)) {
            // Flags are intentionally 0, rather than MATCH_DEFAULT_ONLY, so that activities which
            // only support explicit intents are also found. These are then targeted explicitly.
            List<ResolveInfo> resolveInfoList =
                    packageManager.queryIntentActivities(candidate, /* flags= */ 0);
            if (resolveInfoList.isEmpty()) {
                continue;
            }
            if (resolveInfoList.size() == 1) {
                storeIntents.add(toExplicitIntent(candidate, resolveInfoList.get(0)));
                continue;
            }
            // startActivity() only considers activities declaring CATEGORY_DEFAULT for implicit
            // intents. Let the system resolve the intent, which might involve asking the user to
            // choose an app store, only if it can do so.
            if (packageManager.queryIntentActivities(candidate,
                    PackageManager.MATCH_DEFAULT_ONLY).size() > 1) {
                storeIntents.add(candidate);
            }
            // Fall back to targeting each matching app store explicitly.
            for (ResolveInfo resolveInfo : resolveInfoList) {
                storeIntents.add(toExplicitIntent(candidate, resolveInfo));
            }
        }
        return storeIntents;
    }

    /**
     * Returns the intents that might be used to send the user to an app store where the host can
     * be installed or updated, in order of preference.
     */
    private static List<Intent> getStoreIntentCandidates(PackageManager packageManager) {
        List<Intent> candidates = new ArrayList<>();
        String hostPackage = findHostPackage(packageManager);
        if (hostPackage != null) {
            // Prefer the app store that installed the host, as it is the one able to update it.
            String installerPackage = getInstallerPackageName(packageManager, hostPackage);
            if (installerPackage != null) {
                candidates.add(new Intent(Intent.ACTION_SHOW_APP_INFO)
                        .setPackage(installerPackage)
                        .putExtra(Intent.EXTRA_PACKAGE_NAME, hostPackage));
                candidates.add(getMarketDetailsIntent(hostPackage).setPackage(installerPackage));
            }
            candidates.add(getMarketDetailsIntent(hostPackage));
        } else {
            Log.d(TAG_ERROR, "No host found on the device, falling back to the GAS host page");
            candidates.add(getMarketDetailsIntent(GAS_HOST_PACKAGE).setPackage(VENDING_PACKAGE));
        }
        // As a last resort, open the main page of any app store.
        candidates.add(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET));
        return candidates;
    }

    /** Returns the package name of the host, if exactly one host is installed. */
    @SuppressWarnings("deprecation")
    private static @Nullable String findHostPackage(PackageManager packageManager) {
        List<ResolveInfo> resolveInfoList =
                packageManager.queryIntentServices(new Intent(ACTION_RENDER), /* flags= */ 0);
        if (resolveInfoList.size() == 1) {
            return resolveInfoList.get(0).serviceInfo.packageName;
        }
        return null;
    }

    /** Returns the package name of the app that installed the given package, if known. */
    @SuppressWarnings("deprecation")
    private static @Nullable String getInstallerPackageName(PackageManager packageManager,
            String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Api30Impl.getInstallingPackageName(packageManager, packageName);
            }
            return packageManager.getInstallerPackageName(packageName);
        } catch (NameNotFoundException | IllegalArgumentException e) {
            Log.d(TAG_ERROR, "Unable to find the installer of " + packageName, e);
            return null;
        }
    }

    private static Intent toExplicitIntent(Intent intent, ResolveInfo resolveInfo) {
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        return new Intent(intent).setClassName(activityInfo.packageName, activityInfo.name);
    }

    private static Intent getMarketDetailsIntent(String packageName) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_DETAILS_URI + packageName));
    }

    private FragmentActivity requireActivity() {
        return (FragmentActivity) getContext();
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static final class Api30Impl {
        private Api30Impl() {
        }

        static @Nullable String getInstallingPackageName(PackageManager packageManager,
                String packageName) throws NameNotFoundException {
            return packageManager.getInstallSourceInfo(packageName).getInstallingPackageName();
        }
    }
}
