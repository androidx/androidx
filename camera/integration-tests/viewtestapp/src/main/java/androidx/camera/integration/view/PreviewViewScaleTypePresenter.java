/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.view;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;

/**
 * Provides methods to handle presenting {@linkplain PreviewView.ScaleType PreviewView scale
 * types} as literals on the UI.
 */
class PreviewViewScaleTypePresenter {

    private static final String LITERAL_FILL_START = "FILL START";
    private static final String LITERAL_FILL_CENTER = "FILL CENTER";
    private static final String LITERAL_FILL_END = "FILL END";
    private static final String LITERAL_FIT_START = "FIT START";
    private static final String LITERAL_FIT_CENTER = "FIT CENTER";
    private static final String LITERAL_FIT_END = "FIT END";

    private PreviewViewScaleTypePresenter() {
    }

    /**
     * Returns a list of literals that represent all {@linkplain PreviewView.ScaleType
     * PreviewView scale types}.
     */
    @NonNull
    static String[] getScaleTypesLiterals() {
        return new String[]{LITERAL_FILL_START, LITERAL_FILL_CENTER, LITERAL_FILL_END,
                LITERAL_FIT_START, LITERAL_FIT_CENTER, LITERAL_FIT_END};
    }

    /**
     * Returns the literal corresponding to a {@link PreviewView.ScaleType PreviewView scale type}.
     */
    @NonNull
    static String getLiteralForScaleType(@NonNull final PreviewView.ScaleType scaleType) {
        switch (scaleType) {
            case FILL_START:
                return LITERAL_FILL_START;
            case FILL_CENTER:
                return LITERAL_FILL_CENTER;
            case FILL_END:
                return LITERAL_FILL_END;
            case FIT_START:
                return LITERAL_FIT_START;
            case FIT_CENTER:
                return LITERAL_FIT_CENTER;
            case FIT_END:
                return LITERAL_FIT_END;
            default:
                throw new IllegalArgumentException("Unknown scale type " + scaleType);
        }
    }
}
