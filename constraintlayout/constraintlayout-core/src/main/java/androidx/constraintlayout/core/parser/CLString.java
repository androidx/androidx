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

import androidx.annotation.NonNull;

/**
 * {@link CLElement} implementation for json Strings when used as property values or array elements.
 */
public class CLString extends CLElement {

    public CLString(char[] content) {
        super(content);
    }

    // @TODO: add description
    public static CLElement allocate(char[] content) {
        return new CLString(content);
    }

    /**
     * Creates a {@link CLString} element from a String object.
     */
    @NonNull
    public static CLString from(@NonNull String content) {
        CLString stringElement = new CLString(content.toCharArray());
        stringElement.setStart(0L);
        stringElement.setEnd(content.length() - 1);
        return stringElement;
    }

    @Override
    protected String toJSON() {
        return "'" + content() + "'";
    }

    @Override
    protected String toFormattedJSON(int indent, int forceIndent) {
        StringBuilder json = new StringBuilder();
        addIndent(json, indent);
        json.append("'");
        json.append(content());
        json.append("'");
        return json.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CLString && this.content().equals(((CLString) obj).content())) {
            return true;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}


