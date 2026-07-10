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

import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class SingleCloseImageProxyTest {

    private final FakeImageProxy mImageProxy = new FakeImageProxy(new FakeImageInfo());
    private SingleCloseImageProxy mSingleCloseImageProxy;

    @Before
    public void setUp() {
        mSingleCloseImageProxy = new SingleCloseImageProxy(mImageProxy);
    }

    @Test
    public void wrappedImageIsClosedOnce_whenWrappingImageIsClosedOnce() {
        mSingleCloseImageProxy.close();

        assertThat(mImageProxy.isClosed()).isTrue();
    }

    @Test
    public void wrappedImageIsClosedOnce_whenWrappingImageIsClosedTwice() {
        mSingleCloseImageProxy.close();
        mSingleCloseImageProxy.close();

        assertThat(mImageProxy.isClosed()).isTrue();
    }
}
