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

package androidx.slice;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_BUNDLE;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

/**
 * Class used to see if two Slices are structurally equivalent ignoring
 * specific content such as text or icons.
 *
 * Two structures can be compared using {@link #equals(Object)}.
 */
public class SliceStructure {

    private final String mStructure;

    /**
     * Create a SliceStructure.
     */
    public SliceStructure(Slice s) {
        StringBuilder str = new StringBuilder();
        getStructure(s, str);
        mStructure = str.toString();
    }

    @Override
    public int hashCode() {
        return mStructure.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SliceStructure)) return false;
        SliceStructure other = (SliceStructure) obj;
        return mStructure.equals(other.mStructure);
    }

    private static void getStructure(Slice s, StringBuilder str) {
        str.append("s{");
        for (SliceItem item : s.getItems()) {
            getStructure(item, str);
        }
        str.append("}");
    }

    private static void getStructure(SliceItem item, StringBuilder str) {
        switch (item.getFormat()) {
            case FORMAT_SLICE:
                getStructure(item.getSlice(), str);
                break;
            case FORMAT_ACTION:
                str.append('a');
                getStructure(item.getSlice(), str);
                break;
            case FORMAT_TEXT:
                str.append('t');
                break;
            case FORMAT_IMAGE:
                str.append('i');
                break;
            case FORMAT_INT:
            case FORMAT_LONG:
            case FORMAT_REMOTE_INPUT:
            case FORMAT_BUNDLE:
            default:
                // Generally adding or removing these types is ok.
                break;
        }
    }
}
