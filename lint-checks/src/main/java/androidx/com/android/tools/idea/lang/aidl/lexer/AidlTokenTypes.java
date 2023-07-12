/*
 * Copyright 2023 The Android Open Source Project
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

// ATTENTION: This file has been automatically generated from Aidl.bnf. Do not edit it manually.

package androidx.com.android.tools.idea.lang.aidl.lexer;

import androidx.com.android.tools.idea.lang.aidl.psi.AidlElementType;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlAnnotationElementImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlBodyImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlConstantDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlDottedNameImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlEnumDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlEnumeratorDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlExpressionImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlImportImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlInterfaceDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlMethodDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlNameComponentImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlPackageImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlParameterImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlParcelableDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlQualifiedNameImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlTypeElementImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlUnionDeclarationImpl;
import androidx.com.android.tools.idea.lang.aidl.psi.impl.AidlVariableDeclarationImpl;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;

import androidx.com.android.tools.idea.lang.aidl.psi.impl.*;

public interface AidlTokenTypes {

  IElementType ANNOTATION_ELEMENT = new AidlElementType("ANNOTATION_ELEMENT");
  IElementType BODY = new AidlElementType("BODY");
  IElementType CONSTANT_DECLARATION = new AidlElementType("CONSTANT_DECLARATION");
  IElementType DOTTED_NAME = new AidlElementType("DOTTED_NAME");
  IElementType ENUMERATOR_DECLARATION = new AidlElementType("ENUMERATOR_DECLARATION");
  IElementType ENUM_DECLARATION = new AidlElementType("ENUM_DECLARATION");
  IElementType EXPRESSION = new AidlElementType("EXPRESSION");
  IElementType IMPORT = new AidlElementType("IMPORT");
  IElementType INTERFACE_DECLARATION = new AidlElementType("INTERFACE_DECLARATION");
  IElementType METHOD_DECLARATION = new AidlElementType("METHOD_DECLARATION");
  IElementType NAME_COMPONENT = new AidlElementType("NAME_COMPONENT");
  IElementType PACKAGE = new AidlElementType("PACKAGE");
  IElementType PARAMETER = new AidlElementType("PARAMETER");
  IElementType PARCELABLE_DECLARATION = new AidlElementType("PARCELABLE_DECLARATION");
  IElementType QUALIFIED_NAME = new AidlElementType("QUALIFIED_NAME");
  IElementType TYPE_ELEMENT = new AidlElementType("TYPE_ELEMENT");
  IElementType UNION_DECLARATION = new AidlElementType("UNION_DECLARATION");
  IElementType VARIABLE_DECLARATION = new AidlElementType("VARIABLE_DECLARATION");

  IElementType ASSIGN = new AidlTokenType("=");
  IElementType AT = new AidlTokenType("@");
  IElementType BITWISE_AND = new AidlTokenType("&");
  IElementType BITWISE_COMPLEMENT = new AidlTokenType("~");
  IElementType BITWISE_OR = new AidlTokenType("|");
  IElementType BITWISE_XOR = new AidlTokenType("^");
  IElementType BLOCK_COMMENT = new AidlTokenType("BLOCK_COMMENT");
  IElementType BOOLEAN_KEYWORD = new AidlTokenType("boolean");
  IElementType BYTE_KEYWORD = new AidlTokenType("byte");
  IElementType CHARVALUE = new AidlTokenType("CHARVALUE");
  IElementType CHAR_KEYWORD = new AidlTokenType("char");
  IElementType COLON = new AidlTokenType(":");
  IElementType COMMA = new AidlTokenType(",");
  IElementType COMMENT = new AidlTokenType("COMMENT");
  IElementType CONST_KEYWORD = new AidlTokenType("const");
  IElementType CPP_HEADER_KEYWORD = new AidlTokenType("cpp_header");
  IElementType C_STR = new AidlTokenType("C_STR");
  IElementType DIVIDE = new AidlTokenType("/");
  IElementType DOT = new AidlTokenType(".");
  IElementType DOUBLE_KEYWORD = new AidlTokenType("double");
  IElementType ENUM_KEYWORD = new AidlTokenType("enum");
  IElementType EQUALITY = new AidlTokenType("==");
  IElementType FALSE_KEYWORD = new AidlTokenType("false");
  IElementType FLOATVALUE = new AidlTokenType("FLOATVALUE");
  IElementType FLOAT_KEYWORD = new AidlTokenType("float");
  IElementType GEQ = new AidlTokenType(">=");
  IElementType GT = new AidlTokenType(">");
  IElementType HEXVALUE = new AidlTokenType("HEXVALUE");
  IElementType IDENTIFIER = new AidlTokenType("IDENTIFIER");
  IElementType IMPORT_KEYWORD = new AidlTokenType("import");
  IElementType INOUT_KEYWORD = new AidlTokenType("inout");
  IElementType INTERFACE_KEYWORD = new AidlTokenType("interface");
  IElementType INTVALUE = new AidlTokenType("INTVALUE");
  IElementType INT_KEYWORD = new AidlTokenType("int");
  IElementType IN_KEYWORD = new AidlTokenType("in");
  IElementType LBRACE = new AidlTokenType("{");
  IElementType LBRACKET = new AidlTokenType("[");
  IElementType LEQ = new AidlTokenType("<=");
  IElementType LOGICAL_AND = new AidlTokenType("&&");
  IElementType LOGICAL_OR = new AidlTokenType("||");
  IElementType LONG_KEYWORD = new AidlTokenType("long");
  IElementType LPAREN = new AidlTokenType("(");
  IElementType LSHIFT = new AidlTokenType("<<");
  IElementType LT = new AidlTokenType("<");
  IElementType MINUS = new AidlTokenType("-");
  IElementType MODULO = new AidlTokenType("%");
  IElementType MULTIPLY = new AidlTokenType("*");
  IElementType NEQ = new AidlTokenType("!=");
  IElementType NOT = new AidlTokenType("!");
  IElementType ONEWAY_KEYWORD = new AidlTokenType("oneway");
  IElementType OUT_KEYWORD = new AidlTokenType("out");
  IElementType PACKAGE_KEYWORD = new AidlTokenType("package");
  IElementType PARCELABLE_KEYWORD = new AidlTokenType("parcelable");
  IElementType PLUS = new AidlTokenType("+");
  IElementType RBRACE = new AidlTokenType("}");
  IElementType RBRACKET = new AidlTokenType("]");
  IElementType RPAREN = new AidlTokenType(")");
  IElementType RSHIFT = new AidlTokenType(">>");
  IElementType SEMICOLON = new AidlTokenType(";");
  IElementType SHORT_KEYWORD = new AidlTokenType("short");
  IElementType TRUE_KEYWORD = new AidlTokenType("true");
  IElementType UNION_KEYWORD = new AidlTokenType("union");
  IElementType VOID_KEYWORD = new AidlTokenType("void");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ANNOTATION_ELEMENT) {
        return new AidlAnnotationElementImpl(node);
      }
      else if (type == BODY) {
        return new AidlBodyImpl(node);
      }
      else if (type == CONSTANT_DECLARATION) {
        return new AidlConstantDeclarationImpl(node);
      }
      else if (type == DOTTED_NAME) {
        return new AidlDottedNameImpl(node);
      }
      else if (type == ENUMERATOR_DECLARATION) {
        return new AidlEnumeratorDeclarationImpl(node);
      }
      else if (type == ENUM_DECLARATION) {
        return new AidlEnumDeclarationImpl(node);
      }
      else if (type == EXPRESSION) {
        return new AidlExpressionImpl(node);
      }
      else if (type == IMPORT) {
        return new AidlImportImpl(node);
      }
      else if (type == INTERFACE_DECLARATION) {
        return new AidlInterfaceDeclarationImpl(node);
      }
      else if (type == METHOD_DECLARATION) {
        return new AidlMethodDeclarationImpl(node);
      }
      else if (type == NAME_COMPONENT) {
        return new AidlNameComponentImpl(node);
      }
      else if (type == PACKAGE) {
        return new AidlPackageImpl(node);
      }
      else if (type == PARAMETER) {
        return new AidlParameterImpl(node);
      }
      else if (type == PARCELABLE_DECLARATION) {
        return new AidlParcelableDeclarationImpl(node);
      }
      else if (type == QUALIFIED_NAME) {
        return new AidlQualifiedNameImpl(node);
      }
      else if (type == TYPE_ELEMENT) {
        return new AidlTypeElementImpl(node);
      }
      else if (type == UNION_DECLARATION) {
        return new AidlUnionDeclarationImpl(node);
      }
      else if (type == VARIABLE_DECLARATION) {
        return new AidlVariableDeclarationImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
