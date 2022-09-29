/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.suggestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.model.CarIcon;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.suggestion.model.Suggestion;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link SuggestionManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class SuggestionManagerTest {
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private ISuggestionHost.Stub mSuggestionHost;
    private final HostDispatcher mHostDispatcher = new HostDispatcher();
    private SuggestionManager mSuggestionManager;

    private final String mIdentifier = "1";
    private final String mTitle = "car";
    private final String mSubTitle = "subtitle";
    private final Intent mIntent = new Intent();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent, 0);
    private final CarIcon mIcon = CarIcon.APP_ICON;
    private final Suggestion mSuggestion =
            new Suggestion.Builder().setIdentifier(mIdentifier).setTitle(mTitle).setSubtitle(
                    mSubTitle).setIcon(mIcon).setAction(mPendingIntent).build();

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        TestCarContext testCarContext = TestCarContext.createCarContext(
                ApplicationProvider.getApplicationContext());
        ISuggestionHost suggestionHostStub = new ISuggestionHost.Stub() {
            @Override
            public void updateSuggestions(Bundleable suggestions) throws RemoteException {
                mSuggestionHost.updateSuggestions(suggestions);
            }
        };
        when(mMockCarHost.getHost(any())).thenReturn(suggestionHostStub.asBinder());

        mHostDispatcher.setCarHost(mMockCarHost);

        mSuggestionManager = SuggestionManager.create(testCarContext, mHostDispatcher,
                testCarContext.getLifecycleOwner().mRegistry);
    }

    @Test
    public void sendSuggestions() throws RemoteException {
        List<Suggestion> suggestionList = new ArrayList<>();
        suggestionList.add(mSuggestion);
        mSuggestionManager.updateSuggestions(suggestionList);

        verify(mSuggestionHost).updateSuggestions(any(Bundleable.class));
    }

}
