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

package androidx.constraintlayout.motion.widget;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;
import android.util.Xml;

import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * The parses the KeyFrame structure in a MotionScene xml
 *
 */

public class KeyFrames {
    public static final int UNSET = ConstraintLayout.LayoutParams.UNSET;
    private static final String CUSTOM_METHOD = "CustomMethod";
    private static final String CUSTOM_ATTRIBUTE = "CustomAttribute";
    private HashMap<Integer, ArrayList<Key>> mFramesMap = new HashMap<Integer, ArrayList<Key>>();
    static HashMap<String, Constructor<? extends Key>> sKeyMakers = new HashMap<>();
    private static final String TAG = "KeyFrames";

    static {
        try {
            sKeyMakers.put(KeyAttributes.NAME, KeyAttributes.class.getConstructor());
            sKeyMakers.put(KeyPosition.NAME, KeyPosition.class.getConstructor());
            sKeyMakers.put(KeyCycle.NAME, KeyCycle.class.getConstructor());
            sKeyMakers.put(KeyTimeCycle.NAME, KeyTimeCycle.class.getConstructor());
            sKeyMakers.put(KeyTrigger.NAME, KeyTrigger.class.getConstructor());

        } catch (NoSuchMethodException e) {
            Log.e(TAG, "unable to load", e);
        }
    }

    /**
     * Add a key to this set of keyframes
     * @param key
     */
    public void addKey(Key key) {
        if (!mFramesMap.containsKey(key.mTargetId)) {
            mFramesMap.put(key.mTargetId, new ArrayList<>());
        }
        ArrayList<Key> frames = mFramesMap.get(key.mTargetId);
        if (frames != null) {
            frames.add(key);
        }
    }

    public KeyFrames() {

    }

    public KeyFrames(Context context, XmlPullParser parser) {
        String tagName = null;
        try {
            Key key = null;
            for (int eventType = parser.getEventType();
                    eventType != XmlResourceParser.END_DOCUMENT;
                    eventType = parser.next()) {
                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                        break;
                    case XmlResourceParser.START_TAG:
                        tagName = parser.getName();

                        if (sKeyMakers.containsKey(tagName)) {
                            try {
                                Constructor<? extends Key> keyMaker = sKeyMakers.get(tagName);
                                if (keyMaker != null) {
                                    key = keyMaker.newInstance();
                                    key.load(context, Xml.asAttributeSet(parser));
                                    addKey(key);
                                } else {
                                    throw new NullPointerException(
                                            "Keymaker for " + tagName + " not found");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "unable to create ", e);
                            }
                        } else if (tagName.equalsIgnoreCase(CUSTOM_ATTRIBUTE)) {
                            if (key != null && key.mCustomConstraints != null) {
                                ConstraintAttribute.parse(context, parser, key.mCustomConstraints);
                            }
                        } else if (tagName.equalsIgnoreCase(CUSTOM_METHOD)) {
                            if (key != null && key.mCustomConstraints != null) {
                                ConstraintAttribute.parse(context, parser, key.mCustomConstraints);
                            }
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        if ("KeyFrameSet".equals(parser.getName())) {
                            return;
                        }
                        break;
                    case XmlResourceParser.TEXT:
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        } catch (IOException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        }
    }

    /**
     * Do not filter the set by matches
     * @param motionController
     */
    public void addAllFrames(MotionController motionController) {
        ArrayList<Key> list = mFramesMap.get(UNSET);
        if (list != null) {
            motionController.addKeys(list);
        }
    }

    /**
     * add the key frames to the motion controller
     * @param motionController
     */
    public void addFrames(MotionController motionController) {
        ArrayList<Key> list = mFramesMap.get(motionController.mId);
        if (list != null) {
            motionController.addKeys(list);
        }
        list = mFramesMap.get(UNSET);

        if (list != null) {
            for (Key key : list) {
                String tag =
                        ((ConstraintLayout.LayoutParams)
                                motionController.mView.getLayoutParams()).constraintTag;
                if (key.matches(tag)) {
                    motionController.addKey(key);
                }
            }

        }

    }

    static String name(int viewId, Context context) {
        return context.getResources().getResourceEntryName(viewId);
    }

    public Set<Integer> getKeys() {
        return mFramesMap.keySet();
    }

    /**
     * Get the list of keyframes given and ID
     * @param id
     * @return
     */
    public ArrayList<Key> getKeyFramesForView(int id) {
        return mFramesMap.get(id);
    }
}
