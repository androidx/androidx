/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.core.state;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Bitmap;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Default implementation of {@link StateUpdater}. */
@RestrictTo(LIBRARY_GROUP)
public class StateUpdaterImpl implements StateUpdater {

    private final RemoteContext mRemoteContext;

    public StateUpdaterImpl(@NonNull RemoteContext remoteContext) {
        this.mRemoteContext = remoteContext;
    }

    @Override
    public void setNamedLong(@NonNull String name, @Nullable Long value) {
        if (value != null) {
            mRemoteContext.setNamedLong(name, value);
        } else {
//            mRemoteContext.clearNamedLong(name);
        }
    }

    @Override
    public void setUserLocalFloat(@NonNull String floatName, @Nullable Float value) {
        if (value != null) {
            mRemoteContext.setNamedFloatOverride(
                    StateUpdater.getUserDomainString(floatName), value);
        } else {
            mRemoteContext.clearNamedFloatOverride(StateUpdater.getUserDomainString(floatName));
        }
    }

    @Override
    public void setUserLocalInt(@NonNull String integerName, @Nullable Integer value) {
        if (value != null) {
            mRemoteContext.setNamedIntegerOverride(
                    StateUpdater.getUserDomainString(integerName), value);
        } else {
            mRemoteContext.clearNamedIntegerOverride(StateUpdater.getUserDomainString(integerName));
        }
    }

    @Override
    public void setUserLocalColor(@NonNull String name, @Nullable Integer value) {
        if (value != null) {
            mRemoteContext.setNamedColorOverride(StateUpdater.getUserDomainString(name), value);
        } else {
//            mRemoteContext.clearNamedColorOverride(StateUpdater.getUserDomainString(name));
        }
    }

    @Override
    public void setUserLocalBitmap(@NonNull String name, @Nullable Bitmap content) {
        if (content != null) {
            mRemoteContext.setNamedDataOverride(StateUpdater.getUserDomainString(name), content);
        } else {
            mRemoteContext.clearNamedDataOverride(StateUpdater.getUserDomainString(name));
        }
    }

    @Override
    public void setUserLocalString(@NonNull String stringName, @Nullable String value) {
        if (value != null) {
            mRemoteContext.setNamedStringOverride(StateUpdater.getUserDomainString(stringName),
                    value);
        } else {
            mRemoteContext.clearNamedStringOverride(StateUpdater.getUserDomainString(stringName));
        }
    }
}
