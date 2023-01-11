/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.constraintlayout.helper.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.constraintlayout.widget.ConstraintSet.Layout.UNSET_GONE_MARGIN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.motion.widget.Debug;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a class is a debugging/logging utility to write out the constraints in JSON
 * This is used for debugging purposes
 * <ul>
 *     <li>logJsonTo - defines the output log console or "fileName"</li>
 *     <li>logJsonMode - mode one of:
 *     <b>periodic</b>, <b>delayed</b>, <b>layout</b> or <b>api</b></li>
 *     <li>logJsonDelay - the duration of the delay or the delay between repeated logs</li>
 * </ul>
 * logJsonTo supports:
 * <ul>
 *     <li>log - logs using log.v("JSON5", ...)</li>
 *     <li>console - logs using System.out.println(...)</li>
 *     <li>[fileName] - will write to /storage/emulated/0/Download/[fileName].json5</li>
 * </ul>
 * logJsonMode modes are:
 * <ul>
 *     <li>periodic - after window is attached will log every delay ms</li>
 *     <li>delayed - log once after delay ms</li>
 *     <li>layout - log every time there is a layout call</li>
 *     <li>api - do not automatically log developer will call writeLog</li>
 * </ul>
 *
 * The defaults are:
 * <ul>
 *     <li>logJsonTo="log"</li>
 *     <li>logJsonMode="delayed"</li>
 *     <li>logJsonDelay="1000"</li>
 * </ul>
 *  Usage:
 *  <p></p>
 *  <pre>
 *  {@code
 *      <androidx.constraintlayout.helper.widget.LogJson
 *         android:layout_width="0dp"
 *         android:layout_height="0dp"
 *         android:visibility="gone"
 *         app:logJsonTo="log"
 *         app:logJsonMode="delayed"
 *         app:logJsonDelay="1000"
 *         />
 *  }
 * </pre>
 * </p>
 */
public class LogJson extends ConstraintHelper {
    private static final String TAG = "JSON5";
    private int mDelay = 1000;
    private int mMode = LOG_DELAYED;
    private String mLogToFile = null;
    private boolean mLogConsole = true;

    public static final int LOG_PERIODIC = 1;
    public static final int LOG_DELAYED = 2;
    public static final int LOG_LAYOUT = 3;
    public static final int LOG_API = 4;
    private boolean mPeriodic = false;

    public LogJson(@androidx.annotation.NonNull Context context) {
        super(context);
    }

    public LogJson(@androidx.annotation.NonNull Context context,
                   @androidx.annotation.Nullable AttributeSet attrs) {
        super(context, attrs);
        initLogJson(attrs);

    }

    public LogJson(@androidx.annotation.NonNull Context context,
                   @androidx.annotation.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLogJson(attrs);
    }

    private void initLogJson(AttributeSet attrs) {

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.LogJson);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.LogJson_logJsonDelay) {
                    mDelay = a.getInt(attr, mDelay);
                } else if (attr == R.styleable.LogJson_logJsonMode) {
                    mMode = a.getInt(attr, mMode);
                } else if (attr == R.styleable.LogJson_logJsonTo) {
                    TypedValue v = a.peekValue(attr);
                    if (v.type == TypedValue.TYPE_STRING) {
                        mLogToFile = a.getString(attr);
                    } else {
                        int value = a.getInt(attr, 0);
                        mLogConsole = value == 2;
                    }
                }
            }
            a.recycle();
        }
        setVisibility(GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        switch (mMode) {
            case LOG_PERIODIC:
                mPeriodic = true;
                this.postDelayed(this::periodic, mDelay);
                break;
            case LOG_DELAYED:
                this.postDelayed(this::writeLog, mDelay);
                break;
            case LOG_LAYOUT:
                ConstraintLayout cl = (ConstraintLayout) getParent();
                cl.addOnLayoutChangeListener((v, a, b, c, d, e, f, g, h) -> logOnLayout());
        }
    }

    private void logOnLayout() {
        if (mMode == LOG_LAYOUT) {
            writeLog();
        }
    }

    /**
     * Set the duration of periodic logging of constraints
     *
     * @param duration the time in ms between writing files
     */
    public void setDelay(int duration) {
        mDelay = duration;
    }

    /**
     * Start periodic sampling
     */
    public void periodicStart() {
        if (mPeriodic) {
            return;
        }
        mPeriodic = true;
        this.postDelayed(this::periodic, mDelay);
    }

    /**
     * Stop periodic sampling
     */
    public void periodicStop() {
        mPeriodic = false;
    }

    private void periodic() {
        if (mPeriodic) {
            writeLog();
            this.postDelayed(this::periodic, mDelay);
        }
    }

    /**
     * This writes a JSON5 representation of the constraintSet
     */
    public void writeLog() {
        String str = asString((ConstraintLayout) this.getParent());
        if (mLogToFile == null) {
            if (mLogConsole) {
                System.out.println(str);
            } else {
                logBigString(str);
            }
        } else {
            String name = toFile(str, mLogToFile);
            Log.v("JSON", "\"" + name + "\" written!");
        }
    }

    /**
     * This writes the JSON5 description of the constraintLayout to a file named fileName.json5
     * in the download directory which can be pulled with:
     * "adb pull "/storage/emulated/0/Download/" ."
     *
     * @param str      String to write as a file
     * @param fileName file name
     * @return full path name of file
     */
    private static String toFile(String str, String fileName) {
        FileOutputStream outputStream;
        if (!fileName.endsWith(".json5")) {
            fileName += ".json5";
        }
        try {
            File down =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(down, fileName);
            outputStream = new FileOutputStream(file);
            outputStream.write(str.getBytes());
            outputStream.close();
            return file.getCanonicalPath();
        } catch (IOException e) {
            return e.toString();
        }
    }

    @SuppressLint("LogConditional")
    private void logBigString(String str) {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            int k = str.indexOf("\n", i);
            if (k == -1) {
                Log.v(TAG, str.substring(i));
                break;
            }
            Log.v(TAG, str.substring(i, k));
            i = k;
        }
    }

    /**
     * Get a JSON5 String that represents the Constraints in a running ConstraintLayout
     *
     * @param constraintLayout its constraints are converted to a string
     * @return JSON5 string
     */
    private static String asString(ConstraintLayout constraintLayout) {
        JsonWriter c = new JsonWriter();
        return c.constraintLayoutToJson(constraintLayout);
    }

    // ================================== JSON writer==============================================

    private static class JsonWriter {
        public static final int UNSET = ConstraintLayout.LayoutParams.UNSET;
        ConstraintSet mSet;
        Writer mWriter;
        Context mContext;
        int mUnknownCount = 0;
        final String mLEFT = "left";
        final String mRIGHT = "right";
        final String mBASELINE = "baseline";
        final String mBOTTOM = "bottom";
        final String mTOP = "top";
        final String mSTART = "start";
        final String mEND = "end";
        private static final String INDENT = "    ";
        private static final String SMALL_INDENT = "  ";
        HashMap<Integer, String> mIdMap = new HashMap<>();
        private static final String LOG_JSON = LogJson.class.getSimpleName();
        private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
        HashMap<Integer, String> mNames = new HashMap<>();

        private static int generateViewId() {
            final int max_id = 0x00FFFFFF;
            for (;;) {
                final int result = sNextGeneratedId.get();
                int newValue = result + 1;
                if (newValue > max_id) {
                    newValue = 1;
                }
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        }

        @RequiresApi(17)
        private static class JellyBean {
            static int generateViewId() {
                return View.generateViewId();
            }
        }

        String constraintLayoutToJson(ConstraintLayout constraintLayout) {
            StringWriter writer = new StringWriter();

            int count = constraintLayout.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = constraintLayout.getChildAt(i);
                String name = v.getClass().getSimpleName();
                int id = v.getId();
                if (id == -1) {
                    if (android.os.Build.VERSION.SDK_INT
                            >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        id = JellyBean.generateViewId();
                    } else {
                        id = generateViewId();
                    }
                    v.setId(id);
                    if (!LOG_JSON.equals(name)) {
                        name = "noid_" + name;
                    }
                    mNames.put(id, name);
                } else if (LOG_JSON.equals(name)) {
                    mNames.put(id, name);
                }
            }
            writer.append("{\n");

            writeWidgets(writer, constraintLayout);
            writer.append("  ConstraintSet:{\n");
            ConstraintSet set = new ConstraintSet();
            set.clone(constraintLayout);
            String name =
                    (constraintLayout.getId() == -1) ? "cset" : Debug.getName(constraintLayout);
            try {
                writer.append(name + ":");
                setup(writer, set, constraintLayout);
                writeLayout();
                writer.append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            writer.append("  }\n");
            writer.append("}\n");
            return writer.toString();
        }

        private void writeWidgets(StringWriter writer, ConstraintLayout constraintLayout) {
            writer.append("Widgets:{\n");
            int count = constraintLayout.getChildCount();

            for (int i = -1; i < count; i++) {
                View v = (i == -1) ? constraintLayout : constraintLayout.getChildAt(i);
                int id = v.getId();
                if (LOG_JSON.equals(v.getClass().getSimpleName())) {
                    continue;
                }
                String name = mNames.containsKey(id) ? mNames.get(id)
                        : ((i == -1) ? "parent" : Debug.getName(v));
                String cname = v.getClass().getSimpleName();
                String bounds = ", bounds: [" + v.getLeft() + ", " + v.getTop()
                        + ", " + v.getRight() + ", " + v.getBottom() + "]},\n";
                writer.append("  " + name + ": { ");
                if (i == -1) {
                    writer.append("type: '" + v.getClass().getSimpleName() + "' , ");

                    try {
                        ViewGroup.LayoutParams p = (ViewGroup.LayoutParams) v.getLayoutParams();
                        String wrap = "'WRAP_CONTENT'";
                        String match = "'MATCH_PARENT'";
                        String w = p.width == MATCH_PARENT ? match :
                                (p.width == WRAP_CONTENT) ? wrap : p.width + "";
                        writer.append("width: " + w + ", ");
                        String h = p.height == MATCH_PARENT ? match :
                                (p.height == WRAP_CONTENT) ? wrap : p.height + "";
                        writer.append("height: ").append(h);
                    } catch (Exception e) {
                    }
                } else if (cname.contains("Text")) {
                    if (v instanceof TextView) {
                        writer.append("type: 'Text', label: '"
                                + escape(((TextView) v).getText().toString()) + "'");
                    } else {
                        writer.append("type: 'Text' },\n");
                    }
                } else if (cname.contains("Button")) {
                    if (v instanceof Button) {
                        writer.append("type: 'Button', label: '" + ((Button) v).getText() + "'");
                    } else
                        writer.append("type: 'Button'");
                } else if (cname.contains("Image")) {
                    writer.append("type: 'Image'");
                } else if (cname.contains("View")) {
                    writer.append("type: 'Box'");
                } else {
                    writer.append("type: '" + v.getClass().getSimpleName() + "'");
                }
                writer.append(bounds);
            }
            writer.append("},\n");
        }

        private static String escape(String str) {
            return str.replaceAll("'", "\\'");
        }

        JsonWriter() {
        }

        void setup(Writer writer,
                   ConstraintSet set,
                   ConstraintLayout layout) throws IOException {
            this.mWriter = writer;
            this.mContext = layout.getContext();
            this.mSet = set;
            set.getConstraint(2);
        }

        private int[] getIDs() {
            return mSet.getKnownIds();
        }

        private ConstraintSet.Constraint getConstraint(int id) {
            return mSet.getConstraint(id);
        }

        private void writeLayout() throws IOException {
            mWriter.write("{\n");
            for (Integer id : getIDs()) {
                ConstraintSet.Constraint c = getConstraint(id);
                String idName = getSimpleName(id);
                if (LOG_JSON.equals(idName)) { // skip LogJson it is for used to log
                    continue;
                }
                mWriter.write(SMALL_INDENT + idName + ":{\n");
                ConstraintSet.Layout l = c.layout;
                if (l.mReferenceIds != null) {
                    StringBuilder ref =
                            new StringBuilder("type: '_" + idName + "_' , contains: [");
                    for (int r = 0; r < l.mReferenceIds.length; r++) {
                        int rid = l.mReferenceIds[r];
                        ref.append((r == 0) ? "" : ", ").append(getName(rid));
                    }
                    mWriter.write(ref + "]\n");
                }
                if (l.mReferenceIdString != null) {
                    StringBuilder ref =
                            new StringBuilder(SMALL_INDENT + "type: '???' , contains: [");
                    String[] rids = l.mReferenceIdString.split(",");
                    for (int r = 0; r < rids.length; r++) {
                        String rid = rids[r];
                        ref.append((r == 0) ? "" : ", ").append("`").append(rid).append("`");
                    }
                    mWriter.write(ref + "]\n");
                }
                writeDimension("height", l.mHeight, l.heightDefault, l.heightPercent,
                        l.heightMin, l.heightMax, l.constrainedHeight);
                writeDimension("width", l.mWidth, l.widthDefault, l.widthPercent,
                        l.widthMin, l.widthMax, l.constrainedWidth);

                writeConstraint(mLEFT, l.leftToLeft, mLEFT, l.leftMargin, l.goneLeftMargin);
                writeConstraint(mLEFT, l.leftToRight, mRIGHT, l.leftMargin, l.goneLeftMargin);
                writeConstraint(mRIGHT, l.rightToLeft, mLEFT, l.rightMargin, l.goneRightMargin);
                writeConstraint(mRIGHT, l.rightToRight, mRIGHT, l.rightMargin, l.goneRightMargin);
                writeConstraint(mBASELINE, l.baselineToBaseline, mBASELINE, UNSET,
                        l.goneBaselineMargin);
                writeConstraint(mBASELINE, l.baselineToTop, mTOP, UNSET, l.goneBaselineMargin);
                writeConstraint(mBASELINE, l.baselineToBottom,
                        mBOTTOM, UNSET, l.goneBaselineMargin);

                writeConstraint(mTOP, l.topToBottom, mBOTTOM, l.topMargin, l.goneTopMargin);
                writeConstraint(mTOP, l.topToTop, mTOP, l.topMargin, l.goneTopMargin);
                writeConstraint(mBOTTOM, l.bottomToBottom, mBOTTOM, l.bottomMargin,
                        l.goneBottomMargin);
                writeConstraint(mBOTTOM, l.bottomToTop, mTOP, l.bottomMargin, l.goneBottomMargin);
                writeConstraint(mSTART, l.startToStart, mSTART, l.startMargin, l.goneStartMargin);
                writeConstraint(mSTART, l.startToEnd, mEND, l.startMargin, l.goneStartMargin);
                writeConstraint(mEND, l.endToStart, mSTART, l.endMargin, l.goneEndMargin);
                writeConstraint(mEND, l.endToEnd, mEND, l.endMargin, l.goneEndMargin);

                writeVariable("horizontalBias", l.horizontalBias, 0.5f);
                writeVariable("verticalBias", l.verticalBias, 0.5f);

                writeCircle(l.circleConstraint, l.circleAngle, l.circleRadius);

                writeGuideline(l.orientation, l.guideBegin, l.guideEnd, l.guidePercent);
                writeVariable("dimensionRatio", l.dimensionRatio);
                writeVariable("barrierMargin", l.mBarrierMargin);
                writeVariable("type", l.mHelperType);
                writeVariable("ReferenceId", l.mReferenceIdString);
                writeVariable("mBarrierAllowsGoneWidgets",
                        l.mBarrierAllowsGoneWidgets, true);
                writeVariable("WrapBehavior", l.mWrapBehavior);

                writeVariable("verticalWeight", l.verticalWeight);
                writeVariable("horizontalWeight", l.horizontalWeight);
                writeVariable("horizontalChainStyle", l.horizontalChainStyle);
                writeVariable("verticalChainStyle", l.verticalChainStyle);
                writeVariable("barrierDirection", l.mBarrierDirection);
                if (l.mReferenceIds != null) {
                    writeVariable("ReferenceIds", l.mReferenceIds);
                }
                writeTransform(c.transform);
                writeCustom(c.mCustomConstraints);

                mWriter.write("  },\n");
            }
            mWriter.write("},\n");
        }

        private void writeTransform(ConstraintSet.Transform transform) throws IOException {
            if (transform.applyElevation) {
                writeVariable("elevation", transform.elevation);
            }
            writeVariable("rotationX", transform.rotationX, 0);
            writeVariable("rotationY", transform.rotationY, 0);
            writeVariable("rotationZ", transform.rotation, 0);
            writeVariable("scaleX", transform.scaleX, 1);
            writeVariable("scaleY", transform.scaleY, 1);
            writeVariable("translationX", transform.translationX, 0);
            writeVariable("translationY", transform.translationY, 0);
            writeVariable("translationZ", transform.translationZ, 0);
        }

        private void writeCustom(HashMap<String, ConstraintAttribute> cset) throws IOException {
            if (cset != null && cset.size() > 0) {
                mWriter.write(INDENT + "custom: {\n");
                for (String s : cset.keySet()) {
                    ConstraintAttribute attr = cset.get(s);
                    if (attr == null) {
                        continue;
                    }
                    String custom = INDENT + SMALL_INDENT + attr.getName() + ": ";
                    switch (attr.getType()) {
                        case INT_TYPE:
                            custom += attr.getIntegerValue();
                            break;
                        case COLOR_TYPE:
                            custom += colorString(attr.getColorValue());
                            break;
                        case FLOAT_TYPE:
                            custom += attr.getFloatValue();
                            break;
                        case STRING_TYPE:
                            custom += "'" + attr.getStringValue() + "'";
                            break;
                        case DIMENSION_TYPE:
                            custom = custom + attr.getFloatValue();
                            break;
                        case REFERENCE_TYPE:
                        case COLOR_DRAWABLE_TYPE:
                        case BOOLEAN_TYPE:
                            custom = null;
                    }
                    if (custom != null) {
                        mWriter.write(custom + ",\n");
                    }
                }
                mWriter.write(SMALL_INDENT + "   } \n");
            }
        }

        private static String colorString(int v) {
            String str = "00000000" + Integer.toHexString(v);
            return "#" + str.substring(str.length() - 8);
        }

        private void writeGuideline(int orientation,
                                    int guideBegin,
                                    int guideEnd,
                                    float guidePercent) throws IOException {
            writeVariable("orientation", orientation);
            writeVariable("guideBegin", guideBegin);
            writeVariable("guideEnd", guideEnd);
            writeVariable("guidePercent", guidePercent);
        }

        private void writeDimension(String dimString,
                                    int dim,
                                    int dimDefault,
                                    float dimPercent,
                                    int dimMin,
                                    int dimMax,
                                    boolean unusedConstrainedDim) throws IOException {
            if (dim == 0) {
                if (dimMax != UNSET || dimMin != UNSET) {
                    String s = "-----";
                    switch (dimDefault) {
                        case 0: // spread
                            s = INDENT + dimString + ": {value:'spread'";
                            break;
                        case 1: //  wrap
                            s = INDENT + dimString + ": {value:'wrap'";
                            break;
                        case 2: // percent
                            s = INDENT + dimString + ": {value: '" + dimPercent + "%'";
                            break;
                    }
                    if (dimMax != UNSET) {
                        s += ", max: " + dimMax;
                    }
                    if (dimMax != UNSET) {
                        s += ", min: " + dimMin;
                    }
                    s += "},\n";
                    mWriter.write(s);
                    return;
                }

                switch (dimDefault) {
                    case 0: // spread is the default
                        break;
                    case 1: //  wrap
                        mWriter.write(INDENT + dimString + ": '???????????',\n");
                        return;
                    case 2: // percent
                        mWriter.write(INDENT + dimString + ": '" + dimPercent + "%',\n");
                }

            } else if (dim == -2) {
                mWriter.write(INDENT + dimString + ": 'wrap',\n");
            } else if (dim == -1) {
                mWriter.write(INDENT + dimString + ": 'parent',\n");
            } else {
                mWriter.write(INDENT + dimString + ": " + dim + ",\n");
            }
        }

        private String getSimpleName(int id) {
            if (mIdMap.containsKey(id)) {
                return "" + mIdMap.get(id);
            }
            if (id == 0) {
                return "parent";
            }
            String name = lookup(id);
            mIdMap.put(id, name);
            return "" + name + "";
        }

        private String getName(int id) {
            return "'" + getSimpleName(id) + "'";
        }

        private String lookup(int id) {
            try {
                if (mNames.containsKey(id)) {
                    return mNames.get(id);
                }
                if (id != -1) {
                    return mContext.getResources().getResourceEntryName(id);
                } else {
                    return "unknown" + ++mUnknownCount;
                }
            } catch (Exception ex) {
                return "unknown" + ++mUnknownCount;
            }
        }

        private void writeConstraint(String my,
                                     int constraint,
                                     String other,
                                     int margin,
                                     int goneMargin) throws IOException {
            if (constraint == UNSET) {
                return;
            }
            mWriter.write(INDENT + my);
            mWriter.write(":[");
            mWriter.write(getName(constraint));
            mWriter.write(", ");
            mWriter.write("'" + other + "'");
            if (margin != 0 || goneMargin != UNSET_GONE_MARGIN) {
                mWriter.write(", " + margin);
                if (goneMargin != UNSET_GONE_MARGIN) {
                    mWriter.write(", " + goneMargin);
                }
            }
            mWriter.write("],\n");
        }

        private void writeCircle(int circleConstraint,
                                 float circleAngle,
                                 int circleRadius) throws IOException {
            if (circleConstraint == UNSET) {
                return;
            }
            mWriter.write(INDENT + "circle");
            mWriter.write(":[");
            mWriter.write(getName(circleConstraint));
            mWriter.write(", " + circleAngle);
            mWriter.write(circleRadius + "],\n");
        }

        private void writeVariable(String name, int value) throws IOException {
            if (value == 0 || value == -1) {
                return;
            }
            mWriter.write(INDENT + name);
            mWriter.write(": " + value);
            mWriter.write(",\n");
        }

        private void writeVariable(String name, float value) throws IOException {
            if (value == UNSET) {
                return;
            }
            mWriter.write(INDENT + name);
            mWriter.write(": " + value);
            mWriter.write(",\n");
        }

        private void writeVariable(String name, float value, float def) throws IOException {
            if (value == def) {
                return;
            }
            mWriter.write(INDENT + name);
            mWriter.write(": " + value);
            mWriter.write(",\n");
        }

        private void writeVariable(String name, boolean value, boolean def) throws IOException {
            if (value == def) {
                return;
            }
            mWriter.write(INDENT + name);
            mWriter.write(": " + value);
            mWriter.write(",\n");
        }

        private void writeVariable(String name, int[] value) throws IOException {
            if (value == null) {
                return;
            }
            mWriter.write(INDENT + name);
            mWriter.write(": ");
            for (int i = 0; i < value.length; i++) {
                mWriter.write(((i == 0) ? "[" : ", ") + getName(value[i]));
            }
            mWriter.write("],\n");
        }

        private void writeVariable(String name, String value) throws IOException {
            if (value == null) {
                return;
            }
            mWriter.write(INDENT + name);
            mWriter.write(": '" + value);
            mWriter.write("',\n");
        }
    }
}
