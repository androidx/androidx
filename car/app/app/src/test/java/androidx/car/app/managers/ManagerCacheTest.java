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

package androidx.car.app.managers;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ManagerCache}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ManagerCacheTest {
    private final ManagerCache mManagers = new ManagerCache();

    private static class FooManager implements Manager {};
    private static class BarManager implements Manager {};

    private static class MockFactory<T extends Manager> implements ManagerFactory<T> {
        private final RuntimeException mException;
        private final T mManager;

        MockFactory(@Nullable T manager, @Nullable RuntimeException exception) {
            mException = exception;
            mManager = manager;
        }

        @NonNull
        @Override
        public T create() {
            if (mException != null) {
                throw mException;
            }
            return requireNonNull(mManager);
        }
    }

    private static class SimpleFactory<T extends Manager> implements ManagerFactory<BarManager> {
        @NonNull
        @Override
        public BarManager create() {
            return new BarManager();
        }
    }

    @Before
    public void setup() {
        mManagers.addFactory(FooManager.class, "foo", FooManager::new);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getName_classNotFound_throws() {
        mManagers.getName(BarManager.class);
    }

    @Test
    public void getName_classFound_returnsCorrectName() {
        assertThat(mManagers.getName(FooManager.class)).isEqualTo("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreate_nameNotFound_throws() {
        mManagers.getOrCreate("bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOrCreate_classNotFound_throws() {
        mManagers.getOrCreate(BarManager.class);
    }

    @Test
    public void getOrCreate_firstTime_createsNew() {
        BarManager testManager = new BarManager();
        ManagerFactory<BarManager> mockFactory = spy(new MockFactory<>(testManager, null));
        mManagers.addFactory(BarManager.class, "bar", mockFactory);
        BarManager manager = mManagers.getOrCreate(BarManager.class);
        assertThat(manager).isEqualTo(testManager);
        verify(mockFactory, times(1)).create();
    }

    @Test
    public void getOrCreate_secondTime_returnsPrevious() {
        BarManager testManager = new BarManager();
        ManagerFactory<BarManager> mockFactory = spy(new MockFactory<>(testManager, null));
        mManagers.addFactory(BarManager.class, "bar", mockFactory);
        BarManager manager = mManagers.getOrCreate(BarManager.class);
        assertThat(manager).isEqualTo(testManager);
        manager = mManagers.getOrCreate(BarManager.class);
        assertThat(manager).isEqualTo(testManager);
        verify(mockFactory, times(1)).create();
    }

    @Test
    public void getOrCreate_instantiationFails_throws() {
        RuntimeException testException = new RuntimeException();
        ManagerFactory<BarManager> mockFactory = spy(new MockFactory<>(null, testException));
        mManagers.addFactory(BarManager.class, "bar", mockFactory);
        try {
            mManagers.getOrCreate(BarManager.class);
            fail();
        } catch (RuntimeException ex) {
            assertThat(ex).isEqualTo(testException);
        }
        verify(mockFactory, times(1)).create();
    }

    @Test
    public void getOrCreate_instantiationAlreadyFailed_throwsPreviousException() {
        RuntimeException testException = new RuntimeException();
        ManagerFactory<BarManager> mockFactory = spy(new MockFactory<>(null, testException));
        mManagers.addFactory(BarManager.class, "bar", mockFactory);
        try {
            mManagers.getOrCreate(BarManager.class);
            fail();
        } catch (RuntimeException ex) {
            assertThat(ex).isEqualTo(testException);
        }
        try {
            mManagers.getOrCreate(BarManager.class);
            fail();
        } catch (RuntimeException ex) {
            assertThat(ex).isEqualTo(testException);
        }
        verify(mockFactory, times(1)).create();
    }
}
