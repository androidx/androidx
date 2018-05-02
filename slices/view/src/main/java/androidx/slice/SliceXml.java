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

package androidx.slice;

import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
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
    private static final String TAG_ACTION = "action";
    private static final String TAG_ITEM = "item";

    private static final String ATTR_URI = "uri";
    private static final String ATTR_FORMAT = "format";
    private static final String ATTR_SUBTYPE = "subtype";
    private static final String ATTR_HINTS = "hints";
    private static final String ATTR_ICON_TYPE = "iconType";
    private static final String ATTR_ICON_PACKAGE = "pkg";
    private static final String ATTR_ICON_RES_TYPE = "resType";

    private static final String ICON_TYPE_RES = "res";
    private static final String ICON_TYPE_URI = "uri";
    private static final String ICON_TYPE_DEFAULT = "def";

    public static Slice parseSlice(Context context, InputStream input,
            String encoding, SliceUtils.SliceActionListener listener)
            throws IOException, SliceUtils.SliceParseException {
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
                s = parseSlice(context, parser, listener);
            }
            return s;
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to init XML Serialization", e);
        }
    }

    @SuppressLint("WrongConstant")
    private static Slice parseSlice(Context context, XmlPullParser parser,
            SliceUtils.SliceActionListener listener)
            throws IOException, XmlPullParserException, SliceUtils.SliceParseException {
        if (!TAG_SLICE.equals(parser.getName()) && !TAG_ACTION.equals(parser.getName())) {
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
                parseItem(context, b, parser, listener);
            }
        }
        return b.build();
    }

    @SuppressLint("DefaultCharset")
    private static void parseItem(Context context, Slice.Builder b,
            XmlPullParser parser, final SliceUtils.SliceActionListener listener)
            throws IOException, XmlPullParserException, SliceUtils.SliceParseException {
        int type;
        int outerDepth = parser.getDepth();
        String format = parser.getAttributeValue(NAMESPACE, ATTR_FORMAT);
        String subtype = parser.getAttributeValue(NAMESPACE, ATTR_SUBTYPE);
        String hintStr = parser.getAttributeValue(NAMESPACE, ATTR_HINTS);
        String iconType = parser.getAttributeValue(NAMESPACE, ATTR_ICON_TYPE);
        String pkg = parser.getAttributeValue(NAMESPACE, ATTR_ICON_PACKAGE);
        String resType = parser.getAttributeValue(NAMESPACE, ATTR_ICON_RES_TYPE);
        @Slice.SliceHint String[] hints = hints(hintStr);
        String v;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == TEXT) {
                switch (format) {
                    case android.app.slice.SliceItem.FORMAT_REMOTE_INPUT:
                        // Nothing for now.
                        break;
                    case android.app.slice.SliceItem.FORMAT_IMAGE:
                        switch (iconType) {
                            case ICON_TYPE_RES:
                                String resName = parser.getText();
                                try {
                                    Resources r = context.getPackageManager()
                                                .getResourcesForApplication(pkg);
                                    int id = r.getIdentifier(resName, resType, pkg);
                                    if (id != 0) {
                                        b.addIcon(IconCompat.createWithResource(
                                                context.createPackageContext(pkg, 0), id), subtype,
                                                hints);
                                    } else {
                                        throw new SliceUtils.SliceParseException(
                                                "Cannot find resource " + pkg + ":" + resType
                                                        + "/" + resName);
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    throw new SliceUtils.SliceParseException(
                                            "Invalid icon package " + pkg, e);
                                }
                                break;
                            case ICON_TYPE_URI:
                                v = parser.getText();
                                b.addIcon(IconCompat.createWithContentUri(v), subtype, hints);
                                break;
                            default:
                                v = parser.getText();
                                byte[] data = Base64.decode(v, Base64.NO_WRAP);
                                Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                                b.addIcon(IconCompat.createWithBitmap(image), subtype, hints);
                                break;
                        }
                        break;
                    case android.app.slice.SliceItem.FORMAT_INT:
                        v = parser.getText();
                        b.addInt(Integer.parseInt(v), subtype, hints);
                        break;
                    case android.app.slice.SliceItem.FORMAT_TEXT:
                        v = parser.getText();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                            // 19-21 don't allow special characters in XML, so we base64 encode it.
                            v = new String(Base64.decode(v, Base64.NO_WRAP));
                        }
                        b.addText(Html.fromHtml(v), subtype, hints);
                        break;
                    case android.app.slice.SliceItem.FORMAT_LONG:
                        v = parser.getText();
                        b.addLong(Long.parseLong(v), subtype, hints);
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized format " + format);
                }
            } else if (type == START_TAG && TAG_SLICE.equals(parser.getName())) {
                b.addSubSlice(parseSlice(context, parser, listener), subtype);
            } else if (type == START_TAG && TAG_ACTION.equals(parser.getName())) {
                b.addAction(new Consumer<Uri>() {
                    @Override
                    public void accept(Uri uri) {
                        listener.onSliceAction(uri);
                    }
                }, parseSlice(context, parser, listener), subtype);
            }
        }
    }

    @Slice.SliceHint
    private static String[] hints(String hintStr) {
        return TextUtils.isEmpty(hintStr) ? new String[0] : hintStr.split(",");
    }

    public static void serializeSlice(Slice s, Context context, OutputStream output,
            String encoding, SliceUtils.SerializeOptions options) throws IOException {
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(output, encoding);
            serializer.startDocument(encoding, null);

            serialize(s, context, options, serializer, false, null);

            serializer.endDocument();
            serializer.flush();
        } catch (XmlPullParserException e) {
            throw new IOException("Unable to init XML Serialization", e);
        }
    }

    private static void serialize(Slice s, Context context, SliceUtils.SerializeOptions options,
            XmlSerializer serializer, boolean isAction, String subType) throws IOException {
        serializer.startTag(NAMESPACE, isAction ? TAG_ACTION : TAG_SLICE);
        serializer.attribute(NAMESPACE, ATTR_URI, s.getUri().toString());
        if (subType != null) {
            serializer.attribute(NAMESPACE, ATTR_SUBTYPE, subType);
        }
        if (!s.getHints().isEmpty()) {
            serializer.attribute(NAMESPACE, ATTR_HINTS, hintStr(s.getHints()));
        }
        for (SliceItem item : s.getItems()) {
            serialize(item, context, options, serializer);
        }

        serializer.endTag(NAMESPACE, isAction ? TAG_ACTION : TAG_SLICE);
    }

    @SuppressWarnings("DefaultCharset")
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
                if (options.getActionMode() == SliceUtils.SerializeOptions.MODE_CONVERT) {
                    serialize(item.getSlice(), context, options, serializer, true,
                            item.getSubType());
                } else if (options.getActionMode() == SliceUtils.SerializeOptions.MODE_THROW) {
                    throw new IllegalArgumentException("Slice contains an action " + item);
                }
                break;
            case android.app.slice.SliceItem.FORMAT_REMOTE_INPUT:
                // Nothing for now.
                break;
            case android.app.slice.SliceItem.FORMAT_IMAGE:
                if (options.getImageMode() == SliceUtils.SerializeOptions.MODE_CONVERT) {
                    IconCompat icon = item.getIcon();

                    switch (icon.getType()) {
                        case Icon.TYPE_RESOURCE:
                            serializeResIcon(serializer, icon, context);
                            break;
                        case Icon.TYPE_URI:
                            Uri uri = icon.getUri();
                            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                                serializeFileIcon(serializer, icon, context);
                            } else {
                                serializeIcon(serializer, icon, context, options);
                            }
                            break;
                        default:
                            serializeIcon(serializer, icon, context, options);
                            break;
                    }
                } else if (options.getImageMode() == SliceUtils.SerializeOptions.MODE_THROW) {
                    throw new IllegalArgumentException("Slice contains an image " + item);
                }
                break;
            case android.app.slice.SliceItem.FORMAT_INT:
                serializer.text(String.valueOf(item.getInt()));
                break;
            case android.app.slice.SliceItem.FORMAT_SLICE:
                serialize(item.getSlice(), context, options, serializer, false, item.getSubType());
                break;
            case android.app.slice.SliceItem.FORMAT_TEXT:
                if (item.getText() instanceof Spanned) {
                    String text = Html.toHtml((Spanned) item.getText());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        // 19-21 don't allow special characters in XML, so we base64 encode it.
                        text = Base64.encodeToString(text.getBytes(), Base64.NO_WRAP);
                    }
                    serializer.text(text);
                } else {
                    String text = String.valueOf(item.getText());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        // 19-21 don't allow special characters in XML, so we base64 encode it.
                        text = Base64.encodeToString(text.getBytes(), Base64.NO_WRAP);
                    }
                    serializer.text(text);
                }
                break;
            case android.app.slice.SliceItem.FORMAT_LONG:
                serializer.text(String.valueOf(item.getLong()));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized format " + format);
        }
        serializer.endTag(NAMESPACE, TAG_ITEM);
    }

    private static void serializeResIcon(XmlSerializer serializer, IconCompat icon, Context context)
            throws IOException {
        try {
            Resources res = context.getPackageManager().getResourcesForApplication(
                    icon.getResPackage());
            int id = icon.getResId();
            serializer.attribute(NAMESPACE, ATTR_ICON_TYPE, ICON_TYPE_RES);
            serializer.attribute(NAMESPACE, ATTR_ICON_PACKAGE, res.getResourcePackageName(id));
            serializer.attribute(NAMESPACE, ATTR_ICON_RES_TYPE, res.getResourceTypeName(id));
            serializer.text(res.getResourceEntryName(id));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Slice contains invalid icon", e);
        }
    }

    private static void serializeFileIcon(XmlSerializer serializer, IconCompat icon,
            Context context) throws IOException {
        serializer.attribute(NAMESPACE, ATTR_ICON_TYPE, ICON_TYPE_URI);
        serializer.text(icon.getUri().toString());
    }

    @SuppressWarnings("DefaultCharset")
    private static void serializeIcon(XmlSerializer serializer, IconCompat icon,
            Context context, SliceUtils.SerializeOptions options) throws IOException {
        Drawable d = icon.loadDrawable(context);
        int width = d.getIntrinsicWidth();
        int height = d.getIntrinsicHeight();
        if (width > options.getMaxWidth()) {
            height = (int) (options.getMaxWidth() * height / (double) width);
            width = options.getMaxWidth();
        }
        if (height > options.getMaxHeight()) {
            width = (int) (options.getMaxHeight() * width / (double) height);
            height = options.getMaxHeight();
        }
        Bitmap b = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        b.recycle();

        serializer.attribute(NAMESPACE, ATTR_ICON_TYPE, ICON_TYPE_DEFAULT);
        serializer.text(new String(Base64.encode(outputStream.toByteArray(), Base64.NO_WRAP)));
    }

    private static String hintStr(List<String> hints) {
        return TextUtils.join(",", hints);
    }

    private SliceXml() {
    }
}
