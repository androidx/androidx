/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.graphics;

import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides a utility for testing fonts
 *
 * For the purpose of testing font selection of families or fallbacks, this class provies following
 * regular font files.
 *
 * - ascii_a3em_weight100_upright.ttf
 *   'a' has 3em width and others have 1em width. The metadata has weight=100, non-italic value.
 * - ascii_b3em_weight100_italic.ttf
 *   'b' has 3em width and others have 1em width. The metadata has weight=100, italic value.
 * - ascii_c3em_weight200_upright.ttf
 *   'c' has 3em width and others have 1em width. The metadata has weight=200, non-italic value.
 * - ascii_d3em_weight200_italic.ttf
 *   'd' has 3em width and others have 1em width. The metadata has weight=200, italic value.
 * - ascii_e3em_weight300_upright.ttf
 *   'e' has 3em width and others have 1em width. The metadata has weight=300, non-italic value.
 * - ascii_f3em_weight300_italic.ttf
 *   'f' has 3em width and others have 1em width. The metadata has weight=300, italic value.
 * - ascii_g3em_weight400_upright.ttf
 *   'g' has 3em width and others have 1em width. The metadata has weight=400, non-italic value.
 * - ascii_h3em_weight400_italic.ttf
 *   'h' has 3em width and others have 1em width. The metadata has weight=400, italic value.
 * - ascii_i3em_weight500_upright.ttf
 *   'i' has 3em width and others have 1em width. The metadata has weight=500, non-italic value.
 * - ascii_j3em_weight500_italic.ttf
 *   'j' has 3em width and others have 1em width. The metadata has weight=500, italic value.
 * - ascii_k3em_weight600_upright.ttf
 *   'k' has 3em width and others have 1em width. The metadata has weight=600, non-italic value.
 * - ascii_l3em_weight600_italic.ttf
 *   'l' has 3em width and others have 1em width. The metadata has weight=600, italic value.
 * - ascii_m3em_weight700_upright.ttf
 *   'm' has 3em width and others have 1em width. The metadata has weight=700, non-italic value.
 * - ascii_n3em_weight700_italic.ttf
 *   'n' has 3em width and others have 1em width. The metadata has weight=700, italic value.
 * - ascii_o3em_weight800_upright.ttf
 *   'o' has 3em width and others have 1em width. The metadata has weight=800, non-italic value.
 * - ascii_p3em_weight800_italic.ttf
 *   'p' has 3em width and others have 1em width. The metadata has weight=800, italic value.
 * - ascii_q3em_weight900_upright.ttf
 *   'q' has 3em width and others have 1em width. The metadata has weight=900, non-italic value.
 * - ascii_r3em_weight900_italic.ttf
 *   'r' has 3em width and others have 1em width. The metadata has weight=900, italic value.
 *
 * In addition to above font files, this class provides a font collection file and a variable font
 * file.
 * - ascii.ttc
 *   The collection of above 18 fonts with above order.
 * - ascii_vf.ttf
 *   This font supports a-z characters and all characters has 1em width. This font supports 'wght',
 *   'ital' axes but no effect for the glyph width. This font also supports 'Asc[a-z]' 26 axes which
 *   makes glyph width 3em. For example, 'Asca 1.0' makes a glyph width of 'a' 3em, 'Ascb 1.0' makes
 *   a glyph width of 'b' 3em. With these axes, above font can be replicated like
 *   - 'Asca' 1.0, 'wght' 100.0' is equivalent with ascii_a3em_width100_upright.ttf
 *   - 'Ascb' 1.0, 'wght' 100.0, 'ital' 1.0' is equivalent with ascii_b3em_width100_italic.ttf
 */
public class FontTestUtil {
    private static final String FAMILY_SELECTION_FONT_PATH_IN_ASSET = "fonts_for_family_selection";
    private static final List<Pair<Integer, Boolean>> sStyleList;
    private static final Map<Pair<Integer, Boolean>, String> sFontMap;
    private static final Map<Pair<Integer, Boolean>, Integer> sTtcMap;
    private static final Map<Pair<Integer, Boolean>, String> sVariationSettingsMap;
    private static final Map<Character, Pair<Integer, Boolean>> sReverseMap;
    private static final String[] sFontList = {  // Same order of ascii.ttc
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_a3em_weight100_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_b3em_weight100_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_c3em_weight200_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_d3em_weight200_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_e3em_weight300_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_f3em_weight300_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_g3em_weight400_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_h3em_weight400_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_i3em_weight500_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_j3em_weight500_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_k3em_weight600_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_l3em_weight600_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_m3em_weight700_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_n3em_weight700_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_o3em_weight800_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_p3em_weight800_italic.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_q3em_weight900_upright.ttf",
            FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/fonts/ascii_r3em_weight900_italic.ttf",
    };

    private static final char[] THREE_EM_CHAR_LIST = {
            'a',  // 3em char of ascii_a3em_weight100_upright.ttf
            'b',  // 3em char of ascii_b3em_weight100_italic.ttf
            'c',  // 3em char of ascii_c3em_weight200_upright.ttf
            'd',  // 3em char of ascii_d3em_weight200_italic.ttf
            'e',  // 3em char of ascii_e3em_weight300_upright.ttf
            'f',  // 3em char of ascii_f3em_weight300_italic.ttf
            'g',  // 3em char of ascii_g3em_weight400_upright.ttf
            'h',  // 3em char of ascii_h3em_weight400_italic.ttf
            'i',  // 3em char of ascii_i3em_weight500_upright.ttf
            'j',  // 3em char of ascii_j3em_weight500_italic.ttf
            'k',  // 3em char of ascii_k3em_weight600_upright.ttf
            'l',  // 3em char of ascii_l3em_weight600_italic.ttf
            'm',  // 3em char of ascii_m3em_weight700_upright.ttf
            'n',  // 3em char of ascii_n3em_weight700_italic.ttf
            'o',  // 3em char of ascii_o3em_weight800_upright.ttf
            'p',  // 3em char of ascii_p3em_weight800_italic.ttf
            'q',  // 3em char of ascii_q3em_weight900_upright.ttf
            'r',  // 3em char of ascii_r3em_weight900_italic.ttf
    };

    private static final String[] FONT_VARIATION_SETTING_LIST = {
            "'Asca' 1.0, 'wght' 100.0",
            "'Ascb' 1.0, 'wght' 100.0, 'ital' 1.0",
            "'Ascc' 1.0, 'wght' 200.0",
            "'Ascd' 1.0, 'wght' 200.0, 'ital' 1.0",
            "'Asce' 1.0, 'wght' 300.0",
            "'Ascf' 1.0, 'wght' 300.0, 'ital' 1.0",
            "'Ascg' 1.0, 'wght' 400.0",
            "'Asch' 1.0, 'wght' 400.0, 'ital' 1.0",
            "'Asci' 1.0, 'wght' 500.0",
            "'Ascj' 1.0, 'wght' 500.0, 'ital' 1.0",
            "'Asck' 1.0, 'wght' 600.0",
            "'Ascl' 1.0, 'wght' 600.0, 'ital' 1.0",
            "'Ascm' 1.0, 'wght' 700.0",
            "'Ascn' 1.0, 'wght' 700.0, 'ital' 1.0",
            "'Asco' 1.0, 'wght' 800.0",
            "'Ascp' 1.0, 'wght' 800.0, 'ital' 1.0",
            "'Ascq' 1.0, 'wght' 900.0",
            "'Ascr' 1.0, 'wght' 900.0, 'ital' 1.0",
    };

    static {
        // Style list with the same order of sFontList.
        ArrayList<Pair<Integer, Boolean>> styles = new ArrayList<>();
        styles.add(new Pair<>(100, false));
        styles.add(new Pair<>(100, true));
        styles.add(new Pair<>(200, false));
        styles.add(new Pair<>(200, true));
        styles.add(new Pair<>(300, false));
        styles.add(new Pair<>(300, true));
        styles.add(new Pair<>(400, false));
        styles.add(new Pair<>(400, true));
        styles.add(new Pair<>(500, false));
        styles.add(new Pair<>(500, true));
        styles.add(new Pair<>(600, false));
        styles.add(new Pair<>(600, true));
        styles.add(new Pair<>(700, false));
        styles.add(new Pair<>(700, true));
        styles.add(new Pair<>(800, false));
        styles.add(new Pair<>(800, true));
        styles.add(new Pair<>(900, false));
        styles.add(new Pair<>(900, true));
        sStyleList = Collections.unmodifiableList(styles);

        HashMap<Pair<Integer, Boolean>, String> map = new HashMap<>();
        HashMap<Pair<Integer, Boolean>, Integer> ttcMap = new HashMap<>();
        HashMap<Pair<Integer, Boolean>, String> variationMap = new HashMap<>();
        HashMap<Character, Pair<Integer, Boolean>> reverseMap = new HashMap<>();
        for (int i = 0; i < sFontList.length; ++i) {
            map.put(sStyleList.get(i), sFontList[i]);
            ttcMap.put(sStyleList.get(i), i);
            variationMap.put(sStyleList.get(i), FONT_VARIATION_SETTING_LIST[i]);
            reverseMap.put(THREE_EM_CHAR_LIST[i], sStyleList.get(i));
        }
        sFontMap = Collections.unmodifiableMap(map);
        sTtcMap = Collections.unmodifiableMap(ttcMap);
        sVariationSettingsMap = Collections.unmodifiableMap(variationMap);
        sReverseMap = Collections.unmodifiableMap(reverseMap);
    }

    private static final float DEFAULT_TEXT_SIZE = 10.0f;
    private static final float DEFAULT_1EM_WIDTH = 10.0f;
    private static final float DEFAULT_3EM_WIDTH = 30.0f;

    private static float measureText(char c, Typeface tf) {
        Paint paint = new Paint();
        paint.setTextSize(DEFAULT_TEXT_SIZE);
        paint.setTypeface(tf);
        paint.setTextLocale(Locale.US);
        return paint.measureText(Character.toString(c));
    }

    /**
     * Returns a path to the font collection file in asset directory.
     */
    public static String getTtcFontFileInAsset() {
        return FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/ascii.ttc";
    }

    /**
     * Returns a path to the variable font file in asset directory.
     */
    public static String getVFFontInAsset() {
        return FAMILY_SELECTION_FONT_PATH_IN_ASSET + "/ascii_vf.ttf";
    }

    /**
     * Returns a ttc index of the specified style.
     */
    public static int getTtcIndexFromStyle(int weight, boolean italic) {
        return sTtcMap.get(new Pair<>(weight, italic)).intValue();
    }

    /**
     * Returns a variation settings string of the specified style.
     */
    public static String getVarSettingsFromStyle(int weight, boolean italic) {
        return sVariationSettingsMap.get(new Pair<>(weight, italic));
    }

    /**
     * Returns a font path from the specified style.
     */
    public static String getFontPathFromStyle(int weight, boolean italic) {
        return sFontMap.get(new Pair<>(weight, italic));
    }

    /**
     * Returns selected font style.
     */
    public static Pair<Integer, Boolean> getSelectedFontStyle(Typeface typeface) {
        for (char c = 'a'; c <= 'r'; c++) {
            float w = measureText(c, typeface);
            if (w == DEFAULT_3EM_WIDTH) {
                return sReverseMap.get(c);
            } else if (w != DEFAULT_1EM_WIDTH) {
                throw new RuntimeException("Unknown width for the character.");
            }
        }
        throw new RuntimeException("Unknown Typeface");
    }

    /**
     * Returns all supported styles.
     */
    public static List<Pair<Integer, Boolean>> getAllStyles() {
        return sStyleList;
    }
}
