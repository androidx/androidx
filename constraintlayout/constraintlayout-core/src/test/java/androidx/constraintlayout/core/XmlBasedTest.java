/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.constraintlayout.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * This test the ConstraintWidget system buy loading XML that contain tags with there positions.
 * the xml files can be designed in android studio.
 */
@RunWith(Parameterized.class)
public class XmlBasedTest {
    private static final int ALLOWED_POSITION_ERROR = 1;
    HashMap<String, ConstraintWidget> mWidgetMap;
    HashMap<ConstraintWidget, String> mBoundsMap;
    ConstraintWidgetContainer mContainer;
    ArrayList<Connection> mConnectionList;
    String mFile;

    public XmlBasedTest(String file) {
        this.mFile = file;
    }

    static class Connection {
        ConstraintWidget mFromWidget;
        ConstraintAnchor.Type mFromType, mToType;
        String mToName;
        int mMargin;
        int mGonMargin = Integer.MIN_VALUE;
    }

    private static HashMap<String, Integer> sVisibilityMap = new HashMap<>();
    private static Map<String, Integer> sStringWidthMap = new HashMap<String, Integer>();
    private static Map<String, Integer> sStringHeightMap = new HashMap<String, Integer>();
    private static Map<String, Integer> sButtonWidthMap = new HashMap<String, Integer>();
    private static Map<String, Integer> sButtonHeightMap = new HashMap<String, Integer>();

    static {
        sVisibilityMap.put("gone", ConstraintWidget.GONE);
        sVisibilityMap.put("visible", ConstraintWidget.VISIBLE);
        sVisibilityMap.put("invisible", ConstraintWidget.INVISIBLE);
        sStringWidthMap.put("TextView", 171);
        sStringWidthMap.put("Button", 107);
        sStringWidthMap.put("Hello World!", 200);
        sStringHeightMap.put("TextView", 57);
        sStringHeightMap.put("Button", 51);
        sStringHeightMap.put("Hello World!", 51);
        String s = "12345678 12345678 12345678 12345678 12345678 12345678 12345678 "
                + "12345678 12345678 12345678 12345678 12345678 12345678 12345678";
        sStringWidthMap.put(s, 984);
        sStringHeightMap.put(s, 204);
        sButtonWidthMap.put("Button", 264);
        sButtonHeightMap.put("Button", 144);
    }

    private static String rtl(String v) {
        if (v.equals("START")) return "LEFT";
        if (v.equals("END")) return "RIGHT";
        return v;
    }

    @Test
    public void testAccessToResources() {
        String dirName = getDir();
        assertTrue(" could not find dir " + dirName, new File(dirName).exists());
        Object[][] names = genListOfName();
        assertTrue(" Could not get Path " + dirName, names.length > 1);
    }

    private static String getDir() {
//        String dirName = System.getProperty("user.dir")
//          + File.separator+".."+File.separator+".."+File.separator+".."
//          +File.separator+"constraintLayout"+File.separator+"core"+File.separator
//          +"src"+File.separator+"test"+File.separator+"resources"+File.separator;

        return System.getProperty("user.dir") + "/src/test/resources/";
    }

    @Parameterized.Parameters
    public static Object[][] genListOfName() {

        String dirName = getDir();

        assertTrue(new File(dirName).exists());
        File[] f = new File(dirName).listFiles(pathname -> pathname.getName().startsWith("check"));
        assertNotNull(f);
        Arrays.sort(f, (o1, o2) -> o1.getName().compareTo(o2.getName()));

        Object[][] ret = new Object[f.length][1];
        for (int i = 0; i < ret.length; i++) {
            ret[i][0] = f[i].getAbsolutePath();
        }
        return ret;
    }

    String dim(ConstraintWidget w) {
        if (w instanceof Guideline) {
            return w.getLeft() + "," + w.getTop() + "," + 0 + "," + 0;
        }
        if (w.getVisibility() == ConstraintWidget.GONE) {
            return 0 + "," + 0 + "," + 0 + "," + 0;
        }
        return w.getLeft() + "," + w.getTop() + "," + w.getWidth() + "," + w.getHeight();
    }

    @Test
    public void testSolverXML() {
        parseXML(mFile);
        mContainer.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        int[] perm = new int[mBoundsMap.size()];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        int total = fact(perm.length);
        int skip = 1 + total / 1000;
        populateContainer(perm);
        makeConnections();
        layout();
        validate();
        int k = 0;
        while (nextPermutation(perm)) {
            k++;
            if (k % skip != 0) continue;

            populateContainer(perm);
            makeConnections();
            layout();
            validate();

        }
    }

    @Test
    public void testDirectResolutionXML() {

        parseXML(mFile);
        mContainer.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        int[] perm = new int[mBoundsMap.size()];
        for (int i = 0; i < perm.length; i++) {
            perm[i] = i;
        }
        int total = fact(perm.length);
        int skip = 1 + total / 1000;
        populateContainer(perm);
        makeConnections();
        layout();
        validate();
        int k = 0;
        while (nextPermutation(perm)) {
            k++;
            if (k % skip != 0) continue;

            populateContainer(perm);
            makeConnections();
            layout();
            validate();
        }
    }

    /**
     * Calculate the Factorial of n
     *
     * @param n input number
     * @return Factorial of n
     */
    public static int fact(int n) {
        int ret = 1;
        while (n > 0) {
            ret *= (n--);
        }
        return ret;
    }

    /**
     * Compare two string containing comer separated integers
     */
    private boolean isSame(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String[] a_split = a.split(",");
        String[] b_split = b.split(",");
        if (a_split.length != b_split.length) {
            return false;
        }
        for (int i = 0; i < a_split.length; i++) {
            if (a_split[i].length() == 0) {
                return false;
            }
            int error = ALLOWED_POSITION_ERROR;
            if (b_split[i].startsWith("+")) {
                error += 10;
            }
            int a_value = Integer.parseInt(a_split[i]);
            int b_value = Integer.parseInt(b_split[i]);
            if (Math.abs(a_value - b_value) > error) {
                return false;
            }
        }
        return true;
    }

    /**
     * Simple dimension parser
     * Multiply dp units by 3 because we simulate a screen with 3 pixels per dp
     */
    static int parseDim(String dim) {
        if (dim.endsWith("dp")) {
            return 3 * Integer.parseInt(dim.substring(0, dim.length() - 2));
        }
        if (dim.equals("wrap_content")) {
            return -1;
        }
        return -2;
    }

    /**
     * parse the XML file
     */
    private void parseXML(String fileName) {
        System.err.println(fileName);
        mContainer = new ConstraintWidgetContainer(0, 0, 1080, 1920);
        mContainer.setDebugName("parent");
        mWidgetMap = new HashMap<String, ConstraintWidget>();
        mBoundsMap = new HashMap<ConstraintWidget, String>();

        mConnectionList = new ArrayList<Connection>();

        DefaultHandler handler = new DefaultHandler() {
            String mParentId;

            public void startDocument() throws SAXException {
            }

            public void endDocument() throws SAXException {
            }

            public void startElement(String namespaceURI,
                    String localName,
                    String qName,
                    Attributes attributes)
                    throws SAXException {

                if (qName != null) {

                    Map<String, String> androidAttrs = new HashMap<String, String>();
                    Map<String, String> appAttrs = new HashMap<String, String>();
                    Map<String, String> widgetConstraints = new HashMap<String, String>();
                    Map<String, String> widgetGoneMargins = new HashMap<String, String>();
                    Map<String, String> widgetMargins = new HashMap<String, String>();

                    for (int i = 0; i < attributes.getLength(); i++) {
                        String attrName = attributes.getLocalName(i);
                        String attrValue = attributes.getValue(i);
                        if (!attrName.contains(":")) {
                            continue;
                        }
                        if (attrValue.trim().isEmpty()) {
                            continue;
                        }
                        String[] parts = attrName.split(":");
                        String scheme = parts[0];
                        String attr = parts[1];
                        if (scheme.equals("android")) {
                            androidAttrs.put(attr, attrValue);

                            if (attr.startsWith("layout_margin")) {
                                widgetMargins.put(attr, attrValue);
                            }
                        } else if (scheme.equals("app")) {
                            appAttrs.put(attr, attrValue);

                            if (attr.equals("layout_constraintDimensionRatio")) {
                                // do nothing
                            } else if (attr.equals("layout_constraintGuide_begin")) {
                                // do nothing
                            } else if (attr.equals("layout_constraintGuide_percent")) {
                                // do nothing
                            } else if (attr.equals("layout_constraintGuide_end")) {
                                // do nothing
                            } else if (attr.equals("layout_constraintHorizontal_bias")) {
                                // do nothing
                            } else if (attr.equals("layout_constraintVertical_bias")) {
                                // do nothing
                            } else if (attr.startsWith("layout_constraint")) {
                                widgetConstraints.put(attr, attrValue);
                            }

                            if (attr.startsWith("layout_goneMargin")) {
                                widgetGoneMargins.put(attr, attrValue);
                            }
                        }
                    }

                    String id = androidAttrs.get("id");
                    String tag = androidAttrs.get("tag");
                    int layoutWidth = parseDim(androidAttrs.get("layout_width"));
                    int layoutHeight = parseDim(androidAttrs.get("layout_height"));
                    String text = androidAttrs.get("text");
                    String visibility = androidAttrs.get("visibility");
                    String orientation = androidAttrs.get("orientation");

                    if (qName.endsWith("ConstraintLayout")) {
                        if (id != null) {
                            mContainer.setDebugName(id);
                        }
                        mWidgetMap.put(mContainer.getDebugName(), mContainer);
                        mWidgetMap.put("parent", mContainer);
                    } else if (qName.endsWith("Guideline")) {
                        Guideline guideline = new Guideline();
                        if (id != null) {
                            guideline.setDebugName(id);
                        }
                        mWidgetMap.put(guideline.getDebugName(), guideline);
                        mBoundsMap.put(guideline, tag);
                        boolean horizontal = "horizontal".equals(orientation);
                        System.out.println("Guideline " + id + " "
                                + (horizontal ? "HORIZONTAL" : "VERTICAL"));
                        guideline.setOrientation(horizontal
                                ? Guideline.HORIZONTAL : Guideline.VERTICAL);

                        String constraintGuideBegin = appAttrs.get("layout_constraintGuide_begin");
                        String constraintGuidePercent =
                                appAttrs.get("layout_constraintGuide_percent");
                        String constraintGuideEnd = appAttrs.get("layout_constraintGuide_end");

                        if (constraintGuideBegin != null) {
                            guideline.setGuideBegin(parseDim(constraintGuideBegin));
                            System.out.println("Guideline " + id
                                    + " setGuideBegin " + parseDim(constraintGuideBegin));

                        } else if (constraintGuidePercent != null) {
                            guideline.setGuidePercent(Float.parseFloat(constraintGuidePercent));
                            System.out.println("Guideline " + id + " setGuidePercent "
                                    + Float.parseFloat(constraintGuidePercent));

                        } else if (constraintGuideEnd != null) {
                            guideline.setGuideEnd(parseDim(constraintGuideEnd));
                            System.out.println("Guideline " + id
                                    + " setGuideBegin " + parseDim(constraintGuideEnd));
                        }
                        System.out.println(">>>>>>>>>>>>  " + guideline);

                    } else {
                        ConstraintWidget widget = new ConstraintWidget(200, 51);
                        widget.setBaselineDistance(28);

                        Connection[] connect = new Connection[5];

                        String widgetLayoutConstraintDimensionRatio =
                                appAttrs.get("layout_constraintDimensionRatio");
                        String widgetLayoutConstraintHorizontalBias =
                                appAttrs.get("layout_constraintHorizontal_bias");
                        String widgetLayoutConstraintVerticalBias =
                                appAttrs.get("layout_constraintVertical_bias");

                        if (id != null) {
                            widget.setDebugName(id);
                        } else {
                            widget.setDebugName("widget" + (mWidgetMap.size() + 1));
                        }

                        if (tag != null) {
                            mBoundsMap.put(widget, tag);
                        }

                        ConstraintWidget.DimensionBehaviour hBehaviour =
                                ConstraintWidget.DimensionBehaviour.FIXED;
                        if (layoutWidth == 0) {
                            hBehaviour = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                            widget.setDimension(layoutWidth, widget.getHeight());
                        } else if (layoutWidth == -1) {
                            hBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                        } else {
                            widget.setDimension(layoutWidth, widget.getHeight());
                        }
                        widget.setHorizontalDimensionBehaviour(hBehaviour);

                        ConstraintWidget.DimensionBehaviour vBehaviour =
                                ConstraintWidget.DimensionBehaviour.FIXED;
                        if (layoutHeight == 0) {
                            vBehaviour = ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                            widget.setDimension(widget.getWidth(), layoutHeight);
                        } else if (layoutHeight == -1) {
                            vBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                        } else {
                            widget.setDimension(widget.getWidth(), layoutHeight);
                        }
                        widget.setVerticalDimensionBehaviour(vBehaviour);

                        if (text != null) {
                            System.out.print("text = \"" + text + "\"");
                            Map<String, Integer> wmap = (qName.equals("Button"))
                                    ? sButtonWidthMap : sStringWidthMap;
                            Map<String, Integer> hmap = (qName.equals("Button"))
                                    ? sButtonHeightMap : sStringHeightMap;
                            if (wmap.containsKey(text) && widget.getHorizontalDimensionBehaviour()
                                    == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                                widget.setWidth(wmap.get(text));
                            }
                            if (hmap.containsKey(text) && widget.getVerticalDimensionBehaviour()
                                    == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                                widget.setHeight(hmap.get(text));
                            }
                        }

                        if (visibility != null) {
                            widget.setVisibility(sVisibilityMap.get(visibility));
                        }

                        if (widgetLayoutConstraintDimensionRatio != null) {
                            widget.setDimensionRatio(widgetLayoutConstraintDimensionRatio);
                        }

                        if (widgetLayoutConstraintHorizontalBias != null) {
                            System.out.println("widgetLayoutConstraintHorizontalBias "
                                    + widgetLayoutConstraintHorizontalBias);
                            widget.setHorizontalBiasPercent(
                                    Float.parseFloat(widgetLayoutConstraintHorizontalBias));
                        }

                        if (widgetLayoutConstraintVerticalBias != null) {
                            System.out.println("widgetLayoutConstraintVerticalBias "
                                    + widgetLayoutConstraintVerticalBias);
                            widget.setVerticalBiasPercent(
                                    Float.parseFloat(widgetLayoutConstraintVerticalBias));
                        }

                        Set<String> constraintKeySet = widgetConstraints.keySet();
                        String[] constraintKeys =
                                constraintKeySet.toArray(new String[constraintKeySet.size()]);
                        for (int i = 0; i < constraintKeys.length; i++) {
                            String attrName = constraintKeys[i];
                            String attrValue = widgetConstraints.get(attrName);
                            String[] sp =
                                    attrName.substring("layout_constraint".length()).split("_to");
                            String fromString = rtl(sp[0].toUpperCase());
                            ConstraintAnchor.Type from = ConstraintAnchor.Type.valueOf(fromString);
                            String toString = rtl(sp[1].substring(0,
                                    sp[1].length() - 2).toUpperCase());
                            ConstraintAnchor.Type to = ConstraintAnchor.Type.valueOf(toString);
                            int side = from.ordinal() - 1;
                            if (connect[side] == null) {
                                connect[side] = new Connection();
                            }
                            connect[side].mFromWidget = widget;
                            connect[side].mFromType = from;
                            connect[side].mToType = to;
                            connect[side].mToName = attrValue;
                        }

                        Set<String> goneMarginSet = widgetGoneMargins.keySet();
                        String[] goneMargins =
                                goneMarginSet.toArray(new String[goneMarginSet.size()]);
                        for (int i = 0; i < goneMargins.length; i++) {
                            String attrName = goneMargins[i];
                            String attrValue = widgetGoneMargins.get(attrName);
                            String marginSide = rtl(
                                    attrName.substring("layout_goneMargin".length()).toUpperCase()
                            );
                            ConstraintAnchor.Type marginType =
                                    ConstraintAnchor.Type.valueOf(marginSide);
                            int side = marginType.ordinal() - 1;
                            if (connect[side] == null) {
                                connect[side] = new Connection();
                            }
                            connect[side].mGonMargin = 3 * Integer.parseInt(
                                    attrValue.substring(0, attrValue.length() - 2)
                            );
                        }

                        Set<String> marginSet = widgetMargins.keySet();
                        String[] margins = marginSet.toArray(new String[marginSet.size()]);
                        for (int i = 0; i < margins.length; i++) {
                            String attrName = margins[i];
                            String attrValue = widgetMargins.get(attrName);
                            // System.out.println("margin [" + attrName + "]
                            //    by [" + attrValue +"]");
                            String marginSide =
                                    rtl(attrName.substring("layout_margin".length()).toUpperCase());
                            ConstraintAnchor.Type marginType =
                                    ConstraintAnchor.Type.valueOf(marginSide);
                            int side = marginType.ordinal() - 1;
                            if (connect[side] == null) {
                                connect[side] = new Connection();
                            }
                            connect[side].mMargin = 3 * Integer.parseInt(
                                    attrValue.substring(0, attrValue.length() - 2));
                        }

                        mWidgetMap.put(widget.getDebugName(), widget);

                        for (int i = 0; i < connect.length; i++) {
                            if (connect[i] != null) {
                                mConnectionList.add(connect[i]);
                            }
                        }

                    }
                }


            }
        };

        File file = new File(fileName);
        SAXParserFactory spf = SAXParserFactory.newInstance();

        try {
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(handler);
            xmlReader.parse(file.toURI().toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateContainer(int[] order) {
        System.out.println(Arrays.toString(order));
        ConstraintWidget[] widgetSet = mBoundsMap.keySet().toArray(new ConstraintWidget[0]);
        for (int i = 0; i < widgetSet.length; i++) {
            ConstraintWidget widget = widgetSet[order[i]];
            if (widget.getDebugName().equals("parent")) {
                continue;
            }
            ConstraintWidget.DimensionBehaviour hBehaviour =
                    widget.getHorizontalDimensionBehaviour();
            ConstraintWidget.DimensionBehaviour vBehaviour = widget.getVerticalDimensionBehaviour();

            if (widget instanceof Guideline) {
                Guideline copy = new Guideline();
                copy.copy(widget, new HashMap<>());
                mContainer.remove(widget);
                widget.copy(copy, new HashMap<>());
            } else {
                ConstraintWidget copy = new ConstraintWidget();
                copy.copy(widget, new HashMap<>());
                mContainer.remove(widget);
                widget.copy(copy, new HashMap<>());
            }
            widget.setHorizontalDimensionBehaviour(hBehaviour);
            widget.setVerticalDimensionBehaviour(vBehaviour);
            mContainer.add(widget);
        }
    }

    private void makeConnections() {
        for (Connection connection : mConnectionList) {
            ConstraintWidget toConnect;
            if (connection.mToName.equalsIgnoreCase("parent")
                    || connection.mToName.equals(mContainer.getDebugName())) {
                toConnect = mContainer;
            } else {
                toConnect = mWidgetMap.get(connection.mToName);
            }
            if (toConnect == null) {
                System.err.println("   " + connection.mToName);
            } else {
                connection.mFromWidget.connect(connection.mFromType,
                        toConnect, connection.mToType, connection.mMargin);
                connection.mFromWidget.setGoneMargin(connection.mFromType, connection.mGonMargin);
            }
        }
    }

    private void layout() {
        mContainer.layout();
    }

    private void validate() {
        ConstraintWidgetContainer root = (ConstraintWidgetContainer) mWidgetMap.remove("parent");

        String[] keys = mWidgetMap.keySet().toArray(new String[0]);
        boolean ok = true;
        StringBuilder layout = new StringBuilder("\n");
        for (String key : keys) {
            if (key.contains("activity_main")) {
                continue;
            }
            ConstraintWidget widget = mWidgetMap.get(key);
            String bounds = mBoundsMap.get(widget);
            String dim = dim(widget);
            boolean same = isSame(dim, bounds);
            String compare = rightPad(key, 17) + rightPad(dim, 15) + "   " + bounds;
            ok &= same;
            layout.append(compare).append("\n");
        }
        assertTrue(layout.toString(), ok);
    }

    private static String rightPad(String s, int n) {
        s = s + new String(new byte[n]).replace('\0', ' ');
        return s.substring(0, n);
    }

    private static String r(String s) {
        s = "             " + s;
        return s.substring(s.length() - 13);
    }

    /**
     * Ordered array (1,2,3...) will be cycled till the order is reversed (9,8,7...)
     *
     * @param array to be carried
     * @return false when the order is reversed
     */
    private static boolean nextPermutation(int[] array) {
        int i = array.length - 1;
        while (i > 0 && array[i - 1] >= array[i]) {
            i--;
        }
        if (i <= 0) {
            return false;
        }
        int j = array.length - 1;
        while (array[j] <= array[i - 1]) {
            j--;
        }

        int temp = array[i - 1];
        array[i - 1] = array[j];
        array[j] = temp;

        j = array.length - 1;
        while (i < j) {
            temp = array[i];
            array[i] = array[j];
            array[j] = temp;
            i++;
            j--;
        }

        return true;
    }

    @Test
    public void simpleTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1920);

        final ConstraintWidget a = new ConstraintWidget(0, 0, 200, 51);
        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.setDebugName("A");
        a.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.add(a);
        root.layout();
        System.out.println("f) A: " + a + " " + a.getWidth() + "," + a.getHeight());
    }

    @Test
    public void guideLineTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1080, 1920);
        final ConstraintWidget a = new ConstraintWidget(0, 0, 200, 51);
        final Guideline guideline = new Guideline();
        root.add(guideline);

        guideline.setGuidePercent(0.50f);
        guideline.setOrientation(Guideline.VERTICAL);
        guideline.setDebugName("guideline");

        a.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        a.setDebugName("A");
        a.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT, 0);
        a.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        a.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        a.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.add(a);

        root.layout();
        System.out.println("f) A: " + a + " " + a.getWidth() + "," + a.getHeight());
        System.out.println("f) A: " + guideline + " "
                + guideline.getWidth() + "," + guideline.getHeight());

    }


}
