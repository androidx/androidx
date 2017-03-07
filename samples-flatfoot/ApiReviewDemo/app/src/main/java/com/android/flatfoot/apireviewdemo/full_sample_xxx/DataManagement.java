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

import android.os.Handler;
import android.os.Looper;

import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.flatfoot.apireviewdemo.common.github.GithubDao;
import com.android.flatfoot.apireviewdemo.common.github.GithubDatabaseHelper;
import com.android.flatfoot.apireviewdemo.common.github.GithubService;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DataManagement {
    private static DataManagement sInstance;
    private static ExecutorService sExecutor = Executors.newFixedThreadPool(2);

    public static DataManagement getInstance() {
        if (sInstance == null) {
            sInstance = new DataManagement();
        }
        return sInstance;
    }

    class CallbackWrapper {
        private final Callback mCallback;
        private final CancelRequest mCancelRequest = new CancelRequest();

        CallbackWrapper(Callback callback) {
            mCallback = callback;
        }
    }

    interface Cancelable {
        void cancel();
    }

    class CancelRequest implements Cancelable {
        boolean mCanceled = false;
        Future<?> mFuture;

        @Override
        public void cancel() {
            mFuture.cancel(true);
            mCanceled = true;
        }
    }


    interface Callback {
        void onSuccess();

        void onFail(int code);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final GithubService mGithubService;

    public DataManagement() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mGithubService = retrofit.create(GithubService.class);
    }

    Cancelable refreshIfNeeded(String user, Callback callback) {
        CallbackWrapper wrapper = new CallbackWrapper(callback);
        wrapper.mCancelRequest.mFuture = sExecutor.submit(
                new UpdateIfNeededRunnable(user, wrapper));

        return wrapper.mCancelRequest;
    }

    Cancelable forceRefresh(String user, Callback callback) {
        CallbackWrapper wrapper = new CallbackWrapper(callback);
        wrapper.mCancelRequest.mFuture = sExecutor.submit(new UpdateRunnable(user, wrapper));
        return wrapper.mCancelRequest;
    }

    private class UpdateIfNeededRunnable implements Runnable {

        private final String user;
        private CallbackWrapper mCallback;

        private UpdateIfNeededRunnable(String user, CallbackWrapper callback) {
            this.user = user;
            mCallback = callback;
        }

        @Override
        public void run() {
            Person personData = getGithubDao().getPerson(user);
            if (personData == null) {
                UpdateRunnable runnable = new UpdateRunnable(user, mCallback);
                runnable.run();
            } else {
                postSuccess(mCallback);
            }
        }
    }

    private class UpdateRunnable implements Runnable {

        private final String user;
        private final CallbackWrapper mCallback;

        private UpdateRunnable(String user, CallbackWrapper callback) {
            this.user = user;
            mCallback = callback;
        }

        @Override
        public void run() {
            try {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Response<Person> response = mGithubService.getUser(this.user).execute();
                if (response.isSuccessful()) {
                    getGithubDao().insertOrReplacePerson(response.body());
                    postSuccess(mCallback);
                } else {
                    postFail(mCallback, response.code());
                }
            } catch (IOException e) {
                postFail(mCallback, -2);
            }
        }
    }

    private void postSuccess(final CallbackWrapper callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!callback.mCancelRequest.mCanceled) {
                    callback.mCallback.onSuccess();
                }
            }
        });
    }

    private void postFail(final CallbackWrapper callback, final int code) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!callback.mCancelRequest.mCanceled) {
                    callback.mCallback.onFail(code);
                }
            }
        });
    }

    private static GithubDao getGithubDao() {
        return GithubDatabaseHelper.getDatabase().getGithubDao();
    }


}
