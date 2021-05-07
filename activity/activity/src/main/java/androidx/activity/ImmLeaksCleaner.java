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

package androidx.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.lang.reflect.Field;

@RequiresApi(19)
final class ImmLeaksCleaner implements LifecycleEventObserver {
    private static final int NOT_INITIALIAZED = 0;
    private static final int INIT_SUCCESS = 1;
    private static final int INIT_FAILED = 2;
    private static int sReflectedFieldsInitialized = NOT_INITIALIAZED;
    private static Field sHField;
    private static Field sServedViewField;
    private static Field sNextServedViewField;

    private Activity mActivity;

    ImmLeaksCleaner(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event != Lifecycle.Event.ON_DESTROY) {
            return;
        }
        if (sReflectedFieldsInitialized == NOT_INITIALIAZED) {
            initializeReflectiveFields();
        }
        if (sReflectedFieldsInitialized == INIT_SUCCESS) {
            InputMethodManager inputMethodManager = (InputMethodManager)
                    mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            final Object lock;
            try {
                lock = sHField.get(inputMethodManager);
            } catch (IllegalAccessException e) {
                return;
            }
            if (lock == null) {
                return;
            }
            synchronized (lock) {
                final View servedView;
                try {
                    servedView = (View) sServedViewField.get(inputMethodManager);
                } catch (IllegalAccessException e) {
                    return;
                } catch (ClassCastException e) {
                    return;
                }
                if (servedView == null) {
                    return;
                }
                if (servedView.isAttachedToWindow()) {
                    return;
                }
                // Here we have a detached mServedView.  Set null to mNextServedViewField so that
                // everything will be cleared in the next InputMethodManager#checkFocus().
                try {
                    sNextServedViewField.set(inputMethodManager, null);
                } catch (IllegalAccessException e) {
                    return;
                }
            }
            // Assume that InputMethodManager#isActive() internally triggers
            // InputMethodManager#checkFocus().
            inputMethodManager.isActive();
        }
    }

    @SuppressLint("SoonBlockedPrivateApi") // This class is only used API <=23
    @MainThread
    private static void initializeReflectiveFields() {
        try {
            sReflectedFieldsInitialized = INIT_FAILED;
            sServedViewField = InputMethodManager.class.getDeclaredField("mServedView");
            sServedViewField.setAccessible(true);
            sNextServedViewField = InputMethodManager.class.getDeclaredField("mNextServedView");
            sNextServedViewField.setAccessible(true);
            sHField = InputMethodManager.class.getDeclaredField("mH");
            sHField.setAccessible(true);
            sReflectedFieldsInitialized = INIT_SUCCESS;
        } catch (NoSuchFieldException e) {
            // very oem much custom ¯\_(ツ)_/¯
        }
    }
}
