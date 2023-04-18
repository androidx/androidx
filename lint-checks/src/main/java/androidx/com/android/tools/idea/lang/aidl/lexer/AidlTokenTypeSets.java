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
package androidx.com.android.tools.idea.lang.aidl.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;

public interface AidlTokenTypeSets {
  TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);

  TokenSet COMMENTS = TokenSet.create(AidlTokenTypes.COMMENT, AidlTokenTypes.BLOCK_COMMENT);

  TokenSet BAD_TOKENS = TokenSet.create(TokenType.BAD_CHARACTER);

  TokenSet IDENTIFIERS = TokenSet.create(AidlTokenTypes.IDENTIFIER);

  TokenSet NUMBERS = TokenSet.create(AidlTokenTypes.INTVALUE, AidlTokenTypes.FLOATVALUE, AidlTokenTypes.HEXVALUE);

  TokenSet LITERALS = TokenSet.create(AidlTokenTypes.C_STR, AidlTokenTypes.CHARVALUE);

  TokenSet KEY_WORDS = TokenSet
    .create(
      AidlTokenTypes.BOOLEAN_KEYWORD,
      AidlTokenTypes.BYTE_KEYWORD,
      AidlTokenTypes.CHAR_KEYWORD,
      AidlTokenTypes.CONST_KEYWORD,
      AidlTokenTypes.DOUBLE_KEYWORD,
      AidlTokenTypes.ENUM_KEYWORD,
      AidlTokenTypes.FALSE_KEYWORD,
      AidlTokenTypes.FLOAT_KEYWORD,
      AidlTokenTypes.IMPORT_KEYWORD,
      AidlTokenTypes.INOUT_KEYWORD,
      AidlTokenTypes.INTERFACE_KEYWORD,
      AidlTokenTypes.INT_KEYWORD,
      AidlTokenTypes.IN_KEYWORD,
      AidlTokenTypes.LONG_KEYWORD,
      AidlTokenTypes.ONEWAY_KEYWORD,
      AidlTokenTypes.OUT_KEYWORD,
      AidlTokenTypes.PACKAGE_KEYWORD,
      AidlTokenTypes.PARCELABLE_KEYWORD,
      AidlTokenTypes.SHORT_KEYWORD,
      AidlTokenTypes.TRUE_KEYWORD,
      AidlTokenTypes.UNION_KEYWORD,
      AidlTokenTypes.VOID_KEYWORD
    );

  TokenSet OPERATORS = TokenSet
    .create(
      AidlTokenTypes.PLUS,
      AidlTokenTypes.MINUS,
      AidlTokenTypes.MULTIPLY,
      AidlTokenTypes.DIVIDE,
      AidlTokenTypes.BITWISE_AND,
      AidlTokenTypes.LOGICAL_AND,
      AidlTokenTypes.BITWISE_OR,
      AidlTokenTypes.LOGICAL_OR,
      AidlTokenTypes.BITWISE_XOR,
      AidlTokenTypes.MODULO,
      AidlTokenTypes.ASSIGN,
      AidlTokenTypes.EQUALITY,
      AidlTokenTypes.NEQ,
      AidlTokenTypes.GT,
      AidlTokenTypes.GEQ,
      AidlTokenTypes.LT,
      AidlTokenTypes.LEQ,
      AidlTokenTypes.BITWISE_COMPLEMENT,
      AidlTokenTypes.NOT,
      AidlTokenTypes.LSHIFT,
      AidlTokenTypes.RSHIFT
    );
}
