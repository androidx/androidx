/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.RemoteException;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.IComplicationManager;
import android.support.wearable.complications.IComplicationProvider;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link ComplicationProviderService}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationProviderServiceTest {

    private static final String TAG = "ComplicationProviderServiceTest";

    @Mock
    private IComplicationManager mRemoteManager;
    private IComplicationManager.Stub mLocalManager = new IComplicationManager.Stub() {
        @Override
        public void updateComplicationData(int complicationId, ComplicationData data)
                throws RemoteException {
            mRemoteManager.updateComplicationData(complicationId, data);
        }
    };

    private IComplicationProvider.Stub mComplicationProvider;

    private ComplicationProviderService mTestService = new ComplicationProviderService() {
        private CharSequence mText = "Hello";

        @Override
        public void onComplicationUpdate(
                int complicationId, int type, @NonNull ComplicationUpdateCallback callback) {
            try {
                callback.onUpdateComplication(
                        new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                .setLongText(
                                        ComplicationText.plainText("hello " + complicationId))
                                .build());
            } catch (RemoteException e) {
                Log.e(TAG, "onComplicationUpdate failed with error: ", e);
            }
        }

        @Nullable
        @Override
        public ComplicationData getPreviewData(int type) {
            return new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                    .setLongText(ComplicationText.plainText("hello preview"))
                    .build();
        }
    };

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mComplicationProvider =
                (IComplicationProvider.Stub) mTestService.onBind(
                        new Intent(ComplicationProviderService.ACTION_COMPLICATION_UPDATE_REQUEST));
        mTestService.setRetailModeProvider(() -> false);
    }

    @Test
    public void testOnComplicationUpdate() throws Exception {
        int id = 123;
        mComplicationProvider.onUpdate(id, ComplicationData.TYPE_LONG_TEXT, mLocalManager);
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<ComplicationData> data = ArgumentCaptor.forClass(ComplicationData.class);
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture());
        assertThat(data.getValue().getLongText().getTextAt(null, 0)).isEqualTo(
                "hello " + id
        );
    }

    @Test
    public void testGetComplicationPreviewData() throws Exception {
        assertThat(mComplicationProvider.getComplicationPreviewData(ComplicationData.TYPE_LONG_TEXT)
                .getLongText().getTextAt(null, 0)).isEqualTo("hello preview");
    }
}
