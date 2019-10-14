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

import static androidx.slice.Slice.appendHints;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Annotation;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.StringDef;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Calendar;
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
@VersionedParcelize(allowSerialization = true, ignoreParcelables = true, isCustom = true)
@RequiresApi(19)
public final class SliceItem extends CustomVersionedParcelable {

    private static final String HINTS = "hints";
    private static final String FORMAT = "format";
    private static final String SUBTYPE = "subtype";
    private static final String OBJ = "obj";
    private static final String OBJ_2 = "obj_2";
    private static final String SLICE_CONTENT = "androidx.slice.content";
    private static final String SLICE_CONTENT_SENSITIVE = "sensitive";

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @StringDef({FORMAT_SLICE, FORMAT_TEXT, FORMAT_IMAGE, FORMAT_ACTION, FORMAT_INT,
            FORMAT_LONG, FORMAT_REMOTE_INPUT, FORMAT_LONG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceType {
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @ParcelField(value = 1, defaultValue = "androidx.slice.Slice.NO_HINTS")
    protected @Slice.SliceHint String[] mHints = Slice.NO_HINTS;
    @ParcelField(value = 2, defaultValue = FORMAT_TEXT)
    String mFormat = FORMAT_TEXT;
    @ParcelField(value = 3, defaultValue = "null")
    String mSubType = null;
    @NonParcelField
    Object mObj;
    @NonParcelField
    CharSequence mSanitizedText;

    @ParcelField(4)
    SliceItemHolder mHolder;

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
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
    @RestrictTo(Scope.LIBRARY_GROUP)
    public SliceItem(Object obj, @SliceType String format, String subType,
            @Slice.SliceHint List<String> hints) {
        this (obj, format, subType, hints.toArray(new String[hints.size()]));
    }

    /**
     * Used by VersionedParcelable.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public SliceItem() {
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public SliceItem(PendingIntent intent, Slice slice, String format, String subType,
            @Slice.SliceHint String[] hints) {
        this(new Pair<Object, Slice>(intent, slice), format, subType, hints);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public SliceItem(ActionHandler action, Slice slice, String format, String subType,
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
    public @NonNull @Slice.SliceHint String[] getHintArray() {
        return mHints;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void addHint(@Slice.SliceHint String hint) {
        mHints = ArrayUtils.appendElement(String.class, mHints, hint);
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
     * @hide
     * @return The text held by this {@link android.app.slice.SliceItem#FORMAT_TEXT} SliceItem with
     * ony spans that are unsupported by the androidx Slice renderer removed.
     */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public CharSequence getSanitizedText() {
        if (mSanitizedText == null) mSanitizedText = sanitizeText(getText());
        return mSanitizedText;
    }

    /**
     * Get the same content as {@link #getText()} except with content that should be excluded from
     * persistent logs because it was tagged with {@link #createSensitiveSpan()}.
     *
     * @return The text held by this {@link android.app.slice.SliceItem#FORMAT_TEXT} SliceItem
     */
    @Nullable
    public CharSequence getRedactedText() {
        return redactSensitiveText(getText());
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
    @SuppressWarnings("unchecked")
    public PendingIntent getAction() {
        Object action = ((Pair<Object, Slice>) mObj).first;
        if (action instanceof PendingIntent) {
            return (PendingIntent) action;
        }
        return null;
    }

    /**
     * Trigger the action on this SliceItem.
     * @param context The Context to use when sending the PendingIntent.
     * @param i The intent to use when sending the PendingIntent.
     */
    public void fireAction(@Nullable Context context, @Nullable Intent i)
            throws PendingIntent.CanceledException {
        fireActionInternal(context, i);
    }

    /**
     * @hide
     */
    @SuppressWarnings("unchecked")
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public boolean fireActionInternal(@Nullable Context context, @Nullable Intent i)
            throws PendingIntent.CanceledException {
        Object action = ((Pair<Object, Slice>) mObj).first;
        if (action instanceof PendingIntent) {
            ((PendingIntent) action).send(context, 0, i, null, null);
            return false;
        } else {
            ((ActionHandler) action).onAction(this, context, i);
            return true;
        }
    }

    /**
     * @return The remote input held by this {@link android.app.slice.SliceItem#FORMAT_REMOTE_INPUT}
     * SliceItem
     * @hide
     */
    @RequiresApi(20)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
    @SuppressWarnings("unchecked")
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
    @RestrictTo(Scope.LIBRARY_GROUP)
    public boolean hasAnyHints(@Slice.SliceHint String... hints) {
        if (hints == null) return false;
        for (String hint : hints) {
            if (ArrayUtils.contains(mHints, hint)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent);
        sb.append(getFormat());
        if (getSubType() != null) {
            sb.append('<');
            sb.append(getSubType());
            sb.append('>');
        }
        sb.append(' ');
        if (mHints.length > 0) {
            appendHints(sb, mHints);
            sb.append(' ');
        }
        final String nextIndent = indent + "  ";
        switch (getFormat()) {
            case FORMAT_SLICE:
                sb.append("{\n");
                sb.append(getSlice().toString(nextIndent));
                sb.append('\n').append(indent).append('}');
                break;
            case FORMAT_ACTION:
                // Not using getAction because the action can actually be other types.
                Object action = ((Pair<Object, Slice>) mObj).first;
                sb.append('[').append(action).append("] ");
                sb.append("{\n");
                sb.append(getSlice().toString(nextIndent));
                sb.append('\n').append(indent).append('}');
                break;
            case FORMAT_TEXT:
                sb.append('"').append(getText()).append('"');
                break;
            case FORMAT_IMAGE:
                sb.append(getIcon());
                break;
            case FORMAT_INT:
                if (android.app.slice.Slice.SUBTYPE_COLOR.equals(getSubType())) {
                    int color = getInt();
                    sb.append(String.format("a=0x%02x r=0x%02x g=0x%02x b=0x%02x",
                            Color.alpha(color), Color.red(color), Color.green(color),
                            Color.blue(color)));
                } else if (android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION.equals(getSubType())) {
                    sb.append(layoutDirectionToString(getInt()));
                } else {
                    sb.append(getInt());
                }
                break;
            case FORMAT_LONG:
                if (android.app.slice.Slice.SUBTYPE_MILLIS.equals(getSubType())) {
                    if (getLong() == -1L) {
                        sb.append("INFINITY");
                    } else {
                        sb.append(DateUtils.getRelativeTimeSpanString(getLong(),
                                Calendar.getInstance().getTimeInMillis(),
                                DateUtils.SECOND_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE));
                    }
                } else {
                    sb.append(getLong()).append('L');
                }
                break;
            default:
                sb.append(SliceItem.typeToString(getFormat()));
                break;
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public void onPreParceling(boolean isStream) {
        mHolder = new SliceItemHolder(mFormat, mObj, isStream);
    }

    @Override
    public void onPostParceling() {
        if (mHolder != null) {
            mObj = mHolder.getObj(mFormat);
            mHolder.release();
        } else {
            mObj = null;
        }
        mHolder = null;
    }

    /**
     * Creates a span object that identifies content that should be redacted when acquired using
     * {@link #getRedactedText()}.
     */
    @NonNull
    public static ParcelableSpan createSensitiveSpan() {
        return new Annotation(SLICE_CONTENT, SLICE_CONTENT_SENSITIVE);
    }

    private static String layoutDirectionToString(int layoutDirection) {
        switch (layoutDirection) {
            case android.util.LayoutDirection.LTR:
                return "LTR";
            case android.util.LayoutDirection.RTL:
                return "RTL";
            case android.util.LayoutDirection.INHERIT:
                return "INHERIT";
            case android.util.LayoutDirection.LOCALE:
                return "LOCALE";
            default:
                return Integer.toString(layoutDirection);
        }
    }

    private static CharSequence redactSensitiveText(CharSequence text) {
        if (text instanceof Spannable) {
            return redactSpannableText((Spannable) text);
        } else if (text instanceof Spanned) {
            if (!isRedactionNeeded((Spanned) text)) return text;
            Spannable fixedText = new SpannableString(text);
            return redactSpannableText(fixedText);
        } else {
            return text;
        }
    }

    private static boolean isRedactionNeeded(Spanned text) {
        for (Annotation span : text.getSpans(0, text.length(), Annotation.class)) {
            if (SLICE_CONTENT.equals(span.getKey())
                    && SLICE_CONTENT_SENSITIVE.equals(span.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static CharSequence redactSpannableText(Spannable text) {
        Spanned out = text;
        for (Annotation span : text.getSpans(0, text.length(), Annotation.class)) {
            if (!SLICE_CONTENT.equals(span.getKey())
                    || !SLICE_CONTENT_SENSITIVE.equals(span.getValue())) {
                continue;
            }
            int spanStart = text.getSpanStart(span);
            int spanEnd = text.getSpanEnd(span);
            out = new SpannableStringBuilder()
                    .append(out.subSequence(0, spanStart))
                    .append(createRedacted(spanEnd - spanStart))
                    .append(out.subSequence(spanEnd, text.length()));
        }
        return out;
    }

    private static String createRedacted(final int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) {
            s.append('*');
        }
        return s.toString();
    }

    private static CharSequence sanitizeText(CharSequence text) {
        if (text instanceof Spannable) {
            fixSpannableText((Spannable) text);
            return text;
        } else if (text instanceof Spanned) {
            if (checkSpannedText((Spanned) text)) return text;
            Spannable fixedText = new SpannableString(text);
            fixSpannableText(fixedText);
            return fixedText;
        } else {
            return text;
        }
    }

    private static boolean checkSpannedText(Spanned text) {
        for (Object span : text.getSpans(0, text.length(), Object.class)) {
            if (!checkSpan(span)) return false;
        }
        return true;
    }

    private static void fixSpannableText(Spannable text) {
        for (Object span : text.getSpans(0, text.length(), Object.class)) {
            Object fixedSpan = fixSpan(span);
            if (fixedSpan == span) continue;

            if (fixedSpan != null) {
                int spanStart = text.getSpanStart(span);
                int spanEnd = text.getSpanEnd(span);
                int spanFlags = text.getSpanFlags(span);
                text.setSpan(fixedSpan, spanStart, spanEnd, spanFlags);
            }

            text.removeSpan(span);
        }
    }

    // TODO: Allow only highlight color in ForegroundColorSpan.
    // TODO: Cap smallest/largest sizeChange for RelativeSizeSpans, minding nested ones.

    private static boolean checkSpan(Object span) {
        return span instanceof AlignmentSpan
                || span instanceof ForegroundColorSpan
                || span instanceof RelativeSizeSpan
                || span instanceof StyleSpan;
    }

    private static Object fixSpan(Object span) {
        return checkSpan(span) ? span : null;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public interface ActionHandler {
        /**
         * Called when a pending intent would be sent on a real slice.
         */
        void onAction(SliceItem item, Context context, Intent intent);
    }
}
