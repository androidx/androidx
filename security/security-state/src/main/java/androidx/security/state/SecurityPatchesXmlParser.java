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

package androidx.security.state;

// AUTO-GENERATED FILE. DO NOT MODIFY.
// Use `xsdc supplemental_security_patches.xsd -j -p androidx.security.state` to update.
class SecurityPatchesXmlParser {
  static class Patch {
    private java.lang.String id;

    java.lang.String getId() {
      return id;
    }

    boolean hasId() {
      if (id == null) {
        return false;
      }
      return true;
    }

    void setId(java.lang.String id) {
      this.id = id;
    }

    static SecurityPatchesXmlParser.Patch read(org.xmlpull.v1.XmlPullParser _parser)
        throws org.xmlpull.v1.XmlPullParserException,
            java.io.IOException,
            javax.xml.datatype.DatatypeConfigurationException {
      SecurityPatchesXmlParser.Patch _instance = new SecurityPatchesXmlParser.Patch();
      String _raw = null;
      int type;
      while ((type = _parser.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT
          && type != org.xmlpull.v1.XmlPullParser.END_TAG) {
        if (_parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
        String _tagName = _parser.getName();
        if (_tagName.equals("id")) {
          _raw = readText(_parser);
          java.lang.String _value = _raw;
          _instance.setId(_value);
        } else {
          skip(_parser);
        }
      }
      if (type != org.xmlpull.v1.XmlPullParser.END_TAG) {
        throw new javax.xml.datatype.DatatypeConfigurationException(
            "SecurityPatchesXmlParser.Patch is not closed");
      }
      return _instance;
    }
  }

  private java.util.List<SecurityPatchesXmlParser.Patch> patch;

  java.util.List<SecurityPatchesXmlParser.Patch> getPatch() {
    if (patch == null) {
      patch = new java.util.ArrayList<>();
    }
    return patch;
  }

  public static SecurityPatchesXmlParser read(java.io.InputStream in)
      throws org.xmlpull.v1.XmlPullParserException,
          java.io.IOException,
          javax.xml.datatype.DatatypeConfigurationException {
    org.xmlpull.v1.XmlPullParser _parser =
        org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser();
    _parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    _parser.setInput(in, null);
    _parser.nextTag();
    String _tagName = _parser.getName();
    if (_tagName.equals("security-patches")) {
      SecurityPatchesXmlParser _value = read(_parser);
      return _value;
    }
    return null;
  }

  public static java.lang.String readText(org.xmlpull.v1.XmlPullParser _parser)
      throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
    String result = "";
    if (_parser.next() == org.xmlpull.v1.XmlPullParser.TEXT) {
      result = _parser.getText();
      _parser.nextTag();
    }
    return result;
  }

  public static void skip(org.xmlpull.v1.XmlPullParser _parser)
      throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
    if (_parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) {
      throw new IllegalStateException();
    }
    int depth = 1;
    while (depth != 0) {
      switch (_parser.next()) {
        case org.xmlpull.v1.XmlPullParser.END_TAG:
          depth--;
          break;
        case org.xmlpull.v1.XmlPullParser.START_TAG:
          depth++;
          break;
      }
    }
  }

  static SecurityPatchesXmlParser read(org.xmlpull.v1.XmlPullParser _parser)
      throws org.xmlpull.v1.XmlPullParserException,
          java.io.IOException,
          javax.xml.datatype.DatatypeConfigurationException {
    SecurityPatchesXmlParser _instance = new SecurityPatchesXmlParser();
    int type;
    while ((type = _parser.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT
        && type != org.xmlpull.v1.XmlPullParser.END_TAG) {
      if (_parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
      String _tagName = _parser.getName();
      if (_tagName.equals("patch")) {
        Patch _value = Patch.read(_parser);
        _instance.getPatch().add(_value);
      } else {
        skip(_parser);
      }
    }
    if (type != org.xmlpull.v1.XmlPullParser.END_TAG) {
      throw new javax.xml.datatype.DatatypeConfigurationException(
          "SecurityPatchesXmlParser is not closed");
    }
    return _instance;
  }
}
