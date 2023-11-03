/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.service.quicksettings;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.PendingIntentCompat;

/**
 * A wrapper class for developers to use with
 * {@link TileServiceCompat#startActivityAndCollapse(TileService, PendingIntentActivityWrapper)}.
 */
public class PendingIntentActivityWrapper {

    private final Context mContext;

    private final int mRequestCode;

    @NonNull
    private final Intent mIntent;

    @PendingIntentCompat.Flags
    private final int mFlags;

    @Nullable
    private final Bundle mOptions;

    @Nullable
    private final PendingIntent mPendingIntent;

    private final boolean mIsMutable;

    public PendingIntentActivityWrapper(@NonNull Context context, int requestCode,
            @NonNull Intent intent,
            @PendingIntentCompat.Flags int flags, boolean isMutable) {
        this(context, requestCode, intent, flags, null, isMutable);
    }

    public PendingIntentActivityWrapper(@NonNull Context context, int requestCode,
            @NonNull Intent intent,
            @PendingIntentCompat.Flags int flags, @Nullable Bundle options, boolean isMutable) {
        this.mContext = context;
        this.mRequestCode = requestCode;
        this.mIntent = intent;
        this.mFlags = flags;
        this.mOptions = options;
        this.mIsMutable = isMutable;

        mPendingIntent = createPendingIntent();
    }

    public @NonNull Context getContext() {
        return mContext;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    public @NonNull Intent getIntent() {
        return mIntent;
    }

    public int getFlags() {
        return mFlags;
    }

    public @NonNull Bundle getOptions() {
        return mOptions;
    }

    public boolean isMutable() {
        return mIsMutable;
    }

    public @Nullable PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    private @Nullable PendingIntent createPendingIntent() {
        if (mOptions == null) {
            return PendingIntentCompat.getActivity(mContext, mRequestCode, mIntent, mFlags,
                    mIsMutable);
        }
        return PendingIntentCompat.getActivity(mContext, mRequestCode, mIntent, mFlags, mOptions,
                mIsMutable);
    }
}
