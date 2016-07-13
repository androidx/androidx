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

import android.os.Bundle;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;

final class InputConnectionCompatApi25 {

    public static boolean commitContent(InputConnection ic, Object inputContentInfo, int flags,
            Bundle opts) {
        return ic.commitContent((InputContentInfo)inputContentInfo, flags, opts);
    }

    public interface OnCommitContentListener {
        boolean onCommitContent(Object inputContentInfo, int flags, Bundle opts);
    }

    public static InputConnection createWrapper(InputConnection ic,
            final OnCommitContentListener onCommitContentListener) {
        return new InputConnectionWrapper(ic, false /* mutable */) {
            @Override
            public boolean commitContent(InputContentInfo inputContentInfo, int flags,
                        Bundle opts) {
                if (onCommitContentListener.onCommitContent(inputContentInfo, flags, opts)) {
                    return true;
                }
                return super.commitContent(inputContentInfo, flags, opts);
            }
        };
    }

}
