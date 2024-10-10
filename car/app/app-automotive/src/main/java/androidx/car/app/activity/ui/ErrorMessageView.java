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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.List;

/**
 * A {@link Fragment} to show error message on {@link CarAppActivity}
 *
 */
@RestrictTo(LIBRARY)
public final class ErrorMessageView extends LinearLayout {

    private static final String VENDING_PACKAGE = "com.android.vending";
    private static final String VENDING_DETAIL_URL =
            "https://play.google.com/store/apps/details?id=";
    private static final String ACTION_RENDER = "android.car.template.host.RendererService";

    // TODO(b/194324567): Remove the hard coded Google templates host package name
    private static final String HOST_PACKAGE = "com.google.android.apps.automotive.templates.host";

    private TextView mErrorMessage;
    private Button mActionButton;
    private ErrorHandler.@Nullable ErrorType mErrorType;

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
        mErrorMessage.setText(mErrorType != null
                ? getContext().getString(mErrorType.getMessageResId())
                : null);
        mActionButton.setText(mErrorType != null
                ? getContext().getString(mErrorType.getActionType().getActionResId())
                : null);
        mActionButton.setVisibility(mErrorType != null ? View.VISIBLE : View.GONE);

        // If the vending app is not installed, hide the button and update the message.
        if (mErrorType != null && mErrorType.getActionType() == UPDATE_HOST
                && !isVendingPackageInstalled()) {
            mActionButton.setVisibility(INVISIBLE);
            mErrorMessage.setText(R.string.error_message_no_vending);
        }
    }

    private void onClick() {
        if (mErrorType == null) {
            return;
        }
        switch (mErrorType.getActionType()) {
            case UPDATE_HOST:
                getContext().startActivity(getVendingIntent());
                requireActivity().finish();
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

    @SuppressWarnings("deprecation")
    private boolean isVendingPackageInstalled() {
        try {
            requireActivity().getPackageManager().getPackageInfo(VENDING_PACKAGE, 0);
        } catch (NameNotFoundException e) {
            Log.d(TAG_ERROR, "The vending app not found");
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private Intent getVendingIntent() {
        Intent rendererIntent = new Intent(ACTION_RENDER);
        List<ResolveInfo> resolveInfoList =
                requireActivity().getPackageManager().queryIntentServices(
                        rendererIntent,
                        PackageManager.GET_META_DATA
                );
        // Redirect to the vending app package detail if only one package that handles
        // ACTION_RENDER is found.
        // Redirect to GAS host page if found no package that handles ACTION_RENDER.
        // if found multiple or none, redirect to the vending app main page.
        if (resolveInfoList.size() == 1) {
            Log.d(TAG_ERROR, "Find a host, redirect to the page for this host.");
            return getHostPageIntent(resolveInfoList.get(0).serviceInfo.packageName);
        } else if (resolveInfoList.size() == 0) {
            Log.d(TAG_ERROR, "No host found on the device, redirect to GAS host page");
            return getHostPageIntent(HOST_PACKAGE);
        } else {
            Log.d(TAG_ERROR, "Multiple host found, redirect to the vending app main page");
            return requireActivity().getPackageManager().getLaunchIntentForPackage(VENDING_PACKAGE);
        }
    }

    private Intent getHostPageIntent(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(VENDING_PACKAGE);
        intent.setData(Uri.parse(VENDING_DETAIL_URL + packageName));
        return intent;
    }

    private FragmentActivity requireActivity() {
        return (FragmentActivity) getContext();
    }
}
