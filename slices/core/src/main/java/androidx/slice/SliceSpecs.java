/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice;

import androidx.annotation.RestrictTo;

/**
 * Constants for each of the slice specs
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SliceSpecs {

    /**
     * Most basic slice, only has icon, title, and summary.
     */
    public static final SliceSpec BASIC = new SliceSpec("androidx.slice.BASIC", 1);

    /**
     * List of rows, each row has start/end items, title, summary.
     * Also supports grid rows.
     */
    public static final SliceSpec LIST = new SliceSpec("androidx.slice.LIST", 1);

    /**
     * Messaging template. Each message contains a timestamp and a message, it optionally contains
     * a source of where the message came from.
     */
    public static final SliceSpec MESSAGING = new SliceSpec("androidx.slice.MESSAGING", 1);

    private SliceSpecs() {
    }
}
