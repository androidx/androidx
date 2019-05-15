/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ImmediateSurfaceTest {
    private Surface mMockSurface = Mockito.mock(Surface.class);

    @Test
    public void getSurface_returnsInstance() throws ExecutionException, InterruptedException {
        ImmediateSurface immediateSurface = new ImmediateSurface(mMockSurface);

        ListenableFuture<Surface> surfaceListenableFuture = immediateSurface.getSurface();

        assertThat(surfaceListenableFuture.get()).isSameInstanceAs(mMockSurface);
    }
}
