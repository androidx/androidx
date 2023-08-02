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

import static org.junit.Assert.assertTrue;

import androidx.constraintlayout.core.scout.Scout;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;

import org.junit.Test;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;


/**
 * This test creates a random set of non overlapping rectangles uses the scout
 * to add a sequence of constraints. Verify that the constraint engine will then layout the
 * rectangles to within 12 pixels.
 * It uses
 */
public class RandomLayoutTest {
    private static final int ALLOWED_POSITION_ERROR = 12;

    public static final int MIN_WIDTH = 100;
    public static final int MIN_HEIGHT = 40;
    public static final int MIN_GAP = 40;
    public static final int MAX_TRIES = 100;
    public static final int LAYOUT_WIDTH = 1024;
    public static final int LAYOUT_HEIGHT = 512;
    public static final int MAX_WIDGETS = 20;
    public static final int PERCENT_BIG_WIDGETS = 70;
    public static final int LOOP_FOR = 1000;

    /**
     * Create a collection of rectangles
     *
     * @param count     the number of rectangles to try and generate
     * @param sizeRatio 0 = all small ones, 100 = all big ones
     * @param width     the width of the bounding rectangle
     * @param height    the height of the bounding rectangle
     */
    static ArrayList<Rectangle> random(long seed, int count, int sizeRatio, int width, int height) {
        ArrayList<Rectangle> recs = new ArrayList<Rectangle>();
        int minWidth = MIN_WIDTH;
        int minHeight = MIN_HEIGHT;
        int minGap = MIN_GAP;
        int gapBy2 = MIN_GAP * 2;

        Random rand = new Random(seed);
        Rectangle test = new Rectangle();
        for (int i = 0; i < count; i++) {

            Rectangle rn = new Rectangle();
            boolean found = false;

            int attempt = 0;
            while (!found) {
                if (rand.nextInt(100) < sizeRatio) {
                    rn.x = rand.nextInt(width - minWidth - gapBy2) + minGap;
                    rn.y = rand.nextInt(height - minHeight - gapBy2) + minGap;
                    rn.width = minWidth + rand.nextInt(width - rn.x - minWidth - minGap);
                    rn.height = minHeight + rand.nextInt(height - rn.y - minHeight - minGap);
                } else {
                    rn.x = rand.nextInt(width - minWidth - gapBy2) + minGap;
                    rn.y = rand.nextInt(height - minHeight - gapBy2) + minGap;
                    rn.width = minWidth;
                    rn.height = minHeight;
                }
                test.x = rn.x - minGap;
                test.y = rn.y - minGap;
                test.width = rn.width + gapBy2;
                test.height = rn.height + gapBy2;

                found = true;
                int size = recs.size();
                for (int j = 0; j < size; j++) {
                    if (recs.get(j).intersects(test)) {
                        found = false;
                        break;
                    }
                }
                attempt++;
                if (attempt > MAX_TRIES) {
                    break;
                }

            }
            if (found) {
                recs.add(rn);
            }
        }
        return recs;
    }

    @Test
    public void testRandomLayouts() {
        Random r = new Random(4567890);
        for (int test = 0; test < LOOP_FOR; test++) {
            long seed = r.nextLong();
            System.out.println("seed = " + seed);
            ArrayList<Rectangle> list = random(seed, MAX_WIDGETS,
                    PERCENT_BIG_WIDGETS, LAYOUT_WIDTH, LAYOUT_HEIGHT);

            ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0,
                    LAYOUT_WIDTH, LAYOUT_HEIGHT);

            root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            root.setWidth(LAYOUT_WIDTH);
            root.setHeight(LAYOUT_HEIGHT);
            int k = 0;
            for (Rectangle rec : list) {
                ConstraintWidget widget = new ConstraintWidget();
                widget.setType("TextView");
                String text = ("TextView" + k++);
                widget.setDebugName(text);
                widget.setOrigin(rec.x, rec.y);

                widget.setWidth(widget.getMinWidth());
                widget.setHeight(widget.getMinHeight());
                widget.setWidth(widget.getWidth());
                widget.setHeight(widget.getHeight());
                widget.setHorizontalDimensionBehaviour(
                        ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                widget.setVerticalDimensionBehaviour(
                        ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

                root.add(widget);
                widget.setX(rec.x);
                widget.setY(rec.y);
                if (widget.getMinWidth() < rec.width) {
                    widget.setMinWidth(rec.width);
                }
                if (widget.getMinHeight() < rec.height) {
                    widget.setMinHeight(rec.height);
                }

                widget.setDimension(rec.width, rec.height);
//                widget.setWrapHeight(rec.height);
//                widget.setWrapHeight(rec.width);
            }

            ArrayList<ConstraintWidget> widgetList = root.getChildren();

            Scout.inferConstraints(root);
            for (ConstraintWidget widget : widgetList) {
                widget.setDimension(10, 10);
                widget.setOrigin(10, 10);
            }
            boolean allOk = true;
            root.layout();
            String layout = "\n";
            boolean ok = true;

            for (int i = 0; i < widgetList.size(); i++) {
                ConstraintWidget widget = widgetList.get(i);
                Rectangle rect = list.get(i);
                allOk &= ok = isSame(dim(widget), dim(rect));
                layout += rightPad(dim(widget), 15) + ((ok) ? " == " : " != ") + dim(rect) + "\n";
            }
            assertTrue(layout, allOk);
        }
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

    private static String rightPad(String s, int n) {
        s = s + new String(new byte[n]).replace('\0', ' ');
        return s.substring(0, n);
    }


    String dim(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
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
}
