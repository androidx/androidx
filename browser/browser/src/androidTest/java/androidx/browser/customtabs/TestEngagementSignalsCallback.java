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

package androidx.browser.customtabs;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.customtabs.IEngagementSignalsCallback;

import androidx.annotation.Nullable;

import org.mockito.Mockito;

/**
 * A test class to check the incoming messages through {@link EngagementSignalsCallback}.
 */
public class TestEngagementSignalsCallback implements EngagementSignalsCallback {
    private EngagementSignalsCallback mMock;

    /* package */ TestEngagementSignalsCallback() {
        mMock = Mockito.mock(EngagementSignalsCallback.class);
    }
    private final IEngagementSignalsCallback.Stub mWrapper = new IEngagementSignalsCallback.Stub() {
        @Override
        public void onVerticalScrollEvent(boolean isDirectionUp, Bundle extras) throws
                RemoteException {
            TestEngagementSignalsCallback.this.onVerticalScrollEvent(isDirectionUp, extras);
        }

        @Override
        public void onGreatestScrollPercentageIncreased(int scrollPercentage, Bundle extras) throws
                RemoteException {
            TestEngagementSignalsCallback.this.onGreatestScrollPercentageIncreased(
                    scrollPercentage, extras);
        }

        @Override
        public void onSessionEnded(boolean didUserInteract, Bundle extras) throws RemoteException {
            TestEngagementSignalsCallback.this.onSessionEnded(didUserInteract, extras);
        }
    };

    @Override
    public void onVerticalScrollEvent(boolean isDirectionUp, @Nullable Bundle extras) {
        mMock.onVerticalScrollEvent(isDirectionUp, extras);
    }

    @Override
    public void onGreatestScrollPercentageIncreased(int scrollPercentage, @Nullable Bundle extras) {
        mMock.onGreatestScrollPercentageIncreased(scrollPercentage, extras);
    }

    @Override
    public void onSessionEnded(boolean didUserInteract, @Nullable Bundle extras) {
        mMock.onSessionEnded(didUserInteract, extras);
    }

    /* package */ IEngagementSignalsCallback getStub() {
        return mWrapper;
    }

    /* package */ EngagementSignalsCallback getMock() {
        return mMock;
    }
}
