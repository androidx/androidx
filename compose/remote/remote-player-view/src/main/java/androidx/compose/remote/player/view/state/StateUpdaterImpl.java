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
package androidx.compose.remote.player.view.state;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Bitmap;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;

/** Default implementation of {@link StateUpdater}. */
@RestrictTo(LIBRARY_GROUP)
public class StateUpdaterImpl implements StateUpdater {

    private final RemoteContext mRemoteContext;

    public StateUpdaterImpl(RemoteContext remoteContext) {
        this.mRemoteContext = remoteContext;
    }

    @Override
    public void setNamedLong(@NonNull String name, long value) {
        mRemoteContext.setNamedLong(name, value);
    }

    @Override
    public void setUserLocalInt(String integerName, int value) {
        mRemoteContext.setNamedIntegerOverride(
                StateUpdater.getUserDomainString(integerName), value);
    }

    @Override
    public void setUserLocalColor(String name, int value) {
        mRemoteContext.setNamedColorOverride(StateUpdater.getUserDomainString(name), value);
    }

    @Override
    public void setUserLocalBitmap(String name, Bitmap content) {
        mRemoteContext.setNamedDataOverride(StateUpdater.getUserDomainString(name), content);
    }

    @Override
    public void setUserLocalString(String stringName, String value) {
        mRemoteContext.setNamedStringOverride(StateUpdater.getUserDomainString(stringName), value);
    }
}
