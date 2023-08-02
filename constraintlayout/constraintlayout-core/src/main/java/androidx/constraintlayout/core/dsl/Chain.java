/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class Chain extends Helper {
    public enum Style {
        PACKED,
        SPREAD,
        SPREAD_INSIDE
    }

    private Style mStyle = null;
    protected ArrayList<Ref> references = new ArrayList<>();
    final static protected Map<Style, String> styleMap = new HashMap<>();
    static {
        styleMap.put(Style.SPREAD, "'spread'");
        styleMap.put(Style.SPREAD_INSIDE, "'spread_inside'");
        styleMap.put(Style.PACKED, "'packed'");
    }

    public Chain(String name) {
        super(name, new HelperType(""));
    }

    public Style getStyle() {
        return mStyle;
    }

    public void setStyle(Style style) {
        mStyle = style;
        configMap.put("style", styleMap.get(style));
    }

    /**
     * convert references to a String representation
     *
     * @return a String representation of references
     */
    public String referencesToString() {
        if (references.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("[");
        for (Ref ref : references) {
            builder.append(ref.toString());
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Add a new reference
     *
     * @param ref reference
     * @return Chain
     */
    public Chain addReference(Ref ref) {
        references.add(ref);
        configMap.put("contains", referencesToString());
        return this;
    }

    /**
     * Add a new reference
     *
     * @param ref reference in a String representation
     * @return Chain
     */
    public Chain addReference(String ref) {
        return addReference(Ref.parseStringToRef(ref));
    }

    public class Anchor {
        final Constraint.Side mSide;
        Constraint.Anchor mConnection = null;
        int mMargin;
        int mGoneMargin = Integer.MIN_VALUE;

        Anchor(Constraint.Side side) {
            mSide = side;
        }

        public String getId() {
            return name;
        }

        public void build(StringBuilder builder) {
            if (mConnection != null) {
                builder.append(mSide.toString().toLowerCase())
                        .append(":").append(this).append(",\n");
            }
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder("[");

            if (mConnection != null) {
                ret.append("'").append(mConnection.getId()).append("',")
                        .append("'").append(mConnection.mSide.toString().toLowerCase()).append("'");
            }

            if (mMargin != 0) {
                ret.append(",").append(mMargin);
            }

            if (mGoneMargin != Integer.MIN_VALUE) {
                if ( mMargin == 0) {
                    ret.append(",0,").append(mGoneMargin);
                } else {
                    ret.append(",").append(mGoneMargin);
                }
            }
            ret.append("]");
            return ret.toString();
        }
    }
}
