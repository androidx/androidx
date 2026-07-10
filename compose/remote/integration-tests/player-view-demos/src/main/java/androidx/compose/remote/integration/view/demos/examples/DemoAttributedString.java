/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples;

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB;
import static androidx.compose.remote.integration.view.demos.examples.ExampleUtils.getWriter;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.jspecify.annotations.NonNull;

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Map;

/**
 * This demonstrates the attribute string conversion into RemoteCompose Text components
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoAttributedString {
    private DemoAttributedString() {
    }

    /**
     * demo of the attribute string conversion into RemoteCompose Text components
     * @return RemoteComposeWrite    @SuppressLint("RestrictedApiAndroidX")r
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RemoteComposeWriter demo() {
        RemoteComposeWriterAndroid rc = getWriter(7);
        AttributedString as = sampleAttributedString();
        rc.root(() -> {

                    rc.startBox(new RecordingModifier()
                            .fillMaxSize().background(Color.WHITE).padding(4));
                    rc.startColumn(new RecordingModifier().fillMaxSize(), 1, 1);
                    rc.startRow(new RecordingModifier().fillMaxWidth(), RowLayout.START,
                            RowLayout.TOP);
                    extractRuns(as, (text, start, end, keyValue) -> {
                        String str = text.substring(start, end);
                        if (str.indexOf('\n') >= 0) {
                            String[] split = str.split("\n");
                            boolean notFirst = false;
                            for (String s : split) {
                                if (notFirst) {
                                    rc.endRow();
                                    rc.startRow(new RecordingModifier().fillMaxWidth(),
                                            RowLayout.START,
                                            RowLayout.TOP);
                                }
                                notFirst = true;
                                int textId = rc.textCreateId(s);
                                text(rc, textId, keyValue);
                            }

                        } else {
                            int textId = rc.textCreateId(str);
                            text(rc, textId, keyValue);
                        }
                    });
                    rc.endRow();
                    rc.endColumn();
                    rc.endBox();
                }
        );
        return rc;
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static void text(RemoteComposeWriter rc, int textId, int[] keyValues) {
        RecordingModifier m = new RecordingModifier().alignByBaseline();

        boolean underline = false;
        boolean strikethrough = false;
        int color = 0xFF000000;
        float fontSize = 46f;
        int fontStyle = 0;
        float fontWeight = 400f; // normal
        String fontFamily = null;
        int textAlign = Rc.Text.ALIGN_LEFT;
        int overflow = Rc.Text.OVERFLOW_CLIP;
        int maxLines = Integer.MAX_VALUE;
        for (int i = 0; i < keyValues.length; i += 2) {
            switch (keyValues[i]) {
                case AttributeRun.POSTURE:
                    switch (keyValues[i + 1]) {
                        case AttributeRun.POSTURE_BOLD:
                            fontWeight = 700f;
                            break;
                        case AttributeRun.POSTURE_ITALIC:
                            fontStyle = 1;
                    }
                    break;
                case AttributeRun.FOREGROUND:
                    color = keyValues[i + 1];
                    break;
                case AttributeRun.BACKGROUND:
                    m = m.background(keyValues[i + 1]);
                    break;
                case AttributeRun.UNDERLINE:
                    underline = true;
                    break;
                case AttributeRun.STRIKETHROUGH:
                    strikethrough = true;
                    break;
                case AttributeRun.SUPERSCRIPT:
                    break;
                case AttributeRun.SIZE:
                    fontSize *= Float.intBitsToFloat(keyValues[i + 1]);
                    break;
            }
        }

        rc.startTextComponent(m, textId, color, fontSize, fontStyle, fontWeight,
                fontFamily, textAlign, overflow, maxLines);
        rc.startCanvasOperations();
        rc.drawComponentContent();
        if (underline) {
            float w = rc.addComponentWidthValue();
            float base = rc.floatExpression(rc.addComponentHeightValue(), 8, SUB);
            rc.drawLine(0, base, w, base);
        }

        if (strikethrough) {
            float w = rc.addComponentWidthValue();
            float strike = rc.floatExpression(rc.addComponentHeightValue(), 0.6f, MUL);
            rc.drawLine(0, strike, w, strike);
        }
        rc.endCanvasOperations();
        rc.endTextComponent();
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static AttributedString sampleAttributedString() {

        String text = "AttributedString Demo:\n"
                + "This is Bold, this is Italic.\n"
                + "This text is Red, this is Blue,\n"
                + " and this has a Yellow Background.\n"
                + "This is Underlined, and this has a Strikethrough.\n"
                + "This is Big, and this is Superscript².";

        AttributedString attributedString = new AttributedString(text);

        try {

            int boldStart = text.indexOf("Bold");
            attributedString.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD,
                    boldStart, boldStart + "Bold".length());

            int italicStart = text.indexOf("Italic");
            attributedString.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE,
                    italicStart, italicStart + "Italic".length());

            int redStart = text.indexOf("Red");
            attributedString.addAttribute(TextAttribute.FOREGROUND, Color.RED, redStart,
                    redStart + "Red".length());

            int blueStart = text.indexOf("Blue");
            attributedString.addAttribute(TextAttribute.FOREGROUND, Color.BLUE, blueStart,
                    blueStart + "Blue".length());

            int backgroundStart = text.indexOf("Yellow Background");
            attributedString.addAttribute(TextAttribute.BACKGROUND, Color.YELLOW, backgroundStart,
                    backgroundStart + "Yellow Background".length());

            int underlineStart = text.indexOf("Underlined");
            attributedString.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON,
                    underlineStart, underlineStart + "Underlined".length());

            int strikeStart = text.indexOf("Strikethrough");
            attributedString.addAttribute(TextAttribute.STRIKETHROUGH,
                    TextAttribute.STRIKETHROUGH_ON, strikeStart,
                    strikeStart + "Strikethrough".length());

            int bigStart = text.indexOf("Big");
            attributedString.addAttribute(TextAttribute.SIZE, 2.0f, bigStart,
                    bigStart + "Big".length());

            int superStart = text.indexOf("Superscript²");
            attributedString.addAttribute(TextAttribute.SUPERSCRIPT,
                    TextAttribute.SUPERSCRIPT_SUPER, superStart + "Superscript".length(),
                    superStart + "Superscript²".length());


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return attributedString;

    }

    private interface AttributeRun {

        int POSTURE = 2; // ITALIC = 2
        int FOREGROUND = 3; // color INTEGER
        int BACKGROUND = 4; // color INTEGER
        int UNDERLINE = 5;  // 0 = off 1 = on
        int STRIKETHROUGH = 6; // 0 = off 1 = on
        int SUPERSCRIPT = 7; // 0 = off 1 = on
        int SIZE = 8; // float
        int POSTURE_BOLD = 1;
        int POSTURE_ITALIC = 2;
        //int POSTURE_NORMAL = 0;

        void textRun(String text, int start, int end, int[] keyValue);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static void extractRuns(AttributedString as, AttributeRun cb) {

        AttributedCharacterIterator iterator = as.getIterator();
        // ============ Convert to string
        StringBuilder sb = new StringBuilder();
        char c = iterator.first();
        while (c != AttributedCharacterIterator.DONE) {
            sb.append(c);
            c = iterator.next();
        }
        String text = sb.toString();
        // SpannableString spannableString = new SpannableString(text);
        iterator.first();
        while (iterator.getIndex() < iterator.getEndIndex()) {
            int runStart = iterator.getRunStart();
            int runLimit = iterator.getRunLimit();
            Map<AttributedCharacterIterator.Attribute, Object> attributes =
                    iterator.getAttributes();
            int len = attributes.size();
            int[] keyValues = new int[len * 2];
            int count = 0;
            for (Map.Entry<AttributedCharacterIterator.Attribute, Object> entry :
                    attributes.entrySet()) {
                AttributedCharacterIterator.Attribute attribute = entry.getKey();
                Object value = entry.getValue();
                if (attribute.equals(TextAttribute.WEIGHT) && value.equals(
                        TextAttribute.WEIGHT_BOLD)) {
                    keyValues[count++] = AttributeRun.POSTURE;
                    keyValues[count++] = AttributeRun.POSTURE_BOLD;
                } else if (attribute.equals(TextAttribute.POSTURE) && value.equals(
                        TextAttribute.POSTURE_OBLIQUE)) {
                    keyValues[count++] = AttributeRun.POSTURE;
                    keyValues[count++] = AttributeRun.POSTURE_ITALIC;
                } else if (attribute.equals(TextAttribute.FOREGROUND)) {
                    keyValues[count++] = AttributeRun.FOREGROUND;
                    keyValues[count++] = (Integer) value;
                } else if (attribute.equals(TextAttribute.BACKGROUND)) {
                    keyValues[count++] = AttributeRun.BACKGROUND;
                    keyValues[count++] = (Integer) value;
                } else if (attribute.equals(TextAttribute.UNDERLINE) && value.equals(
                        TextAttribute.UNDERLINE_ON)) {
                    keyValues[count++] = AttributeRun.UNDERLINE;
                    keyValues[count++] = 1;
                } else if (attribute.equals(TextAttribute.STRIKETHROUGH) && value.equals(
                        TextAttribute.STRIKETHROUGH_ON)) {
                    keyValues[count++] = AttributeRun.STRIKETHROUGH;
                    keyValues[count++] = 1;
                } else if (attribute.equals(TextAttribute.SUPERSCRIPT) && value.equals(
                        TextAttribute.SUPERSCRIPT_SUPER)) {
                    keyValues[count++] = AttributeRun.SUPERSCRIPT;
                    keyValues[count++] = 1;
                } else if (attribute.equals(TextAttribute.SIZE)) {
                    keyValues[count++] = AttributeRun.SIZE;
                    keyValues[count++] = Float.floatToRawIntBits((Float) value);
                }
            }
            cb.textRun(text, runStart, runLimit, keyValues);
            iterator.setIndex(runLimit);
        }
    }


}
