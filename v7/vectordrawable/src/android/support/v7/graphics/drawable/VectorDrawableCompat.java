/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v7.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Support lib for {@link android.graphics.drawable.VectorDrawable}
 * This is a duplication of VectorDrawable.java from frameworks/base, with major
 * changes in XML parsing parts.
 */
public class VectorDrawableCompat extends Drawable {
    private static final String LOG_TAG = "VectorDrawable";

    private static final String SHAPE_CLIP_PATH = "clip-path";
    private static final String SHAPE_GROUP = "group";
    private static final String SHAPE_PATH = "path";
    private static final String SHAPE_VECTOR = "vector";

    private static final boolean DBG_VECTOR_DRAWABLE = false;

    private Path mPath;

    @Override
    public void draw(Canvas canvas) {
        // Now just draw the last path.
        // TODO: Be able to draw the vector drawable's group tree into the canvas.
        canvas.drawRGB(255, 0, 0);

        Paint testPaint = new Paint();
        testPaint.setColor(0xff101010);
        canvas.drawPath(mPath, testPaint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    public static VectorDrawableCompat createFromResource(Resources res, int id) {
        XmlPullParserFactory xppf = null;
        XmlPullParser parser = null;
        try {
            xppf = XmlPullParserFactory.newInstance();
            parser = xppf.newPullParser();
            InputStream is = res.openRawResource(id);
            parser.setInput(is, null);
            // TODO: Use this getXml when the aapt is able to help us to keep the
            // attributes for v-21 in the compiled version.
            // XmlPullParser parser = res.getXml(id);

            final AttributeSet attrs = Xml.asAttributeSet(parser);
            final VectorDrawableCompat drawable = new VectorDrawableCompat();

            drawable.inflateInternal(res, parser, attrs);

            return drawable;
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "XmlPullParser exception for res id : " + id);
        }
        return null;
    }

    private Map<String, String> getAttributes(XmlPullParser parser) {
        Map<String, String> attrs = null;
        int attrCount = parser.getAttributeCount();
        if (attrCount != -1) {
            if (DBG_VECTOR_DRAWABLE) {
                Log.v(LOG_TAG, "Attributes for [" + parser.getName() + "] " + attrCount);
            }
            attrs = new HashMap<String, String>(attrCount);
            for (int i = 0; i < attrCount; i++) {
                if (DBG_VECTOR_DRAWABLE) {
                    Log.v(LOG_TAG, "\t[" + parser.getAttributeName(i) + "]=" +
                            "[" + parser.getAttributeValue(i) + "]");
                }
                attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
            }
        }
        return attrs;
    }

    private void inflateInternal(Resources res, XmlPullParser parser, AttributeSet attrs) {
        // TODO: Add more details in the parsing to reconstruct the
        // VectorDrawable data structure.
        int eventType;
        try {
            eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    if (DBG_VECTOR_DRAWABLE) {
                        Log.v(LOG_TAG, "Start document");
                    }
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (DBG_VECTOR_DRAWABLE) {
                        Log.v(LOG_TAG,"Parsing Attributes for ["+parser.getName()+"]");
                    }
                    Map<String,String> attributes = getAttributes(parser);
                    if (attributes != null) {
                        final String tagName = parser.getName();
                        if (SHAPE_PATH.equals(tagName)) {
                            String pathString = attributes.get("android:pathData");
                            if (DBG_VECTOR_DRAWABLE) {
                                Log.v(LOG_TAG, "pathData is " + pathString);
                            }
                            mPath = PathParser.createPathFromPathData(pathString);
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (DBG_VECTOR_DRAWABLE) {
                        Log.v(LOG_TAG, "End tag " + parser.getName());
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (DBG_VECTOR_DRAWABLE) {
                        Log.v(LOG_TAG, "Text " + parser.getText());
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (DBG_VECTOR_DRAWABLE) {
            Log.v(LOG_TAG, "End document");
        }

    }
}