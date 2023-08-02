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

public class CLToken extends CLElement {
    int mIndex = 0;
    Type mType = Type.UNKNOWN;

    // @TODO: add description
    public boolean getBoolean() throws CLParsingException {
        if (mType == Type.TRUE) {
            return true;
        }
        if (mType == Type.FALSE) {
            return false;
        }
        throw new CLParsingException("this token is not a boolean: <" + content() + ">", this);
    }

    // @TODO: add description
    public boolean isNull() throws CLParsingException {
        if (mType == Type.NULL) {
            return true;
        }
        throw new CLParsingException("this token is not a null: <" + content() + ">", this);
    }

    enum Type {UNKNOWN, TRUE, FALSE, NULL}

    char[] mTokenTrue = "true".toCharArray();
    char[] mTokenFalse = "false".toCharArray();
    char[] mTokenNull = "null".toCharArray();

    public CLToken(char[] content) {
        super(content);
    }

    // @TODO: add description
    public static CLElement allocate(char[] content) {
        return new CLToken(content);
    }

    @Override
    protected String toJSON() {
        if (CLParser.sDebug) {
            return "<" + content() + ">";
        } else {
            return content();
        }
    }

    @Override
    protected String toFormattedJSON(int indent, int forceIndent) {
        StringBuilder json = new StringBuilder();
        addIndent(json, indent);
        json.append(content());
        return json.toString();
    }

    @SuppressWarnings("HiddenTypeParameter")
    public Type getType() {
        return mType;
    }

    // @TODO: add description
    public boolean validate(char c, long position) {
        boolean isValid = false;
        switch (mType) {
            case TRUE: {
                isValid = (mTokenTrue[mIndex] == c);
                if (isValid && mIndex + 1 == mTokenTrue.length) {
                    setEnd(position);
                }
            }
            break;
            case FALSE: {
                isValid = (mTokenFalse[mIndex] == c);
                if (isValid && mIndex + 1 == mTokenFalse.length) {
                    setEnd(position);
                }
            }
            break;
            case NULL: {
                isValid = (mTokenNull[mIndex] == c);
                if (isValid && mIndex + 1 == mTokenNull.length) {
                    setEnd(position);
                }
            }
            break;
            case UNKNOWN: {
                if (mTokenTrue[mIndex] == c) {
                    mType = Type.TRUE;
                    isValid = true;
                } else if (mTokenFalse[mIndex] == c) {
                    mType = Type.FALSE;
                    isValid = true;
                } else if (mTokenNull[mIndex] == c) {
                    mType = Type.NULL;
                    isValid = true;
                }
            }
        }

        mIndex++;
        return isValid;
    }

}
