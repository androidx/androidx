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
package androidx.remotecallback;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An instance of a callback to a specific class/method with a specific set
 * of arguments. Can only be obtained from a {@link CallbackReceiver}.
 */
public class RemoteCallback {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String EXTRA_METHOD = "remotecallback.method";

    /**
     * Constant indicating this callback will be triggered on a {@link BroadcastReceiver}.
     */
    public static final int TYPE_RECEIVER = 0;

    /**
     * Constant indicating this callback will be triggered on a {@link ContentProvider}.
     */
    public static final int TYPE_PROVIDER = 1;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({TYPE_RECEIVER, TYPE_PROVIDER})
    public @interface RemoteCallbackType {
    }

    private final Context mContext;
    private final int mType;
    private final Intent mIntent;
    private final Bundle mArguments;
    private final String mReceiverClass;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public RemoteCallback(@NonNull Context context,
            @RemoteCallbackType int type,
            @NonNull Intent intent,
            @NonNull String receiverClass,
            @NonNull Bundle arguments) {
        mContext = context;
        mType = type;
        mIntent = intent;
        mReceiverClass = receiverClass;
        mArguments = arguments;
    }

    /**
     * Get the type of the receiver of this callback.
     */
    @RemoteCallbackType
    public int getType() {
        return mType;
    }

    /**
     * Gets the class the callback will be called on.
     */
    @NonNull
    public String getReceiverClass() {
        return mReceiverClass;
    }

    /**
     * Gets the name of the method this callback will call.
     */
    public String getMethodName() {
        return mArguments.getString(EXTRA_METHOD);
    }

    /**
     * Gets the bundle of arguments that will be used to trigger the method.
     */
    public Bundle getArgumentBundle() {
        return mArguments;
    }

    /**
     * Create a {@link PendingIntent} that will trigger this callback.
     */
    public PendingIntent toPendingIntent() {
        mIntent.setData(generateUri(mIntent));
        mIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent intent = PendingIntent.getBroadcast(mContext, 0, mIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return intent;
    }

    private static Uri generateUri(Intent intent) {
        if (intent.getData() != null) {
            return intent.getData();
        }
        Uri.Builder builder = new Uri.Builder()
                .scheme("remotecallback")
                .authority("");
        Bundle extras = intent.getExtras();
        for (String key : extras.keySet()) {
            builder.appendQueryParameter(key, String.valueOf(extras.get(key)));
        }
        return builder.build();
    }

    /**
     * Static version of {@link CallbackReceiver#createRemoteCallback(Context)}.
     */
    public static <T extends CallbackReceiver> T create(Class<T> cls, Context context) {
        return CallbackHandlerRegistry.sInstance.getAndResetStub(cls, context, null);
    }

    /**
     * Constant value that actual implementations of {@link RemoteCallable} should return.
     */
    public static final RemoteCallback LOCAL = new RemoteCallback(null, -1, null, null, null) {
        @Override
        public int getType() {
            throw new UnsupportedOperationException("RemoteCallback.LOCAL cannot be used");
        }

        @Override
        public Bundle getArgumentBundle() {
            throw new UnsupportedOperationException("RemoteCallback.LOCAL cannot be used");
        }

        @Override
        public String getMethodName() {
            throw new UnsupportedOperationException("RemoteCallback.LOCAL cannot be used");
        }

        @NonNull
        @Override
        public String getReceiverClass() {
            throw new UnsupportedOperationException("RemoteCallback.LOCAL cannot be used");
        }

        @Override
        public PendingIntent toPendingIntent() {
            throw new UnsupportedOperationException("RemoteCallback.LOCAL cannot be used");
        }
    };
}
