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

package com.android.flatfoot.apireviewdemo.full_sample_xxx;

import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.android.flatfoot.apireviewdemo.R;
import com.android.flatfoot.apireviewdemo.common.entity.Person;

import java.util.Random;

public class SwitchAccountsActivity extends LifecycleActivity {

    public final String[] USERS = new String[]{"yigit", "JakeWharton"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AccountViewModel viewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        viewModel.setUser(USERS[0]);
        setContentView(R.layout.switch_accounts);

        findViewById(R.id.switch_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setUser(USERS[new Random().nextInt(USERS.length)]);
            }
        });

        viewModel.personData.observe(this, new Observer<Person>() {
            @Override
            public void onChanged(@Nullable Person data) {
                if (data != null) {
                    TextView emailView = (TextView) findViewById(R.id.name);
                    emailView.setText(data.getName());
                    TextView nameView = (TextView) findViewById(R.id.company);
                    nameView.setText(data.getCompany());
                }
            }
        });

        viewModel.statusData.observe(this, new Observer<AccountViewModel.Status>() {
            @Override
            public void onChanged(AccountViewModel.Status status) {
                findViewById(R.id.loading_spinner).setVisibility(
                        status.updating ? View.VISIBLE : View.GONE);
            }
        });

        findViewById(R.id.force_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.forceRefresh();
            }
        });
    }
}
