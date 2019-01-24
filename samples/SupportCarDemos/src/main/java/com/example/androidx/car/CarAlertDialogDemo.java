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

package com.example.androidx.car;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.app.AlertDialog;
import androidx.car.app.CarAlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

/**
 * A demo activity that will display a {@link CarAlertDialog} with configurable options for what is
 * in the contents of that resulting dialog.
 */
public class CarAlertDialogDemo extends FragmentActivity {
    private static final String DIALOG_TAG = "alert_dialog_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_dialog_activity);

        CheckBox hasTitleView = findViewById(R.id.has_title);
        CheckBox hasImageView = findViewById(R.id.has_image);
        CheckBox hasBodyText = findViewById(R.id.has_body_text);
        CheckBox hasClickableBodyText = findViewById(R.id.has_clickable_body_text);
        CheckBox hasSingleLineBody = findViewById(R.id.has_single_line_body);
        CheckBox hasAction1 = findViewById(R.id.has_action_1);
        CheckBox hasAction2 = findViewById(R.id.has_action_2);

        View createSupportDialogButton = findViewById(R.id.create_support_dialog);

        View.OnClickListener mButtonListener = v -> {
            AlertDialogFragment alertDialog = AlertDialogFragment.newInstance(
                    /* createSupportDialog= */ v == createSupportDialogButton,
                    hasTitleView.isChecked(),
                    hasImageView.isChecked(),
                    hasBodyText.isChecked(),
                    hasClickableBodyText.isChecked(),
                    hasSingleLineBody.isChecked(),
                    hasAction1.isChecked(),
                    hasAction2.isChecked());

            alertDialog.show(getSupportFragmentManager(), DIALOG_TAG);
        };

        createSupportDialogButton.setOnClickListener(mButtonListener);
        findViewById(R.id.create_car_dialog).setOnClickListener(mButtonListener);
    }

    /** A {@link DialogFragment} that will inflate a {@link CarAlertDialog}. */
    public static class AlertDialogFragment extends DialogFragment {
        private static final String CREATE_SUPPORT_DIALOG_KEY = "create_support_dialog_key";
        private static final String HAS_TITLE_KEY = "has_title_key";
        private static final String HAS_IMAGE_KEY = "has_image_key";
        private static final String HAS_BODY_KEY = "has_body_key";
        private static final String HAS_CLICKABLE_BODY_KEY = "has_clickable_body_key";
        private static final String HAS_SINGLE_LINE_BODY_KEY = "has_single_line_body_key";
        private static final String HAS_ACTION_1_KEY = "has_action_1_key";
        private static final String HAS_ACTION_2_KEY = "has_action_2_key";

        static AlertDialogFragment newInstance(
                boolean createSupportDialog,
                boolean hasTitle,
                boolean hasImage,
                boolean hasBody,
                boolean hasClickableBodyText,
                boolean hasSingleLineBody,
                boolean hasAction1,
                boolean hasAction2) {
            Bundle args = new Bundle();
            args.putBoolean(CREATE_SUPPORT_DIALOG_KEY, createSupportDialog);
            args.putBoolean(HAS_TITLE_KEY, hasTitle);
            args.putBoolean(HAS_IMAGE_KEY, hasImage);
            args.putBoolean(HAS_BODY_KEY, hasBody);
            args.putBoolean(HAS_CLICKABLE_BODY_KEY, hasClickableBodyText);
            args.putBoolean(HAS_SINGLE_LINE_BODY_KEY, hasSingleLineBody);
            args.putBoolean(HAS_ACTION_1_KEY, hasAction1);
            args.putBoolean(HAS_ACTION_2_KEY, hasAction2);

            AlertDialogFragment fragment = new AlertDialogFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return getArguments().getBoolean(CREATE_SUPPORT_DIALOG_KEY)
                    ? createSupportDialog()
                    : createCarDialog();
        }

        private Dialog createSupportDialog() {
            Context context = getContext();
            Bundle args = getArguments();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            if (args.getBoolean(HAS_TITLE_KEY)) {
                builder.setTitle(context.getString(R.string.alert_dialog_title));
            }

            if (args.getBoolean(HAS_IMAGE_KEY)) {
                int sampleIcon = android.R.drawable.sym_def_app_icon;
                builder.setIcon(sampleIcon);
            }

            if (args.getBoolean(HAS_BODY_KEY)) {
                builder.setMessage(context.getString(R.string.alert_dialog_body));
            }

            if (args.getBoolean(HAS_SINGLE_LINE_BODY_KEY)) {
                builder.setMessage(context.getString(R.string.alert_dialog_body_single_line));
            }

            if (args.getBoolean(HAS_ACTION_1_KEY)) {
                builder.setPositiveButton(context.getString(R.string.alert_dialog_action1),
                        /* listener= */ null);
            }

            if (args.getBoolean(HAS_ACTION_2_KEY)) {
                builder.setNegativeButton(context.getString(R.string.alert_dialog_action2),
                        /* listener= */ null);
            }
            return builder.create();
        }

        private Dialog createCarDialog() {
            Context context = getContext();
            Bundle args = getArguments();
            CarAlertDialog.Builder builder = new CarAlertDialog.Builder(context);

            if (args.getBoolean(HAS_TITLE_KEY)) {
                builder.setTitle(context.getString(R.string.alert_dialog_title));
            }

            if (args.getBoolean(HAS_IMAGE_KEY)) {
                int sampleIcon = android.R.drawable.sym_def_app_icon;
                builder.setHeaderIcon(sampleIcon);
            }

            if (args.getBoolean(HAS_BODY_KEY)) {
                if (args.getBoolean(HAS_CLICKABLE_BODY_KEY)) {
                    builder.setBody(context.getText(R.string.test_link));
                    builder.setBodyMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    builder.setBody(context.getString(R.string.alert_dialog_body));
                }
            }

            if (args.getBoolean(HAS_SINGLE_LINE_BODY_KEY)) {
                builder.setBody(context.getString(R.string.alert_dialog_body_single_line));
            }

            if (args.getBoolean(HAS_ACTION_1_KEY)) {
                builder.setPositiveButton(context.getString(R.string.alert_dialog_action1),
                        /* listener= */ null);
            }

            if (args.getBoolean(HAS_ACTION_2_KEY)) {
                builder.setNegativeButton(context.getString(R.string.alert_dialog_action2),
                        /* listener= */ null);
            }
            return builder.create();
        }
    }
}
