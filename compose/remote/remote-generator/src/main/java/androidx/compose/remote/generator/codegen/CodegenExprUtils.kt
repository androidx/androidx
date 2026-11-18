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

package androidx.compose.remote.generator.codegen

import androidx.compose.remote.generator.OperationSchema
import androidx.compose.remote.generator.SchemaBitField
import androidx.compose.remote.generator.SchemaConditionalSkip
import androidx.compose.remote.generator.SchemaSkipConditionBranch

internal enum class ExprLang {
    KOTLIN,
    CPP,
    RUST,
    PYTHON,
    SWIFT,
    TYPESCRIPT,
}

internal object CodegenExprUtils {

    fun cleanSchemaIdentifier(name: String): String =
        name.replace("[0..n]", "Array").replace("[", "_").replace("]", "").replace(".", "_")

    /**
     * Minimum number of wire bytes required to encode a single element of [elemType]. When [schema]
     * is provided, recursively computes the exact minimum wire size of struct elements from their
     * unconditional fields.
     */
    fun minWireElementBytes(
        elemType: String,
        schema: OperationSchema? = null,
        visitedStructs: Set<String> = emptySet(),
    ): Int =
        when (elemType) {
            "byte",
            "boolean" -> 1
            "short" -> 2
            "int",
            "int_id",
            "color",
            "float",
            "float_or_id",
            "string",
            "bytes" -> 4
            "long" -> 8
            else -> {
                val structDef = schema?.structs?.get(elemType)
                if (structDef != null && elemType !in visitedStructs) {
                    val nextVisited = visitedStructs + elemType
                    var total = 0
                    for (field in structDef.fields) {
                        if (field.presentIf != null || field.addedVersion != null) continue
                        total +=
                            if (field.bits != null && field.unpackBits) {
                                4
                            } else if (field.type.endsWith("[]")) {
                                if (field.countFrom != null) {
                                    0
                                } else {
                                    when (field.effectiveLengthType) {
                                        "int" -> 4
                                        else -> 2
                                    }
                                }
                            } else {
                                minWireElementBytes(field.type, schema, nextVisited)
                            }
                    }
                    total.coerceAtLeast(1)
                } else {
                    1
                }
            }
        }

    fun toSnakeCase(name: String): String {
        val result = StringBuilder()
        for (i in name.indices) {
            val c = name[i]
            if (c.isUpperCase()) {
                if (i > 0 && result.isNotEmpty() && result.last() != '_') {
                    val prev = name[i - 1]
                    val next = if (i + 1 < name.length) name[i + 1] else ' '
                    if (
                        prev.isLowerCase() ||
                            prev.isDigit() ||
                            (prev.isUpperCase() && next.isLowerCase())
                    ) {
                        result.append('_')
                    }
                }
                result.append(c.lowercaseChar())
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }

    fun translateCondition(
        expr: String,
        lang: ExprLang,
        needsKotlinIntCast: (String) -> Boolean = { true },
        isKotlinUnsignedByte: (String) -> Boolean = { false },
        resolveIdent: (String) -> String,
    ): String {
        val tokens = tokenize(expr)
        val parser =
            ConditionParser(tokens, lang, needsKotlinIntCast, isKotlinUnsignedByte, resolveIdent)
        return parser.parseOr()
    }

    fun extractBitExpr(src: String, bit: SchemaBitField, lang: ExprLang): String {
        val condRange = bit.whenBitSet
        if (condRange != null) {
            val b = condRange.bit
            require(b in 0..31) {
                "Invalid whenBitSet bit $b for bitfield '${bit.name}' (must be 0..31)"
            }
            val normalBit = SchemaBitField(name = bit.name, range = bit.range, signed = bit.signed)
            val narrowBit = SchemaBitField(name = bit.name, range = condRange.range, signed = false)
            val normalExpr = extractBitExpr(src, normalBit, lang)
            val narrowExpr = extractBitExpr(src, narrowBit, lang)
            return when (lang) {
                ExprLang.KOTLIN -> "(if ((($src ushr $b) and 1) != 0) $narrowExpr else $normalExpr)"
                ExprLang.CPP ->
                    "(((static_cast<uint32_t>($src) >> $b) & 1u) != 0u ? $narrowExpr : $normalExpr)"
                ExprLang.RUST ->
                    "(if ((($src as u32) >> $b) & 1) != 0 { $narrowExpr } else { $normalExpr })"
                ExprLang.SWIFT ->
                    "(((UInt32(bitPattern: $src) >> $b) & 1) != 0 ? $narrowExpr : $normalExpr)"
                ExprLang.TYPESCRIPT -> "(((($src >>> $b) & 1) !== 0) ? $narrowExpr : $normalExpr)"
                ExprLang.PYTHON ->
                    "($narrowExpr if ((($src & 0xFFFFFFFF) >> $b) & 1) != 0 else $normalExpr)"
            }
        }
        val low = bit.lowBit
        val high = bit.highBit
        require(low in 0..31 && high in low..31) {
            "Invalid bit range $low..$high for bitfield '${bit.name}'"
        }
        val width = bit.bitWidth
        val maskHex = "0x" + bit.bitMask.toString(16).uppercase()
        if (width >= 32) return src

        return if (bit.signed) {
            val leftShift = 31 - high
            val rightShift = 31 - high + low
            when (lang) {
                ExprLang.KOTLIN ->
                    if (leftShift == 0) "($src shr $rightShift)"
                    else "(($src shl $leftShift) shr $rightShift)"
                ExprLang.CPP -> {
                    val signBitHex = "0x" + (1L shl (width - 1)).toString(16).uppercase()
                    val unsignedExtract =
                        if (low == 0) "(static_cast<uint32_t>($src) & ${maskHex}u)"
                        else "((static_cast<uint32_t>($src) >> $low) & ${maskHex}u)"
                    "static_cast<int32_t>(($unsignedExtract ^ ${signBitHex}u) - ${signBitHex}u)"
                }
                ExprLang.SWIFT ->
                    if (leftShift == 0) "($src >> $rightShift)"
                    else
                        "(Int32(bitPattern: UInt32(bitPattern: $src) << $leftShift) >> $rightShift)"
                ExprLang.RUST,
                ExprLang.TYPESCRIPT ->
                    if (leftShift == 0) "($src >> $rightShift)"
                    else "(($src << $leftShift) >> $rightShift)"
                ExprLang.PYTHON -> {
                    val signSub = "0x" + (1L shl width).toString(16).uppercase()
                    "(((($src & 0xFFFFFFFF) >> $low) & $maskHex) - ($signSub if ((($src & 0xFFFFFFFF) >> $high) & 1) else 0))"
                }
            }
        } else {
            when (lang) {
                ExprLang.KOTLIN ->
                    if (low == 0) "($src and $maskHex)" else "(($src ushr $low) and $maskHex)"
                ExprLang.CPP ->
                    if (low == 0) "static_cast<int32_t>(static_cast<uint32_t>($src) & ${maskHex}u)"
                    else "static_cast<int32_t>((static_cast<uint32_t>($src) >> $low) & ${maskHex}u)"
                ExprLang.RUST ->
                    if (low == 0) "(($src as u32) & $maskHex) as i32"
                    else "((($src as u32) >> $low) & $maskHex) as i32"
                ExprLang.SWIFT ->
                    if (low == 0) "Int32(UInt32(bitPattern: $src) & $maskHex)"
                    else "Int32((UInt32(bitPattern: $src) >> $low) & $maskHex)"
                ExprLang.TYPESCRIPT ->
                    if (low == 0) "($src & $maskHex)" else "(($src >>> $low) & $maskHex)"
                ExprLang.PYTHON ->
                    if (low == 0) "($src & $maskHex)"
                    else "((($src & 0xFFFFFFFF) >> $low) & $maskHex)"
            }
        }
    }

    fun packBitsExpr(
        terms: List<Pair<String, SchemaBitField>>,
        lang: ExprLang,
    ): String {
        if (terms.isEmpty()) return "0"
        val pieces = terms.map { (valExpr, bit) ->
            val low = bit.lowBit
            val maskHex = "0x" + bit.bitMask.toString(16).uppercase()
            when (lang) {
                ExprLang.KOTLIN ->
                    if (low == 0) "($valExpr and $maskHex)"
                    else "(($valExpr and $maskHex) shl $low)"
                ExprLang.CPP ->
                    if (low == 0) "(static_cast<uint32_t>($valExpr) & ${maskHex}u)"
                    else "((static_cast<uint32_t>($valExpr) & ${maskHex}u) << $low)"
                ExprLang.RUST ->
                    if (low == 0) "(($valExpr as u32) & $maskHex)"
                    else "((($valExpr as u32) & $maskHex) << $low)"
                ExprLang.SWIFT ->
                    if (low == 0) "(UInt32(bitPattern: Int32($valExpr)) & $maskHex)"
                    else "((UInt32(bitPattern: Int32($valExpr)) & $maskHex) << $low)"
                ExprLang.TYPESCRIPT,
                ExprLang.PYTHON ->
                    if (low == 0) "(($valExpr & $maskHex))" else "((($valExpr & $maskHex) << $low))"
            }
        }
        val joined =
            when (lang) {
                ExprLang.KOTLIN -> pieces.joinToString(" or ")
                else -> pieces.joinToString(" | ")
            }
        return when (lang) {
            ExprLang.CPP -> "static_cast<int32_t>($joined)"
            ExprLang.RUST -> "($joined) as i32"
            ExprLang.SWIFT -> "Int32(bitPattern: $joined)"
            ExprLang.TYPESCRIPT -> "($joined) | 0"
            ExprLang.PYTHON -> {
                val raw = "($joined) & 0xFFFFFFFF"
                "(($raw) - 0x100000000 if ($raw) >= 0x80000000 else ($raw))"
            }
            ExprLang.KOTLIN -> joined
        }
    }

    fun nonZeroBitsCheckExpr(src: String, range: String, lang: ExprLang): String {
        val low = range.substringBefore("..").toInt()
        val high = range.substringAfter("..", range).toInt()
        require(low in 0..31 && high in low..31) {
            "Invalid mustBeZeroBits range '$range'"
        }
        val width = high - low + 1
        val mask = if (width >= 32) 0xFFFFFFFFL else ((1L shl width) - 1L) shl low
        val maskHex = "0x" + mask.toString(16).uppercase()
        return when (lang) {
            ExprLang.KOTLIN -> "(($src.toLong() and ${maskHex}L) != 0L)"
            ExprLang.CPP -> "((static_cast<uint32_t>($src) & ${maskHex}u) != 0u)"
            ExprLang.RUST -> "((($src as u32) & $maskHex) != 0)"
            ExprLang.SWIFT -> "((UInt32(bitPattern: Int32($src)) & $maskHex) != 0)"
            ExprLang.TYPESCRIPT -> "(($src & $maskHex) !== 0)"
            ExprLang.PYTHON -> "(($src & $maskHex) != 0)"
        }
    }

    fun extractSkipConditionExpr(
        cs: SchemaConditionalSkip,
        condArg: String,
        lang: ExprLang,
    ): String {
        val bit =
            SchemaBitField(
                name = "condition",
                range = cs.conditionBits,
                signed = cs.conditionSigned,
            )
        return extractBitExpr(condArg, bit, lang)
    }

    fun evaluateSkipBranchExpr(
        branch: SchemaSkipConditionBranch,
        valArg: String,
        apiArg: String,
        profArg: String,
        lang: ExprLang,
    ): String {
        val targetExpr = if (branch.target == "apiLevel") apiArg else profArg
        return when (branch.op) {
            "<" -> "$targetExpr < $valArg"
            ">" -> "$targetExpr > $valArg"
            "==" ->
                if (lang == ExprLang.TYPESCRIPT) "$targetExpr === $valArg"
                else "$targetExpr == $valArg"
            "!=" ->
                if (lang == ExprLang.TYPESCRIPT) "$targetExpr !== $valArg"
                else "$targetExpr != $valArg"
            "mask_any" ->
                when (lang) {
                    ExprLang.KOTLIN -> "($targetExpr and $valArg) != 0"
                    ExprLang.TYPESCRIPT -> "($targetExpr & $valArg) !== 0"
                    else -> "($targetExpr & $valArg) != 0"
                }
            "mask_none" ->
                when (lang) {
                    ExprLang.KOTLIN -> "($targetExpr and $valArg) == 0"
                    ExprLang.TYPESCRIPT -> "($targetExpr & $valArg) === 0"
                    else -> "($targetExpr & $valArg) == 0"
                }
            else -> error("Unsupported conditionalSkip op '${branch.op}'")
        }
    }

    private fun tokenize(expr: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                i + 1 < expr.length &&
                    expr.substring(i, i + 2) in setOf("||", "&&", "==", "!=", ">=", "<=", ">>") -> {
                    result.add(expr.substring(i, i + 2))
                    i += 2
                }
                c in setOf('(', ')', '&', '>', '<') -> {
                    result.add(c.toString())
                    i++
                }
                c.isDigit() || (c == '-' && i + 1 < expr.length && expr[i + 1].isDigit()) -> {
                    val start = i
                    i++
                    while (i < expr.length && (expr[i].isLetterOrDigit() || expr[i] == '_')) i++
                    result.add(expr.substring(start, i))
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    i++
                    while (i < expr.length && (expr[i].isLetterOrDigit() || expr[i] == '_')) i++
                    result.add(expr.substring(start, i))
                }
                else -> throw IllegalArgumentException("Unexpected char '$c' in '$expr'")
            }
        }
        return result
    }

    private data class PrimaryExpr(val code: String, val rawIdent: String?)

    private class ConditionParser(
        private val tokens: List<String>,
        private val lang: ExprLang,
        private val needsKotlinIntCast: (String) -> Boolean,
        private val isKotlinUnsignedByte: (String) -> Boolean,
        private val resolveIdent: (String) -> String,
    ) {
        private var pos = 0

        private fun peek(): String? = tokens.getOrNull(pos)

        private fun next(): String = tokens[pos++]

        fun parseOr(): String {
            val parts = mutableListOf(parseAnd())
            while (peek() == "||") {
                next()
                parts.add(parseAnd())
            }
            val sep = if (lang == ExprLang.PYTHON) " or " else " || "
            return parts.joinToString(sep)
        }

        private fun parseAnd(): String {
            val parts = mutableListOf(parseCmp())
            while (peek() == "&&") {
                next()
                parts.add(parseCmp())
            }
            val sep = if (lang == ExprLang.PYTHON) " and " else " && "
            return parts.joinToString(sep)
        }

        private fun toKotlinIntOperand(expr: PrimaryExpr): String {
            if (lang == ExprLang.KOTLIN && expr.rawIdent != null) {
                if (isKotlinUnsignedByte(expr.rawIdent)) {
                    return "(${expr.code}.toInt() and 0xFF)"
                }
                if (needsKotlinIntCast(expr.rawIdent)) {
                    return "${expr.code}.toInt()"
                }
            }
            return expr.code
        }

        private fun parseCmp(): String {
            val left = parseBitAnd()
            val op = peek()
            if (op in setOf("==", "!=", ">=", "<=", ">", "<")) {
                next()
                val right = parseBitAnd()
                val mappedOp =
                    if (lang == ExprLang.TYPESCRIPT) {
                        when (op) {
                            "==" -> "==="
                            "!=" -> "!=="
                            else -> op!!
                        }
                    } else {
                        op!!
                    }
                val lhsCode = toKotlinIntOperand(left)
                val rhsCode = toKotlinIntOperand(right)
                return "$lhsCode $mappedOp $rhsCode"
            }
            return left.code
        }

        private fun parseBitAnd(): PrimaryExpr {
            val first = parseShift()
            if (peek() != "&") return first
            val parts = mutableListOf(first)
            while (peek() == "&") {
                next()
                parts.add(parseShift())
            }
            val code =
                if (lang == ExprLang.KOTLIN) {
                    val mapped = parts.map { toKotlinIntOperand(it) }
                    "(" + mapped.joinToString(" and ") + ")"
                } else {
                    "(" + parts.joinToString(" & ") { it.code } + ")"
                }
            return PrimaryExpr(code, null)
        }

        private fun parseShift(): PrimaryExpr {
            val first = parsePrimary()
            if (peek() != ">>") return first
            next()
            val shiftAmount = parsePrimary()
            return if (lang == ExprLang.KOTLIN) {
                val lhs = toKotlinIntOperand(first)
                PrimaryExpr("($lhs shr ${shiftAmount.code})", null)
            } else {
                PrimaryExpr("(${first.code} >> ${shiftAmount.code})", null)
            }
        }

        private fun parsePrimary(): PrimaryExpr {
            val tok = next()
            if (tok == "(") {
                val inner = parseOr()
                require(next() == ")") { "Expected ')' in condition expression" }
                return PrimaryExpr("($inner)", null)
            }
            if (tok.first().isDigit() || tok.startsWith("-")) {
                return PrimaryExpr(tok, null)
            }
            return PrimaryExpr(resolveIdent(tok), tok)
        }
    }
}
