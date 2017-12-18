/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.app.slice;

import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class SliceXml {

    private static final String NAMESPACE = null;

    private static final String TAG_SLICE = "slice";
    private static final String TAG_ITEM = "item";

    private static final String ATTR_URI = "uri";
    private static final String ATTR_FORMAT = "format";
    private static final String ATTR_SUBTYPE = "subtype";
    private static final String ATTR_HINTS = "hints";

    public static Slice parseSlice(InputStream input, String encoding) throws IOException {
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(input, encoding);

            int outerDepth = parser.getDepth();
            int type;
            Slice s = null;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type != START_TAG) {
                    continue;
                }
                s = parseSlice(parser);
            }
            return s;
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to init XML Serialization", e);
        }
    }

    @SuppressLint("WrongConstant")
    private static Slice parseSlice(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        if (!TAG_SLICE.equals(parser.getName())) {
            throw new IOException("Unexpected tag " + parser.getName());
        }
        int outerDepth = parser.getDepth();
        int type;
        String uri = parser.getAttributeValue(NAMESPACE, ATTR_URI);
        Slice.Builder b = new Slice.Builder(Uri.parse(uri));
        String[] hints = hints(parser.getAttributeValue(NAMESPACE, ATTR_HINTS));
        b.addHints(hints);

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == START_TAG && TAG_ITEM.equals(parser.getName())) {
                parseItem(b, parser);
            }
        }
        return b.build();
    }

    @SuppressLint("WrongConstant")
    private static void parseItem(Slice.Builder b, XmlPullParser parser)
            throws IOException, XmlPullParserException {
        int type;
        int outerDepth = parser.getDepth();
        String format = parser.getAttributeValue(NAMESPACE, ATTR_FORMAT);
        String subtype = parser.getAttributeValue(NAMESPACE, ATTR_SUBTYPE);
        String hintStr = parser.getAttributeValue(NAMESPACE, ATTR_HINTS);
        String[] hints = hints(hintStr);
        String v;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == TEXT) {
                switch (format) {
                    case android.app.slice.SliceItem.FORMAT_REMOTE_INPUT:
                        // Nothing for now.
                        break;
                    case android.app.slice.SliceItem.FORMAT_IMAGE:
                        v = parser.getText();
                        if (!TextUtils.isEmpty(v)) {
                            if (android.os.Build.VERSION.SDK_INT
                                    >= android.os.Build.VERSION_CODES.M) {
                                String[] split = v.split(",");
                                int w = Integer.parseInt(split[0]);
                                int h = Integer.parseInt(split[1]);
                                Bitmap image = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
                                b.addIcon(Icon.createWithBitmap(image), subtype, hints);
                            }
                        }
                        break;
                    case android.app.slice.SliceItem.FORMAT_INT:
                        v = parser.getText();
                        b.addInt(Integer.parseInt(v), subtype, hints);
                        break;
                    case android.app.slice.SliceItem.FORMAT_TEXT:
                        v = parser.getText();
                        b.addText(Html.fromHtml(v), subtype, hints);
                        break;
                    case android.app.slice.SliceItem.FORMAT_TIMESTAMP:
                        v = parser.getText();
                        b.addTimestamp(Long.parseLong(v), subtype, hints);
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized format " + format);
                }
            } else if (type == START_TAG && TAG_SLICE.equals(parser.getName())) {
                b.addSubSlice(parseSlice(parser), subtype);
            }
        }
    }

    private static String[] hints(String hintStr) {
        return TextUtils.isEmpty(hintStr) ? new String[0] : hintStr.split(",");
    }

    public static void serializeSlice(Slice s, Context context, OutputStream output,
            String encoding, SliceUtils.SerializeOptions options) throws IOException {
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(output, encoding);
            serializer.startDocument(encoding, null);

            serialize(s, context, options, serializer);

            serializer.endDocument();
            serializer.flush();
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to init XML Serialization", e);
        }
    }

    private static void serialize(Slice s, Context context, SliceUtils.SerializeOptions options,
            XmlSerializer serializer) throws IOException {
        serializer.startTag(NAMESPACE, TAG_SLICE);
        serializer.attribute(NAMESPACE, ATTR_URI, s.getUri().toString());
        if (!s.getHints().isEmpty()) {
            serializer.attribute(NAMESPACE, ATTR_HINTS, hintStr(s.getHints()));
        }
        for (SliceItem item : s.getItems()) {
            serialize(item, context, options, serializer);
        }

        serializer.endTag(NAMESPACE, TAG_SLICE);
    }

    private static void serialize(SliceItem item, Context context,
            SliceUtils.SerializeOptions options, XmlSerializer serializer) throws IOException {
        String format = item.getFormat();
        options.checkThrow(format);

        serializer.startTag(NAMESPACE, TAG_ITEM);
        serializer.attribute(NAMESPACE, ATTR_FORMAT, format);
        if (item.getSubType() != null) {
            serializer.attribute(NAMESPACE, ATTR_SUBTYPE, item.getSubType());
        }
        if (!item.getHints().isEmpty()) {
            serializer.attribute(NAMESPACE, ATTR_HINTS, hintStr(item.getHints()));
        }

        switch (format) {
            case android.app.slice.SliceItem.FORMAT_ACTION:
                if (options.getActionMode() == SliceUtils.SerializeOptions.MODE_DISABLE) {
                    serialize(item.getSlice(), context, options, serializer);
                }
                break;
            case android.app.slice.SliceItem.FORMAT_REMOTE_INPUT:
                // Nothing for now.
                break;
            case android.app.slice.SliceItem.FORMAT_IMAGE:
                if (options.getImageMode() == SliceUtils.SerializeOptions.MODE_DISABLE) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Drawable d = item.getIcon().loadDrawable(context);
                        serializer.text(String.format("%d,%d",
                                d.getIntrinsicWidth(), d.getIntrinsicHeight()));
                    }
                }
                break;
            case android.app.slice.SliceItem.FORMAT_INT:
                serializer.text(String.valueOf(item.getInt()));
                break;
            case android.app.slice.SliceItem.FORMAT_SLICE:
                serialize(item.getSlice(), context, options, serializer);
                break;
            case android.app.slice.SliceItem.FORMAT_TEXT:
                if (item.getText() instanceof Spanned) {
                    serializer.text(Html.toHtml((Spanned) item.getText()));
                } else {
                    serializer.text(String.valueOf(item.getText()));
                }
                break;
            case android.app.slice.SliceItem.FORMAT_TIMESTAMP:
                serializer.text(String.valueOf(item.getTimestamp()));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized format " + format);
        }
        serializer.endTag(NAMESPACE, TAG_ITEM);
    }

    private static String hintStr(List<String> hints) {
        return TextUtils.join(",", hints);
    }
}
