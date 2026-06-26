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
package androidx.compose.remote.creation.json;

import static org.junit.Assert.assertNotNull;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.json.JSONException;
import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;

public class RemoteComposeJsonParserTest {
    private RemoteComposeWriter mWriter;
    private RemoteComposeJsonParser mParser;

    @Before
    public void setup() {
        mWriter = new RemoteComposeWriter(400, 800, "Test", new MockPlatform());
        mParser = new RemoteComposeJsonParser(mWriter);
    }

    @Test
    public void testSimpleDocument() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"column\","
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"text\","
                + "        \"value\": \"Hello JSON\""
                + "      }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testCanvasWithExpressions() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"canvas\","
                + "    \"commands\": ["
                + "      {"
                + "        \"type\": \"drawCircle\","
                + "        \"cx\": \"width / 2\","
                + "        \"cy\": \"height / 2\","
                + "        \"radius\": \"50 + sin(time) * 10\""
                + "      }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testModifiers() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"box\","
                + "    \"modifiers\": ["
                + "      { \"padding\": 16 },"
                + "      { \"background\": \"#FF0000\" },"
                + "      { \"fillMaxSize\": 1.0 },"
                + "      { \"collapsiblePriority\": 1 }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testAdvancedLayouts() throws JSONException {
        String[] layouts = {"flow", "collapsibleColumn", "collapsibleRow", "fitBox"};
        for (String layout : layouts) {
            String json = "{"
                    + "  \"root\": {"
                    + "    \"type\": \"" + layout + "\","
                    + "    \"horizontalAlignment\": \"start\","
                    + "    \"verticalAlignment\": \"top\","
                    + "    \"children\": ["
                    + "      { \"type\": \"text\", \"value\": \"test\" }"
                    + "    ]"
                    + "  }"
                    + "}";
            mParser.parse(json);
        }
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testInlineGlobalBlock() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"global\","
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"resources\","
                + "        \"variables\": ["
                + "          { \"name\": \"pulse\", \"value\": \"50 + sin(time) * 10\" }"
                + "        ]"
                + "      },"
                + "      {"
                + "        \"type\": \"canvas\","
                + "        \"commands\": ["
                + "          {"
                + "            \"type\": \"global\","
                + "            \"commands\": ["
                + "              { \"type\": \"drawCircle\", \"cx\": 100, \"cy\": 100,"
                + " \"radius\": \"$vars.pulse\" }"
                + "            ]"
                + "          }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testArrayResourceShorthand() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"global\","
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"resources\","
                + "        \"colors\": ["
                + "          { \"blue\": \"#0047AB\" },"
                + "          { \"brand\": { \"light\": \"@colors.blue\","
                + " \"dark\": \"#001122\", \"export\": false } }"
                + "        ]"
                + "      },"
                + "      {"
                + "        \"type\": \"canvas\","
                + "        \"commands\": ["
                + "          { \"paint\": { \"color\": \"@colors.brand\" } }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testStringModifierShorthand() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"column\","
                + "    \"modifiers\": [ \"fillMaxWidth\", { \"padding\": 16 } ],"
                + "    \"children\": ["
                + "      { \"text\": { \"value\": \"Hello Shorthand\" } }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testCompactShorthandSyntax() throws JSONException {
        String json = "{"
                + "  \"root\": ["
                + "    {"
                + "      \"column\": ["
                + "        { \"text\": \"Hello Shorthand\" },"
                + "        {"
                + "          \"canvas\": ["
                + "            { \"drawCircle\": { \"cx\": 100, \"cy\": 100, \"radius\": 50 } }"
                + "          ]"
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testMixedSyntaxAbilities() throws JSONException {
        String json = "{"
                + "  \"root\": ["
                + "    {"
                + "      \"column\": ["
                + "        { \"text\": \"Shorthand text node\" },"
                + "        {"
                + "          \"type\": \"row\","
                + "          \"modifiers\": [ { \"padding\": 8 } ],"
                + "          \"children\": ["
                + "            { \"text\": { \"value\": \"Compact text inside verbose row\","
                + " \"modifiers\": [ { \"size\": 20 } ] } }"
                + "          ]"
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        assertNotNull(result);
    }

    @Test
    public void testParsingExceptionContainsDiagnosticPath() {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"column\","
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"row\","
                + "        \"children\": ["
                + "          {"
                + "            \"type\": \"unknown_layout\""
                + "          }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
        try {
            mParser.parse(json);
            org.junit.Assert.fail("Should have thrown JSONException");
        } catch (JSONException e) {
            String msg = e.getMessage();
            System.out.println("### Caught diagnostic error message:\n" + msg);
            org.junit.Assert.assertTrue(msg.contains("root -> children[0] -> children[0]"));
            org.junit.Assert.assertTrue(msg.contains("Unknown component type: unknown_layout"));
        }
    }

    @Test
    public void testExpressionParserBasicArithmetic() throws JSONException {
        ExpressionParser exprParser = new ExpressionParser(mParser);
        java.util.List<Object> rpn = exprParser.infixToRpn("2 + 3 * 4");
        org.junit.Assert.assertEquals(5, rpn.size());
        org.junit.Assert.assertEquals(2f, (Float) rpn.get(0), 0.001f);
        org.junit.Assert.assertEquals(3f, (Float) rpn.get(1), 0.001f);
        org.junit.Assert.assertEquals(4f, (Float) rpn.get(2), 0.001f);
        float result = exprParser.parseExpression("2 + 3 * 4");
        org.junit.Assert.assertNotEquals(0f, result);
    }

    @Test
    public void testExpressionParserUnaryMinus() throws JSONException {
        ExpressionParser exprParser = new ExpressionParser(mParser);
        java.util.List<Object> rpn = exprParser.infixToRpn("-5");
        org.junit.Assert.assertEquals(1, rpn.size());
        org.junit.Assert.assertEquals(-5f, (Float) rpn.get(0), 0.001f);

        java.util.List<Object> rpnVar = exprParser.infixToRpn("-time");
        org.junit.Assert.assertEquals(2, rpnVar.size());
        org.junit.Assert.assertEquals(Utils.asNan(1), (Float) rpnVar.get(0), 0.001f);
    }

    @Test
    public void testExpressionParserVariables() throws JSONException {
        ExpressionParser exprParser = new ExpressionParser(mParser);
        org.junit.Assert.assertTrue(exprParser.isVariable("time"));
        org.junit.Assert.assertTrue(exprParser.isVariable("width"));
        org.junit.Assert.assertTrue(exprParser.isVariable("touchX"));

        mParser.mVariables.put("myVar", 10.0f);
        org.junit.Assert.assertTrue(exprParser.isVariable("myVar"));
        org.junit.Assert.assertTrue(exprParser.isVariable("$vars.myVar"));
        org.junit.Assert.assertTrue(exprParser.isVariable("@myVar"));
        org.junit.Assert.assertTrue(exprParser.isVariable("$myVar"));
        org.junit.Assert.assertEquals(10.0f, exprParser.getVariableNan("myVar"), 0.001f);
        org.junit.Assert.assertEquals(10.0f, exprParser.getVariableNan("@myVar"), 0.001f);
        org.junit.Assert.assertEquals(10.0f, exprParser.getVariableNan("$myVar"), 0.001f);
    }

    @Test
    public void testExpressionParserFunctions() throws JSONException {
        ExpressionParser exprParser = new ExpressionParser(mParser);
        java.util.List<Object> rpn = exprParser.infixToRpn("sin(time)");
        org.junit.Assert.assertEquals(2, rpn.size());
        org.junit.Assert.assertEquals(Utils.asNan(1), (Float) rpn.get(0), 0.001f);

        java.util.List<Object> rpnMin = exprParser.infixToRpn("min(width, height)");
        org.junit.Assert.assertEquals(3, rpnMin.size());
    }

    @Test
    public void testExpressionParserParentheses() throws JSONException {
        ExpressionParser exprParser = new ExpressionParser(mParser);
        java.util.List<Object> rpn = exprParser.infixToRpn("(2 + 3) * 4");
        org.junit.Assert.assertEquals(5, rpn.size());
        org.junit.Assert.assertEquals(2f, (Float) rpn.get(0), 0.001f);
        org.junit.Assert.assertEquals(3f, (Float) rpn.get(1), 0.001f);
    }

    @Test
    public void testExpressionParserErrors() {
        ExpressionParser exprParser = new ExpressionParser(mParser);
        try {
            exprParser.infixToRpn("1 + 2)");
            org.junit.Assert.fail("Should throw JSONException for mismatched parentheses");
        } catch (JSONException e) {
            org.junit.Assert.assertTrue(e.getMessage().contains("Mismatched parentheses"));
        }

        try {
            exprParser.getVariableNan("unknown_var");
            org.junit.Assert.fail("Should throw JSONException for unknown variable");
        } catch (JSONException e) {
            org.junit.Assert.assertTrue(e.getMessage().contains("Unknown variable"));
        }

        try {
            exprParser.infixToRpn("1 ? 2");
            org.junit.Assert.fail("Should throw JSONException for unknown token");
        } catch (JSONException e) {
            org.junit.Assert.assertTrue(e.getMessage().contains("Unknown token"));
        }
    }

    @Test
    public void testExpressionParserComplexMath() throws JSONException {
        ExpressionParser exprParser = new ExpressionParser(mParser);

        // 1. Complex wave motion superposition
        String complexTrig = "50 + sin(time * 2 * 3.14159) * 20 + cos(time * 0.5) * 10";
        java.util.List<Object> rpnTrig = exprParser.infixToRpn(complexTrig);
        org.junit.Assert.assertTrue(rpnTrig.size() > 10);
        float parseTrig = exprParser.parseExpression(complexTrig);
        org.junit.Assert.assertNotEquals(0f, parseTrig);

        // 2. Nested multi-argument functions with math operations
        String nestedFunc = "clamp(min(width, height) * 0.8, 50, max(touchX, touchY) + 10)";
        java.util.List<Object> rpnNested = exprParser.infixToRpn(nestedFunc);
        org.junit.Assert.assertTrue(rpnNested.size() > 8);
        float parseNested = exprParser.parseExpression(nestedFunc);
        org.junit.Assert.assertNotEquals(0f, parseNested);

        // 3. Linear interpolation (lerp) & ping-pong animation helper
        String lerpPing = "lerp(0, width, ping_pong(time / 2))";
        java.util.List<Object> rpnLerp = exprParser.infixToRpn(lerpPing);
        org.junit.Assert.assertTrue(rpnLerp.size() > 6);
        float parseLerp = exprParser.parseExpression(lerpPing);
        org.junit.Assert.assertNotEquals(0f, parseLerp);

        // 4. Expression inside JSON configuration with custom animation duration
        org.json.JSONObject animObj = new org.json.JSONObject();
        animObj.put("value", "mad(sin(time), 10, 50)");
        animObj.put("anim", 2.5);
        float parseAnim = exprParser.parseExpression(animObj);
        org.junit.Assert.assertNotEquals(0f, parseAnim);
    }

    @Test
    public void testDefinePatternOverridePreExistingComponentThrows() {
        String json = "{"
                + "  \"root\": ["
                + "    {"
                + "      \"type\": \"definePattern\","
                + "      \"name\": \"box\","
                + "      \"parameters\": [\"label\"],"
                + "      \"children\": []"
                + "    }"
                + "  ]"
                + "}";
        try {
            mParser.parse(json);
            org.junit.Assert.fail(
                    "Should have thrown JSONException for overriding pre-existing component");
        } catch (JSONException e) {
            org.junit.Assert.assertTrue(e.getMessage()
                    .contains("Cannot override pre-existing component: box"));
        }
    }

    @Test
    public void testLoomTemplatesAndReferencedOperations() throws JSONException {
        String json = "{"
                + "  \"root\": ["
                + "    {"
                + "      \"type\": \"definePattern\","
                + "      \"name\": \"MyButton\","
                + "      \"parameters\": [\"label\", \"color\"],"
                + "      \"children\": ["
                + "        {"
                + "          \"type\": \"box\","
                + "          \"modifiers\": ["
                + "            { \"background\": \"@color\" }"
                + "          ],"
                + "          \"children\": ["
                + "            {"
                + "              \"type\": \"text\","
                + "              \"value\": \"@label\""
                + "            }"
                + "          ]"
                + "        }"
                + "      ]"
                + "    },"
                + "    {"
                + "      \"type\": \"referencedOperations\","
                + "      \"name\": \"MySharedStyle\","
                + "      \"modifiers\": ["
                + "        { \"padding\": 16 }"
                + "      ]"
                + "    },"
                + "    {"
                + "      \"type\": \"column\","
                + "      \"modifiers\": ["
                + "        { \"include\": \"@MySharedStyle\" }"
                + "      ],"
                + "      \"children\": ["
                + "        {"
                + "          \"type\": \"MyButton\","
                + "          \"label\": \"Click Me\","
                + "          \"color\": \"#FF0000\""
                + "        },"
                + "        {"
                + "          \"type\": \"patternInflation\","
                + "          \"pattern\": \"MyButton\","
                + "          \"arguments\": [\"Cancel\", \"#00FF00\"]"
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        org.junit.Assert.assertNotNull(result);
    }

    @Test
    public void testMacroLocalState() throws JSONException {
        String json = "{"
                + "  \"root\": ["
                + "    {"
                + "      \"type\": \"definePattern\","
                + "      \"name\": \"CounterButton\","
                + "      \"parameters\": [\"label\"],"
                + "      \"locals\": [\"counter\"],"
                + "      \"children\": ["
                + "        {"
                + "          \"type\": \"variable\","
                + "          \"name\": \"counter\","
                + "          \"value\": 0"
                + "        },"
                + "        {"
                + "          \"type\": \"variable\","
                + "          \"name\": \"displayText\","
                + "          \"vtype\": \"string\","
                + "          \"value\": {"
                + "            \"type\": \"textFromFloat\","
                + "            \"value\": \"@counter\","
                + "            \"decimal\": 0,"
                + "            \"whole\": 3,"
                + "            \"flags\": 0"
                + "          }"
                + "        },"
                + "        {"
                + "          \"type\": \"column\","
                + "          \"children\": ["
                + "            {"
                + "              \"type\": \"text\","
                + "              \"value\": \"@label\""
                + "            },"
                + "            {"
                + "              \"type\": \"row\","
                + "              \"children\": ["
                + "                { \"type\": \"text\", \"value\": \"Count: \" },"
                + "                { \"type\": \"text\", \"value\": \"@displayText\" }"
                + "              ]"
                + "            }"
                + "          ]"
                + "        }"
                + "      ]"
                + "    },"
                + "    {"
                + "      \"type\": \"column\","
                + "      \"children\": ["
                + "        {"
                + "          \"type\": \"CounterButton\","
                + "          \"label\": \"Button Alpha\""
                + "        },"
                + "        {"
                + "          \"type\": \"CounterButton\","
                + "          \"label\": \"Button Beta\""
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        org.junit.Assert.assertNotNull(result);
    }

    @Test
    public void testStage1Features() throws JSONException {
        String json = "{"
                + "  \"root\": {"
                + "    \"type\": \"flow\","
                + "    \"maxColumns\": 4,"
                + "    \"maxLines\": 2,"
                + "    \"modifiers\": ["
                + "      { \"verticalScroll\": { \"position\": 10.0, \"notches\": 5 } },"
                + "      { \"collapsiblePriority\": { \"orientation\": \"vertical\","
                + " \"priority\": 2.0 } }"
                + "    ],"
                + "    \"children\": ["
                + "      {"
                + "        \"type\": \"text\","
                + "        \"value\": \"Styled Text\","
                + "        \"fontSize\": 18,"
                + "        \"fontStyle\": \"italic\","
                + "        \"fontFamily\": \"sans-serif\","
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
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        org.junit.Assert.assertNotNull(result);
    }

    @Test
    public void testStage2Features() throws JSONException {
        String json = "{"
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
                + " \"start\": 0.0, \"len\": 4.0, \"varName\": \"subText\" },"
                + "          { \"type\": \"textTransform\", \"text\": \"subText\","
                + " \"start\": 0.0, \"len\": 4.0, \"operation\": \"uppercase\","
                + " \"varName\": \"upperText\" }"
                + "        ]"
                + "      }"
                + "    ]"
                + "  }"
                + "}";
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        org.junit.Assert.assertNotNull(result);
    }

    @Test
    public void testStage3Features() throws JSONException {
        String json = "{"
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
        mParser.parse(json);
        byte[] result = mWriter.encodeToByteArray();
        org.junit.Assert.assertNotNull(result);
    }

    private static class MockPlatform implements RcPlatformServices {
        @Override
        public float[] pathToFloatArray(Object path) {
            return new float[0];
        }

        @Override
        public Object parsePath(String path) {
            return new Object();
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
