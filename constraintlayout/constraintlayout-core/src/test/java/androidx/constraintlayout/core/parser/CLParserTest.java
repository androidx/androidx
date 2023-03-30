/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CLParserTest {

    private void testBasicFormat(String content) {
        try {
            CLObject parsedContent = CLParser.parse(content);
            assertEquals(parsedContent.toJSON(), content);
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testParsing() {
        testBasicFormat("{ a: { start: ['parent', 'start', 20], "
                + "top: ['parent', 'top', 30] } }");
        testBasicFormat("{ test: 'hello, the', key: 'world' }");
        testBasicFormat("{ test: [1, 2, 3] }");
        testBasicFormat("{ test: ['hello', 'world', { value: 42 }] }");
        testBasicFormat("{ test: [null] }");
        testBasicFormat("{ test: [null, false, true] }");
        testBasicFormat("{ test: ['hello', 'world', { value: 42 }], value: false, "
                + "plop: 23, hello: { key: 42, text: 'bonjour' } }");
    }

    @Test
    public void testValue() {
        try {
            String test = "{ test: ['hello', 'world', { value: 42 }], value: false, plop: 23, "
                    + "hello: { key: 49, text: 'bonjour' } }";
            CLObject parsedContent = CLParser.parse(test);
            assertTrue(parsedContent.toJSON().equals(test));
            assertEquals("hello", parsedContent.getArray("test").getString(0));
            assertEquals("world", parsedContent.getArray("test").getString(1));
            assertEquals(42, parsedContent.getArray("test").getObject(2).get("value").getInt());
            assertEquals(false, parsedContent.getBoolean("value"));
            assertEquals(23, parsedContent.getInt("plop"));
            assertEquals(49, parsedContent.getObject("hello").getInt("key"));
            assertEquals("bonjour", parsedContent.getObject("hello").getString("text"));
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testException() {
        try {
            String test = "{ test: ['hello', 'world', { value: 42 }], value: false, "
                    + "plop: 23, hello: { key: 49, text: 'bonjour' } }";
            CLObject parsedContent = CLParser.parse(test);
            parsedContent.getObject("test").getString(0);
        } catch (CLParsingException e) {
            assertEquals("no object found for key <test>, found [CLArray] : "
                    + "CLArray (9 : 39) <<'hello', 'world', { value: 42 }>> = <CLString (10 : 14) "
                    + "<<hello>>; CLString (19 : 23) <<world>>; CLObject (28 : 39) "
                    + "<< value: 42 }>> = <CLKey (29 : 33) <<value>> = <CLNumber (36 : 37) "
                    + "<<42>> > > > (CLObject at line 1)", e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testTrailingCommas() {
        try {
            String test = "{ test: ['hello', 'world'],,,,,,, }";
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("hello", parsedContent.getArray("test").getString(0));
            assertEquals("world", parsedContent.getArray("test").getString(1));
            assertEquals("{ test: ['hello', 'world'] }", parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }


    @Test
    public void testIncompleteObject() {
        try {
            String test = "{ test: ['hello', 'world";
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("hello", parsedContent.getArray("test").getString(0));
            assertEquals("world", parsedContent.getArray("test").getString(1));
            assertEquals("{ test: ['hello', 'world'] }", parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testDoubleQuotes() {
        try {
            String test = "{ test: [\"hello\", \"world\"] }";
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("hello", parsedContent.getArray("test").getString(0));
            assertEquals("world", parsedContent.getArray("test").getString(1));
            assertEquals("{ test: ['hello', 'world'] }", parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testDoubleQuotesKey() {
        try {
            String test = "{ \"test\": [\"hello\", \"world\"] }";
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{ test: ['hello', 'world'] }", parsedContent.toJSON());
            assertEquals("hello", parsedContent.getArray("test").getString(0));
            assertEquals("world", parsedContent.getArray("test").getString(1));
            assertEquals("{ test: ['hello', 'world'] }", parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testMultilines() {
        String test = "{\n"
                + "  firstName: 'John',\n"
                + "  lastName: 'Smith',\n"
                + "  isAlive: true,\n"
                + "  age: 27,\n"
                + "  address: {\n"
                + "    streetAddress: '21 2nd Street',\n"
                + "    city: 'New York',\n"
                + "    state: 'NY',\n"
                + "    postalCode: '10021-3100'\n"
                + "  },\n"
                + "  phoneNumbers: [\n"
                + "    {\n"
                + "      type: 'home',\n"
                + "      number: '212 555-1234'\n"
                + "    },\n"
                + "    {\n"
                + "      type: 'office',\n"
                + "      number: '646 555-4567'\n"
                + "    }\n"
                + "  ],\n"
                + "  children: [],\n"
                + "  spouse: null\n"
                + "}          ";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("John", parsedContent.getString("firstName"));
            assertEquals("{ firstName: 'John', lastName: 'Smith', isAlive: true, "
                            + "age: 27, address: { streetAddress: '21 2nd Street', city: 'New "
                            + "York', "
                            + "state: 'NY', postalCode: '10021-3100' }, "
                            + "phoneNumbers: [{ type: 'home', number: '212 555-1234' }, "
                            + "{ type: 'office', number: '646 555-4567' }], "
                            + "children: [], spouse: null }",
                    parsedContent.toJSON());
            assertEquals(2, parsedContent.getArray("phoneNumbers").size());
            CLElement element = parsedContent.get("spouse");
            if (element instanceof CLToken) {
                CLToken token = (CLToken) element;
                assertEquals(CLToken.Type.NULL, token.mType);
            }
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testDoubleQuotesMultilines() {
        String test = "{\n"
                + "  \"firstName\": \"John\",\n"
                + "  \"lastName\": \"Smith\",\n"
                + "  \"isAlive\": true,\n"
                + "  \"age\": 27,\n"
                + "  \"address\": {\n"
                + "    \"streetAddress\": \"21 2nd Street\",\n"
                + "    \"city\": \"New York\",\n"
                + "    \"state\": \"NY\",\n"
                + "    \"postalCode\": \"10021-3100\"\n"
                + "  },\n"
                + "  \"phoneNumbers\": [\n"
                + "    {\n"
                + "      \"type\": \"home\",\n"
                + "      \"number\": \"212 555-1234\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"type\": \"office\",\n"
                + "      \"number\": \"646 555-4567\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"children\": [],\n"
                + "  \"spouse\": null\n"
                + "}          ";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("John", parsedContent.getString("firstName"));
            assertEquals("{ firstName: 'John', lastName: 'Smith', isAlive: true, "
                    + "age: 27, address: { streetAddress: '21 2nd Street', city: 'New York', "
                    + "state: 'NY', postalCode: '10021-3100' }, "
                    + "phoneNumbers: [{ type: 'home', number: '212 555-1234' }, "
                    + "{ type: 'office', number: '646 555-4567' }], "
                    + "children: [], spouse: null }", parsedContent.toJSON());
            assertEquals(2, parsedContent.getArray("phoneNumbers").size());
            CLElement element = parsedContent.get("spouse");
            if (element instanceof CLToken) {
                CLToken token = (CLToken) element;
                assertEquals(CLToken.Type.NULL, token.mType);
            }
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testJSON5() {
        String test = "{\n"
                + "  // comments\n  unquoted: 'and you can quote me on that',\n"
                + "  singleQuotes: 'I can use \"double quotes\" here',\n"
                // "  hexadecimal: 0xdecaf,\n" +
                + "  leadingDecimalPoint: .8675309, andTrailing: 8675309.,\n"
                + "  positiveSign: +1,\n"
                + "  trailingComma: 'in objects', andIn: ['arrays',],\n"
                + "  \"backwardsCompatible\": \"with JSON\",\n"
                + "}";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{ unquoted: 'and you can quote me on that', "
                            + "singleQuotes: 'I can use \"double quotes\" here', "
                            + "leadingDecimalPoint: 0.8675309, andTrailing: 8675309, "
                            + "positiveSign: 1, trailingComma: 'in objects', "
                            + "andIn: ['arrays'], backwardsCompatible: 'with JSON' }",
                    parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testConstraints() {
        String test = "{\n"
                + "  g1 : { type: 'vGuideline', start: 44 },\n"
                + "  g2 : { type: 'vGuideline', end: 44 },\n"
                + "  image: {\n"
                + "    width: 201, height: 179,\n"
                + "    top: ['parent','top', 32],\n"
                + "    start: 'g1'\n"
                + "  },\n";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{ g1: { type: 'vGuideline', start: 44 }, "
                            + "g2: { type: 'vGuideline', end: 44 }, "
                            + "image: { width: 201, height: 179, top: ['parent', 'top', 32], "
                            + "start: 'g1' } }",
                    parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testConstraints2() {
        String test = "            {\n"
                + "              Variables: {\n"
                + "                bottom: 20\n"
                + "              },\n"
                + "              Helpers: [\n"
                + "                ['hChain', ['a','b','c'], {\n"
                + "                  start: ['leftGuideline1', 'start'],\n"
                + "                  style: 'packed'\n"
                + "                }],\n"
                + "                ['hChain', ['d','e','f']],\n"
                + "                ['vChain', ['d','e','f'], {\n"
                + "                  bottom: ['topGuideline1', 'top']\n"
                + "                }],\n"
                + "                ['vGuideline', {\n"
                + "                  id: 'leftGuideline1', start: 100\n"
                + "                }],\n"
                + "                ['hGuideline', {\n"
                + "                  id: 'topGuideline1', percent: 0.5\n"
                + "                }]\n"
                + "              ],\n"
                + "              a: {\n"
                + "                bottom: ['b', 'top', 'bottom']\n"
                + "              },\n"
                + "              b: {\n"
                + "                width: '30%',\n"
                + "                height: '1:1',\n"
                + "                centerVertically: 'parent'\n"
                + "              },\n"
                + "              c: {\n"
                + "                top: ['b', 'bottom']\n"
                + "              }\n"
                + "            }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{ "
                            + "Variables: { bottom: 20 }, "
                            + "Helpers: ["
                            + "['hChain', ['a', 'b', 'c'], { start: ['leftGuideline1', 'start'], "
                            + "style: 'packed' }], "
                            + "['hChain', ['d', 'e', 'f']], "
                            + "['vChain', ['d', 'e', 'f'], { bottom: ['topGuideline1', 'top'] }], "
                            + "['vGuideline', { id: 'leftGuideline1', start: 100 }], "
                            + "['hGuideline', { id: 'topGuideline1', percent: 0.5 }]"
                            + "], "
                            + "a: { bottom: ['b', 'top', 'bottom'] }, "
                            + "b: { width: '30%', height: '1:1', centerVertically: 'parent' }, "
                            + "c: { top: ['b', 'bottom'] } }",
                    parsedContent.toJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }


    @Test
    public void testConstraints3() {
        String test = "{\n"
                + "                ConstraintSets: {\n"
                + "                  start: {\n"
                + "                    a: {\n"
                + "                      width: 40,\n"
                + "                      height: 40,\n"
                + "                      start: ['parent', 'start', 16],\n"
                + "                      bottom: ['parent', 'bottom', 16]\n"
                + "                    }\n"
                + "                  },\n"
                + "                  end: {\n"
                + "                    a: {\n"
                + "                      width: 40,\n"
                + "                      height: 40,\n"
                + "                      //rotationZ: 390,\n"
                + "                      end: ['parent', 'end', 16],\n"
                + "                      top: ['parent', 'top', 16]\n"
                + "                    }\n"
                + "                  }\n"
                + "                },\n"
                + "                Transitions: {\n"
                + "                  default: {\n"
                + "                    from: 'start',\n"
                + "                    to: 'end',\n"
                + "                    pathMotionArc: 'startHorizontal',\n"
                + "                    KeyFrames: {\n"
                + "//                      KeyPositions: [\n"
                + "//                        {\n"
                + "//                          target: ['a'],\n"
                + "//                          frames: [25, 50, 75],\n"
                + "////                          percentX: [0.4, 0.8, 0.1],\n"
                + "////                          percentY: [0.4, 0.8, 0.3]\n"
                + "//                        }\n"
                + "//                      ],\n"
                + "                      KeyAttributes: [\n"
                + "                        {\n"
                + "                          target: ['a'],\n"
                + "                          frames: [25, 50],\n"
                + "                          scaleX: 3,\n"
                + "                          scaleY: .3\n"
                + "                        }\n"
                + "                      ]\n"
                + "                    }\n"
                + "                  }\n"
                + "                }\n"
                + "            }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{ ConstraintSets: { start: { a: { width: 40, height: 40, "
                            + "start: ['parent', 'start', 16], bottom: ['parent', 'bottom', 16] }"
                            + " }, "
                            + "end: { a: { width: 40, height: 40, end: ['parent', 'end', 16],"
                            + " top: ['parent', 'top', 16] } } }, "
                            + "Transitions: { default: { from: 'start', to: 'end', "
                            + "pathMotionArc: 'startHorizontal', "
                            + "KeyFrames: { KeyAttributes: [{ target: ['a'], frames: [25, 50], "
                            + "scaleX: 3, scaleY: 0.3 }] } } } }",
                    parsedContent.toJSON());
            CLObject transitions = parsedContent.getObject("Transitions");
            CLObject transition = transitions.getObject("default");
            CLObject keyframes = transition.getObjectOrNull("KeyFrames");
            CLArray keyattributes = keyframes.getArrayOrNull("KeyAttributes");
            assertNotNull(keyattributes);
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testFormatting() {
        String test = "{ firstName: 'John', lastName: 'Smith', isAlive: true, "
                + "age: 27, address: { streetAddress: '21 2nd Street', city: 'New York', "
                + "state: 'NY', postalCode: '10021-3100' }, "
                + "phoneNumbers: [{ type: 'home', number: '212 555-1234' }, "
                + "{ type: 'office', number: '646 555-4567' }], "
                + "children: [], spouse: null }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  firstName: 'John',\n"
                    + "  lastName: 'Smith',\n"
                    + "  isAlive: true,\n"
                    + "  age: 27,\n"
                    + "  address: {\n"
                    + "    streetAddress: '21 2nd Street',\n"
                    + "    city: 'New York',\n"
                    + "    state: 'NY',\n"
                    + "    postalCode: '10021-3100'\n"
                    + "  },\n"
                    + "  phoneNumbers: [\n"
                    + "    {\n"
                    + "      type: 'home',\n"
                    + "      number: '212 555-1234'\n"
                    + "    },\n"
                    + "    {\n"
                    + "      type: 'office',\n"
                    + "      number: '646 555-4567'\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  children: [],\n"
                    + "  spouse: null\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting2() {
        String test = "{ ConstraintSets: { start: { a: { width: 40, height: 40, "
                + "start: ['parent', 'start', 16], bottom: ['parent', 'bottom', 16] } }, end: "
                + "{ a: { width: 40, height: 40, end: ['parent', 'end', 16],"
                + " top: ['parent', 'top', 16]"
                + " } } }, Transitions: { default: { from: 'start', to: 'end', "
                + "pathMotionArc: 'startHorizontal', KeyFrames: { KeyAttributes: [{ target: ['a'], "
                + "frames: [25, 50], scaleX: 3, scaleY: 0.3 }] } } } }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  ConstraintSets: {\n"
                    + "    start: {\n"
                    + "      a: {\n"
                    + "        width: 40,\n"
                    + "        height: 40,\n"
                    + "        start: ['parent', 'start', 16],\n"
                    + "        bottom: ['parent', 'bottom', 16]\n"
                    + "      }\n"
                    + "    },\n"
                    + "    end: {\n"
                    + "      a: {\n"
                    + "        width: 40,\n"
                    + "        height: 40,\n"
                    + "        end: ['parent', 'end', 16],\n"
                    + "        top: ['parent', 'top', 16]\n"
                    + "      }\n"
                    + "    }\n"
                    + "  },\n"
                    + "  Transitions: {\n"
                    + "    default: {\n"
                    + "      from: 'start',\n"
                    + "      to: 'end',\n"
                    + "      pathMotionArc: 'startHorizontal',\n"
                    + "      KeyFrames: {\n"
                    + "        KeyAttributes: [\n"
                    + "          {\n"
                    + "            target: ['a'],\n"
                    + "            frames: [25, 50],\n"
                    + "            scaleX: 3,\n"
                    + "            scaleY: 0.3\n"
                    + "          }\n"
                    + "        ]\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting3() {
        String test = "{ ConstraintSets: {\n"
                + "      Generate: { texts: { top: ['parent', 'top', 'margin'], "
                + "start: ['parent', 'end', 16] } } } }\n";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  ConstraintSets: {\n"
                    + "    Generate: {\n"
                    + "      texts: {\n"
                    + "        top: ['parent', 'top', 'margin'],\n"
                    + "        start: ['parent', 'end', 16]\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting4() {
        String test = "{ Transitions: { default: { from: 'start', to: 'end', "
                + "pathMotionArc: 'startHorizontal', KeyFrames: { KeyAttributes: [{ target: ['a'], "
                + "frames: [25, 50], scaleX: 3, scaleY: 0.3 }] } } } }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  Transitions: {\n"
                    + "    default: {\n"
                    + "      from: 'start',\n"
                    + "      to: 'end',\n"
                    + "      pathMotionArc: 'startHorizontal',\n"
                    + "      KeyFrames: {\n"
                    + "        KeyAttributes: [\n"
                    + "          {\n"
                    + "            target: ['a'],\n"
                    + "            frames: [25, 50],\n"
                    + "            scaleX: 3,\n"
                    + "            scaleY: 0.3\n"
                    + "          }\n"
                    + "        ]\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting5() {
        String test = "{ Debug: { name: 'motion6' }, ConstraintSets: {\n"
                + "    start: { Variables: { texts: { tag: 'text' }, "
                + "margin: { from: 0, step: 50 }\n"
                + "      }, Generate: { texts: { top: ['parent', 'top', 'margin'], "
                + "start: ['parent', 'end', 16] }\n"
                + "      }, box: { width: 'spread', height: 64, centerHorizontally: 'parent',\n"
                + "        bottom: ['parent', 'bottom'] }, content: { width: 'spread',\n"
                + "        height: '400', centerHorizontally: 'parent', "
                + "top: ['box', 'bottom', 32]\n"
                + "      }, name: { centerVertically: 'box', start: ['parent', 'start', 16] }\n"
                + "    }, end: { Variables: { texts: { tag: 'text' },\n"
                + "        margin: { from: 0, step: 50 } }, Generate: {\n"
                + "        texts: { start: ['parent', 'start', 32], "
                + "top: ['content', 'top', 'margin'] }\n"
                + "      }, box: { width: 'spread', height: 200, centerHorizontally: 'parent',\n"
                + "        top: ['parent', 'top'] }, content: {\n"
                + "        width: 'spread', height: 'spread', centerHorizontally: 'parent',\n"
                + "        top: ['box', 'bottom'], bottom: ['parent', 'bottom']\n"
                + "      }, name: { rotationZ: 90, scaleX: 2, scaleY: 2,\n"
                + "        end: ['parent', 'end', 16], top: ['parent', 'top', 90]\n"
                + "      } } }, Transitions: { default: { from: 'start', to: 'end',\n"
                + "      pathMotionArc: 'startHorizontal', KeyFrames: {\n"
                + "        KeyAttributes: [ { target: ['box', 'content'],\n"
                + "            frames: [50], rotationZ: [25], rotationY: [25]\n"
                + "          } ] } } } }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  Debug: { name: 'motion6' },\n"
                    + "  ConstraintSets: {\n"
                    + "    start: {\n"
                    + "      Variables: {\n"
                    + "        texts: {\n"
                    + "          tag: 'text'\n"
                    + "        },\n"
                    + "        margin: {\n"
                    + "          from: 0,\n"
                    + "          step: 50\n"
                    + "        }\n"
                    + "      },\n"
                    + "      Generate: {\n"
                    + "        texts: {\n"
                    + "          top: ['parent', 'top', 'margin'],\n"
                    + "          start: ['parent', 'end', 16]\n"
                    + "        }\n"
                    + "      },\n"
                    + "      box: {\n"
                    + "        width: 'spread',\n"
                    + "        height: 64,\n"
                    + "        centerHorizontally: 'parent',\n"
                    + "        bottom: ['parent', 'bottom']\n"
                    + "      },\n"
                    + "      content: {\n"
                    + "        width: 'spread',\n"
                    + "        height: '400',\n"
                    + "        centerHorizontally: 'parent',\n"
                    + "        top: ['box', 'bottom', 32]\n"
                    + "      },\n"
                    + "      name: { centerVertically: 'box', start: ['parent', 'start', 16] }\n"
                    + "    },\n"
                    + "    end: {\n"
                    + "      Variables: {\n"
                    + "        texts: {\n"
                    + "          tag: 'text'\n"
                    + "        },\n"
                    + "        margin: {\n"
                    + "          from: 0,\n"
                    + "          step: 50\n"
                    + "        }\n"
                    + "      },\n"
                    + "      Generate: {\n"
                    + "        texts: {\n"
                    + "          start: ['parent', 'start', 32],\n"
                    + "          top: ['content', 'top', 'margin']\n"
                    + "        }\n"
                    + "      },\n"
                    + "      box: {\n"
                    + "        width: 'spread',\n"
                    + "        height: 200,\n"
                    + "        centerHorizontally: 'parent',\n"
                    + "        top: ['parent', 'top']\n"
                    + "      },\n"
                    + "      content: {\n"
                    + "        width: 'spread',\n"
                    + "        height: 'spread',\n"
                    + "        centerHorizontally: 'parent',\n"
                    + "        top: ['box', 'bottom'],\n"
                    + "        bottom: ['parent', 'bottom']\n"
                    + "      },\n"
                    + "      name: {\n"
                    + "        rotationZ: 90,\n"
                    + "        scaleX: 2,\n"
                    + "        scaleY: 2,\n"
                    + "        end: ['parent', 'end', 16],\n"
                    + "        top: ['parent', 'top', 90]\n"
                    + "      }\n"
                    + "    }\n"
                    + "  },\n"
                    + "  Transitions: {\n"
                    + "    default: {\n"
                    + "      from: 'start',\n"
                    + "      to: 'end',\n"
                    + "      pathMotionArc: 'startHorizontal',\n"
                    + "      KeyFrames: {\n"
                    + "        KeyAttributes: [\n"
                    + "          {\n"
                    + "            target: ['box', 'content'],\n"
                    + "            frames: [50],\n"
                    + "            rotationZ: [25],\n"
                    + "            rotationY: [25]\n"
                    + "          }\n"
                    + "        ]\n"
                    + "      }\n"
                    + "    }\n"
                    + "  }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting6() {
        String test = "{ root: {interpolated: {left: 0, top: 0, right: 800, bottom: 772}}, "
                + "button: {interpolated: {left: 0, top: 372, right: 800, bottom: 401}}, "
                + "text1: {interpolated: {left: 100, top: 285, right: 208, bottom: 301}}, "
                + "text2: {interpolated: {left: 723, top: 736, right: 780, bottom: 752}}, "
                + "g1: {type: 'vGuideline',interpolated: {left: 100, top: 0,"
                + " right: 100, bottom: 772}}, }";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  root: { interpolated: { left: 0, top: 0, right: 800, bottom: 772 } },\n"
                    + "  button: { interpolated: { left: 0, top: 372, "
                    + "right: 800, bottom: 401 } },\n"
                    + "  text1: { interpolated: { left: 100, top: 285, "
                    + "right: 208, bottom: 301 } },\n"
                    + "  text2: { interpolated: { left: 723, top: 736, "
                    + "right: 780, bottom: 752 } },\n"
                    + "  g1: {\n"
                    + "    type: 'vGuideline',\n"
                    + "    interpolated: { left: 100, top: 0, right: 100, bottom: 772 }\n"
                    + "  }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting7() {
        String test = "{ root: {left: 0, top: 0, right: 800, bottom: 772}, "
                + "button: {left: 0, top: 372, right: 800, bottom: 401}, ";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  root: { left: 0, top: 0, right: 800, bottom: 772 },\n"
                    + "  button: { left: 0, top: 372, right: 800, bottom: 401 }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }

    @Test
    public void testFormatting8() {
        String test = "{ root: { bottom: 772}, "
                + "button: { bottom: 401 }, ";
        try {
            CLObject parsedContent = CLParser.parse(test);
            assertEquals("{\n"
                    + "  root: { bottom: 772 },\n"
                    + "  button: { bottom: 401 }\n"
                    + "}", parsedContent.toFormattedJSON());
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
        }
    }
}


