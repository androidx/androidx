/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * {@link BroadcastReceiver} used to detect host updates, and trigger automatic re-connection.
 *
 */
@RestrictTo(LIBRARY)
public class HostUpdateReceiver extends BroadcastReceiver {
    private final CarAppViewModel mViewModel;

    public HostUpdateReceiver(@NonNull CarAppViewModel viewModel) {
        mViewModel = viewModel;
    }

    /** Registers this broadcast receiver */
    public void register(@NonNull Context context) {
        context.registerReceiver(this, createFilter(Intent.ACTION_PACKAGE_REPLACED));
    }

    private IntentFilter createFilter(String intentAction) {
        IntentFilter filter = new IntentFilter(intentAction);
        filter.addDataScheme("package");
        return filter;
    }

    /** Unregisters this broadcast receiver */
    public void unregister(@NonNull Context context) {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            // Ignore
            return;
        }
        mViewModel.onHostUpdated();
    }
}
