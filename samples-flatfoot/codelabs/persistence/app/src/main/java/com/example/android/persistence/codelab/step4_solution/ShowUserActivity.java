/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.persistence.codelab.step4_solution;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;
import com.example.android.toolkitcodelab.R;


public class ShowUserActivity extends LifecycleActivity {

    private ShowUserViewModel mShowUserViewModel;

    private TextView mBooksTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.db_activity);
        mBooksTextView = (TextView) findViewById(R.id.books_tv);

        mShowUserViewModel = ViewModelStore.get(this, "ShowUserActivity", ShowUserViewModel.class);

        populateDb();

        subscribeUiLoans();
    }

    private void populateDb() {
        mShowUserViewModel.createDb();
    }

    private void subscribeUiLoans() {
        mShowUserViewModel.getLoansResult().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String result) {
                mBooksTextView.setText(result);
            }
        });
    }

    public void onRefreshBtClicked(View view) {
        populateDb();
        subscribeUiLoans();
    }
}
