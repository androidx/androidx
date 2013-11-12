/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v8.renderscript;

import java.lang.Exception;

class ExceptionThunker {
    static RuntimeException convertException (RuntimeException e) {
        if (e instanceof android.renderscript.RSIllegalArgumentException) {
            return new android.support.v8.renderscript.RSIllegalArgumentException(e.getMessage());
        } else if (e instanceof android.renderscript.RSInvalidStateException) {
            return new android.support.v8.renderscript.RSInvalidStateException(e.getMessage());
        } else if (e instanceof android.renderscript.RSDriverException) {
            return new android.support.v8.renderscript.RSDriverException(e.getMessage());
        } else if (e instanceof android.renderscript.RSRuntimeException) {
            return new android.support.v8.renderscript.RSRuntimeException(e.getMessage());
        }
        return e;
    }

}