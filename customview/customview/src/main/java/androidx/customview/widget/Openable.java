/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.customview.widget;

/**
 * A layout that has two states: opened and closed.
 */
public interface Openable {

    /**
     * Check if the layout is currently in the opened state.
     *
     * @return true if the layout's state is considered opened.
     */
    boolean isOpen();

    /**
     * Move the layout to the opened state.
     */
    void open();

    /**
     * Move the layout to the closed state.
     */
    void close();
}
