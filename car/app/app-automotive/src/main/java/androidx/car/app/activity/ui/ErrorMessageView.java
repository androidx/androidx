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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.activity.CarAppActivity;
import androidx.car.app.activity.CarAppViewModel;
import androidx.car.app.activity.ErrorHandler;
import androidx.car.app.automotive.R;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

/**
 * A {@link Fragment} to show error message on {@link CarAppActivity}
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class ErrorMessageView extends LinearLayout {
    private static final String VENDING_PACKAGE = "com.android.vending";
    private static final String VENDING_DETAIL_URL =
            "https://play.google.com/store/apps/details?id=";
    private static final String ACTION_RENDER = "android.car.template.host.RendererService";

    private TextView mErrorMessage;
    private Button mActionButton;
    @Nullable
    private ErrorHandler.ErrorType mErrorType;

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

    /** Updates the error displayed by this view */
    public void setError(@Nullable ErrorHandler.ErrorType errorType) {
        mErrorType = errorType;
        mErrorMessage.setText(mErrorType != null
                ? getContext().getString(mErrorType.getMessageResId())
                : null);
        mActionButton.setText(mErrorType != null
                ? getContext().getString(mErrorType.getActionType().getActionResId())
                : null);
        mActionButton.setVisibility(mErrorType != null ? View.VISIBLE : View.GONE);
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

    private Intent getVendingIntent() {
        Intent rendererIntent = new Intent(ACTION_RENDER);
        List<ResolveInfo> resolveInfoList =
                requireActivity().getPackageManager().queryIntentServices(
                    rendererIntent,
                    PackageManager.GET_META_DATA
            );
        // Redirect to the PlayStore package detail if only one package that handles
        // ACTION_RENDER is found. if found multiple or none, redirect to the PlayStore main
        // page.
        if (resolveInfoList.size() == 1) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(VENDING_PACKAGE);
            intent.setData(Uri.parse(VENDING_DETAIL_URL
                    + resolveInfoList.get(0).serviceInfo.packageName));
            return intent;
        } else {
            return requireActivity().getPackageManager().getLaunchIntentForPackage(VENDING_PACKAGE);
        }
    }

    private FragmentActivity requireActivity() {
        return (FragmentActivity) getContext();
    }
}
