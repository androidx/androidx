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

package androidx.recyclerview.selection.testing;

import java.util.ArrayList;
import java.util.List;

public class TestData {

    public static List<String> createStringData(int num) {
        List<String> items = new ArrayList<>(num);
        for (int i = 0; i < num; ++i) {
            items.add(Integer.toString(i));
        }
        return items;
    }

    public static List<Long> createLongData(int num) {
        List<Long> items = new ArrayList<>(num);
        for (long i = 0; i < num; ++i) {
            items.add(i);
        }
        return items;
    }
}
