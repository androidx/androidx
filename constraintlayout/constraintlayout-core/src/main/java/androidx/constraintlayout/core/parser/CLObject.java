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

import java.util.Iterator;

public class CLObject extends CLContainer implements Iterable<CLKey> {

    public CLObject(char[] content) {
        super(content);
    }

    /**
     * Allocate a CLObject around an array of chars
     */
    public static CLObject allocate(char[] content) {
        return new CLObject(content);
    }

    /**
     * Returns objet as a JSON5 String
     */
    @Override
    public String toJSON() {
        StringBuilder json = new StringBuilder(getDebugName() + "{ ");
        boolean first = true;
        for (CLElement element : mElements) {
            if (!first) {
                json.append(", ");
            } else {
                first = false;
            }
            json.append(element.toJSON());
        }
        json.append(" }");
        return json.toString();
    }

    /**
     * Returns a object as a formatted JSON5 String
     */
    public String toFormattedJSON() {
        return toFormattedJSON(0, 0);
    }

    /**
     * Returns as a formatted JSON5 String with an indentation
     */
    @Override
    public String toFormattedJSON(int indent, int forceIndent) {
        StringBuilder json = new StringBuilder(getDebugName());
        json.append("{\n");
        boolean first = true;
        for (CLElement element : mElements) {
            if (!first) {
                json.append(",\n");
            } else {
                first = false;
            }
            json.append(element.toFormattedJSON(indent + sBaseIndent, forceIndent - 1));
        }
        json.append("\n");
        addIndent(json, indent);
        json.append("}");
        return json.toString();
    }

    @Override
    public Iterator<CLKey> iterator() {
        return new CLObjectIterator(this);
    }

    private static class CLObjectIterator implements Iterator<CLKey> {
        CLObject mObject;
        int mIndex = 0;

        CLObjectIterator(CLObject clObject) {
            mObject = clObject;
        }

        @Override
        public boolean hasNext() {
            return mIndex < mObject.size();
        }

        @Override
        public CLKey next() {
            CLKey key = (CLKey) mObject.mElements.get(mIndex);
            mIndex++;
            return key;
        }
    }

    @NonNull
    @Override
    public CLObject clone() {
        // Overriding to get expected return type
        return (CLObject) super.clone();
    }
}
