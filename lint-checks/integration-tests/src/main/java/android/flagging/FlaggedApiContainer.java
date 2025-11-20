/*
 * Copyright 2025 The Android Open Source Project
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

package android.flagging;

import android.annotation.FlaggedApi;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Class that is flagged and exposes APIs.
 */
@SuppressWarnings("unused")
@FlaggedApi("flaggedapi.myFlag")
public class FlaggedApiContainer {
    private FlaggedApiContainer() {
        // Do not instantiate.
    }

    /**
     * API contained within a flagged class.
     */
    public static boolean innerApi() {
        return false;
    }

    /**
     * API that uses type arguments.
     */
    public static List<int[]> apiWithTypeArgument(BiConsumer<Integer, Float> param) {
        return null;
    }

    /**
     * API that uses primitive arrays.
     */
    public static float[][] apiWithTwoDimensionalArray(int[] param) {
        return null;
    }

    /**
     * API that uses generic types.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T, R> T apiWithGenericType(R param) {
        return null;
    }
}
