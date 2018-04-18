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

import java.util.Collection;

/**
 * An interface that you can implement on your Adapter to enable support for Alpha-Jump.
 */
public interface IAlphaJumpAdapter {
    /**
     * A bucket represents a "button" in the alpha-jump menu.
     */
    interface Bucket {
        /**
         * Return true if the bucket is empty (and therefore the button should be displayed
         * disabled).
         */
        boolean isEmpty();

        /**
         * Return the label for this bucket, that is displayed at the text to the user.
         */
        CharSequence getLabel();

        /**
         * Return the index of the first item that this bucket matches in the list.
         */
        int getIndex();
    }

    /**
     * Generate and populate the buckets used to populate the alpha-jump menu.
     */
    Collection<Bucket> getAlphaJumpBuckets();

    /**
     * Called every time the alpha-jump menu is entered.
     */
    void onAlphaJumpEnter();

    /**
     * Called every time the alpha-jump menu is left.
     *
     * @param bucket The {@link Bucket} that the user clicked on, and should be jumped to.
     */
    void onAlphaJumpLeave(Bucket bucket);
}
