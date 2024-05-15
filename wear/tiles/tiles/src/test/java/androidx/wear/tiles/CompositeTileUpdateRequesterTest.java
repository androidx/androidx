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

package androidx.wear.tiles;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.ResourceBuilders;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class CompositeTileUpdateRequesterTest {
    private FakeUpdateRequester mFakeUpdateRequester1;
    private FakeUpdateRequester mFakeUpdateRequester2;

    private CompositeTileUpdateRequester mCompositeTileUpdateRequesterUnderTest;

    @Before
    public void setUp() {
        mFakeUpdateRequester1 = new FakeUpdateRequester();
        mFakeUpdateRequester2 = new FakeUpdateRequester();

        mCompositeTileUpdateRequesterUnderTest =
                new CompositeTileUpdateRequester(
                    ImmutableList.of(mFakeUpdateRequester1, mFakeUpdateRequester2));
    }

    @Test
    public void requestUpdate_callsThrough() {
        mCompositeTileUpdateRequesterUnderTest.requestUpdate(FakeService.class);

        assertThat(mFakeUpdateRequester1.mCalledService).isEqualTo(FakeService.class);
        assertThat(mFakeUpdateRequester2.mCalledService).isEqualTo(FakeService.class);
    }

    private static class FakeUpdateRequester implements TileUpdateRequester {
        @Nullable Class<? extends TileService> mCalledService = null;

        @Override
        public void requestUpdate(@NonNull Class<? extends TileService> tileService) {
            this.mCalledService = tileService;
        }
    }

    private static class FakeService extends TileService {
        @NonNull
        @Override
        protected ListenableFuture<TileBuilders.Tile> onTileRequest(
                @NonNull RequestBuilders.TileRequest requestParams) {
            ResolvableFuture<TileBuilders.Tile> f = ResolvableFuture.create();
            f.set(null);
            return f;
        }

        @NonNull
        @Override
        protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(
                @NonNull RequestBuilders.ResourcesRequest requestParams) {
            ResolvableFuture<ResourceBuilders.Resources> f = ResolvableFuture.create();
            f.set(null);
            return f;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
