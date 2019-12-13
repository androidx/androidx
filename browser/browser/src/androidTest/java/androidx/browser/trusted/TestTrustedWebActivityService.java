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

package androidx.browser.trusted;

import android.app.Notification;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class TestTrustedWebActivityService extends TrustedWebActivityService {
    public static final int SMALL_ICON_ID = 666;
    private static final TokenStore sTokenStore = new InMemoryTokenStore();

    @Override
    public boolean onNotifyNotificationWithChannel(@NonNull String platformTag, int platformId,
            @NonNull Notification notification, @NonNull String channelName) {
        return true;
    }

    @Override
    public void onCancelNotification(@NonNull String platformTag, int platformId) {
    }

    @NonNull
    @Override
    public Parcelable[] onGetActiveNotifications() {
        return new Parcelable[] { null };
    }

    @Override
    public int onGetSmallIconId() {
        return SMALL_ICON_ID;
    }

    @NonNull
    @Override
    public TokenStore getTokenStore() {
        return sTokenStore;
    }

    public static void setVerifiedProvider(@Nullable Token provider) {
        sTokenStore.store(provider);
    }

    private static class InMemoryTokenStore implements TokenStore {
        private AtomicReference<byte[]> mSerialized = new AtomicReference<>();

        @Override
        public void store(@Nullable Token token) {
            mSerialized.set(token == null ? null : token.serialize());
        }

        @Nullable
        @Override
        public Token load() {
            byte[] serialized = mSerialized.get();
            return serialized == null ? null : Token.deserialize(serialized);
        }
    }
}
