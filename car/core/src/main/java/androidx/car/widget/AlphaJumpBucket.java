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

/**
 * A bucket represents a "button" in the alpha-jump menu.
 *
 * <p>Alpha-Jump buckets only support characters from the {@code en} language. Characters from other
 * languages are not supported and bucketing behavior is undefined.
 */
public interface AlphaJumpBucket {

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
