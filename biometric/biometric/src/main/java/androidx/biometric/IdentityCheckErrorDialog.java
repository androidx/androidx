/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.biometric;


import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


import androidx.appcompat.app.AlertDialog;

import org.jspecify.annotations.NonNull;


/** Initializes and shows biometric error dialogs related to identity check. */
class IdentityCheckErrorDialog {
    private static final String TAG = "ICErrorDialog";

    private IdentityCheckErrorDialog() {
    }

    interface IdentityCheckErrorDialogListener {
        void onDismissed();
    }

    static void createDialog(Context context, boolean isLockoutError,
            IdentityCheckErrorDialogListener listener) {
        // TODO(b/375693808): support dark theme and rounded corner
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater.inflate(R.layout.identity_check_error_dialog, null);

        setPositiveButton(alertDialogBuilder, isLockoutError);
        setNegativeButton(alertDialogBuilder, isLockoutError);

        final AlertDialog dialog = alertDialogBuilder.create();
        setTitle(customView, isLockoutError);
        setBody(dialog, customView, isLockoutError);
        dialog.setView(customView);

        if (isLockoutError) {
            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        dialog.dismiss();
                    }
                }
            };
            context.registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            dialog.setOnDismissListener(d -> {
                listener.onDismissed();
                context.unregisterReceiver(broadcastReceiver);
            });
        }

        dialog.show();
    }

    private static void setPositiveButton(AlertDialog.Builder alertDialogBuilder, boolean lockout) {
        if (lockout) {
            alertDialogBuilder.setPositiveButton(
                    R.string.identity_check_lockout_error_lock_screen,
                    (dialog, which) -> {
                        dialog.dismiss();
                        lockScreen(alertDialogBuilder.getContext());
                    });
        } else {
            alertDialogBuilder.setPositiveButton(R.string.common_ok,
                    (dialog, which) -> dialog.dismiss());
        }
    }

    private static void setNegativeButton(AlertDialog.Builder alertDialogBuilder, boolean lockout) {
        if (lockout) {
            alertDialogBuilder.setNegativeButton(R.string.common_cancel,
                    (dialog, which) -> dialog.dismiss());
        } else {
            alertDialogBuilder.setNegativeButton(R.string.identity_check_go_to_settings,
                    (dialog, which) -> goToIdentityCheckSettings(alertDialogBuilder.getContext()));
        }
    }

    private static void setTitle(View view, boolean lockout) {
        final TextView titleTextView = view.findViewById(R.id.title);
        if (lockout) {
            titleTextView.setText(R.string.identity_check_lockout_error_title);
        } else {
            titleTextView.setText(R.string.identity_check_general_error_title);
        }
    }

    private static void setBody(AlertDialog dialog, View view, boolean lockout) {
        final TextView textView1 = view.findViewById(R.id.description_1);
        final TextView textView2 = view.findViewById(R.id.description_2);

        if (lockout) {
            textView1.setText(
                    R.string.identity_check_lockout_error_two_factor_auth_description_1);
            textView2.setText(getClickableDescriptionForLockoutError(dialog));
            textView2.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            textView1.setText(R.string.identity_check_general_error_description_1);
            textView2.setVisibility(View.GONE);
        }
    }

    private static SpannableString getClickableDescriptionForLockoutError(AlertDialog dialog) {
        Context context = dialog.getContext();
        final String description = context.getString(
                R.string.identity_check_lockout_error_description_2);
        final SpannableString spannableString = new SpannableString(description);
        final ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View textView) {
                dialog.dismiss();
                goToIdentityCheckSettings(context);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };

        final String goToSettings = context.getString(R.string.identity_check_go_to_settings);
        final int startIndex = description.indexOf(goToSettings);
        final int endIndex = startIndex + goToSettings.length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    private static void goToIdentityCheckSettings(Context context) {
        String actionIdentityCheckSettings = Settings.ACTION_SETTINGS;
        final String identityCheckSettingsAction = context.getString(
                context.getResources().getIdentifier("identity_check_settings_action",
                        "string", "android"));
        actionIdentityCheckSettings =
                identityCheckSettingsAction.isEmpty() ? actionIdentityCheckSettings
                        : identityCheckSettingsAction;

        final Intent autoLockSettingsIntent = new Intent(actionIdentityCheckSettings);
        final ResolveInfo autoLockSettingsInfo = context.getPackageManager()
                .resolveActivity(autoLockSettingsIntent, 0 /* flags */);
        if (autoLockSettingsInfo != null) {
            context.startActivity(autoLockSettingsIntent);
        } else {
            Log.e(TAG, "Identity check settings intent could not be resolved.");
        }
    }

    private static void lockScreen(Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm == null) {
            Log.e(TAG, "Failed to lock screen: DevicePolicyManager was null");
            return;
        }

        try {
            dpm.lockNow();
        } catch (SecurityException ex) {
            Log.e(TAG, "Failed to lock screen: " + ex.getMessage());
        }
    }
}
