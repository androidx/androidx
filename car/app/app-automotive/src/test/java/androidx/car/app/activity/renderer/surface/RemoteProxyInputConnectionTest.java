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

package androidx.car.app.activity.renderer.surface;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.RemoteException;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.SurroundingText;

import androidx.car.app.activity.CarAppViewModel;
import androidx.car.app.activity.ServiceDispatcher;
import androidx.car.app.activity.renderer.IProxyInputConnection;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link RemoteProxyInputConnection} */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class RemoteProxyInputConnectionTest {
    private RemoteProxyInputConnection mRemoteProxyInputConnection;
    private final CarAppViewModel mViewModel = mock(CarAppViewModel.class);
    private final ServiceDispatcher mServiceDispatcher = new ServiceDispatcher(mViewModel,
            () -> true);
    private final IProxyInputConnection mProxyInputConnection =
            mock(IProxyInputConnection.class);


    @Before
    public void setUp() throws Exception {
        mRemoteProxyInputConnection =
                new RemoteProxyInputConnection(mServiceDispatcher, mProxyInputConnection);

    }

    @Config(minSdk = 31)
    @Test
    public void getSurroundingText_proxyInputReturnsValidValue_returnsValidValue()
            throws RemoteException,
            BundlerException {
        SurroundingText surroundingText = new SurroundingText("Test Text", 0, 0, 0);
        when(mProxyInputConnection.getSurroundingText(10, 10, 0)).thenReturn(
                Bundleable.create(surroundingText));
        assertThat(mRemoteProxyInputConnection.getSurroundingText(10, 10, 0)).isEqualTo(
                surroundingText);
    }

    @Config(minSdk = 31)
    @Test
    public void getSurroundingText_proxyInputReturnsNull_returnsNull() throws RemoteException,
            BundlerException {
        SurroundingText surroundingText = new SurroundingText("Test Text", 0, 0, 0);
        when(mProxyInputConnection.getSurroundingText(10, 10, 0)).thenReturn(null);
        assertThat(mRemoteProxyInputConnection.getSurroundingText(10, 10, 0)).isNull();
    }

    @Config(minSdk = 31)
    @Test
    public void getSurroundingText_throwsRemoteException_returnsNull() throws RemoteException,
            BundlerException {
        SurroundingText surroundingText = new SurroundingText("Test Text", 0, 0, 0);
        when(mProxyInputConnection.getSurroundingText(10, 10, 0)).thenThrow(RemoteException.class);
        assertThat(mRemoteProxyInputConnection.getSurroundingText(10, 10, 0)).isNull();
    }

    @Config(minSdk = 31)
    @Test
    public void getSurroundingText_throwsBundlerException_returnsNull() throws RemoteException,
            BundlerException {
        SurroundingText surroundingText = new SurroundingText("Test Text", 0, 0, 0);
        when(mProxyInputConnection.getSurroundingText(10, 10, 0)).thenReturn(
                Bundleable.create("random string"));
        assertThat(mRemoteProxyInputConnection.getSurroundingText(10, 10, 0)).isNull();
    }

    @Test
    public void deleteSurroundingTextInCodePoints_proxyInputReturnsValidValue_returnsValidValue()
            throws RemoteException,
            BundlerException {
        when(mProxyInputConnection.deleteSurroundingTextInCodePoints(1, 5)).thenReturn(true);
        assertThat(mRemoteProxyInputConnection.deleteSurroundingTextInCodePoints(1, 5)).isTrue();
    }

    @Test
    public void commitContent_proxyInputReturnsValidValue_returnsValidValue()
            throws RemoteException,
            BundlerException {
        when(mProxyInputConnection.commitContent(any(Bundleable.class), anyInt(),
                isNull())).thenReturn(true);
        InputContentInfo inputContentInfo = new InputContentInfo(Uri.parse("content://com"
                + ".example/path"), new ClipDescription("sample content",
                new String[]{"image/png"}));
        assertThat(mRemoteProxyInputConnection.commitContent(inputContentInfo,
                5, null)).isTrue();
    }
}
