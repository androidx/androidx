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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Manager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ManagerTest {
    private static class MockManager implements Manager {
        @Nullable
        public final Integer mFoo;
        @Nullable
        public final String mBar;

        /** Accessed via reflection */
        @SuppressWarnings("unused")
        MockManager() {
            mFoo = null;
            mBar = null;
        }

        /** Accessed via reflection */
        @SuppressWarnings("unused")
        MockManager(@NonNull Integer foo, @NonNull String bar) {
            mFoo = foo;
            mBar = bar;
        }
    }

    @Test
    public void createManager_classNotFound_returnsNull() {
        MockManager manager = Manager.create(MockManager.class, "foo.bar");
        assertThat(manager).isNull();
    }

    @Test(expected = IllegalStateException.class)
    public void createManager_mismatchingConstructor_throws() {
        Manager.create(MockManager.class, "androidx.car.app.managers.ManagerTest$MockManager",
                123f);
    }

    @Test
    public void createManager_defaultConstructor_returnsInstance() {
        MockManager manager = Manager.create(MockManager.class,
                "androidx.car.app.managers.ManagerTest$MockManager");
        assertThat(manager).isNotNull();
        assertThat(manager.mFoo).isNull();
        assertThat(manager.mBar).isNull();
    }

    @Test
    public void createManager_multipleParamsConstructor_returnsInstance() {
        MockManager manager = Manager.create(MockManager.class,
                "androidx.car.app.managers.ManagerTest$MockManager", 123, "abc");
        assertThat(manager).isNotNull();
        assertThat(manager.mFoo).isEqualTo(123);
        assertThat(manager.mBar).isEqualTo("abc");
    }
}
