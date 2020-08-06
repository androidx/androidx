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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.IComplicationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ComplicationManager}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationManagerTest {

    @Mock private IComplicationManager mRemoteManager;
    private ComplicationManager mManagerUnderTest;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mManagerUnderTest = new ComplicationManager(mRemoteManager);
    }

    @Test
    public void testUpdateComplicationData() throws RemoteException {
        // GIVEN a complication id and a ComplicationData object...
        int id = 5;
        ComplicationData data =
                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText("hello"))
                        .build();

        // WHEN updateComplicationManager is called with that id and data...
        mManagerUnderTest.updateComplicationData(id, data);
        try {
            // THEN updateComplicationData is called on the wrapped IComplicationManager instance
            // for the same id and with the expected data.
            verify(mRemoteManager).updateComplicationData(eq(id), eq(data));
        } catch (RemoteException e) {
            fail("RemoteException");
        }
    }
}
