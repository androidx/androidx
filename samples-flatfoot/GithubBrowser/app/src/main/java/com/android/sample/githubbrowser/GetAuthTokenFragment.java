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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import com.android.sample.githubbrowser.di.InjectableLifecycleProvider;
import com.android.sample.githubbrowser.di.LifecycleProviderComponent;
import com.android.sample.githubbrowser.model.AuthTokenModel;
import com.android.support.lifecycle.LifecycleRegistry;
import com.android.support.lifecycle.LifecycleRegistryProvider;

import javax.inject.Inject;

/**
 * UI for getting an auth token for Github API calls.
 */
public class GetAuthTokenFragment extends DialogFragment implements LifecycleRegistryProvider,
        InjectableLifecycleProvider {
    LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    @Inject
    AuthTokenModel mAuthTokenModel;

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder editBuilder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.auth_token_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mAuthTokenModel.saveToken(getCurrentAuthToken());
                    }
                })
                .setNegativeButton(R.string.cancel, null);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup editor = (ViewGroup) inflater.inflate(R.layout.get_auth_token, null, false);
        editBuilder.setView(editor);

        return editBuilder.create();
    }

    private String getCurrentAuthToken() {
        EditText runtime = (EditText) getDialog().findViewById(R.id.token);
        return runtime.getText().toString();
    }

    @Override
    public void inject(LifecycleProviderComponent component) {
        component.inject(this);
    }
}
