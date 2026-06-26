/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation;

import static org.junit.Assert.assertArrayEquals;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemotePathBase;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.Custom;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.MatrixOperations;
import androidx.compose.remote.creation.actions.ValueFloatChange;
import androidx.compose.remote.creation.dsl.RcFloat;
import androidx.compose.remote.creation.dsl.VerticalScrollRcFloatModifier;
import androidx.compose.remote.creation.json.RemoteComposeJsonParser;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.json.JSONException;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoteComposeComparisonTest {

    @Test
    public void testCube3DFullComparison() throws JSONException {
        MockPlatform platform = new MockPlatform();
        RemoteComposeWriter.HTag[] tags = new RemoteComposeWriter.HTag[] {
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Cube"),
            RemoteComposeWriter.hTag(Header.DOC_DESIRED_FPS, 120)
        };
        java.util.Arrays.sort(tags, (a, b) -> Short.compare(a.mTag, b.mTag));
        RemoteComposeWriter expectedWriter = new RemoteComposeWriter(platform, 7, tags);
        generateFullCubeKotlin(expectedWriter);
        byte[] expected = expectedWriter.encodeToByteArray();

        RemoteComposeWriter.HTag[] actualTags =
                RemoteComposeJsonParser.parseHeaderOnly(getFullCubeJson());
        int actualApiLevel = RemoteComposeJsonParser.parseApiLevel(getFullCubeJson());
        RemoteComposeWriter actualWriter =
                new RemoteComposeWriter(platform, actualApiLevel, actualTags);
        RemoteComposeJsonParser parser = new RemoteComposeJsonParser(actualWriter);
        parser.parse(getFullCubeJson());
        byte[] actual = actualWriter.encodeToByteArray();

        System.out.println("FullCube expected size: " + expected.length);
        System.out.println("FullCube actual size: " + actual.length);

        if (!Arrays.equals(expected, actual)) {
            printMismatch("FullCube", expected, actual);
        }
        assertArrayEquals(expected, actual);
    }

    private void generateFullCubeKotlin(RemoteComposeWriter writer) {
        float width = writer.addComponentWidthValue();
        float centerX = writer.floatExpression(width, 2f, AnimatedFloatExpression.DIV);
        writer.setFloatName(Utils.idFromNan(centerX), "centerX");
        float height = writer.addComponentHeightValue();
        float centerY = writer.floatExpression(height, 2f, AnimatedFloatExpression.DIV);
        writer.setFloatName(Utils.idFromNan(centerY), "centerY");
        float radius = writer.floatExpression(centerX, centerY, AnimatedFloatExpression.MIN);
        writer.setFloatName(Utils.idFromNan(radius), "radius");

        // Variables for rotation
        float time = RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC;
        float time2 = writer.floatExpression(time, 2f, AnimatedFloatExpression.MUL);
        writer.setFloatName(Utils.idFromNan(time2), "time2");
        float rot = writer.floatExpression(time2, 20f, AnimatedFloatExpression.MUL, 360f,
                AnimatedFloatExpression.MOD);
        writer.setFloatName(Utils.idFromNan(rot), "rot");
        float t1_base = writer.floatExpression(time2, 18f, AnimatedFloatExpression.DIV);
        writer.setFloatName(Utils.idFromNan(t1_base), "t1_base");
        float t1 = writer.floatExpression(t1_base, AnimatedFloatExpression.ROUND, 1f,
                AnimatedFloatExpression.ADD, 3f, AnimatedFloatExpression.MOD,
                AnimatedFloatExpression.SIGN);
        writer.setFloatName(Utils.idFromNan(t1), "t1");
        float t2 = writer.floatExpression(t1_base, AnimatedFloatExpression.ROUND, 3f,
                AnimatedFloatExpression.MOD, AnimatedFloatExpression.SIGN);
        writer.setFloatName(Utils.idFromNan(t2), "t2");
        float rotX = writer.floatExpression(rot, t1, AnimatedFloatExpression.MUL);
        writer.setFloatName(Utils.idFromNan(rotX), "rotX");
        float rotY = writer.floatExpression(rot, t2, AnimatedFloatExpression.MUL);
        writer.setFloatName(Utils.idFromNan(rotY), "rotY");

        float worldId = writer.matrixExpression(6f, MatrixOperations.TRANSLATE_Z, rotX,
                MatrixOperations.ROT_X, rotY, MatrixOperations.ROT_Y);
        float pMatrixId = writer.matrixExpression(60f, 1f, 0.1f, 100f, MatrixOperations.PROJECTION,
                writer.floatExpression(centerX, 0.4f, AnimatedFloatExpression.MUL),
                writer.floatExpression(centerX, -0.4f, AnimatedFloatExpression.MUL),
                MatrixOperations.SCALE2);

        writer.root(() -> {
            RecordingModifier boxMod = new RecordingModifier();
            boxMod.fillMaxWidth(1.0f).fillMaxHeight(1.0f);
            writer.startBox(boxMod, BoxLayout.CENTER, BoxLayout.CENTER);

            RecordingModifier canvasMod = new RecordingModifier();
            canvasMod.fillMaxWidth(1.0f).fillMaxHeight(1.0f);
            writer.startCanvas(canvasMod);

            writer.getRcPaint().setColor(0xFF444444).commit();
            writer.getRcPaint().setStyle(1).commit();
            writer.drawCircle(centerX, centerY, radius);

            writer.getRcPaint().setColor(0xFFD3D3D3).commit();

            float[] v0 = new float[3];
            v0[0] = writer.createNamedVariable("v0x", NamedVariable.FLOAT_TYPE);
            v0[1] = writer.createNamedVariable("v0y", NamedVariable.FLOAT_TYPE);
            v0[2] = writer.createNamedVariable("v0z", NamedVariable.FLOAT_TYPE);
            writer.addMatrixMultiply(worldId, (short) 0, new float[]{-1, -1, -1}, v0);

            float[] t0 = new float[3];
            t0[0] = writer.createNamedVariable("t0x", NamedVariable.FLOAT_TYPE);
            t0[1] = writer.createNamedVariable("t0y", NamedVariable.FLOAT_TYPE);
            t0[2] = writer.createNamedVariable("t0z", NamedVariable.FLOAT_TYPE);
            writer.addMatrixMultiply(pMatrixId, (short) 1, v0, t0);

            int f0 = writer.pathCreate(
                    writer.floatExpression(t0[0], centerX, AnimatedFloatExpression.ADD),
                    writer.floatExpression(t0[1], centerY, AnimatedFloatExpression.ADD));
            writer.pathAppendClose(f0);

            writer.conditionalOperations((byte) ConditionalOperations.TYPE_GT, rotX, 0.5f, () -> {
                writer.drawCircle(200f, 200f, 50f);
            });

            writer.endCanvas();
            writer.endBox();
        });
    }

    private String getFullCubeJson() {
        return "{"
                + "  \"header\": { \"apiLevel\": 7, \"width\": 400, \"height\": 400,"
                + " \"contentDescription\": \"Cube\", \"fps\": 120 },"
                + "  \"resources\": {"
                + "    \"order\": [\"variables\", \"matrices\"],"
                + "    \"variables\": ["
                + "      { \"name\": \"centerX\", \"value\": \"width / 2\", \"export\": true },"
                + "      { \"name\": \"centerY\", \"value\": \"height / 2\", \"export\": true },"
                + "      { \"name\": \"radius\","
                + " \"value\": \"min(@vars.centerX, @vars.centerY)\", \"export\": true },"
                + "      { \"name\": \"time2\", \"value\": \"time * 2\", \"export\": true },"
                + "      { \"name\": \"rot\", \"value\": \"(@vars.time2 * 20) % 360\","
                + " \"export\": true },"
                + "      { \"name\": \"t1_base\", \"value\": \"@vars.time2 / 18\","
                + " \"export\": true },"
                + "      { \"name\": \"t1\","
                + " \"value\": \"sign((round(@vars.t1_base) + 1) % 3)\", \"export\": true },"
                + "      { \"name\": \"t2\","
                + " \"value\": \"sign(round(@vars.t1_base) % 3)\", \"export\": true },"
                + "      { \"name\": \"rotX\", \"value\": \"@vars.rot * @vars.t1\","
                + " \"export\": true },"
                + "      { \"name\": \"rotY\", \"value\": \"@vars.rot * @vars.t2\","
                + " \"export\": true }"
                + "    ],"
                + "    \"matrices\": ["
                + "      { \"name\": \"world\", \"value\": [ 6, \"matrix:TRANSLATE_Z\","
                + " \"@vars.rotX\", \"matrix:ROT_X\", \"@vars.rotY\", \"matrix:ROT_Y\" ] },"
                + "      { \"name\": \"pMatrix\", \"value\": [ 60, 1, 0.1, 100,"
                + " \"matrix:PROJECTION\", \"@vars.centerX * 0.4\","
                + " \"@vars.centerX * -0.4\", \"matrix:SCALE2\" ] }"
                + "    ]"
                + "  },"
                + "  \"root\": {"
                + "    \"type\": \"box\","
                + "    \"horizontalAlignment\": \"center\","
                + "    \"verticalAlignment\": \"center\","
                + "    \"modifiers\": ["
                + "      { \"fillMaxWidth\": 1.0 },"
                + "      { \"fillMaxHeight\": 1.0 }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"canvas\","
                + "        \"modifiers\": ["
                + "          { \"fillMaxWidth\": 1.0 },"
                + "          { \"fillMaxHeight\": 1.0 }"
                + "        ],"
                + "        \"commands\": ["
                + "          { \"type\": \"setColor\", \"color\": \"#444444\" },"
                + "          { \"type\": \"setStyle\", \"style\": \"stroke\" },"
                + "          { \"type\": \"drawCircle\", \"cx\": \"@vars.centerX\","
                + " \"cy\": \"@vars.centerY\", \"radius\": \"@vars.radius\" },"
                + "          { \"type\": \"setColor\", \"color\": \"#D3D3D3\" },"
                + "          { \"type\": \"matrixMultiply\", \"matrix\": \"@matrices.world\","
                + " \"mType\": 0, \"from\": [ -1, -1, -1 ],"
                + " \"out\": [ \"v0x\", \"v0y\", \"v0z\" ] },"
                + "          { \"type\": \"matrixMultiply\", \"matrix\": \"@matrices.pMatrix\","
                + " \"mType\": 1, \"from\": [ \"@vars.v0x\", \"@vars.v0y\", \"@vars.v0z\" ],"
                + " \"out\": [ \"t0x\", \"t0y\", \"t0z\" ] },"
                + "          { \"type\": \"pathCreate\", \"x\": \"@vars.t0x + @vars.centerX\","
                + " \"y\": \"@vars.t0y + @vars.centerY\", \"id\": \"f0\" },"
                + "          { \"type\": \"pathAppendClose\", \"path\": \"@paths.f0\" },"
                + "          {"
                + "            \"type\": \"conditionalOperations\","
                + "            \"condition\": \"gt\","
                + "            \"v1\": \"@vars.rotX\","
                + "            \"v2\": 0.5,"
                + "            \"commands\": ["
                + "              { \"type\": \"drawCircle\", \"cx\": 200, \"cy\": 200,"
                + " \"radius\": 50 }"
                + "            ]"
                + "          }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
    }

    @Test
    public void testTickerComparison() throws JSONException {
        MockPlatform platform = new MockPlatform();
        RemoteComposeWriter.HTag[] tags = new RemoteComposeWriter.HTag[] {
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 800),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Ticker"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, 769)
        };
        java.util.Arrays.sort(tags, (a, b) -> Short.compare(a.mTag, b.mTag));
        RemoteComposeWriter expectedWriter = new RemoteComposeWriter(platform, 7, tags);
        generateTickerKotlin(expectedWriter);
        byte[] expected = expectedWriter.encodeToByteArray();

        RemoteComposeWriter.HTag[] actualTags =
                RemoteComposeJsonParser.parseHeaderOnly(getTickerJson());
        int actualApiLevel = RemoteComposeJsonParser.parseApiLevel(getTickerJson());
        RemoteComposeWriter actualWriter =
                new RemoteComposeWriter(platform, actualApiLevel, actualTags);
        RemoteComposeJsonParser parser = new RemoteComposeJsonParser(actualWriter);
        parser.parse(getTickerJson());
        byte[] actual = actualWriter.encodeToByteArray();

        System.out.println("Ticker expected size: " + expected.length);
        System.out.println("Ticker actual size: " + actual.length);

        if (!Arrays.equals(expected, actual)) {
            printMismatch("Ticker", expected, actual);
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testStage1Comparison() throws JSONException {
        MockPlatform platform = new MockPlatform();
        RemoteComposeWriter.HTag[] tags = new RemoteComposeWriter.HTag[] {
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 800),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Stage1"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, 769)
        };
        java.util.Arrays.sort(tags, (a, b) -> Short.compare(a.mTag, b.mTag));
        RemoteComposeWriter expectedWriter = new RemoteComposeWriter(platform, 7, tags);

        expectedWriter.root(() -> {
            RecordingModifier mod = new RecordingModifier();
            mod.collapsiblePriority(1 /* VERTICAL */, 2.0f);
            mod.verticalScroll(10.0f, 5);
            expectedWriter.startFlow(mod, BoxLayout.START, BoxLayout.TOP, 4, 2);

            int strId = expectedWriter.textCreateId("Hello Stage 1");
            expectedWriter.startTextComponent(
                    new RecordingModifier(),
                    strId,
                    -1, // textStyleId
                    0xFF000000,
                    -1,
                    18f,
                    -1f, // minFontSize
                    -1f, // maxFontSize
                    1, // fontStyle italic
                    400f,
                    "sans-serif", // fontFamily
                    1, // textAlign start
                    1, // overflow clip
                    Integer.MAX_VALUE, // maxLines
                    1.5f, // letterSpacing
                    2.0f, // lineHeightAdd
                    1.2f, // lineHeightMultiplier
                    0, // lineBreakStrategy
                    0, // hyphenationFrequency
                    0, // justificationMode
                    true, // underline
                    true, // strikethrough
                    null, // fontAxis
                    null, // fontAxisValues
                    true, // autosize
                    0 // flags
            );
            expectedWriter.endTextComponent();
            expectedWriter.endFlow();
        });
        byte[] expected = expectedWriter.encodeToByteArray();

        String json = "{"
                + "  \"header\": { \"apiLevel\": 7, \"profiles\": 769,"
                + " \"width\": 400, \"height\": 800,"
                + " \"contentDescription\": \"Stage1\" },"
                + "  \"root\": {"
                + "    \"type\": \"flow\","
                + "    \"maxColumns\": 4,"
                + "    \"maxLines\": 2,"
                + "    \"modifiers\": ["
                + "      { \"collapsiblePriority\": { \"orientation\": \"vertical\","
                + " \"priority\": 2.0 } },"
                + "      { \"verticalScroll\": { \"position\": 10.0, \"notches\": 5 } }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"text\","
                + "        \"value\": \"Hello Stage 1\","
                + "        \"fontSize\": 18,"
                + "        \"fontStyle\": \"italic\","
                + "        \"fontFamily\": \"sans-serif\","
                + "        \"textAlign\": \"left\","
                + "        \"letterSpacing\": 1.5,"
                + "        \"lineHeightAdd\": 2.0,"
                + "        \"lineHeightMultiplier\": 1.2,"
                + "        \"underline\": true,"
                + "        \"strikethrough\": true,"
                + "        \"autoSize\": true"
                + "      }"
                + "    ]"
                + "  }"
                + "}";

        RemoteComposeWriter.HTag[] actualTags = RemoteComposeJsonParser.parseHeaderOnly(json);
        int actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json);
        RemoteComposeWriter actualWriter =
                new RemoteComposeWriter(platform, actualApiLevel, actualTags);
        RemoteComposeJsonParser parser = new RemoteComposeJsonParser(actualWriter);
        parser.parse(json);
        byte[] actual = actualWriter.encodeToByteArray();

        if (!java.util.Arrays.equals(expected, actual)) {
            printMismatch("Stage1", expected, actual);
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testStage2Comparison() throws JSONException {
        MockPlatform platform = new MockPlatform();
        RemoteComposeWriter.HTag[] tags = new RemoteComposeWriter.HTag[] {
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 800),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Stage2"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, 769)
        };
        java.util.Arrays.sort(tags, (a, b) -> Short.compare(a.mTag, b.mTag));
        RemoteComposeWriter expectedWriter = new RemoteComposeWriter(platform, 7, tags);
        float targetIdFloat = expectedWriter.addNamedFloat("val", 0.0f);
        int targetId = Utils.idFromNan(targetIdFloat);

        expectedWriter.root(() -> {
            RecordingModifier mod = new RecordingModifier();
            mod.onTouchDown(new ValueFloatChange(targetId, 1.0f));
            mod.onTouchUp(new ValueFloatChange(targetId, 0.0f));
            mod.onTouchCancel(new ValueFloatChange(targetId, 0.0f));
            expectedWriter.startBox(mod);

            expectedWriter.startCanvas(new RecordingModifier());
            expectedWriter.performHaptic(1);
            int clickSoundId = expectedWriter.textCreateId("clickSound");
            expectedWriter.playSound(clickSoundId);

            int fullTxtId = expectedWriter.textCreateId("Full String");
            int subId = expectedWriter.textSubtext(fullTxtId, 0.0f, 4.0f);
            expectedWriter.textTransform(fullTxtId, 0.0f, 4.0f, 1 /* UPPERCASE */);

            expectedWriter.endCanvas();
            expectedWriter.endBox();
        });
        byte[] expected = expectedWriter.encodeToByteArray();

        String json = "{"
                + "  \"header\": { \"apiLevel\": 7, \"profiles\": 769, \"width\": 400,"
                + " \"height\": 800, \"contentDescription\": \"Stage2\" },"
                + "  \"resources\": {"
                + "    \"variables\": ["
                + "      { \"name\": \"val\", \"value\": 0.0, \"export\": true }"
                + "    ]"
                + "  },"
                + "  \"root\": {"
                + "    \"type\": \"box\","
                + "    \"modifiers\": ["
                + "      { \"onTouchDown\": { \"type\": \"ValueFloatChange\", \"targetId\":"
                + " \"@vars.val\", \"value\": 1.0 } },"
                + "      { \"onTouchUp\": { \"type\": \"ValueFloatChange\", \"targetId\":"
                + " \"@vars.val\", \"value\": 0.0 } },"
                + "      { \"onTouchCancel\": { \"type\": \"ValueFloatChange\", \"targetId\":"
                + " \"@vars.val\", \"value\": 0.0 } }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"canvas\","
                + "        \"commands\": ["
                + "          { \"type\": \"performHaptic\", \"constant\": 1 },"
                + "          { \"type\": \"playSound\", \"id\": \"clickSound\" },"
                + "          { \"type\": \"textSubtext\", \"text\": \"Full String\","
                + " \"start\": 0.0, \"len\": 4.0 },"
                + "          { \"type\": \"textTransform\", \"text\": \"Full String\","
                + " \"start\": 0.0, \"len\": 4.0, \"operation\": \"uppercase\" }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";

        RemoteComposeWriter.HTag[] actualTags = RemoteComposeJsonParser.parseHeaderOnly(json);
        int actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json);
        RemoteComposeWriter actualWriter =
                new RemoteComposeWriter(platform, actualApiLevel, actualTags);
        RemoteComposeJsonParser parser = new RemoteComposeJsonParser(actualWriter);
        parser.parse(json);
        byte[] actual = actualWriter.encodeToByteArray();

        if (!java.util.Arrays.equals(expected, actual)) {
            printMismatch("Stage2", expected, actual);
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testStage3Comparison() throws JSONException {
        MockPlatform platform = new MockPlatform();
        RemoteComposeWriter.HTag[] tags = new RemoteComposeWriter.HTag[] {
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 800),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Stage3"),
            RemoteComposeWriter.hTag(Header.DOC_PROFILES, 513)
        };
        java.util.Arrays.sort(tags, (a, b) -> Short.compare(a.mTag, b.mTag));
        RemoteComposeWriter expectedWriter = new RemoteComposeWriter(platform, 7, tags);
        float xVal = expectedWriter.addFloatConstant(0.0f);

        expectedWriter.root(() -> {
            RecordingModifier mod = new RecordingModifier();
            mod.drawWithContent();
            List<Custom.CustomProperty> props = new ArrayList<>();
            props.add(new Custom.CustomProperty((short) 1, (short) 0, 10));
            props.add(new Custom.CustomProperty((short) 2, (short) 1, 3.14f));
            expectedWriter.startCustom(mod, "testConfig", props);

            expectedWriter.startCanvas(new RecordingModifier());
            expectedWriter.wakeIn(2.5f);
            expectedWriter.particlesComparison(1.0f, (short) 0, 0.0f, 10.0f,
                    new float[] { xVal },
                    new float[][] { new float[] { xVal } }, null);

            expectedWriter.endCanvas();
            expectedWriter.endCustom();
        });
        byte[] expected = expectedWriter.encodeToByteArray();

        String json = "{"
                + "  \"header\": { \"apiLevel\": 7, \"profiles\": 513, \"width\": 400,"
                + " \"height\": 800, \"contentDescription\": \"Stage3\" },"
                + "  \"resources\": {"
                + "    \"variables\": ["
                + "      { \"name\": \"x\", \"value\": 0.0 }"
                + "    ]"
                + "  },"
                + "  \"root\": {"
                + "    \"type\": \"custom\","
                + "    \"config\": \"testConfig\","
                + "    \"properties\": ["
                + "      { \"type\": 1, \"dataType\": 0, \"value\": 10 },"
                + "      { \"type\": 2, \"dataType\": 1, \"value\": 3.14 }"
                + "    ],"
                + "    \"modifiers\": ["
                + "      { \"drawWithContent\": {} }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"canvas\","
                + "        \"commands\": ["
                + "          { \"type\": \"wakeIn\", \"seconds\": 2.5 },"
                + "          { \"type\": \"particlesComparison\", \"systemId\": 1.0,"
                + " \"flags\": 0, \"min\": 0.0, \"max\": 10.0, \"condition\": \"x\","
                + " \"then1\": [\"x\"] }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";

        RemoteComposeWriter.HTag[] actualTags = RemoteComposeJsonParser.parseHeaderOnly(json);
        int actualApiLevel = RemoteComposeJsonParser.parseApiLevel(json);
        RemoteComposeWriter actualWriter =
                new RemoteComposeWriter(platform, actualApiLevel, actualTags);
        RemoteComposeJsonParser parser = new RemoteComposeJsonParser(actualWriter);
        parser.parse(json);
        byte[] actual = actualWriter.encodeToByteArray();

        if (!java.util.Arrays.equals(expected, actual)) {
            printMismatch("Stage3", expected, actual);
        }
        assertArrayEquals(expected, actual);
    }

    private void generateTickerKotlin(RemoteComposeWriter writer) {
        writer.beginGlobal();
        int bgId = writer.addThemedColor(null, 0xFFEEEEEE, null, 0xFF111111);
        writer.setColorName(bgId, "bg");
        int textId = writer.createTextFromFloat(123.45f, 0, 2, 0);
        writer.setStringName(textId, "priceText");

        writer.root(() -> {
            writer.endGlobal();
            RecordingModifier rootMod = new RecordingModifier();
            rootMod.fillMaxSize(1.0f).backgroundId((short) bgId);
            writer.startColumn(rootMod, BoxLayout.START, BoxLayout.TOP);

            RecordingModifier scrollMod = new RecordingModifier();
            scrollMod.fillMaxWidth(1.0f).height(400f);
            scrollMod.then(new VerticalScrollRcFloatModifier(new RcFloat(writer, 0f)));
            writer.startColumn(scrollMod, BoxLayout.START, BoxLayout.TOP);

            writer.startTextComponent(
                    new RecordingModifier(),
                    textId,
                    -1, // textStyleId
                    0xFF000000,
                    -1,
                    24f,
                    -1f, // minFontSize
                    -1f, // maxFontSize
                    0, // fontStyle
                    400f,
                    null, // fontFamily
                    1, // textAlign start
                    1, // overflow clip
                    Integer.MAX_VALUE, // maxLines
                    0f, // letterSpacing
                    0f, // lineHeightAdd
                    1f, // lineHeightMultiplier
                    0, // lineBreakStrategy
                    0, // hyphenationFrequency
                    0, // justificationMode
                    false, // underline
                    false, // strikethrough
                    null, // fontAxis
                    null, // fontAxisValues
                    false, // autosize
                    (short) 0 // flags
            );
            writer.endTextComponent();

            RecordingModifier canvasMod = new RecordingModifier();
            canvasMod.fillMaxWidth(1.0f).height(100f);
            writer.startCanvas(canvasMod);
            int pathId = writer.pathCreate(0f, 100f);
            int indexTextId = writer.textCreateId("idx");
            float index = Utils.asNan(indexTextId);
            writer.startLoop(indexTextId, 0f, 10f, 100f);
            writer.pathAppendLineTo(pathId, index,
                    writer.floatExpression(index, index, AnimatedFloatExpression.MUL, 0.01f,
                            AnimatedFloatExpression.MUL));
            writer.endLoop();
            writer.getRcPaint().setColor(0xFFFF0000).commit();
            writer.getRcPaint().setStyle(1).commit();
            writer.getRcPaint().setStrokeWidth(2f).commit();
            writer.drawPath(pathId);
            writer.endCanvas();

            writer.endColumn();
            writer.endColumn();
        });
    }

    private String getTickerJson() {
        return "{"
                + "  \"header\": { \"apiLevel\": 7, \"profiles\": 769,"
                + " \"width\": 400, \"height\": 800,"
                + " \"contentDescription\": \"Ticker\" },"
                + "  \"resources\": {"
                + "    \"beginGlobal\": true,"
                + "    \"colors\": ["
                + "      { \"name\": \"bg\", \"value\": { \"light\": \"#EEEEEE\","
                + " \"dark\": \"#111111\" } }"
                + "    ],"
                + "    \"variables\": ["
                + "      { \"name\": \"priceText\", \"value\": { \"type\": \"textFromFloat\","
                + " \"value\": 123.45, \"after\": 2 }, \"export\": true }"
                + "    ]"
                + "  },"
                + "  \"root\": {"
                + "    \"type\": \"column\","
                + "    \"horizontalAlignment\": \"start\","
                + "    \"verticalAlignment\": \"top\","
                + "    \"modifiers\": ["
                + "      { \"fillMaxSize\": 1.0 },"
                + "      { \"background\": \"@colors.bg\" }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"column\","
                + "        \"horizontalAlignment\": \"start\","
                + "        \"verticalAlignment\": \"top\","
                + "        \"modifiers\": ["
                + "          { \"fillMaxWidth\": 1.0 },"
                + "          { \"height\": 400 },"
                + "          { \"verticalScroll\": 0.0 }"
                + "        ],"
                + "        \"children\": ["
                + "          {"
                + "            \"type\": \"text\","
                + "            \"value\": \"@vars.priceText\","
                + "            \"fontSize\": 24,"
                + "            \"textAlign\": \"left\","
                + "            \"overflow\": \"clip\""
                + "          },"
                + "          {"
                + "            \"type\": \"canvas\","
                + "            \"modifiers\": ["
                + "          { \"fillMaxWidth\": 1.0 },"
                + "          { \"height\": 100 }"
                + "            ],"
                + "            \"commands\": ["
                + "          { \"type\": \"pathCreate\", \"x\": 0, \"y\": 100, \"id\": \"p1\" },"
                + "          {"
                + "            \"type\": \"loop\","
                + "            \"from\": 0, \"step\": 10, \"until\": 100,"
                + "            \"index\": \"idx\","
                + "            \"commands\": ["
                + "              { \"type\": \"pathAppendLineTo\", \"path\": \"@paths.p1\","
                + " \"x\": \"@vars.idx\", \"y\": \"@vars.idx * @vars.idx * 0.01\" }"
                + "            ]"
                + "          },"
                + "          { \"type\": \"setColor\", \"color\": \"#FF0000\" },"
                + "          { \"type\": \"setStyle\", \"style\": \"stroke\" },"
                + "          { \"type\": \"setStrokeWidth\", \"width\": 2 },"
                + "          { \"type\": \"drawPath\", \"path\": \"@paths.p1\" }"
                + "            ]"
                + "          }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
    }

    @Test
    public void testFitBoxComparison() throws JSONException {
        MockPlatform platform = new MockPlatform();
        RemoteComposeWriter.HTag[] tags = new RemoteComposeWriter.HTag[] {
            RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
            RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
            RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "FitBox")
        };
        java.util.Arrays.sort(tags, (a, b) -> Short.compare(a.mTag, b.mTag));
        RemoteComposeWriter expectedWriter = new RemoteComposeWriter(platform, 7, tags);
        generateFitBoxKotlin(expectedWriter);
        byte[] expected = expectedWriter.encodeToByteArray();

        RemoteComposeWriter.HTag[] actualTags =
                RemoteComposeJsonParser.parseHeaderOnly(getFitBoxJson());
        int actualApiLevel = RemoteComposeJsonParser.parseApiLevel(getFitBoxJson());
        RemoteComposeWriter actualWriter =
                new RemoteComposeWriter(platform, actualApiLevel, actualTags);
        RemoteComposeJsonParser parser = new RemoteComposeJsonParser(actualWriter);
        parser.parse(getFitBoxJson());
        byte[] actual = actualWriter.encodeToByteArray();

        System.out.println("FitBox expected size: " + expected.length);
        System.out.println("FitBox actual size: " + actual.length);

        if (!Arrays.equals(expected, actual)) {
            printMismatch("FitBox", expected, actual);
        }
        assertArrayEquals(expected, actual);
    }

    private void generateFitBoxKotlin(RemoteComposeWriter writer) {
        writer.root(() -> {
            RecordingModifier mod = new RecordingModifier();
            mod.fillMaxSize(1.0f).background(0xFF0000FF);
            writer.startFitBox(mod, BoxLayout.CENTER, BoxLayout.CENTER);

            writer.startColumn(new RecordingModifier(), BoxLayout.CENTER, BoxLayout.CENTER);
            int textId = writer.addText("Fitted Text");
            writer.startTextComponent(
                    new RecordingModifier(),
                    textId,
                    -1, // textStyleId
                    0xFFFFFFFF,
                    -1,
                    100f,
                    -1f,
                    -1f,
                    0,
                    400f,
                    null,
                    1,
                    1,
                    Integer.MAX_VALUE,
                    0f,
                    0f,
                    1f,
                    0,
                    0,
                    0,
                    false,
                    false,
                    null,
                    null,
                    false,
                    (short) 0
            );
            writer.endTextComponent();
            writer.endColumn();

            writer.endFitBox();
        });
    }

    private String getFitBoxJson() {
        return "{"
                + "  \"header\": { \"apiLevel\": 7, \"width\": 400, \"height\": 400,"
                + " \"contentDescription\": \"FitBox\" },"
                + "  \"root\": {"
                + "    \"type\": \"fitBox\","
                + "    \"horizontalAlignment\": \"center\","
                + "    \"verticalAlignment\": \"center\","
                + "    \"modifiers\": ["
                + "      { \"fillMaxSize\": 1.0 },"
                + "      { \"background\": \"#0000FF\" }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"column\","
                + "        \"horizontalAlignment\": \"center\","
                + "        \"verticalAlignment\": \"center\","
                + "        \"children\": ["
                + "          {"
                + "            \"type\": \"text\","
                + "            \"value\": \"Fitted Text\","
                + "            \"color\": \"#FFFFFF\","
                + "            \"fontSize\": 100,"
                + "            \"textAlign\": \"left\""
                + "          }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
    }

    private void printMismatch(String name, byte[] expected, byte[] actual) {
        System.out.println(name + " mismatch!");
        System.out.println("Expected size: " + expected.length);
        System.out.println("Actual size:   " + actual.length);

        System.out.print("Expected: ");
        for (int i = 0; i < Math.min(expected.length, 1000); i++) {
            System.out.format("%02X ", expected[i]);
        }
        System.out.println();

        System.out.print("Actual:   ");
        for (int i = 0; i < Math.min(actual.length, 1000); i++) {
            System.out.format("%02X ", actual[i]);
        }
        System.out.println();

        int minLen = Math.min(expected.length, actual.length);
        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                System.out.println("First mismatch at [" + i + "]: expected="
                        + String.format("%02X", expected[i])
                        + " actual=" + String.format("%02X", actual[i]));
                System.out.print("Expected window around mismatch: ");
                for (int j = Math.max(0, i - 10); j < Math.min(expected.length, i + 20); j++) {
                    if (j == i) System.out.print("[");
                    System.out.format("%02X ", expected[j]);
                    if (j == i) System.out.print("] ");
                }
                System.out.println();
                System.out.print("Actual window around mismatch:   ");
                for (int j = Math.max(0, i - 10); j < Math.min(actual.length, i + 20); j++) {
                    if (j == i) System.out.print("[");
                    System.out.format("%02X ", actual[j]);
                    if (j == i) System.out.print("] ");
                }
                System.out.println();
                break;
            }
        }
    }

    private static class MockPlatform implements RcPlatformServices {
        @Override
        public float[] pathToFloatArray(Object path) {
            if (path instanceof RemotePathBase) {
                return ((RemotePathBase) path).getPath();
            }
            return new float[0];
        }
        @Override
        public Object parsePath(String path) {
            return new RemotePathBase(path);
        }

        @Override
        public byte[] imageToByteArray(Object image) {
            return new byte[0];
        }

        @Override
        public int getImageWidth(Object image) {
            return 0;
        }

        @Override
        public int getImageHeight(Object image) {
            return 0;
        }

        @Override
        public boolean isAlpha8Image(Object image) {
            return false;
        }

        @Override
        public void log(@NonNull LogCategory category, @NonNull String message) {}
    }
}
