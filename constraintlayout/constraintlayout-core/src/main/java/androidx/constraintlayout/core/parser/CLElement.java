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

import java.util.Arrays;
import java.util.Objects;

/**
 * Base element to represent a piece of parsed Json.
 */
public class CLElement implements Cloneable {

    private final char[] mContent;
    protected long mStart = -1;
    protected long mEnd = Long.MAX_VALUE;
    protected CLContainer mContainer;
    private int mLine;

    protected static int sMaxLine = 80; // Max number of characters before the formatter indents
    protected static int sBaseIndent = 2; // default indentation value

    public CLElement(char[] content) {
        mContent = content;
    }

    // @TODO: add description
    public boolean notStarted() {
        return mStart == -1;
    }

    public void setLine(int line) {
        this.mLine = line;
    }

    /**
     * get the line Number
     *
     * @return return the line number this element was on
     */
    public int getLine() {
        return mLine;
    }

    public void setStart(long start) {
        this.mStart = start;
    }

    /**
     * The character index this element was started on
     */
    public long getStart() {
        return this.mStart;
    }

    /**
     * The character index this element was ended on
     */
    public long getEnd() {
        return this.mEnd;
    }

    // @TODO: add description
    public void setEnd(long end) {
        if (this.mEnd != Long.MAX_VALUE) {
            return;
        }
        this.mEnd = end;
        if (CLParser.sDebug) {
            System.out.println("closing " + this.hashCode() + " -> " + this);
        }
        if (mContainer != null) {
            mContainer.add(this);
        }
    }

    protected void addIndent(StringBuilder builder, int indent) {
        for (int i = 0; i < indent; i++) {
            builder.append(' ');
        }
    }

    @Override
    public String toString() {
        if (mStart > mEnd || mEnd == Long.MAX_VALUE) {
            return this.getClass() + " (INVALID, " + mStart + "-" + mEnd + ")";
        }
        String content = new String(mContent);
        content = content.substring((int) mStart, (int) mEnd + 1);

        return getStrClass() + " (" + mStart + " : " + mEnd + ") <<" + content + ">>";
    }

    protected String getStrClass() {
        String myClass = this.getClass().toString();
        return myClass.substring(myClass.lastIndexOf('.') + 1);
    }

    protected String getDebugName() {
        if (CLParser.sDebug) {
            return getStrClass() + " -> ";
        }
        return "";
    }

    // @TODO: add description
    public String content() {
        String content = new String(mContent);
        // Handle empty string
        if (content.length() < 1) {
            return "";
        }
        if (mEnd == Long.MAX_VALUE || mEnd < mStart) {
            return content.substring((int) mStart, (int) mStart + 1);
        }
        return content.substring((int) mStart, (int) mEnd + 1);
    }

    /**
     * Whether this element has any valid content defined.
     * <p>
     * The content is valid when {@link #content()} can be called without causing exceptions.
     */
    public boolean hasContent() {
        return mContent != null && mContent.length >= 1;
    }

    public boolean isDone() {
        return mEnd != Long.MAX_VALUE;
    }

    public void setContainer(CLContainer element) {
        mContainer = element;
    }

    public CLElement getContainer() {
        return mContainer;
    }

    public boolean isStarted() {
        return mStart > -1;
    }

    protected String toJSON() {
        return "";
    }

    protected String toFormattedJSON(int indent, int forceIndent) {
        return "";
    }

    // @TODO: add description
    public int getInt() {
        if (this instanceof CLNumber) {
            return ((CLNumber) this).getInt();
        }
        return 0;
    }

    // @TODO: add description
    public float getFloat() {
        if (this instanceof CLNumber) {
            return ((CLNumber) this).getFloat();
        }
        return Float.NaN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CLElement)) return false;

        CLElement clElement = (CLElement) o;

        if (mStart != clElement.mStart) return false;
        if (mEnd != clElement.mEnd) return false;
        if (mLine != clElement.mLine) return false;
        if (!Arrays.equals(mContent, clElement.mContent)) return false;
        return Objects.equals(mContainer, clElement.mContainer);
    }

    @Override
    public int hashCode() {
        // Auto-generated with Intellij Action "equals() and hashcode()"
        int result = Arrays.hashCode(mContent);
        result = 31 * result + (int) (mStart ^ (mStart >>> 32));
        result = 31 * result + (int) (mEnd ^ (mEnd >>> 32));
        result = 31 * result + (mContainer != null ? mContainer.hashCode() : 0);
        result = 31 * result + mLine;
        return result;
    }

    @Override
    public CLElement clone() {
        try {
            CLElement clone = (CLElement) super.clone();
            if (mContainer != null) {
                clone.mContainer = (CLContainer) mContainer.clone();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}