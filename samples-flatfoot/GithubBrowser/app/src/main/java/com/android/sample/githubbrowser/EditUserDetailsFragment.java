/*
 * Copyright (C) 2017 The Android Open Source Project
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

/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.sample.githubbrowser;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.databinding.EditUserDetailsBinding;
import com.android.sample.githubbrowser.viewmodel.PersonDataModel;
import com.android.support.lifecycle.LifecycleRegistry;
import com.android.support.lifecycle.LifecycleRegistryProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

/**
 * Edit user details fragment.
 */
public class EditUserDetailsFragment extends DialogFragment implements LifecycleRegistryProvider {
    private static final String LOGIN = "editUser.login";
    private static final int CODE_EDIT = 1;

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private PersonDataModel mPersonDataModel;

    public EditUserDetailsFragment() {
    }

    public static EditUserDetailsFragment createFor(Fragment target, PersonData personData) {
        EditUserDetailsFragment editUserDetailsFragment = new EditUserDetailsFragment();
        Bundle editUserDetailsFragmentArgs = new Bundle();
        editUserDetailsFragmentArgs.putString(EditUserDetailsFragment.LOGIN, personData.login);
        editUserDetailsFragment.setArguments(editUserDetailsFragmentArgs);
        editUserDetailsFragment.setTargetFragment(target, CODE_EDIT);
        return editUserDetailsFragment;
    }

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String login = getArguments().getString(LOGIN);

        // Configure the dialog to pass the data back when "OK" button is clicked
        AlertDialog.Builder editBuilder = new AlertDialog.Builder(getContext())
                .setTitle("Edit details")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Ask the model to update the two fields on the user
                        mPersonDataModel.update(login, getCurrentEmail(),
                                getCurrentLocation());
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, null);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_CANCELED, null);
                    }
                });

        // Inflate the main editor area and set it as custom view on the dialog
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final EditUserDetailsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.edit_user_details, null, false);
        final ViewGroup editor = (ViewGroup) binding.getRoot();
        editBuilder.setView(editor);
        final AlertDialog result = editBuilder.create();

        // Get our view model instance and register ourselves to observe change to the
        // user data. When a change is reported, update all UI elements based on the new
        // data.
        mPersonDataModel = ViewModelStore.get(this, login, PersonDataModel.class);
        // Ask the model to load the data for this user. When the data becomes available (either
        // immediately from the previous load or later on when it's fetched from remote API call),
        // we will be notified since this fragment registered itself as an observer on the matching
        // live data object.
        mPersonDataModel.loadData(login, false);
        mPersonDataModel.getPersonData().observe(this, new Observer<PersonData>() {
            @Override
            public void onChanged(@Nullable PersonData personData) {
                if (!isDetached() && (personData != null)) {
                    android.util.Log.e("GithubBrowser", "Got data for editing from model");
                    getDialog().setTitle(personData.name);
                    binding.setUser(personData);
                    binding.executePendingBindings();
                }
            }
        });

        return result;
    }

    private String getCurrentEmail() {
        EditText runtime = (EditText) getDialog().findViewById(R.id.email);
        return runtime.getText().toString();
    }

    private String getCurrentLocation() {
        EditText rated = (EditText) getDialog().findViewById(R.id.location);
        return rated.getText().toString();
    }
}
