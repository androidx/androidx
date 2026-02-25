/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.spatial.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.runtime.math.BoundingBox;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.GltfEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {Config.TARGET_SDK})
public class BoundsComponentImplTest {

    private BoundsComponentImpl mBoundsComponent;
    private GltfEntity mGltfEntity;
    private Entity mEntity;
    private Executor mExecutor;
    private Consumer<BoundingBox> mListener;

    @Before
    public void setUp() {
        mBoundsComponent = new BoundsComponentImpl();
        mGltfEntity = mock(GltfEntity.class);
        mEntity = mock(Entity.class);
        mExecutor = Runnable::run;
        mListener = mock(TestConsumer.class);
        when(mGltfEntity.getGltfModelBoundingBox())
                .thenReturn(BoundingBox.fromMinMax(new Vector3(0, 0, 0), new Vector3(0, 0, 0)));
    }

    @Test
    public void onAttach_succeedsForGltfEntity() {
        assertThat(mBoundsComponent.onAttach(mGltfEntity)).isTrue();
    }

    @Test
    public void onAttach_failsForNonGltfEntity() {
        assertThat(mBoundsComponent.onAttach(mEntity)).isFalse();
    }

    @Test
    public void onAttach_failsIfAlreadyAttached() {
        mBoundsComponent.onAttach(mGltfEntity);

        assertThat(mBoundsComponent.onAttach(mGltfEntity)).isFalse();
    }

    @Test
    public void onDetach_clearsEntityAndRemovesListener() {
        mBoundsComponent.onAttach(mGltfEntity);
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, mListener);
        mBoundsComponent.onDetach(mGltfEntity);

        verify(mGltfEntity).removeOnBoundsUpdateListener(any());
        assertThat(mBoundsComponent.onAttach(mGltfEntity)).isTrue();
    }

    @Test
    public void addOnBoundsUpdateListener_addsListenerToGltfEntity() {
        mBoundsComponent.onAttach(mGltfEntity);
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, mListener);

        verify(mGltfEntity).addOnBoundsUpdateListener(any());
    }

    @Test
    public void removeOnBoundsUpdateListener_removesListenerFromGltfEntity() {
        mBoundsComponent.onAttach(mGltfEntity);
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, mListener);
        mBoundsComponent.removeOnBoundsUpdateListener(mListener);

        verify(mGltfEntity).removeOnBoundsUpdateListener(any());
    }

    @Test
    public void frameListener_notifiesListeners() {
        mBoundsComponent.onAttach(mGltfEntity);
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, mListener);

        ArgumentCaptor<TestConsumer> captor = ArgumentCaptor.forClass(TestConsumer.class);
        verify(mGltfEntity).addOnBoundsUpdateListener(captor.capture());

        Consumer<BoundingBox> frameListener = captor.getValue();
        BoundingBox boundingBox =
                BoundingBox.fromMinMax(new Vector3(0, 0, 0), new Vector3(1, 1, 1));
        frameListener.accept(boundingBox);

        verify(mListener, times(1)).accept(boundingBox);
    }

    @Test
    public void onAttach_withExistingListeners_addsListenerToGltfEntityAndNotifies() {
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, mListener);
        mBoundsComponent.onAttach(mGltfEntity);

        verify(mGltfEntity).addOnBoundsUpdateListener(any());
        verify(mListener, times(1)).accept(any(BoundingBox.class));
    }

    @Test
    public void onDetach_withExistingListeners_removesListenerFromGltfEntity() {
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, mListener);
        mBoundsComponent.onAttach(mGltfEntity);
        mBoundsComponent.onDetach(mGltfEntity);

        verify(mGltfEntity).removeOnBoundsUpdateListener(any());
    }

    @Test
    public void onDetach_withoutListeners_removesListenerFromGltfEntity() {
        mBoundsComponent.onAttach(mGltfEntity);
        mBoundsComponent.onDetach(mGltfEntity);

        verify(mGltfEntity).removeOnBoundsUpdateListener(any());
    }

    @Test
    public void addMultipleListeners_receivesUpdates() {
        mBoundsComponent.onAttach(mGltfEntity);
        Consumer<BoundingBox> listener1 = mock(TestConsumer.class);
        Consumer<BoundingBox> listener2 = mock(TestConsumer.class);
        ArgumentCaptor<TestConsumer> captor = ArgumentCaptor.forClass(TestConsumer.class);
        BoundingBox initialBoundingBox =
                BoundingBox.fromMinMax(new Vector3(0, 0, 0), new Vector3(0, 0, 0));
        when(mGltfEntity.getGltfModelBoundingBox()).thenReturn(initialBoundingBox);

        // Add first listener and capture the frame listener
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, listener1);
        verify(mGltfEntity).addOnBoundsUpdateListener(captor.capture());
        Consumer<BoundingBox> frameListener = captor.getValue();

        // Verify listener1 received the initial bounding box
        verify(listener1, times(1)).accept(initialBoundingBox);

        // Trigger first update
        BoundingBox boundingBox1 =
                BoundingBox.fromMinMax(new Vector3(0, 0, 0), new Vector3(1, 1, 1));
        frameListener.accept(boundingBox1);
        verify(listener1, times(1)).accept(boundingBox1);
        verify(listener2, never()).accept(any());

        // Add second listener
        mBoundsComponent.addOnBoundsUpdateListener(mExecutor, listener2);
        verify(listener2, times(1)).accept(initialBoundingBox);

        // Trigger second update
        BoundingBox boundingBox2 =
                BoundingBox.fromMinMax(new Vector3(0, 0, 0), new Vector3(2, 2, 2));
        frameListener.accept(boundingBox2);
        verify(listener1, times(1)).accept(boundingBox2);
        verify(listener2, times(1)).accept(boundingBox2);
    }

    private static class TestConsumer implements Consumer<BoundingBox> {
        @Override
        public void accept(BoundingBox boundingBox) {}
    }
}
