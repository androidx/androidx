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

package androidx.wear.watchface.samples.minimal.style;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/** A base class for a {@link FutureCallback} that logs the outcome. */
abstract class BaseFutureCallback<T> implements FutureCallback<T> {

    private final Context mContext;
    private final String mTag;
    private final String mName;

    BaseFutureCallback(Context context, String tag, String name) {
        mContext = context;
        mTag = tag;
        mName = name;
    }

    @Override
    public void onPending() {
        Log.d(mTag, mName + ".onPending()");
    }

    @Override
    public void onSuccess(T value) {
        Log.d(mTag, mName + ".onSuccess(" + value + ")");
    }

    @Override
    public void onFailure(Throwable throwable) {
        Log.d(mTag, mName + ".onFailure(" + throwable.getMessage() + ")", throwable);
        Toast.makeText(mContext, "Failure", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCancelled() {
        Log.d(mTag, mName + ".onCancelled()");
        Toast.makeText(mContext, "Cancelled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInterrupted() {
        Thread.currentThread().interrupt();
        Log.d(mTag, mName + ".onInterrupted()");
        Toast.makeText(mContext, "Interrupted", Toast.LENGTH_LONG).show();
    }
}
