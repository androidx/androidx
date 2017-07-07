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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.flatfoot.apireviewdemo.common.github.GithubDatabaseHelper;
import com.android.flatfoot.apireviewdemo.full_sample_xxx.DataManagement.Cancelable;

public class AccountViewModel extends ViewModel {

    public final MutableLiveData<Status> statusData = new MutableLiveData<>();
    public final MutableLiveData<Person> personData = new MutableLiveData<>();

    private final DataManagement mDataManagement = DataManagement.getInstance();

    private final DataManagement.Callback mCallback = new DataManagement.Callback() {
        @Override
        public void onSuccess() {
            finishRequest(200);
        }

        @Override
        public void onFail(int code) {
            finishRequest(code);
        }
    };

    private boolean inForceRefresh = false;
    private Cancelable mCurrentRequest;
    private String mLogin;
    private LiveData<Person> mPrivatePersonData;
    private Observer<Person> mObserver = new Observer<Person>() {
        @Override
        public void onChanged(@Nullable Person person) {
            personData.setValue(person);
        }
    };

    private void cancelRequest() {
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            statusData.setValue(new Status(0, false));
            mCurrentRequest = null;
        }
    }

    private void finishRequest(int code) {
        statusData.setValue(new Status(code, false));
        inForceRefresh = false;
        mCurrentRequest = null;
    }

    public void setUser(String login) {
        if (TextUtils.equals(mLogin, login)) {
            return;
        }
        if (mPrivatePersonData != null) {
            mPrivatePersonData.removeObserver(mObserver);
        }

        cancelRequest();
        mLogin = login;
        mPrivatePersonData = GithubDatabaseHelper.getDatabase().getGithubDao().getLivePerson(
                mLogin);
        mPrivatePersonData.observeForever(mObserver);
        statusData.setValue(new Status(0, true));
        mCurrentRequest = mDataManagement.refreshIfNeeded(mLogin, mCallback);
    }

    public void forceRefresh() {
        if (inForceRefresh || mLogin == null) {
            return;
        }
        cancelRequest();
        inForceRefresh = true;

        statusData.setValue(new Status(0, true));
        mCurrentRequest = mDataManagement.forceRefresh(mLogin, mCallback);
    }


    static class Status {
        final int status;
        final boolean updating;

        Status(int status, boolean loading) {
            this.status = status;
            this.updating = loading;
        }
    }

    @Override
    protected void onCleared() {
        if (mPrivatePersonData != null) {
            mPrivatePersonData.removeObserver(mObserver);
        }
    }
}
