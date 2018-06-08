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
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.car.app.CarListDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

/**
 * A demo activity that will display a {@link CarListDialog} with configurable options for what is
 * in the contents of that resulting dialog.
 */
public class CarListDialogDemo extends FragmentActivity {
    private static final String DIALOG_TAG = "list_dialog_tag";

    private static final int DEFAULT_NUM_OF_SECTIONS = 0;
    private static final int DEFAULT_NUM_OF_ITEMS = 4;
    private static final int DEFAULT_INITIAL_POSITION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_dialog_activity);

        EditText numOfSectionsEdit = findViewById(R.id.num_of_sections_edit);
        EditText numOfItemsEdit = findViewById(R.id.num_of_items_edit);
        EditText initialPositionEdit = findViewById(R.id.initial_position_edit);

        findViewById(R.id.create_dialog).setOnClickListener(v -> {
            CharSequence numOfSectionsText = numOfSectionsEdit.getText();
            int numOfSections = TextUtils.isEmpty(numOfSectionsText)
                    ? DEFAULT_NUM_OF_SECTIONS
                    : Integer.parseInt(numOfSectionsText.toString());

            CharSequence numOfItemsText = numOfItemsEdit.getText();
            int numOfItems = TextUtils.isEmpty(numOfItemsText)
                    ? DEFAULT_NUM_OF_ITEMS
                    : Integer.parseInt(numOfItemsText.toString());

            CharSequence initialPositionText = initialPositionEdit.getText();
            int initialPosition = TextUtils.isEmpty(initialPositionText)
                    ? DEFAULT_INITIAL_POSITION
                    : Integer.parseInt(initialPositionText.toString());

            ListDialogFragment alertDialog = ListDialogFragment.newInstance(
                    ((CheckBox) findViewById(R.id.has_title)).isChecked(),
                    numOfSections,
                    numOfItems,
                    initialPosition);

            alertDialog.show(getSupportFragmentManager(), DIALOG_TAG);
        });
    }

    /** A {@link DialogFragment} that will inflate a {@link CarListDialog}. */
    public static class ListDialogFragment extends DialogFragment {
        private static final String HAS_TITLE_KEY = "has_title_key";
        private static final String NUM_OF_SECTIONS_KEY = "num_of_sections_key";
        private static final String NUM_OF_ITEMS_KEY = "num_of_items_key";
        private static final String INITIAL_POSITION_KEY = "initial_position_key";

        static ListDialogFragment newInstance(boolean hasTitle, int numOfSections,
                int numOfItems, int initialPosition) {
            Bundle args = new Bundle();
            args.putBoolean(HAS_TITLE_KEY, hasTitle);
            args.putInt(NUM_OF_SECTIONS_KEY, numOfSections);
            args.putInt(NUM_OF_ITEMS_KEY, numOfItems);
            args.putInt(INITIAL_POSITION_KEY, initialPosition);

            ListDialogFragment fragment = new ListDialogFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CarListDialog.Builder builder = new CarListDialog.Builder(getContext())
                    .setInitialPosition(getArguments().getInt(INITIAL_POSITION_KEY));

            int numOfSections = getArguments().getInt(NUM_OF_SECTIONS_KEY);
            int numOfItems = getArguments().getInt(NUM_OF_ITEMS_KEY);

            if (numOfSections != 0) {
                builder.setItems(createSections(numOfSections, numOfItems), null);
            } else {
                builder.setItems(createItems(numOfItems), null);
            }

            if (getArguments().getBoolean(HAS_TITLE_KEY)) {
                builder.setTitle(getContext().getString(R.string.list_dialog_title));
            }

            return builder.create();
        }

        private CarListDialog.DialogSubSection[] createSections(int numOfSections, int numOfItems) {
            CarListDialog.DialogSubSection[] items =
                    new CarListDialog.DialogSubSection[numOfSections];

            for (int i = 0; i < numOfSections; i++) {
                items[i] = new CarListDialog.DialogSubSection(
                        /* title= */ "Section " + (i + 1),
                        createItems(numOfItems));
            }

            return items;
        }

        private String[] createItems(int numOfItems) {
            String[] items = new String[numOfItems];
            for (int i = 0; i < numOfItems; i++) {
                items[i] = "Item " + (i + 1);
            }
            return items;
        }
    }
}
