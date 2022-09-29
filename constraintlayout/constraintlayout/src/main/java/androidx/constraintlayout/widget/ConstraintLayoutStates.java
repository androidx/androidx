/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public class ConstraintLayoutStates {
    public static final String TAG = "ConstraintLayoutStates";
    private static final boolean DEBUG = false;
    private final ConstraintLayout mConstraintLayout;
    ConstraintSet mDefaultConstraintSet;
    int mCurrentStateId = -1; // default
    int mCurrentConstraintNumber = -1; // default
    private SparseArray<State> mStateList = new SparseArray<>();
    private SparseArray<ConstraintSet> mConstraintSetMap = new SparseArray<>();
    private ConstraintsChangedListener mConstraintsChangedListener = null;

    ConstraintLayoutStates(Context context, ConstraintLayout layout, int resourceID) {
        mConstraintLayout = layout;
        load(context, resourceID);
    }

    /**
     * Return true if it needs to change
     * @param id
     * @param width
     * @param height
     * @return
     */
    public boolean needsToChange(int id, float width, float height) {
        if (mCurrentStateId != id) {
            return true;
        }

        State state = (id == -1) ? mStateList.valueAt(0) : mStateList.get(mCurrentStateId);

        if (mCurrentConstraintNumber != -1) {
            if (state.mVariants.get(mCurrentConstraintNumber).match(width, height)) {
                return false;
            }
        }

        if (mCurrentConstraintNumber == state.findMatch(width, height)) {
            return false;
        }
        return true;
    }

    /**
     * updateConstraints for the view with the id and width and height
     * @param id
     * @param width
     * @param height
     */
    public void updateConstraints(int id, float width, float height) {
        if (mCurrentStateId == id) {
            State state;
            if (id == -1) {
                state = mStateList.valueAt(0); // id not being used take the first
            } else {
                state = mStateList.get(mCurrentStateId);

            }
            if (mCurrentConstraintNumber != -1) {
                if (state.mVariants.get(mCurrentConstraintNumber).match(width, height)) {
                    return;
                }
            }
            int match = state.findMatch(width, height);
            if (mCurrentConstraintNumber == match) {
                return;
            }

            ConstraintSet constraintSet = (match == -1) ? mDefaultConstraintSet :
                    state.mVariants.get(match).mConstraintSet;
            int cid = (match == -1) ? state.mConstraintID :
                    state.mVariants.get(match).mConstraintID;
            if (constraintSet == null) {
                return;
            }
            mCurrentConstraintNumber = match;
            if (mConstraintsChangedListener != null) {
                mConstraintsChangedListener.preLayoutChange(-1, cid);
            }
            constraintSet.applyTo(mConstraintLayout);
            if (mConstraintsChangedListener != null) {
                mConstraintsChangedListener.postLayoutChange(-1, cid);
            }

        } else {
            mCurrentStateId = id;
            State state = mStateList.get(mCurrentStateId);
            int match = state.findMatch(width, height);
            ConstraintSet constraintSet = (match == -1) ? state.mConstraintSet :
                    state.mVariants.get(match).mConstraintSet;
            int cid = (match == -1) ? state.mConstraintID :
                    state.mVariants.get(match).mConstraintID;

            if (constraintSet == null) {
                Log.v(TAG, "NO Constraint set found ! id=" + id
                        + ", dim =" + width + ", " + height);
                return;
            }
            mCurrentConstraintNumber = match;
            if (mConstraintsChangedListener != null) {
                mConstraintsChangedListener.preLayoutChange(id, cid);
            }
            constraintSet.applyTo(mConstraintLayout);
            if (mConstraintsChangedListener != null) {
                mConstraintsChangedListener.postLayoutChange(id, cid);
            }
        }

    }

    public void setOnConstraintsChanged(ConstraintsChangedListener constraintsChangedListener) {
        this.mConstraintsChangedListener = constraintsChangedListener;
    }

    /////////////////////////////////////////////////////////////////////////
    //      This represents one state
    /////////////////////////////////////////////////////////////////////////
    static class State {
        int mId;
        ArrayList<Variant> mVariants = new ArrayList<>();
        int mConstraintID = -1;
        ConstraintSet mConstraintSet;

        State(Context context, XmlPullParser parser) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.State);
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.State_android_id) {
                    mId = a.getResourceId(attr, mId);
                } else if (attr == R.styleable.State_constraints) {
                    mConstraintID = a.getResourceId(attr, mConstraintID);
                    String type = context.getResources().getResourceTypeName(mConstraintID);
                    String name = context.getResources().getResourceName(mConstraintID);

                    if ("layout".equals(type)) {
                        mConstraintSet = new ConstraintSet();
                        mConstraintSet.clone(context, mConstraintID);
                        if (DEBUG) {
                            Log.v(TAG, "############### mConstraintSet.load(" + name + ")");
                        }
                    }
                }
            }
            a.recycle();
        }

        void add(Variant size) {
            mVariants.add(size);
        }

        public int findMatch(float width, float height) {
            for (int i = 0; i < mVariants.size(); i++) {
                if (mVariants.get(i).match(width, height)) {
                    return i;
                }
            }
            return -1;
        }
    }

    static class Variant {
        int mId;
        float mMinWidth = Float.NaN;
        float mMinHeight = Float.NaN;
        float mMaxWidth = Float.NaN;
        float mMaxHeight = Float.NaN;
        int mConstraintID = -1;
        ConstraintSet mConstraintSet;

        Variant(Context context, XmlPullParser parser) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Variant);
            final int count = a.getIndexCount();
            if (DEBUG) {
                Log.v(TAG, "############### Variant");
            }

            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.Variant_constraints) {
                    mConstraintID = a.getResourceId(attr, mConstraintID);
                    String type = context.getResources().getResourceTypeName(mConstraintID);
                    String name = context.getResources().getResourceName(mConstraintID);

                    if ("layout".equals(type)) {
                        mConstraintSet = new ConstraintSet();
                        if (DEBUG) {
                            Log.v(TAG, "############### mConstraintSet.load(" + name + ")");
                        }
                        mConstraintSet.clone(context, mConstraintID);
                        if (DEBUG) {
                            Log.v(TAG, "############### mConstraintSet.load(" + name + ")");
                        }
                    } else {
                        if (DEBUG) {
                            Log.v(TAG, "############### id -> "
                                    + "ConstraintSet should be in this file");
                        }

                    }
                } else if (attr == R.styleable.Variant_region_heightLessThan) {
                    mMaxHeight = a.getDimension(attr, mMaxHeight);
                } else if (attr == R.styleable.Variant_region_heightMoreThan) {
                    mMinHeight = a.getDimension(attr, mMinHeight);
                } else if (attr == R.styleable.Variant_region_widthLessThan) {
                    mMaxWidth = a.getDimension(attr, mMaxWidth);
                } else if (attr == R.styleable.Variant_region_widthMoreThan) {
                    mMinWidth = a.getDimension(attr, mMinWidth);
                } else {
                    Log.v(TAG, "Unknown tag");
                }
            }
            a.recycle();
            if (DEBUG) {
                Log.v(TAG, "############### Variant");
                if (!Float.isNaN(mMinWidth)) {
                    Log.v(TAG, "############### Variant mMinWidth " + mMinWidth);
                }
                if (!Float.isNaN(mMinHeight)) {
                    Log.v(TAG, "############### Variant mMinHeight " + mMinHeight);
                }
                if (!Float.isNaN(mMaxWidth)) {
                    Log.v(TAG, "############### Variant mMaxWidth " + mMaxWidth);
                }
                if (!Float.isNaN(mMaxHeight)) {
                    Log.v(TAG, "############### Variant mMinWidth " + mMaxHeight);
                }
            }
        }

        boolean match(float widthDp, float heightDp) {
            if (DEBUG) {
                Log.v(TAG, "width = " + (int) widthDp
                        + " < " + mMinWidth + " && " + (int) widthDp + " > " + mMaxWidth
                        + " height = " + (int) heightDp
                        + " < " + mMinHeight + " && " + (int) heightDp + " > " + mMaxHeight);
            }
            if (!Float.isNaN(mMinWidth)) {
                if (widthDp < mMinWidth) return false;
            }
            if (!Float.isNaN(mMinHeight)) {
                if (heightDp < mMinHeight) return false;
            }
            if (!Float.isNaN(mMaxWidth)) {
                if (widthDp > mMaxWidth) return false;
            }
            if (!Float.isNaN(mMaxHeight)) {
                if (heightDp > mMaxHeight) return false;
            }
            return true;

        }
    }

    /**
     * Load a constraint set from a constraintSet.xml file
     *
     * @param context    the context for the inflation
     * @param resourceId mId of xml file in res/xml/
     */
    private void load(Context context, int resourceId) {
        if (DEBUG) {
            Log.v(TAG, "############### ");
        }
        Resources res = context.getResources();
        XmlPullParser parser = res.getXml(resourceId);
        try {
            Variant match;
            State state = null;
            for (int eventType = parser.getEventType();
                    eventType != XmlResourceParser.END_DOCUMENT;
                    eventType = parser.next()) {
                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                    case XmlResourceParser.END_TAG:
                    case XmlResourceParser.TEXT:
                        break;
                    case XmlResourceParser.START_TAG:
                        String tagName = parser.getName();
                        switch (tagName) {
                            case "layoutDescription":
                            case "StateSet":
                                break;
                            case "State":
                                state = new State(context, parser);
                                mStateList.put(state.mId, state);
                                break;
                            case "Variant":
                                match = new Variant(context, parser);
                                if (state != null) {
                                    state.add(match);
                                }
                                break;
                            case "ConstraintSet":
                                parseConstraintSet(context, parser);
                                break;
                            default:
                                if (DEBUG) {
                                    Log.v(TAG, "unknown tag " + tagName);
                                }
                        }
                        break;
                }
            }
//            for (Variant sizeMatch : mSizeMatchList) {
//                if (sizeMatch.mConstraintSet == null) {
//                    continue;
//                }
//                if (sizeMatch.mConstraintID != -1) {
//                    sizeMatch.mConstraintSet = mConstraintSetMap.get(sizeMatch.mConstraintID);
//                }
//            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing resource: " + resourceId, e);
        } catch (IOException e) {
            Log.e(TAG, "Error parsing resource: " + resourceId, e);
        }
    }

    private void parseConstraintSet(Context context, XmlPullParser parser) {
        ConstraintSet set = new ConstraintSet();
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            String s = parser.getAttributeValue(i);
            if (name == null || s == null) continue;
            if ("id".equals(name)) {
                int id = -1;
                if (s.contains("/")) {
                    String tmp = s.substring(s.indexOf('/') + 1);
                    id = context.getResources().getIdentifier(tmp, "id", context.getPackageName());

                }
                if (id == -1) {
                    if (s.length() > 1) {
                        id = Integer.parseInt(s.substring(1));
                    } else {
                        Log.e(TAG, "error in parsing id");
                    }
                }
                set.load(context, parser);
                if (DEBUG) {
                    Log.v(TAG, " id name " + context.getResources().getResourceName(id));
                }
                mConstraintSetMap.put(id, set);
                break;
            }
        }
    }

}
