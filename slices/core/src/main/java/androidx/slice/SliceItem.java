/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.Slice.addHints;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.StringDef;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import java.util.Arrays;
import java.util.List;


/**
 * A SliceItem is a single unit in the tree structure of a {@link Slice}.
 * <p>
 * A SliceItem a piece of content and some hints about what that content
 * means or how it should be displayed. The types of content can be:
 * <li>{@link android.app.slice.SliceItem#FORMAT_SLICE}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_TEXT}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_IMAGE}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_ACTION}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_INT}</li>
 * <li>{@link android.app.slice.SliceItem#FORMAT_LONG}</li>
 * <p>
 * The hints that a {@link SliceItem} are a set of strings which annotate
 * the content. The hints that are guaranteed to be understood by the system
 * are defined on {@link Slice}.
 */
public class SliceItem {

    private static final String HINTS = "hints";
    private static final String FORMAT = "format";
    private static final String SUBTYPE = "subtype";
    private static final String OBJ = "obj";
    private static final String OBJ_2 = "obj_2";

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @StringDef({FORMAT_SLICE, FORMAT_TEXT, FORMAT_IMAGE, FORMAT_ACTION, FORMAT_INT,
            FORMAT_LONG, FORMAT_REMOTE_INPUT, FORMAT_LONG})
    public @interface SliceType {
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    protected @Slice.SliceHint String[] mHints;
    private final String mFormat;
    private final String mSubType;
    private final Object mObj;

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(Object obj, @SliceType String format, String subType,
            @Slice.SliceHint String[] hints) {
        mHints = hints;
        mFormat = format;
        mSubType = subType;
        mObj = obj;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(Object obj, @SliceType String format, String subType,
            @Slice.SliceHint List<String> hints) {
        this (obj, format, subType, hints.toArray(new String[hints.size()]));
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(PendingIntent intent, Slice slice, String format, String subType,
            @Slice.SliceHint String[] hints) {
        this(new Pair<Object, Slice>(intent, slice), format, subType, hints);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(Consumer<Uri> action, Slice slice, String format, String subType,
            @Slice.SliceHint String[] hints) {
        this(new Pair<Object, Slice>(action, slice), format, subType, hints);
    }

    /**
     * Gets all hints associated with this SliceItem.
     *
     * @return Array of hints.
     */
    public @NonNull @Slice.SliceHint List<String> getHints() {
        return Arrays.asList(mHints);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public void addHint(@Slice.SliceHint String hint) {
        mHints = ArrayUtils.appendElement(String.class, mHints, hint);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public void removeHint(String hint) {
        ArrayUtils.removeElement(String.class, mHints, hint);
    }

    /**
     * Get the format of this SliceItem.
     * <p>
     * The format will be one of the following types supported by the platform:
     * <li>{@link android.app.slice.SliceItem#FORMAT_SLICE}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_TEXT}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_IMAGE}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_ACTION}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_INT}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_LONG}</li>
     * <li>{@link android.app.slice.SliceItem#FORMAT_REMOTE_INPUT}</li>
     * @see #getSubType() ()
     */
    public @SliceType String getFormat() {
        return mFormat;
    }

    /**
     * Get the sub-type of this SliceItem.
     * <p>
     * Subtypes provide additional information about the type of this information beyond basic
     * interpretations inferred by {@link #getFormat()}. For example a slice may contain
     * many {@link android.app.slice.SliceItem#FORMAT_TEXT} items, but only some of them may be
     * {@link android.app.slice.Slice#SUBTYPE_MESSAGE}.
     * @see #getFormat()
     */
    public String getSubType() {
        return mSubType;
    }

    /**
     * @return The text held by this {@link android.app.slice.SliceItem#FORMAT_TEXT} SliceItem
     */
    public CharSequence getText() {
        return (CharSequence) mObj;
    }

    /**
     * @return The icon held by this {@link android.app.slice.SliceItem#FORMAT_IMAGE} SliceItem
     */
    public IconCompat getIcon() {
        return (IconCompat) mObj;
    }

    /**
     * @return The pending intent held by this {@link android.app.slice.SliceItem#FORMAT_ACTION}
     * SliceItem
     */
    public PendingIntent getAction() {
        return (PendingIntent) ((Pair<Object, Slice>) mObj).first;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void fireAction(Context context, Intent i) throws PendingIntent.CanceledException {
        Object action = ((Pair<Object, Slice>) mObj).first;
        if (action instanceof PendingIntent) {
            ((PendingIntent) action).send(context, 0, i, null, null);
        } else {
            ((Consumer<Uri>) action).accept(getSlice().getUri());
        }
    }

    /**
     * @return The remote input held by this {@link android.app.slice.SliceItem#FORMAT_REMOTE_INPUT}
     * SliceItem
     * @hide
     */
    @RequiresApi(20)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public RemoteInput getRemoteInput() {
        return (RemoteInput) mObj;
    }

    /**
     * @return The color held by this {@link android.app.slice.SliceItem#FORMAT_INT} SliceItem
     */
    public int getInt() {
        return (Integer) mObj;
    }

    /**
     * @return The slice held by this {@link android.app.slice.SliceItem#FORMAT_ACTION} or
     * {@link android.app.slice.SliceItem#FORMAT_SLICE} SliceItem
     */
    public Slice getSlice() {
        if (FORMAT_ACTION.equals(getFormat())) {
            return ((Pair<Object, Slice>) mObj).second;
        }
        return (Slice) mObj;
    }

    /**
     * @return The long held by this {@link android.app.slice.SliceItem#FORMAT_LONG}
     * SliceItem
     */
    public long getLong() {
        return (Long) mObj;
    }

    /**
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    public long getTimestamp() {
        return (Long) mObj;
    }

    /**
     * @param hint The hint to check for
     * @return true if this item contains the given hint
     */
    public boolean hasHint(@Slice.SliceHint String hint) {
        return ArrayUtils.contains(mHints, hint);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public SliceItem(Bundle in) {
        mHints = in.getStringArray(HINTS);
        mFormat = in.getString(FORMAT);
        mSubType = in.getString(SUBTYPE);
        mObj = readObj(mFormat, in);
    }

    /**
     * @hide
     * @return
     */
    @RestrictTo(Scope.LIBRARY)
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putStringArray(HINTS, mHints);
        b.putString(FORMAT, mFormat);
        b.putString(SUBTYPE, mSubType);
        writeObj(b, mObj, mFormat);
        return b;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public boolean hasHints(@Slice.SliceHint String[] hints) {
        if (hints == null) return true;
        for (String hint : hints) {
            if (!TextUtils.isEmpty(hint) && !ArrayUtils.contains(mHints, hint)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public boolean hasAnyHints(@Slice.SliceHint String... hints) {
        if (hints == null) return false;
        for (String hint : hints) {
            if (ArrayUtils.contains(mHints, hint)) {
                return true;
            }
        }
        return false;
    }

    private void writeObj(Bundle dest, Object obj, String type) {
        switch (type) {
            case FORMAT_IMAGE:
                dest.putBundle(OBJ, ((IconCompat) obj).toBundle());
                break;
            case FORMAT_REMOTE_INPUT:
                dest.putParcelable(OBJ, (Parcelable) obj);
                break;
            case FORMAT_SLICE:
                dest.putParcelable(OBJ, ((Slice) obj).toBundle());
                break;
            case FORMAT_ACTION:
                dest.putParcelable(OBJ, (PendingIntent) ((Pair<Object, Slice>) obj).first);
                dest.putBundle(OBJ_2, ((Pair<Object, Slice>) obj).second.toBundle());
                break;
            case FORMAT_TEXT:
                dest.putCharSequence(OBJ, (CharSequence) obj);
                break;
            case FORMAT_INT:
                dest.putInt(OBJ, (Integer) mObj);
                break;
            case FORMAT_LONG:
                dest.putLong(OBJ, (Long) mObj);
                break;
        }
    }

    private static Object readObj(String type, Bundle in) {
        switch (type) {
            case FORMAT_IMAGE:
                return IconCompat.createFromBundle(in.getBundle(OBJ));
            case FORMAT_REMOTE_INPUT:
                return in.getParcelable(OBJ);
            case FORMAT_SLICE:
                return new Slice(in.getBundle(OBJ));
            case FORMAT_TEXT:
                return in.getCharSequence(OBJ);
            case FORMAT_ACTION:
                return new Pair<>(
                        in.getParcelable(OBJ),
                        new Slice(in.getBundle(OBJ_2)));
            case FORMAT_INT:
                return in.getInt(OBJ);
            case FORMAT_LONG:
                return in.getLong(OBJ);
        }
        throw new RuntimeException("Unsupported type " + type);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static String typeToString(String format) {
        switch (format) {
            case FORMAT_SLICE:
                return "Slice";
            case FORMAT_TEXT:
                return "Text";
            case FORMAT_IMAGE:
                return "Image";
            case FORMAT_ACTION:
                return "Action";
            case FORMAT_INT:
                return "Int";
            case FORMAT_LONG:
                return "Long";
            case FORMAT_REMOTE_INPUT:
                return "RemoteInput";
        }
        return "Unrecognized format: " + format;
    }

    /**
     * @return A string representation of this slice item.
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * @return A string representation of this slice item.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        switch (getFormat()) {
            case FORMAT_SLICE:
                sb.append(getSlice().toString(indent));
                break;
            case FORMAT_ACTION:
                sb.append(indent).append(getAction()).append(",\n");
                sb.append(getSlice().toString(indent));
                break;
            case FORMAT_TEXT:
                sb.append(indent).append('"').append(getText()).append('"');
                break;
            case FORMAT_IMAGE:
                sb.append(indent).append(getIcon());
                break;
            case FORMAT_INT:
                sb.append(indent).append(getInt());
                break;
            case FORMAT_LONG:
                sb.append(indent).append(getLong());
                break;
            default:
                sb.append(indent).append(SliceItem.typeToString(getFormat()));
                break;
        }
        if (!FORMAT_SLICE.equals(getFormat())) {
            sb.append(' ');
            addHints(sb, mHints);
        }
        sb.append(",\n");
        return sb.toString();
    }
}
