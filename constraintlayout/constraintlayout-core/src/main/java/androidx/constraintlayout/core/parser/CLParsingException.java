/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.parser;

@SuppressWarnings("OverrideThrowableToString")
public class CLParsingException extends Exception {
    private final String mReason;
    private final int mLineNumber;
    private final String mElementClass;

    public CLParsingException(String reason, CLElement element) {
        mReason = reason;
        if (element != null) {
            mElementClass = element.getStrClass();
            mLineNumber = element.getLine();
        } else {
            mElementClass = "unknown";
            mLineNumber = 0;
        }
    }

    // @TODO: add description
    public String reason() {
        return mReason + " (" + mElementClass + " at line " + mLineNumber + ")";
    }


    @Override
    public String toString() {
        return "CLParsingException (" + this.hashCode() + ") : " + reason();
    }
}
