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
import java.util.Arrays;

public class Ref {
    private String mId;
    private float mWeight = Float.NaN;
    private float mPreMargin = Float.NaN;
    private float mPostMargin = Float.NaN;

    Ref(String id) {
        mId = id;
    }

    Ref(String id, float weight) {
        mId = id;
        mWeight = weight;
    }

    Ref(String id, float weight, float preMargin) {
        mId = id;
        mWeight = weight;
        mPreMargin = preMargin;
    }

    Ref(String id, float weight, float preMargin, float postMargin) {
        mId = id;
        mWeight = weight;
        mPreMargin = preMargin;
        mPostMargin = postMargin;
    }

    /**
     * Get the Id of the reference
     *
     * @return the Id of the reference
     */
    public String getId() {
        return mId;
    }

    /**
     * Set the Id of the reference
     *
     * @param id
     */
    public void setId(String id) {
        mId = id;
    }

    /**
     * Get the weight of the reference
     *
     * @return the weight of the reference
     */
    public float getWeight() {
        return mWeight;
    }

    /**
     * Set the weight of the reference
     *
     * @param weight
     */
    public void setWeight(float weight) {
        mWeight = weight;
    }

    /**
     * Get the preMargin of the reference
     *
     * @return the preMargin of the reference
     */
    public float getPreMargin() {
        return mPreMargin;
    }

    /**
     * Set the preMargin of the reference
     *
     * @param preMargin
     */
    public void setPreMargin(float preMargin) {
        mPreMargin = preMargin;
    }

    /**
     * Get the postMargin of the reference
     *
     * @return the preMargin of the reference
     */
    public float getPostMargin() {
        return mPostMargin;
    }

    /**
     * Set the postMargin of the reference
     *
     * @param postMargin
     */
    public void setPostMargin(float postMargin) {
        mPostMargin = postMargin;
    }

    /**
     * Try to parse an object into a float number
     *
     * @param obj object to be parsed
     * @return a number
     */
    static public float parseFloat(Object obj) {
        float val = Float.NaN;
        try {
            val = Float.parseFloat(obj.toString());
        } catch (Exception e) {
            // ignore
        }
        return val;
    }

    static public Ref parseStringToRef(String str) {
        String[] values = str.replaceAll("[\\[\\]\\']", "").split(",");
        if (values.length == 0) {
            return null;
        }
        Object[] arr = new Object[4];
        for (int i = 0; i < values.length; i++) {
            if (i >= 4) {
                break;
            }
            arr[i] = values[i];
        }
        return new Ref(arr[0].toString().replace("'", ""), parseFloat(arr[1]),
                parseFloat(arr[2]), parseFloat(arr[3]));
    }

    /**
     * Add references in a String representation to a Ref ArrayList
     * Used to add the Ref(s) property in the Config to references
     *
     * @param str references in a String representation
     * @param refs  a Ref ArrayList
     */
    static public void addStringToReferences(String str, ArrayList<Ref> refs) {
        if (str == null || str.length() == 0) {
            return;
        }

        Object[] arr = new Object[4];
        StringBuilder builder = new StringBuilder();
        int squareBrackets = 0;
        int varCount = 0;
        char ch;

        for (int i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            switch (ch) {
                case '[':
                    squareBrackets++;
                    break;
                case ']':
                    if (squareBrackets > 0) {
                        squareBrackets--;
                        arr[varCount] = builder.toString();
                        builder.setLength(0);
                        if (arr[0] != null) {
                            refs.add(new Ref(arr[0].toString(), parseFloat(arr[1]),
                                    parseFloat(arr[2]), parseFloat(arr[3])));
                            varCount = 0;
                            Arrays.fill(arr, null);
                        }
                    }
                    break;
                case ',':
                    // deal with the first 3 values in the nested array,
                    // the fourth value (postMargin) would be handled at case ']'
                    if (varCount < 3) {
                        arr[varCount++] = builder.toString();
                        builder.setLength(0);
                    }
                    // squareBrackets == 1 indicate the value is not in a nested array.
                    if (squareBrackets == 1 && arr[0] != null) {
                        refs.add(new Ref(arr[0].toString()));
                        varCount = 0;
                        arr[0] = null;
                    }
                    break;
                case ' ':
                case '\'':
                    break;
                default:
                    builder.append(ch);
            }
        }
    }

    @Override
    public String toString() {
        if (mId == null || mId.length() == 0) {
            return "";
        }

        StringBuilder ret = new StringBuilder();
        boolean isArray = false;
        if (!Float.isNaN(mWeight) || !Float.isNaN(mPreMargin)
                || !Float.isNaN(mPostMargin)) {
            isArray = true;
        }
        if (isArray) {
            ret.append("[");
        }
        ret.append("'").append(mId).append("'");

        if (!Float.isNaN(mPostMargin)) {
            ret.append(",").append(!Float.isNaN(mWeight) ? mWeight : 0).append(",");
            ret.append(!Float.isNaN(mPreMargin) ? mPreMargin : 0).append(",");
            ret.append(mPostMargin);
        } else if (!Float.isNaN(mPreMargin)) {
            ret.append(",").append(!Float.isNaN(mWeight) ? mWeight : 0).append(",");
            ret.append(mPreMargin);
        } else if (!Float.isNaN(mWeight)) {
            ret.append(",").append(mWeight);
        }

        if(isArray) {
            ret.append("]");
        }
        ret.append(",");
        return ret.toString();
    }
}
