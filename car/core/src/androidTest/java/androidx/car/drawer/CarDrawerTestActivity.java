/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.drawer;

import android.os.Bundle;
import android.widget.FrameLayout;

/**
 * Test activity for {@link CarDrawerActivity}.
 *
 * <p>This class sets MainContent as an empty {@link FrameLayout}, and does not provide drawer
 * content. To populate drawer, use {@link CarDrawerController#setRootAdapter(CarDrawerAdapter)}
 * to set implementation of {@link CarDrawerAdapter}.
 */
public final class CarDrawerTestActivity extends CarDrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMainContent(new FrameLayout(this));
    }
}
