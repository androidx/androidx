/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.widget;

import java.util.List;

/**
 * An interface that you can implement on your Adapter to enable support for Alpha-Jump.
 *
 * <p>Alpha-Jump buckets only support characters from the {@code en} language. Characters from other
 * languages are not supported and bucketing behavior is undefined.
 */
public interface AlphaJumpAdapter {

    /**
     * Generate and populate the buckets used to populate the alpha-jump menu.
     */
    List<AlphaJumpBucket> getAlphaJumpBuckets();

    /**
     * Called every time the alpha-jump menu is entered.
     */
    void onAlphaJumpEnter();

    /**
     * Called every time the alpha-jump menu is left.
     *
     * @param bucket The {@link AlphaJumpBucket} that the user clicked on, and should be jumped to.
     */
    void onAlphaJumpLeave(AlphaJumpBucket bucket);
}
