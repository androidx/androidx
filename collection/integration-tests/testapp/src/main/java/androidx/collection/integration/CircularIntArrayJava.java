/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.integration;

import androidx.collection.CircularIntArray;

/**
 * Integration (actually build) test for source compatibility in CircularIntArray usage from Java.
 */
@SuppressWarnings("unused")
public class CircularIntArrayJava {
    private CircularIntArrayJava() {
    }

    public static boolean sourceCompatibility() {
        CircularIntArray circularIntArray = new CircularIntArray();
        circularIntArray.addFirst(1);
        circularIntArray.addLast(1);
        circularIntArray.clear();
        return circularIntArray.isEmpty()
                && circularIntArray.get(0) >= 0
                && circularIntArray.size() == 0
                && circularIntArray.getFirst() == circularIntArray.getLast()
                && circularIntArray.popFirst() == circularIntArray.popLast();
    }
}
