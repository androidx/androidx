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

package androidx.com.android.tools.idea.lang.aidl.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static androidx.com.android.tools.idea.lang.aidl.lexer.AidlTokenTypes.*;
import static androidx.com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment", "NullableProblems",
        "UnknownNullness"})
public class AidlParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return document(b, l + 1);
  }

  /* ********************************************************** */
  // AT qualified_name LPAREN const_expr RPAREN
  //   |   AT qualified_name LPAREN [annotation_parameter_list] RPAREN
  //   |   AT qualified_name
  public static boolean annotation_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_element")) return false;
    if (!nextTokenIs(b, AT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = annotation_element_0(b, l + 1);
    if (!r) r = annotation_element_1(b, l + 1);
    if (!r) r = annotation_element_2(b, l + 1);
    exit_section_(b, m, ANNOTATION_ELEMENT, r);
    return r;
  }

  // AT qualified_name LPAREN const_expr RPAREN
  private static boolean annotation_element_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_element_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AT);
    r = r && qualified_name(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && const_expr(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // AT qualified_name LPAREN [annotation_parameter_list] RPAREN
  private static boolean annotation_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_element_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AT);
    r = r && qualified_name(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && annotation_element_1_3(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // [annotation_parameter_list]
  private static boolean annotation_element_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_element_1_3")) return false;
    annotation_parameter_list(b, l + 1);
    return true;
  }

  // AT qualified_name
  private static boolean annotation_element_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_element_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AT);
    r = r && qualified_name(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // annotation_element*
  static boolean annotation_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_list")) return false;
    while (true) {
      int c = current_position_(b);
      if (!annotation_element(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "annotation_list", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // name_component ASSIGN const_expr
  static boolean annotation_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_parameter")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = name_component(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && const_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // annotation_parameter (COMMA annotation_parameter)*
  static boolean annotation_parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_parameter_list")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = annotation_parameter(b, l + 1);
    r = r && annotation_parameter_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA annotation_parameter)*
  private static boolean annotation_parameter_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_parameter_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!annotation_parameter_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "annotation_parameter_list_1", c)) break;
    }
    return true;
  }

  // COMMA annotation_parameter
  private static boolean annotation_parameter_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "annotation_parameter_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && annotation_parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // declaration*
  public static boolean body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "body")) return false;
    Marker m = enter_section_(b, l, _NONE_, BODY, "<body>");
    while (true) {
      int c = current_position_(b);
      if (!declaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "body", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // LBRACE expression RBRACE
  static boolean brace_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "brace_expr")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // qualified_name typeArguments?
  static boolean class_or_interface_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_or_interface_type")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = qualified_name(b, l + 1);
    p = r; // pin = 1
    r = r && class_or_interface_type_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // typeArguments?
  private static boolean class_or_interface_type_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_or_interface_type_1")) return false;
    typeArguments(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // TRUE_KEYWORD | FALSE_KEYWORD
  static boolean const_boolean(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "const_boolean")) return false;
    if (!nextTokenIs(b, "", FALSE_KEYWORD, TRUE_KEYWORD)) return false;
    boolean r;
    r = consumeToken(b, TRUE_KEYWORD);
    if (!r) r = consumeToken(b, FALSE_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // expression
  static boolean const_expr(PsiBuilder b, int l) {
    return expression(b, l + 1);
  }

  /* ********************************************************** */
  // [PLUS] [MINUS] (INTVALUE | FLOATVALUE | HEXVALUE)
  static boolean const_number(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "const_number")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = const_number_0(b, l + 1);
    r = r && const_number_1(b, l + 1);
    r = r && const_number_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [PLUS]
  private static boolean const_number_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "const_number_0")) return false;
    consumeToken(b, PLUS);
    return true;
  }

  // [MINUS]
  private static boolean const_number_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "const_number_1")) return false;
    consumeToken(b, MINUS);
    return true;
  }

  // INTVALUE | FLOATVALUE | HEXVALUE
  private static boolean const_number_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "const_number_2")) return false;
    boolean r;
    r = consumeToken(b, INTVALUE);
    if (!r) r = consumeToken(b, FLOATVALUE);
    if (!r) r = consumeToken(b, HEXVALUE);
    return r;
  }

  /* ********************************************************** */
  // CONST_KEYWORD type_element name_component ASSIGN const_expr SEMICOLON
  public static boolean constant_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constant_declaration")) return false;
    if (!nextTokenIs(b, CONST_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CONST_KEYWORD);
    r = r && type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && const_expr(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, CONSTANT_DECLARATION, r);
    return r;
  }

  /* ********************************************************** */
  // const_expr (COMMA const_expr)*
  static boolean constant_value_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constant_value_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = const_expr(b, l + 1);
    r = r && constant_value_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA const_expr)*
  private static boolean constant_value_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constant_value_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!constant_value_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "constant_value_list_1", c)) break;
    }
    return true;
  }

  // COMMA const_expr
  private static boolean constant_value_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constant_value_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && const_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !<<eof>> annotation_list unannotated_declaration
  static boolean declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null, "<interface, parcelable, enum or union>");
    r = declaration_0(b, l + 1);
    r = r && annotation_list(b, l + 1);
    r = r && unannotated_declaration(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !<<eof>>
  private static boolean declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !eof(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // IN_KEYWORD | OUT_KEYWORD | INOUT_KEYWORD
  static boolean direction(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "direction")) return false;
    boolean r;
    r = consumeToken(b, IN_KEYWORD);
    if (!r) r = consumeToken(b, OUT_KEYWORD);
    if (!r) r = consumeToken(b, INOUT_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // optional_package imports body
  static boolean document(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "document")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = optional_package(b, l + 1);
    r = r && imports(b, l + 1);
    r = r && body(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // name_component (DOT name_component)*
  public static boolean dotted_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dotted_name")) return false;
    if (!nextTokenIs(b, "<name>", IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, DOTTED_NAME, "<name>");
    r = name_component(b, l + 1);
    r = r && dotted_name_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (DOT name_component)*
  private static boolean dotted_name_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dotted_name_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!dotted_name_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "dotted_name_1", c)) break;
    }
    return true;
  }

  // DOT name_component
  private static boolean dotted_name_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dotted_name_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && name_component(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LBRACE enumerators RBRACE | LBRACE enumerators COMMA RBRACE
  static boolean enum_decl_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_decl_body")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = enum_decl_body_0(b, l + 1);
    if (!r) r = enum_decl_body_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // LBRACE enumerators RBRACE
  private static boolean enum_decl_body_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_decl_body_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && enumerators(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // LBRACE enumerators COMMA RBRACE
  private static boolean enum_decl_body_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_decl_body_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && enumerators(b, l + 1);
    r = r && consumeTokens(b, 0, COMMA, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ENUM_KEYWORD dotted_name enum_decl_body
  public static boolean enum_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_declaration")) return false;
    if (!nextTokenIs(b, ENUM_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, ENUM_DECLARATION, null);
    r = consumeToken(b, ENUM_KEYWORD);
    r = r && dotted_name(b, l + 1);
    p = r; // pin = 2
    r = r && enum_decl_body(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // name_component ASSIGN const_expr | name_component
  public static boolean enumerator_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumerator_declaration")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = enumerator_declaration_0(b, l + 1);
    if (!r) r = name_component(b, l + 1);
    exit_section_(b, m, ENUMERATOR_DECLARATION, r);
    return r;
  }

  // name_component ASSIGN const_expr
  private static boolean enumerator_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumerator_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = name_component(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && const_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // enumerator_declaration (COMMA enumerator_declaration)*
  static boolean enumerators(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumerators")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = enumerator_declaration(b, l + 1);
    r = r && enumerators_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA enumerator_declaration)*
  private static boolean enumerators_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumerators_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!enumerators_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "enumerators_1", c)) break;
    }
    return true;
  }

  // COMMA enumerator_declaration
  private static boolean enumerators_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumerators_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && enumerator_declaration(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // const_number (PLUS|MINUS|MULTIPLY|DIVIDE|MODULO|EQUALITY|NEQ|LSHIFT|RSHIFT|LEQ|LT|GEQ|GT) expression
  //   |  paren_expr (PLUS|MINUS|MULTIPLY|DIVIDE|MODULO|EQUALITY|NEQ|LSHIFT|RSHIFT|LEQ|LT|GEQ|GT) expression
  //   |  const_number (BITWISE_XOR|BITWISE_OR|BITWISE_AND|LOGICAL_OR|LOGICAL_AND) expression
  //   |  paren_expr (BITWISE_XOR|BITWISE_OR|BITWISE_AND|LOGICAL_OR|LOGICAL_AND) expression
  //   |  LPAREN expression RPAREN
  //   |  (BITWISE_COMPLEMENT|NOT) expression
  //   |  LBRACE constant_value_list [COMMA] RBRACE // allow trailing commas
  //   | C_STR PLUS (C_STR | CHARVALUE)
  //   |  brace_expr
  //   |  const_number
  //   |  const_boolean
  //   |  CHARVALUE
  //   |  C_STR
  //   |  qualified_name
  //   |  LBRACE RBRACE
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, EXPRESSION, "<expression>");
    r = expression_0(b, l + 1);
    if (!r) r = expression_1(b, l + 1);
    if (!r) r = expression_2(b, l + 1);
    if (!r) r = expression_3(b, l + 1);
    if (!r) r = expression_4(b, l + 1);
    if (!r) r = expression_5(b, l + 1);
    if (!r) r = expression_6(b, l + 1);
    if (!r) r = expression_7(b, l + 1);
    if (!r) r = brace_expr(b, l + 1);
    if (!r) r = const_number(b, l + 1);
    if (!r) r = const_boolean(b, l + 1);
    if (!r) r = consumeTokenFast(b, CHARVALUE);
    if (!r) r = consumeTokenFast(b, C_STR);
    if (!r) r = qualified_name(b, l + 1);
    if (!r) r = expression_14(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // const_number (PLUS|MINUS|MULTIPLY|DIVIDE|MODULO|EQUALITY|NEQ|LSHIFT|RSHIFT|LEQ|LT|GEQ|GT) expression
  private static boolean expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = const_number(b, l + 1);
    r = r && expression_0_1(b, l + 1);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // PLUS|MINUS|MULTIPLY|DIVIDE|MODULO|EQUALITY|NEQ|LSHIFT|RSHIFT|LEQ|LT|GEQ|GT
  private static boolean expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_0_1")) return false;
    boolean r;
    r = consumeTokenFast(b, PLUS);
    if (!r) r = consumeTokenFast(b, MINUS);
    if (!r) r = consumeTokenFast(b, MULTIPLY);
    if (!r) r = consumeTokenFast(b, DIVIDE);
    if (!r) r = consumeTokenFast(b, MODULO);
    if (!r) r = consumeTokenFast(b, EQUALITY);
    if (!r) r = consumeTokenFast(b, NEQ);
    if (!r) r = consumeTokenFast(b, LSHIFT);
    if (!r) r = consumeTokenFast(b, RSHIFT);
    if (!r) r = consumeTokenFast(b, LEQ);
    if (!r) r = consumeTokenFast(b, LT);
    if (!r) r = consumeTokenFast(b, GEQ);
    if (!r) r = consumeTokenFast(b, GT);
    return r;
  }

  // paren_expr (PLUS|MINUS|MULTIPLY|DIVIDE|MODULO|EQUALITY|NEQ|LSHIFT|RSHIFT|LEQ|LT|GEQ|GT) expression
  private static boolean expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = paren_expr(b, l + 1);
    r = r && expression_1_1(b, l + 1);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // PLUS|MINUS|MULTIPLY|DIVIDE|MODULO|EQUALITY|NEQ|LSHIFT|RSHIFT|LEQ|LT|GEQ|GT
  private static boolean expression_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_1_1")) return false;
    boolean r;
    r = consumeTokenFast(b, PLUS);
    if (!r) r = consumeTokenFast(b, MINUS);
    if (!r) r = consumeTokenFast(b, MULTIPLY);
    if (!r) r = consumeTokenFast(b, DIVIDE);
    if (!r) r = consumeTokenFast(b, MODULO);
    if (!r) r = consumeTokenFast(b, EQUALITY);
    if (!r) r = consumeTokenFast(b, NEQ);
    if (!r) r = consumeTokenFast(b, LSHIFT);
    if (!r) r = consumeTokenFast(b, RSHIFT);
    if (!r) r = consumeTokenFast(b, LEQ);
    if (!r) r = consumeTokenFast(b, LT);
    if (!r) r = consumeTokenFast(b, GEQ);
    if (!r) r = consumeTokenFast(b, GT);
    return r;
  }

  // const_number (BITWISE_XOR|BITWISE_OR|BITWISE_AND|LOGICAL_OR|LOGICAL_AND) expression
  private static boolean expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = const_number(b, l + 1);
    r = r && expression_2_1(b, l + 1);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BITWISE_XOR|BITWISE_OR|BITWISE_AND|LOGICAL_OR|LOGICAL_AND
  private static boolean expression_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2_1")) return false;
    boolean r;
    r = consumeTokenFast(b, BITWISE_XOR);
    if (!r) r = consumeTokenFast(b, BITWISE_OR);
    if (!r) r = consumeTokenFast(b, BITWISE_AND);
    if (!r) r = consumeTokenFast(b, LOGICAL_OR);
    if (!r) r = consumeTokenFast(b, LOGICAL_AND);
    return r;
  }

  // paren_expr (BITWISE_XOR|BITWISE_OR|BITWISE_AND|LOGICAL_OR|LOGICAL_AND) expression
  private static boolean expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = paren_expr(b, l + 1);
    r = r && expression_3_1(b, l + 1);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BITWISE_XOR|BITWISE_OR|BITWISE_AND|LOGICAL_OR|LOGICAL_AND
  private static boolean expression_3_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_3_1")) return false;
    boolean r;
    r = consumeTokenFast(b, BITWISE_XOR);
    if (!r) r = consumeTokenFast(b, BITWISE_OR);
    if (!r) r = consumeTokenFast(b, BITWISE_AND);
    if (!r) r = consumeTokenFast(b, LOGICAL_OR);
    if (!r) r = consumeTokenFast(b, LOGICAL_AND);
    return r;
  }

  // LPAREN expression RPAREN
  private static boolean expression_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, LPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (BITWISE_COMPLEMENT|NOT) expression
  private static boolean expression_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression_5_0(b, l + 1);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // BITWISE_COMPLEMENT|NOT
  private static boolean expression_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_5_0")) return false;
    boolean r;
    r = consumeTokenFast(b, BITWISE_COMPLEMENT);
    if (!r) r = consumeTokenFast(b, NOT);
    return r;
  }

  // LBRACE constant_value_list [COMMA] RBRACE
  private static boolean expression_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_6")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, LBRACE);
    r = r && constant_value_list(b, l + 1);
    r = r && expression_6_2(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // [COMMA]
  private static boolean expression_6_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_6_2")) return false;
    consumeTokenFast(b, COMMA);
    return true;
  }

  // C_STR PLUS (C_STR | CHARVALUE)
  private static boolean expression_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_7")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, C_STR, PLUS);
    r = r && expression_7_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // C_STR | CHARVALUE
  private static boolean expression_7_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_7_2")) return false;
    boolean r;
    r = consumeTokenFast(b, C_STR);
    if (!r) r = consumeTokenFast(b, CHARVALUE);
    return r;
  }

  // LBRACE RBRACE
  private static boolean expression_14(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_14")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACE, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IMPORT_KEYWORD qualified_name SEMICOLON
  public static boolean import_$(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_$")) return false;
    if (!nextTokenIs(b, IMPORT_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IMPORT, null);
    r = consumeToken(b, IMPORT_KEYWORD);
    r = r && qualified_name(b, l + 1);
    p = r; // pin = 2
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // import*
  static boolean imports(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "imports")) return false;
    while (true) {
      int c = current_position_(b);
      if (!import_$(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "imports", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // [ONEWAY_KEYWORD] INTERFACE_KEYWORD dotted_name SEMICOLON
  //      |   [ONEWAY_KEYWORD] INTERFACE_KEYWORD dotted_name LBRACE interface_member* RBRACE
  public static boolean interface_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_declaration")) return false;
    if (!nextTokenIs(b, "<interface declaration>", INTERFACE_KEYWORD, ONEWAY_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, INTERFACE_DECLARATION, "<interface declaration>");
    r = interface_declaration_0(b, l + 1);
    if (!r) r = interface_declaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // [ONEWAY_KEYWORD] INTERFACE_KEYWORD dotted_name SEMICOLON
  private static boolean interface_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = interface_declaration_0_0(b, l + 1);
    r = r && consumeToken(b, INTERFACE_KEYWORD);
    r = r && dotted_name(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ONEWAY_KEYWORD]
  private static boolean interface_declaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_declaration_0_0")) return false;
    consumeToken(b, ONEWAY_KEYWORD);
    return true;
  }

  // [ONEWAY_KEYWORD] INTERFACE_KEYWORD dotted_name LBRACE interface_member* RBRACE
  private static boolean interface_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_declaration_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = interface_declaration_1_0(b, l + 1);
    r = r && consumeToken(b, INTERFACE_KEYWORD);
    r = r && dotted_name(b, l + 1);
    r = r && consumeToken(b, LBRACE);
    r = r && interface_declaration_1_4(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ONEWAY_KEYWORD]
  private static boolean interface_declaration_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_declaration_1_0")) return false;
    consumeToken(b, ONEWAY_KEYWORD);
    return true;
  }

  // interface_member*
  private static boolean interface_declaration_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_declaration_1_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!interface_member(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "interface_declaration_1_4", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // method_declaration | constant_declaration | declaration
  static boolean interface_member(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_member")) return false;
    boolean r;
    r = method_declaration(b, l + 1);
    if (!r) r = constant_declaration(b, l + 1);
    if (!r) r = declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // type_element name_component LPAREN parameter_list RPAREN SEMICOLON
  //     | annotation_list ONEWAY_KEYWORD type_element name_component LPAREN parameter_list RPAREN SEMICOLON
  //     | type_element name_component LPAREN parameter_list RPAREN ASSIGN INTVALUE SEMICOLON
  //     | annotation_list ONEWAY_KEYWORD type_element name_component LPAREN parameter_list RPAREN ASSIGN INTVALUE SEMICOLON
  public static boolean method_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, METHOD_DECLARATION, "<method declaration>");
    r = method_declaration_0(b, l + 1);
    if (!r) r = method_declaration_1(b, l + 1);
    if (!r) r = method_declaration_2(b, l + 1);
    if (!r) r = method_declaration_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // type_element name_component LPAREN parameter_list RPAREN SEMICOLON
  private static boolean method_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && parameter_list(b, l + 1);
    r = r && consumeTokens(b, 0, RPAREN, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // annotation_list ONEWAY_KEYWORD type_element name_component LPAREN parameter_list RPAREN SEMICOLON
  private static boolean method_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method_declaration_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = annotation_list(b, l + 1);
    r = r && consumeToken(b, ONEWAY_KEYWORD);
    r = r && type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && parameter_list(b, l + 1);
    r = r && consumeTokens(b, 0, RPAREN, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // type_element name_component LPAREN parameter_list RPAREN ASSIGN INTVALUE SEMICOLON
  private static boolean method_declaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method_declaration_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && parameter_list(b, l + 1);
    r = r && consumeTokens(b, 0, RPAREN, ASSIGN, INTVALUE, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // annotation_list ONEWAY_KEYWORD type_element name_component LPAREN parameter_list RPAREN ASSIGN INTVALUE SEMICOLON
  private static boolean method_declaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method_declaration_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = annotation_list(b, l + 1);
    r = r && consumeToken(b, ONEWAY_KEYWORD);
    r = r && type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && parameter_list(b, l + 1);
    r = r && consumeTokens(b, 0, RPAREN, ASSIGN, INTVALUE, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean name_component(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "name_component")) return false;
    if (!nextTokenIs(b, "<name>", IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NAME_COMPONENT, "<name>");
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // package?
  static boolean optional_package(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "optional_package")) return false;
    package_$(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (LT type_params GT)*
  static boolean optional_type_params(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "optional_type_params")) return false;
    while (true) {
      int c = current_position_(b);
      if (!optional_type_params_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "optional_type_params", c)) break;
    }
    return true;
  }

  // LT type_params GT
  private static boolean optional_type_params_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "optional_type_params_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LT);
    r = r && type_params(b, l + 1);
    r = r && consumeToken(b, GT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PACKAGE_KEYWORD qualified_name SEMICOLON
  public static boolean package_$(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "package_$")) return false;
    if (!nextTokenIs(b, PACKAGE_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PACKAGE, null);
    r = consumeToken(b, PACKAGE_KEYWORD);
    r = r && qualified_name(b, l + 1);
    p = r; // pin = 2
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // direction? type_element name_component
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER, "<parameter>");
    r = parameter_0(b, l + 1);
    r = r && type_element(b, l + 1);
    p = r; // pin = 2
    r = r && name_component(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // direction?
  private static boolean parameter_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_0")) return false;
    direction(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (parameter (COMMA parameter)*)*
  static boolean parameter_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameter_list_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list", c)) break;
    }
    return true;
  }

  // parameter (COMMA parameter)*
  private static boolean parameter_list_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parameter(b, l + 1);
    r = r && parameter_list_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA parameter)*
  private static boolean parameter_list_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameter_list_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameter_list_0_1", c)) break;
    }
    return true;
  }

  // COMMA parameter
  private static boolean parameter_list_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_list_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // PARCELABLE_KEYWORD dotted_name optional_type_params LBRACE parcelable_member* RBRACE
  //     |   PARCELABLE_KEYWORD dotted_name optional_type_params SEMICOLON
  //     |   PARCELABLE_KEYWORD dotted_name CPP_HEADER_KEYWORD C_STR SEMICOLON
  public static boolean parcelable_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelable_declaration")) return false;
    if (!nextTokenIs(b, PARCELABLE_KEYWORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parcelable_declaration_0(b, l + 1);
    if (!r) r = parcelable_declaration_1(b, l + 1);
    if (!r) r = parcelable_declaration_2(b, l + 1);
    exit_section_(b, m, PARCELABLE_DECLARATION, r);
    return r;
  }

  // PARCELABLE_KEYWORD dotted_name optional_type_params LBRACE parcelable_member* RBRACE
  private static boolean parcelable_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelable_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PARCELABLE_KEYWORD);
    r = r && dotted_name(b, l + 1);
    r = r && optional_type_params(b, l + 1);
    r = r && consumeToken(b, LBRACE);
    r = r && parcelable_declaration_0_4(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // parcelable_member*
  private static boolean parcelable_declaration_0_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelable_declaration_0_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parcelable_member(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parcelable_declaration_0_4", c)) break;
    }
    return true;
  }

  // PARCELABLE_KEYWORD dotted_name optional_type_params SEMICOLON
  private static boolean parcelable_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelable_declaration_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PARCELABLE_KEYWORD);
    r = r && dotted_name(b, l + 1);
    r = r && optional_type_params(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // PARCELABLE_KEYWORD dotted_name CPP_HEADER_KEYWORD C_STR SEMICOLON
  private static boolean parcelable_declaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelable_declaration_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PARCELABLE_KEYWORD);
    r = r && dotted_name(b, l + 1);
    r = r && consumeTokens(b, 0, CPP_HEADER_KEYWORD, C_STR, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // variable_declaration | constant_declaration | declaration
  static boolean parcelable_member(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parcelable_member")) return false;
    boolean r;
    r = variable_declaration(b, l + 1);
    if (!r) r = constant_declaration(b, l + 1);
    if (!r) r = declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LPAREN expression RPAREN
  static boolean paren_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paren_expr")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // BOOLEAN_KEYWORD
  //   |   BYTE_KEYWORD
  //   |   CHAR_KEYWORD
  //   |   SHORT_KEYWORD
  //   |   INT_KEYWORD
  //   |   LONG_KEYWORD
  //   |   FLOAT_KEYWORD
  //   |   DOUBLE_KEYWORD
  static boolean primitiveType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "primitiveType")) return false;
    boolean r;
    r = consumeToken(b, BOOLEAN_KEYWORD);
    if (!r) r = consumeToken(b, BYTE_KEYWORD);
    if (!r) r = consumeToken(b, CHAR_KEYWORD);
    if (!r) r = consumeToken(b, SHORT_KEYWORD);
    if (!r) r = consumeToken(b, INT_KEYWORD);
    if (!r) r = consumeToken(b, LONG_KEYWORD);
    if (!r) r = consumeToken(b, FLOAT_KEYWORD);
    if (!r) r = consumeToken(b, DOUBLE_KEYWORD);
    return r;
  }

  /* ********************************************************** */
  // name_component (DOT name_component)*
  public static boolean qualified_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualified_name")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = name_component(b, l + 1);
    r = r && qualified_name_1(b, l + 1);
    exit_section_(b, m, QUALIFIED_NAME, r);
    return r;
  }

  // (DOT name_component)*
  private static boolean qualified_name_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualified_name_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!qualified_name_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "qualified_name_1", c)) break;
    }
    return true;
  }

  // DOT name_component
  private static boolean qualified_name_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualified_name_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && name_component(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LT type_element (COMMA type_element)* GT
  static boolean typeArguments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeArguments")) return false;
    if (!nextTokenIs(b, LT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LT);
    p = r; // pin = 1
    r = r && report_error_(b, type_element(b, l + 1));
    r = p && report_error_(b, typeArguments_2(b, l + 1)) && r;
    r = p && consumeToken(b, GT) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (COMMA type_element)*
  private static boolean typeArguments_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeArguments_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!typeArguments_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "typeArguments_2", c)) break;
    }
    return true;
  }

  // COMMA type_element
  private static boolean typeArguments_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeArguments_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && type_element(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // annotation_list VOID_KEYWORD | annotation_list ((primitiveType | class_or_interface_type) (LBRACKET RBRACKET)*)
  public static boolean type_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, TYPE_ELEMENT, "<type element>");
    r = type_element_0(b, l + 1);
    if (!r) r = type_element_1(b, l + 1);
    exit_section_(b, l, m, r, false, AidlParser::type_recover);
    return r;
  }

  // annotation_list VOID_KEYWORD
  private static boolean type_element_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = annotation_list(b, l + 1);
    r = r && consumeToken(b, VOID_KEYWORD);
    exit_section_(b, m, null, r);
    return r;
  }

  // annotation_list ((primitiveType | class_or_interface_type) (LBRACKET RBRACKET)*)
  private static boolean type_element_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = annotation_list(b, l + 1);
    r = r && type_element_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (primitiveType | class_or_interface_type) (LBRACKET RBRACKET)*
  private static boolean type_element_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_element_1_1_0(b, l + 1);
    r = r && type_element_1_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // primitiveType | class_or_interface_type
  private static boolean type_element_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element_1_1_0")) return false;
    boolean r;
    r = primitiveType(b, l + 1);
    if (!r) r = class_or_interface_type(b, l + 1);
    return r;
  }

  // (LBRACKET RBRACKET)*
  private static boolean type_element_1_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element_1_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!type_element_1_1_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "type_element_1_1_1", c)) break;
    }
    return true;
  }

  // LBRACKET RBRACKET
  private static boolean type_element_1_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_element_1_1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACKET, RBRACKET);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // name_component (COMMA name_component)*
  static boolean type_params(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_params")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = name_component(b, l + 1);
    r = r && type_params_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (COMMA name_component)*
  private static boolean type_params_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_params_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!type_params_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "type_params_1", c)) break;
    }
    return true;
  }

  // COMMA name_component
  private static boolean type_params_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_params_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && name_component(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(qualified_name | LPAREN | COMMA | GT | LBRACE)
  static boolean type_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !type_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // qualified_name | LPAREN | COMMA | GT | LBRACE
  private static boolean type_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_recover_0")) return false;
    boolean r;
    r = qualified_name(b, l + 1);
    if (!r) r = consumeToken(b, LPAREN);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, GT);
    if (!r) r = consumeToken(b, LBRACE);
    return r;
  }

  /* ********************************************************** */
  // parcelable_declaration | interface_declaration | enum_declaration | union_declaration
  static boolean unannotated_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unannotated_declaration")) return false;
    boolean r;
    r = parcelable_declaration(b, l + 1);
    if (!r) r = interface_declaration(b, l + 1);
    if (!r) r = enum_declaration(b, l + 1);
    if (!r) r = union_declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // UNION_KEYWORD dotted_name optional_type_params LBRACE parcelable_member* RBRACE
  public static boolean union_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_declaration")) return false;
    if (!nextTokenIs(b, UNION_KEYWORD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, UNION_DECLARATION, null);
    r = consumeToken(b, UNION_KEYWORD);
    r = r && dotted_name(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, optional_type_params(b, l + 1));
    r = p && report_error_(b, consumeToken(b, LBRACE)) && r;
    r = p && report_error_(b, union_declaration_4(b, l + 1)) && r;
    r = p && consumeToken(b, RBRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // parcelable_member*
  private static boolean union_declaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_declaration_4")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parcelable_member(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "union_declaration_4", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // type_element name_component SEMICOLON
  //   |   type_element name_component ASSIGN const_expr SEMICOLON
  public static boolean variable_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE_DECLARATION, "<variable declaration>");
    r = variable_declaration_0(b, l + 1);
    if (!r) r = variable_declaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // type_element name_component SEMICOLON
  private static boolean variable_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_declaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // type_element name_component ASSIGN const_expr SEMICOLON
  private static boolean variable_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_declaration_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = type_element(b, l + 1);
    r = r && name_component(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && const_expr(b, l + 1);
    r = r && consumeToken(b, SEMICOLON);
    exit_section_(b, m, null, r);
    return r;
  }

}
