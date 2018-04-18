/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.example.android.supportv7.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.android.supportv7.R;

/**
 * This demonstrates idiomatic usage of AppCompatDialogFragment.
 */
public class DialogFragmentUsage extends AppCompatActivity {

    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_usage);

        mSpinner = findViewById(R.id.spinner_dialogs);

        // Add an OnClickListener to show our selected dialog
        findViewById(R.id.btn_show_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectedDialog();
            }
        });
    }

    private void showSelectedDialog() {
        switch (mSpinner.getSelectedItemPosition()) {
            case 0:
                showSimpleDialog();
                break;
            case 1:
                showButtonBarDialog();
                break;
        }
    }

    private void showSimpleDialog() {
        MenuDialogFragment fragment = MenuDialogFragment.create(R.layout.dialog_content);
        fragment.show(getSupportFragmentManager(), null);
    }

    private void showButtonBarDialog() {
        MenuDialogFragment fragment = MenuDialogFragment.create(R.layout.dialog_content_buttons);
        fragment.show(getSupportFragmentManager(), null);
    }

    /**
     * A simple {@link AppCompatDialog} implementation which
     * inflates some items into it's options menu, and shows a toast when one is selected.
     */
    public static class MenuDialogFragment extends AppCompatDialogFragment {

        private static final String PARAM_CONTENT_VIEW = "content_view";

        static MenuDialogFragment create(int contentView) {
            Bundle b = new Bundle();
            b.putInt(PARAM_CONTENT_VIEW, contentView);

            MenuDialogFragment fragment = new MenuDialogFragment();
            fragment.setArguments(b);

            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            int contentView = args.getInt(PARAM_CONTENT_VIEW);
            return inflater.inflate(contentView, container, false);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.actions, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Toast.makeText(getActivity(), "Dialog action selected: " + item.getTitle(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
    }

}
