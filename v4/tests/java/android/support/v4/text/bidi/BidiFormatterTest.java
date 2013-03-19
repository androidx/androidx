/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.text.bidi;

import android.test.AndroidTestCase;
import android.support.v4.text.TextDirectionHeuristicsCompat;

import java.util.Locale;

public class BidiFormatterTest extends AndroidTestCase {

    private static final BidiFormatter LTR_FMT = BidiFormatter.getInstance(false /* LTR context */);
    private static final BidiFormatter RTL_FMT = BidiFormatter.getInstance(true /* RTL context */);

    private static final BidiFormatter LTR_FMT_EXIT_RESET =
            new BidiFormatter.Builder(false /* LTR context */).stereoReset(false).build();
    private static final BidiFormatter RTL_FMT_EXIT_RESET =
            new BidiFormatter.Builder(true /* RTL context */).stereoReset(false).build();

    private static final String EN = "abba";
    private static final String HE = "\u05e0\u05e1";

    private static final String LRM = "\u200E";
    private static final String RLM = "\u200F";
    private static final String LRE = "\u202A";
    private static final String RLE = "\u202B";
    private static final String PDF = "\u202C";

    private static final String LEFT = "left";
    private static final String RIGHT = "right";


    public void testIsRtlContext() {
        assertEquals(false, LTR_FMT.isRtlContext());
        assertEquals(true, RTL_FMT.isRtlContext());

        assertEquals(false, BidiFormatter.getInstance(Locale.ENGLISH).isRtlContext());
        assertEquals(true, BidiFormatter.getInstance(true).isRtlContext());
    }

    public void testBuilderIsRtlContext() {
        assertEquals(false, new BidiFormatter.Builder(false).build().isRtlContext());
        assertEquals(true, new BidiFormatter.Builder(true).build().isRtlContext());
    }

    public void testIsRtl() {
        assertEquals(true, BidiFormatter.getInstance(true).isRtl(HE));
        assertEquals(true, BidiFormatter.getInstance(false).isRtl(HE));

        assertEquals(false, BidiFormatter.getInstance(true).isRtl(EN));
        assertEquals(false, BidiFormatter.getInstance(false).isRtl(EN));
    }

    public void testDirAttrValue() {
        assertEquals("ltr", LTR_FMT.dirAttrValue(EN));
        assertEquals("ltr", RTL_FMT.dirAttrValue(EN));

        assertEquals("rtl", LTR_FMT.dirAttrValue(HE));
        assertEquals("rtl", RTL_FMT.dirAttrValue(HE));

        assertEquals("ltr", LTR_FMT.dirAttrValue(EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("rtl", LTR_FMT.dirAttrValue(EN, TextDirectionHeuristicsCompat.RTL));

        assertEquals("ltr", RTL_FMT.dirAttrValue(EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("rtl", RTL_FMT.dirAttrValue(EN, TextDirectionHeuristicsCompat.RTL));

        assertEquals("ltr", LTR_FMT.dirAttrValue(HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("rtl", LTR_FMT.dirAttrValue(HE, TextDirectionHeuristicsCompat.RTL));

        assertEquals("ltr", RTL_FMT.dirAttrValue(HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("rtl", RTL_FMT.dirAttrValue(HE, TextDirectionHeuristicsCompat.RTL));

        assertEquals("ltr", LTR_FMT.dirAttrValue("", TextDirectionHeuristicsCompat.LTR));
        assertEquals("rtl", LTR_FMT.dirAttrValue("", TextDirectionHeuristicsCompat.RTL));

        assertEquals("ltr", RTL_FMT.dirAttrValue("", TextDirectionHeuristicsCompat.LTR));
        assertEquals("rtl", RTL_FMT.dirAttrValue("", TextDirectionHeuristicsCompat.RTL));
    }

    public void testDirAttr() {
        assertEquals("", LTR_FMT.dirAttr(EN));
        assertEquals("dir=\"ltr\"", RTL_FMT.dirAttr(EN));

        assertEquals("dir=\"rtl\"", LTR_FMT.dirAttr(HE));
        assertEquals("", RTL_FMT.dirAttr(HE));

        assertEquals("", LTR_FMT.dirAttr(".", TextDirectionHeuristicsCompat.LTR));
        assertEquals("dir=\"ltr\"", RTL_FMT.dirAttr(".", TextDirectionHeuristicsCompat.LTR));

        assertEquals("dir=\"rtl\"", LTR_FMT.dirAttr(".", TextDirectionHeuristicsCompat.RTL));
        assertEquals("", RTL_FMT.dirAttr(".", TextDirectionHeuristicsCompat.RTL));
    }

    public void testMarkAfter() {
        assertEquals("uniform dir matches LTR context",
                "", LTR_FMT.markAfter(EN));
        assertEquals("uniform dir matches RTL context",
                "", RTL_FMT.markAfter(HE));

        assertEquals("exit dir opposite to LTR context",
                LRM, LTR_FMT.markAfter(EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir opposite to RTL context",
                RLM, RTL_FMT.markAfter(HE + EN, TextDirectionHeuristicsCompat.RTL));

        assertEquals("overall dir (but not exit dir) opposite to LTR context",
                LRM, LTR_FMT.markAfter(HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("overall dir (but not exit dir) opposite to RTL context",
                RLM, RTL_FMT.markAfter(EN + HE, TextDirectionHeuristicsCompat.LTR));

        assertEquals("exit dir neutral, overall dir matches LTR context",
                "", LTR_FMT.markAfter(".", TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir neutral, overall dir matches RTL context",
                "", RTL_FMT.markAfter(".", TextDirectionHeuristicsCompat.RTL));
    }

    public void testMarkBefore() {
        assertEquals("uniform dir matches LTR context",
                "", LTR_FMT.markBefore(EN));
        assertEquals("uniform dir matches RTL context",
                "", RTL_FMT.markBefore(HE));

        assertEquals("entry dir opposite to LTR context",
                LRM, LTR_FMT.markBefore(HE + EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry dir opposite to RTL context",
                RLM, RTL_FMT.markBefore(EN + HE, TextDirectionHeuristicsCompat.RTL));

        assertEquals("overall dir (but not entry dir) opposite to LTR context",
                LRM, LTR_FMT.markBefore(EN + HE, TextDirectionHeuristicsCompat.RTL));
        assertEquals("overall dir (but not entry dir) opposite to RTL context",
                RLM, RTL_FMT.markBefore(HE + EN, TextDirectionHeuristicsCompat.LTR));

        assertEquals("exit dir neutral, overall dir matches LTR context",
                "", LTR_FMT.markBefore(".", TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir neutral, overall dir matches RTL context",
                "", RTL_FMT.markBefore(".", TextDirectionHeuristicsCompat.RTL));
    }


    public void testMark() {
        assertEquals(LRM, LTR_FMT.mark());
        assertEquals(RLM, RTL_FMT.mark());
    }

    public void testStartEdge() {
        assertEquals(LEFT, LTR_FMT.startEdge());
        assertEquals(RIGHT, RTL_FMT.startEdge());
    }

    public void testEndEdge() {
        assertEquals(RIGHT, LTR_FMT.endEdge());
        assertEquals(LEFT, RTL_FMT.endEdge());
    }

    public void testUnicodeWrap() {
        // Uniform directionality in opposite context.
        assertEquals("uniform dir opposite to LTR context",
                RLE + "." + HE + "." + PDF + LRM,
                LTR_FMT_EXIT_RESET.unicodeWrap("." + HE + "."));
        assertEquals("uniform dir opposite to LTR context, stereo reset",
                LRM + RLE + "." + HE + "." + PDF + LRM,
                LTR_FMT.unicodeWrap("." + HE + "."));
        assertEquals("uniform dir opposite to LTR context, stereo reset, no isolation",
                RLE + "." + HE + "." + PDF,
                LTR_FMT.unicodeWrap("." + HE + ".", false));
        assertEquals("neutral treated as opposite to LTR context",
                RLE + "." + PDF + LRM,
                LTR_FMT_EXIT_RESET.unicodeWrap(".", TextDirectionHeuristicsCompat.RTL));
        assertEquals("uniform dir opposite to RTL context",
                LRE + "." + EN + "." + PDF + RLM,
                RTL_FMT_EXIT_RESET.unicodeWrap("." + EN + "."));
        assertEquals("uniform dir opposite to RTL context, stereo reset",
                RLM + LRE + "." + EN + "." + PDF + RLM,
                RTL_FMT.unicodeWrap("." + EN + "."));
        assertEquals("uniform dir opposite to RTL context, stereo reset, no isolation",
                LRE + "." + EN + "." + PDF,
                RTL_FMT.unicodeWrap("." + EN + ".", false));
        assertEquals("neutral treated as opposite to RTL context",
                LRE + "." + PDF + RLM,
                RTL_FMT_EXIT_RESET.unicodeWrap(".", TextDirectionHeuristicsCompat.LTR));

        // We test mixed-directionality cases only with an explicit overall directionality parameter
        // because the estimation logic is outside the sphere of BidiFormatter, and different
        // estimators will treat them differently.

        // Overall directionality matching context, but with opposite exit directionality.
        assertEquals("exit dir opposite to LTR context",
                EN + HE + LRM,
                LTR_FMT_EXIT_RESET.unicodeWrap(EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir opposite to LTR context, stereo reset",
                EN + HE + LRM,
                LTR_FMT.unicodeWrap(EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir opposite to LTR context, stereo reset, no isolation",
                EN + HE,
                LTR_FMT.unicodeWrap(EN + HE, TextDirectionHeuristicsCompat.LTR, false));

        assertEquals("exit dir opposite to RTL context",
                HE + EN + RLM,
                RTL_FMT_EXIT_RESET.unicodeWrap(HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("exit dir opposite to RTL context, stereo reset",
                HE + EN + RLM,
                RTL_FMT.unicodeWrap(HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("exit dir opposite to RTL context, stereo reset, no isolation",
                HE + EN,
                RTL_FMT.unicodeWrap(HE + EN, TextDirectionHeuristicsCompat.RTL, false));

        // Overall directionality matching context, but with opposite entry directionality.
        assertEquals("entry dir opposite to LTR context",
                HE + EN,
                LTR_FMT_EXIT_RESET.unicodeWrap(HE + EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry dir opposite to LTR context, stereo reset",
                LRM + HE + EN,
                LTR_FMT.unicodeWrap(HE + EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry dir opposite to LTR context, stereo reset, no isolation",
                HE + EN,
                LTR_FMT.unicodeWrap(HE + EN, TextDirectionHeuristicsCompat.LTR, false));

        assertEquals("entry dir opposite to RTL context",
                EN + HE,
                RTL_FMT_EXIT_RESET.unicodeWrap(EN + HE, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry dir opposite to RTL context, stereo reset",
                RLM + EN + HE,
                RTL_FMT.unicodeWrap(EN + HE, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry dir opposite to RTL context, stereo reset, no isolation",
                EN + HE,
                RTL_FMT.unicodeWrap(EN + HE, TextDirectionHeuristicsCompat.RTL, false));

        // Overall directionality matching context, but with opposite entry and exit directionality.
        assertEquals("entry and exit dir opposite to LTR context",
                HE + EN + HE + LRM,
                LTR_FMT_EXIT_RESET.unicodeWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry and exit dir opposite to LTR context, stereo reset",
                LRM + HE + EN + HE + LRM,
                LTR_FMT.unicodeWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry and exit dir opposite to LTR context, no isolation",
                HE + EN + HE,
                LTR_FMT_EXIT_RESET.unicodeWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR, false));

        assertEquals("entry and exit dir opposite to RTL context",
                EN + HE + EN + RLM,
                RTL_FMT_EXIT_RESET.unicodeWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry and exit dir opposite to RTL context, no isolation",
                EN + HE + EN,
                RTL_FMT_EXIT_RESET.unicodeWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL, false));

        // Entry and exit directionality matching context, but with opposite overall directionality.
        assertEquals("overall dir (but not entry or exit dir) opposite to LTR context",
                RLE + EN + HE + EN + PDF + LRM,
                LTR_FMT_EXIT_RESET.unicodeWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("overall dir (but not entry or exit dir) opposite to LTR context, stereo reset",
                LRM + RLE + EN + HE + EN + PDF + LRM,
                LTR_FMT.unicodeWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("overall dir (but not entry or exit dir) opposite to LTR context, no isolation",
                RLE + EN + HE + EN + PDF,
                LTR_FMT_EXIT_RESET.unicodeWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL, false));

        assertEquals("overall dir (but not entry or exit dir) opposite to RTL context",
                LRE + HE + EN + HE + PDF + RLM,
                RTL_FMT_EXIT_RESET.unicodeWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("overall dir (but not entry or exit dir) opposite to RTL context, stereo reset",
                RLM + LRE + HE + EN + HE + PDF + RLM,
                RTL_FMT.unicodeWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("overall dir (but not entry or exit dir) opposite to RTL context, no isolation",
                LRE + HE + EN + HE + PDF,
                RTL_FMT_EXIT_RESET.unicodeWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR, false));
    }


    public void testSpanWrap() {
        // Uniform directionality in matching context.
        assertEquals("uniform dir matches LTR context",
                "&amp; " + EN + "&lt;", LTR_FMT.spanWrap("& " + EN + "<"));
        assertEquals("neutral treated as matching LTR context",
                ".", LTR_FMT.spanWrap(".", TextDirectionHeuristicsCompat.LTR));
        assertEquals("uniform dir matches RTL context",
                "&amp; " + HE + "&lt;", RTL_FMT.spanWrap("& " + HE + "<"));
        assertEquals("neutral treated as matching RTL context",
                ".", RTL_FMT.spanWrap(".", TextDirectionHeuristicsCompat.RTL));

        // Uniform directionality in opposite context.
        assertEquals("uniform dir opposite to LTR context",
                "<span dir=\"rtl\">." + HE + ".</span>" + LRM,
                LTR_FMT_EXIT_RESET.spanWrap("." + HE + "."));
        assertEquals("uniform dir opposite to LTR context, stereo reset",
                LRM + "<span dir=\"rtl\">." + HE + ".</span>" + LRM,
                LTR_FMT.spanWrap("." + HE + "."));
        assertEquals("uniform dir opposite to LTR context, no isolation",
                "<span dir=\"rtl\">." + HE + ".</span>",
                LTR_FMT_EXIT_RESET.spanWrap("." + HE + ".", false));
        assertEquals("uniform dir opposite to LTR context, stereo reset, no isolation",
                "<span dir=\"rtl\">." + HE + ".</span>",
                LTR_FMT.spanWrap("." + HE + ".", false));
        assertEquals("neutral treated as opposite to LTR context",
                "<span dir=\"rtl\">" + "." + "</span>" + LRM,
                LTR_FMT_EXIT_RESET.spanWrap(".", TextDirectionHeuristicsCompat.RTL));
        assertEquals("uniform dir opposite to RTL context",
                "<span dir=\"ltr\">." + EN + ".</span>" + RLM,
                RTL_FMT_EXIT_RESET.spanWrap("." + EN + "."));
        assertEquals("uniform dir opposite to RTL context, stereo reset",
                RLM + "<span dir=\"ltr\">." + EN + ".</span>" + RLM,
                RTL_FMT.spanWrap("." + EN + "."));
        assertEquals("uniform dir opposite to RTL context, no isolation",
                "<span dir=\"ltr\">." + EN + ".</span>",
                RTL_FMT_EXIT_RESET.spanWrap("." + EN + ".", false));
        assertEquals("uniform dir opposite to RTL context, stereo reset, no isolation",
                "<span dir=\"ltr\">." + EN + ".</span>",
                RTL_FMT.spanWrap("." + EN + ".", false));
        assertEquals("neutral treated as opposite to RTL context",
                "<span dir=\"ltr\">" + "." + "</span>" + RLM,
                RTL_FMT_EXIT_RESET.spanWrap(".", TextDirectionHeuristicsCompat.LTR));

        // We test mixed-directionality cases only with an explicit overall directionality parameter
        // because the estimation logic is outside the sphere of BidiFormatter, and different
        // estimators will treat them differently.

        // Overall directionality matching context, but with opposite exit directionality.
        assertEquals("exit dir opposite to LTR context",
                EN + HE + LRM,
                LTR_FMT_EXIT_RESET.spanWrap(EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir opposite to LTR context, stereo reset",
                EN + HE + LRM,
                LTR_FMT.spanWrap(EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("exit dir opposite to LTR context, no isolation",
                EN + HE,
                LTR_FMT_EXIT_RESET.spanWrap(EN + HE, TextDirectionHeuristicsCompat.LTR, false));
        assertEquals("exit dir opposite to LTR context, stereo reset, no isolation",
                EN + HE,
                LTR_FMT.spanWrap(EN + HE, TextDirectionHeuristicsCompat.LTR, false));
        assertEquals("exit dir opposite to RTL context",
                HE + EN + RLM,
                RTL_FMT_EXIT_RESET.spanWrap(HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("exit dir opposite to RTL context, stereo reset",
                HE + EN + RLM,
                RTL_FMT.spanWrap(HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("exit dir opposite to RTL context, no isolation",
                HE + EN,
                RTL_FMT_EXIT_RESET.spanWrap(HE + EN, TextDirectionHeuristicsCompat.RTL, false));
        assertEquals("exit dir opposite to RTL context, stereo reset, no isolation",
                HE + EN,
                RTL_FMT.spanWrap( HE + EN, TextDirectionHeuristicsCompat.RTL, false));

        // Overall directionality matching context, but with opposite entry directionality.
        assertEquals("entry dir opposite to LTR context",
                HE + EN,
                LTR_FMT_EXIT_RESET.spanWrap(HE + EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry dir opposite to LTR context, stereo reset",
                LRM + HE + EN,
                LTR_FMT.spanWrap(HE + EN, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry dir opposite to LTR context, no isolation",
                HE + EN,
                LTR_FMT_EXIT_RESET.spanWrap(HE + EN, TextDirectionHeuristicsCompat.LTR, false));
        assertEquals("entry dir opposite to LTR context, stereo reset, no isolation",
                HE + EN,
                LTR_FMT.spanWrap(HE + EN, TextDirectionHeuristicsCompat.LTR, false));
        assertEquals("entry dir opposite to RTL context",
                EN + HE,
                RTL_FMT_EXIT_RESET.spanWrap(EN + HE, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry dir opposite to RTL context, stereo reset",
                RLM + EN + HE,
                RTL_FMT.spanWrap(EN + HE, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry dir opposite to RTL context, no isolation",
                EN + HE,
                RTL_FMT_EXIT_RESET.spanWrap(EN + HE, TextDirectionHeuristicsCompat.RTL, false));
        assertEquals("entry dir opposite to RTL context, stereo reset, no isolation",
                EN + HE,
                RTL_FMT.spanWrap(EN + HE, TextDirectionHeuristicsCompat.RTL, false));

        // Overall directionality matching context, but with opposite entry and exit directionality.
        assertEquals("entry and exit dir opposite to LTR context",
                HE + EN + HE + LRM,
                LTR_FMT_EXIT_RESET.spanWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry and exit dir opposite to LTR context, stereo reset",
                LRM + HE + EN + HE + LRM,
                LTR_FMT.spanWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("entry and exit dir opposite to LTR context, no isolation",
                HE + EN + HE,
                LTR_FMT_EXIT_RESET.spanWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR, false));
        assertEquals("entry and exit dir opposite to RTL context",
                EN + HE + EN + RLM,
                RTL_FMT_EXIT_RESET.spanWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry and exit dir opposite to RTL context, stereo reset",
                RLM + EN + HE + EN + RLM,
                RTL_FMT.spanWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("entry and exit dir opposite to RTL context, no isolation",
                EN + HE + EN,
                RTL_FMT_EXIT_RESET.spanWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL, false));

        // Entry and exit directionality matching context, but with opposite overall directionality.
        assertEquals("overall dir (but not entry or exit dir) opposite to LTR context",
                "<span dir=\"rtl\">" + EN + HE + EN + "</span>" + LRM,
                LTR_FMT_EXIT_RESET.spanWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("overall dir (but not entry or exit dir) opposite to LTR context, stereo reset",
                LRM + "<span dir=\"rtl\">" + EN + HE + EN + "</span>" + LRM,
                LTR_FMT.spanWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL));
        assertEquals("overall dir (but not entry or exit dir) opposite to LTR context, no isolation",
                "<span dir=\"rtl\">" + EN + HE + EN + "</span>",
                LTR_FMT_EXIT_RESET.spanWrap(EN + HE + EN, TextDirectionHeuristicsCompat.RTL, false));
        assertEquals("overall dir (but not entry or exit dir) opposite to RTL context",
                "<span dir=\"ltr\">" + HE + EN + HE + "</span>" + RLM,
                RTL_FMT_EXIT_RESET.spanWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("overall dir (but not entry or exit dir) opposite to RTL context, stereo reset",
                RLM + "<span dir=\"ltr\">" + HE + EN + HE + "</span>" + RLM,
                RTL_FMT.spanWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR));
        assertEquals("overall dir (but not entry or exit dir) opposite to RTL context, no isolation",
                "<span dir=\"ltr\">" + HE + EN + HE + "</span>",
                RTL_FMT_EXIT_RESET.spanWrap(HE + EN + HE, TextDirectionHeuristicsCompat.LTR, false));
    }
}
