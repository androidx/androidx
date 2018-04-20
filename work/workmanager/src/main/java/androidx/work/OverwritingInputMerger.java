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

package androidx.work;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link InputMerger} that attempts to add all keys from all inputs to the output.  In case of a
 * conflict, this class will overwrite the previously-set key.  Because there is no defined order
 * for inputs, this implementation is best suited for cases where conflicts will not happen, or
 * where overwriting is a valid strategy to deal with them.
 */

public final class OverwritingInputMerger extends InputMerger {

    @Override
    public @NonNull Data merge(@NonNull List<Data> inputs) {
        Data.Builder output = new Data.Builder();
        Map<String, Object> mergedValues = new HashMap<>();

        for (Data input : inputs) {
            mergedValues.putAll(input.getKeyValueMap());
        }

        output.putAll(mergedValues);
        return output.build();
    }
}
