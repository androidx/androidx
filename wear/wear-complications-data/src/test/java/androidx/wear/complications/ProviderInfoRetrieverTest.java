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

package androidx.wear.complications;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.support.wearable.complications.IPreviewComplicationDataCallback;
import android.support.wearable.complications.IProviderInfoService;

import androidx.test.core.app.ApplicationProvider;
import androidx.wear.complications.data.ComplicationData;
import androidx.wear.complications.data.ComplicationText;
import androidx.wear.complications.data.ComplicationType;
import androidx.wear.complications.data.LongTextComplicationData;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;

@RunWith(SharedRobolectricTestRunner.class)
public class ProviderInfoRetrieverTest {
    private IProviderInfoService mMockService = Mockito.mock(IProviderInfoService.class);
    private ProviderInfoRetriever mProviderInfoRetriever = new ProviderInfoRetriever(mMockService);

    @Test
    public void requestPreviewComplicationData() throws Exception {
        final ComponentName component = new ComponentName("provider.package", "provider.class");
        final ComplicationType type = ComplicationType.LONG_TEXT;
        when(mMockService.getApiVersion()).thenReturn(1);
        final ArgumentCaptor<IPreviewComplicationDataCallback> callbackCaptor =
                ArgumentCaptor.forClass(IPreviewComplicationDataCallback.class);
        when(mMockService.requestPreviewComplicationData(
                eq(component), eq(type.asWireComplicationType()), callbackCaptor.capture()))
                .thenReturn(true);
        ListenableFuture<ComplicationData> future =
                mProviderInfoRetriever.requestPreviewComplicationData(component, type);

        ComplicationData testData =
                new LongTextComplicationData.Builder(ComplicationText.plain("Test Text")).build();
        callbackCaptor.getValue().updateComplicationData(testData.asWireComplicationData());
        assertThat(future.get().getType()).isEqualTo(type);
        assertThat(((LongTextComplicationData) testData).getText().getTextAt(
                ApplicationProvider.getApplicationContext().getResources(), 0))
                .isEqualTo("Test Text");
    }

    @Test
    public void requestPreviewComplicationDataProviderReturnsNull() throws Exception {
        final ComponentName component = new ComponentName("provider.package", "provider.class");
        final ComplicationType type = ComplicationType.LONG_TEXT;
        when(mMockService.getApiVersion()).thenReturn(1);
        final ArgumentCaptor<IPreviewComplicationDataCallback> callbackCaptor =
                ArgumentCaptor.forClass(IPreviewComplicationDataCallback.class);
        when(mMockService.requestPreviewComplicationData(
                eq(component), eq(type.asWireComplicationType()), callbackCaptor.capture()))
                .thenReturn(true);
        ListenableFuture<ComplicationData> future =
                mProviderInfoRetriever.requestPreviewComplicationData(component, type);

        callbackCaptor.getValue().updateComplicationData(null);
        assertThat(future.get()).isNull();
    }

    @Test
    public void requestPreviewComplicationDataApiNotSupported() throws Exception {
        final ComponentName component = new ComponentName("provider.package", "provider.class");
        final ComplicationType type = ComplicationType.LONG_TEXT;
        when(mMockService.getApiVersion()).thenReturn(0);
        when(mMockService.requestPreviewComplicationData(
                eq(component), eq(type.asWireComplicationType()), Mockito.any()))
                .thenReturn(true);
        ListenableFuture<ComplicationData> future =
                mProviderInfoRetriever.requestPreviewComplicationData(component, type);

        try {
            future.get();
            fail("Should have thrown an ExecutionException.");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(
                    ProviderInfoRetriever.PreviewNotAvailableException.class);
        }
    }

    @Test
    public void requestPreviewComplicationDataApiReturnsFalse() throws Exception {
        final ComponentName component = new ComponentName("provider.package", "provider.class");
        final ComplicationType type = ComplicationType.LONG_TEXT;
        when(mMockService.getApiVersion()).thenReturn(1);
        when(mMockService.requestPreviewComplicationData(
                eq(component), eq(type.asWireComplicationType()), Mockito.any()))
                .thenReturn(false);
        ListenableFuture<ComplicationData> future =
                mProviderInfoRetriever.requestPreviewComplicationData(component, type);

        try {
            future.get();
            fail("Should have thrown an ExecutionException.");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(
                    ProviderInfoRetriever.PreviewNotAvailableException.class);
        }
    }
}
