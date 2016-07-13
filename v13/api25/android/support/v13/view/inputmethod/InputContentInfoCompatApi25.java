/**
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v13.view.inputmethod;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.Parcel;
import android.view.inputmethod.InputContentInfo;

final class InputContentInfoCompatApi25 {

    public static Object create(Uri contentUri, ClipDescription description, Uri linkUri) {
        return new InputContentInfo(contentUri, description, linkUri);
    }

    public static Uri getContentUri(Object inputContentInfo) {
        return ((InputContentInfo) inputContentInfo).getContentUri();
    }

    public static ClipDescription getDescription(Object inputContentInfo) {
        return ((InputContentInfo) inputContentInfo).getDescription();
    }

    public static Uri getLinkUri(Object inputContentInfo) {
        return ((InputContentInfo) inputContentInfo).getLinkUri();
    }

    public static void requestPermission(Object inputContentInfo) {
        ((InputContentInfo) inputContentInfo).requestPermission();
    }

    public static void releasePermission(Object inputContentInfo) {
        ((InputContentInfo) inputContentInfo).requestPermission();
    }
}
