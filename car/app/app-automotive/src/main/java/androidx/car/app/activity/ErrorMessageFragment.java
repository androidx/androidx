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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.R;
import androidx.fragment.app.Fragment;

import java.util.List;

/**
 * A {@link Fragment} to show error message on {@link CarAppActivity}
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class ErrorMessageFragment extends Fragment {
    private static final String ERROR_TYPE_ARGS_KEY = "errorType";
    private static final String VENDING_PACKAGE = "com.android.vending";
    static final String ACTION_RENDER = "android.car.template.host.RendererService";

    /** Returns an new Instance of {@link ErrorMessageFragment} */
    @NonNull
    static ErrorMessageFragment newInstance(@NonNull ErrorHandler.ErrorType errorType) {
        ErrorMessageFragment errorMessageFragment = new ErrorMessageFragment();
        errorMessageFragment.setArguments(getBundle(errorType));
        return errorMessageFragment;
    }

    @VisibleForTesting
    @NonNull
    static Bundle getBundle(ErrorHandler.ErrorType errorType) {
        Bundle bundle = new Bundle();
        bundle.putInt(ERROR_TYPE_ARGS_KEY, errorType.ordinal());
        return bundle;
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.error_message_fragment, container, false);
        update(rootView);
        return rootView;
    }

    private void update(@NonNull View rootView) {
        ErrorHandler.ErrorType errorType = getErrorType();
        TextView errorMessage = rootView.findViewById(R.id.error_message);
        Button actionButton = rootView.findViewById(R.id.action_button);
        errorMessage.setText(getString(errorType.getMessageResId()));
        actionButton.setText(getString(errorType.getActionType().getActionResId()));
        actionButton.setOnClickListener(v -> onClick(errorType.getActionType()));
    }

    @VisibleForTesting
    @NonNull
    ErrorHandler.ErrorType getErrorType() {
        Bundle args = requireArguments();
        return ErrorHandler.ErrorType.values()[args.getInt(ERROR_TYPE_ARGS_KEY)];
    }

    private void onClick(@NonNull ErrorHandler.ActionType actionType) {
        switch (actionType) {
            case UPDATE_HOST:
                startActivity(getVendingIntent());
                // Fall through
            case FINISH:
                requireActivity().finish();
                return;
        }
        throw new IllegalArgumentException("Unknown action type: " + actionType);
    }

    private Intent getVendingIntent() {
        Intent rendererIntent = new Intent(ACTION_RENDER);
        List<ResolveInfo> resolveInfoList =
                requireActivity().getPackageManager().queryIntentServices(
                    rendererIntent,
                    PackageManager.GET_META_DATA
            );
        // Redirect to the PlayStore package detail if only one packages that handles
        // ACTION_RENDER is found. if found multiple or none, redirect to the PlayStore main
        // page.
        if (resolveInfoList.size() == 1) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage("com.android.vending");
            intent.setData(Uri.parse(
                    "https://play.google.com/store/apps/details?id="
                        + resolveInfoList.get(0).serviceInfo.packageName)
            );
            return intent;
        } else {
            return requireActivity().getPackageManager().getLaunchIntentForPackage(VENDING_PACKAGE);
        }
    }
}
