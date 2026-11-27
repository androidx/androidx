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
import androidx.compose.remote.generator.SchemaField
import androidx.compose.remote.generator.SchemaOperation
import androidx.compose.remote.generator.SchemaPathCommand

/**
 * Generates schema-driven protocol constants, op/struct enum constants, and sub-wire codecs
 * (`FloatAnimationSpec`, `PathEffect`, `PathCommand`, `IdEncoding`, `PaintConstants`,
 * `RemoteContextIds`) across C++, Rust, Python, Swift, and TypeScript from `operations.json`.
 */
internal class MultiLangProtocolGenerator(private val schema: OperationSchema) {

    fun collectEnumConstants(fields: List<SchemaField>): List<Pair<String, Int>> {
        val seen = linkedMapOf<String, Int>()
        for (f in fields) {
            f.enumValues
                ?.entries
                ?.sortedBy { it.value }
                ?.forEach { (k, v) ->
                    seen.putIfAbsent(k, v)
                }
            f.bits?.forEach { b ->
                b.enumValues
                    ?.entries
                    ?.sortedBy { it.value }
                    ?.forEach { (k, v) ->
                        seen.putIfAbsent(k, v)
                    }
            }
        }
        return seen.entries.map { it.key to it.value }
    }

    private fun collectPaintConstants(): List<Pair<String, Int>> {
        val out = mutableListOf<Pair<String, Int>>()
        val byName = schema.paintCommands.values.associateBy { it.name }
        fun add(cmdName: String, fieldName: String, prefix: String) {
            val em =
                byName[cmdName]?.fields?.firstOrNull { it.name == fieldName }?.enumValues ?: return
            for ((k, v) in em.entries.sortedBy { it.value }) {
                val constName =
                    if (prefix == "BLEND_MODE_" && k == "PORTER_MODE_ADD") {
                        "PORTER_MODE_ADD"
                    } else {
                        "$prefix$k"
                    }
                out.add(constName to v)
            }
        }
        add("Style", "style", "STYLE_")
        add("StrokeCap", "cap", "CAP_")
        add("StrokeJoin", "join", "JOIN_")
        add("Gradient", "tileMode", "TILE_")
        add("Typeface", "fontId", "FONT_TYPE_")
        add("BlendMode", "mode", "BLEND_MODE_")
        add("Gradient", "type", "GRADIENT_")
        out.add("TYPEFACE_WEIGHT_MASK" to 0x3FF)
        out.add("TYPEFACE_FONT_DATA_BIT" to (1 shl 10))
        out.add("TYPEFACE_ITALIC_BIT" to (1 shl 11))
        return out
    }

    private fun pathCmdVariantName(cmd: SchemaPathCommand): String =
        when (cmd.name) {
            "Move" -> "MoveTo"
            "Line" -> "LineTo"
            "Quadratic" -> "QuadTo"
            "Conic" -> "ConicTo"
            "Cubic" -> "CubicTo"
            else -> cmd.name
        }

    private fun isBitmapDataReady(op: SchemaOperation): Boolean =
        op.name == "BitmapData" &&
            op.fields
                .firstOrNull { it.name == "widthAndType" }
                ?.bits
                ?.firstOrNull { it.name == "imageType" }
                ?.enumValues != null

    // =========================================================================================
    // C++17
    // =========================================================================================

    fun emitCppOpEnumConstants(w: CodeWriter, fields: List<SchemaField>) {
        val consts = collectEnumConstants(fields)
        if (consts.isEmpty()) return
        for ((k, v) in consts) {
            w.line("static constexpr int32_t $k = $v;")
        }
    }

    fun emitCppHeaderProtocol(w: CodeWriter) {
        w.line("inline int32_t floatToRawBits(float v) {")
        w.indent {
            w.line("int32_t bits = 0;")
            w.line("std::memcpy(&bits, &v, sizeof(bits));")
            w.line("return bits;")
        }
        w.line("}")
        w.line("inline float bitsToFloat(int32_t bits) {")
        w.indent {
            w.line("float v = 0.0f;")
            w.line("std::memcpy(&v, &bits, sizeof(v));")
            w.line("return v;")
        }
        w.line("}")
        w.line()

        schema.idEncoding?.let { enc ->
            w.block("namespace IdEncoding", close = "} // namespace IdEncoding") {
                line("inline constexpr int32_t NAN_PREFIX = ${enc.nanPrefix};")
                line("inline constexpr int32_t NAN_EXPONENT = ${enc.nanExponent};")
                line("inline constexpr int32_t PAYLOAD_MASK = ${enc.payloadMask};")
                line("inline constexpr int32_t NO_ID = ${enc.noId};")
                line("inline constexpr int32_t START_ID = ${enc.startId};")
                line("inline constexpr int32_t REGION_MASK = ${enc.regionMask};")
                line("inline constexpr int32_t REGION_SUB_ID_MASK = ${enc.regionSubIdMask};")
                line("inline constexpr int32_t REGION_SYSTEM = ${enc.regionSystem};")
                line("inline constexpr int32_t REGION_VARIABLE = ${enc.regionVariable};")
                line("inline constexpr int32_t REGION_ARRAY = ${enc.regionArray};")
                line("inline constexpr int32_t REGION_OPERATION = ${enc.regionOperation};")
                line("inline constexpr int32_t START_ARRAY_ID = ${enc.regionArray + enc.startId};")
                line("inline constexpr int64_t LONG_ID_OFFSET = ${enc.longIdOffset}LL;")
                line("inline constexpr int32_t PTR_DEREFERENCE = ${enc.ptrDereference};")
                line("inline constexpr int32_t VALUE_MASK = ${enc.valueMask};")
                line("inline constexpr int32_t MACRO_LOCAL_START_ID = ${enc.macroLocalStartId};")
                line("inline constexpr int32_t MACRO_LOCAL_END_ID = ${enc.macroLocalEndId};")
                line(
                    "inline bool isNanBits(int32_t bits) { return (bits & NAN_EXPONENT) == NAN_EXPONENT && (bits & PAYLOAD_MASK) != 0; }"
                )
                line(
                    "inline int32_t idFromNanBits(int32_t bits) { return isNanBits(bits) ? (bits & PAYLOAD_MASK) : NO_ID; }"
                )
                line(
                    "inline int32_t asNanBits(int32_t id) { return (id & PAYLOAD_MASK) | NAN_PREFIX; }"
                )
                line(
                    "inline bool isDereferenced(int32_t raw) { return (raw & PTR_DEREFERENCE) != 0; }"
                )
                line(
                    "inline std::optional<int32_t> directId(int32_t raw) { return isDereferenced(raw) ? std::nullopt : std::optional<int32_t>(raw & VALUE_MASK); }"
                )
                line(
                    "inline std::optional<int32_t> pointerId(int32_t raw) { return isDereferenced(raw) ? std::optional<int32_t>(raw & VALUE_MASK) : std::nullopt; }"
                )
            }
            w.line()
        }

        if (schema.headerConstants.isNotEmpty()) {
            w.block("namespace HeaderConstants", close = "} // namespace HeaderConstants") {
                for ((_, group) in schema.headerConstants) {
                    for ((k, v) in group.entries.sortedBy { it.value }) {
                        line("inline constexpr int32_t $k = $v;")
                    }
                }
            }
            w.line()
        }

        if (schema.paintCommands.isNotEmpty()) {
            w.block("namespace PaintCommandTags", close = "} // namespace PaintCommandTags") {
                for ((id, cmd) in schema.paintCommands.entries.sortedBy { it.key }) {
                    line(
                        "inline constexpr int32_t ${SchemaOperation.toScreamingSnakeCase(cmd.name)} = $id;"
                    )
                }
            }
            w.line()
            w.block("namespace PaintConstants", close = "} // namespace PaintConstants") {
                for ((k, v) in collectPaintConstants()) {
                    line("inline constexpr int32_t $k = $v;")
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty()) {
            w.block("namespace PathCommandTags", close = "} // namespace PathCommandTags") {
                for ((id, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    line(
                        "inline constexpr int32_t ${SchemaOperation.toScreamingSnakeCase(cmd.name)} = $id;"
                    )
                }
            }
            w.line()
        }

        if (schema.systemVariables.isNotEmpty()) {
            val timeIds = schema.systemVariables.filter { it.isTime }.map { it.id }.sorted()
            w.block("namespace RemoteContextIds", close = "} // namespace RemoteContextIds") {
                for (sv in schema.systemVariables) {
                    line("inline constexpr int32_t ID_${sv.name} = ${sv.id};")
                }
                val isTimeExpr =
                    if (timeIds.isEmpty()) {
                        "false"
                    } else if (timeIds.size == timeIds.last() - timeIds.first() + 1) {
                        "id >= ${timeIds.first()} && id <= ${timeIds.last()}"
                    } else {
                        timeIds.joinToString(" || ") { "id == $it" }
                    }
                line("inline bool isTime(int32_t id) { return $isTimeExpr; }")
            }
            w.line()
        }

        schema.animationSpec?.let { anim ->
            w.block("namespace AnimationEasing", close = "} // namespace AnimationEasing") {
                for (e in anim.easings.sortedBy { it.id }) {
                    line("inline constexpr int32_t ${e.name} = ${e.id};")
                    for (a in e.aliases) {
                        line("inline constexpr int32_t $a = ${e.id};")
                    }
                }
                block(
                    "inline std::optional<std::array<float, 4>> cubicControlPoints(int32_t type)"
                ) {
                    block("switch (type)") {
                        for (e in anim.easings.sortedBy { it.id }) {
                            val pts = e.cubicControlPoints ?: continue
                            line(
                                "case ${e.name}: return std::array<float, 4>{${pts[0]}f, ${pts[1]}f, ${pts[2]}f, ${pts[3]}f};"
                            )
                        }
                        line("default: return std::nullopt;")
                    }
                }
            }
            w.line()
            w.block("struct FloatAnimationSpec", close = "};") {
                line("float duration = 1.0f;")
                line("int32_t type = AnimationEasing::CUBIC_STANDARD;")
                line("std::vector<float> params;")
                line("float initialValue = std::numeric_limits<float>::quiet_NaN();")
                line("float wrap = std::numeric_limits<float>::quiet_NaN();")
                line("int32_t directionalSnap = 0;")
                line("bool isPropagate = false;")
            }
            w.line()
            w.line(
                "inline bool isSpringAnimationSpec(const std::vector<float>& data) { return data.size() > 4 && data[0] == 0.0f; }"
            )
            w.block(
                "inline FloatAnimationSpec unpackAnimationSpec(const std::vector<float>& data)"
            ) {
                line(
                    "if (isSpringAnimationSpec(data)) throw std::invalid_argument(\"Spring animation specs cannot be unpacked with unpackAnimationSpec\");"
                )
                line("FloatAnimationSpec out;")
                line("if (data.empty()) return out;")
                line("out.duration = data[0];")
                line("if (data.size() == 1) return out;")
                line("uint32_t bits = static_cast<uint32_t>(floatToRawBits(data[1]));")
                line("out.type = static_cast<int32_t>(bits & 0xFFu);")
                line("bool hasWrap = ((bits >> 8) & 0x1u) != 0u;")
                line("bool hasInit = ((bits >> 9) & 0x1u) != 0u;")
                line("out.directionalSnap = static_cast<int32_t>((bits >> 10) & 0x3u);")
                line("out.isPropagate = ((bits >> 12) & 0x1u) != 0u;")
                line("size_t paramLen = static_cast<size_t>((bits >> 16) & 0xFFFFu);")
                line(
                    "if (2u + paramLen > data.size()) throw std::invalid_argument(\"Truncated animation spec params\");"
                )
                line("out.params.assign(data.begin() + 2, data.begin() + 2 + paramLen);")
                line("size_t off = 2u + paramLen;")
                line(
                    "if (hasInit) { if (off >= data.size()) throw std::invalid_argument(\"Truncated animation spec initialValue\"); out.initialValue = data[off++]; }"
                )
                line(
                    "if (hasWrap) { if (off >= data.size()) throw std::invalid_argument(\"Truncated animation spec wrap\"); out.wrap = data[off++]; }"
                )
                line("return out;")
            }
            w.line()
            w.block(
                "inline std::vector<float> packAnimationSpec(float duration = 1.0f, int32_t type = AnimationEasing::CUBIC_STANDARD, const std::vector<float>* spec = nullptr, float initialValue = std::numeric_limits<float>::quiet_NaN(), float wrap = std::numeric_limits<float>::quiet_NaN())"
            ) {
                line("size_t count = 0;")
                line("bool hasInit = !std::isnan(initialValue);")
                line("bool hasWrap = !std::isnan(wrap);")
                line("if (hasInit) count++;")
                line("if (spec != nullptr) count++;")
                line(
                    "if (spec != nullptr || type != AnimationEasing::CUBIC_STANDARD) { count++; count += spec != nullptr ? spec->size() : 0; }"
                )
                line("if (hasInit) count++;")
                line("if (hasWrap) count++;")
                line("if (duration != 1.0f || count > 0) count++;")
                line("if (hasWrap || hasInit) count++;")
                line("std::vector<float> ret(count, 0.0f);")
                line("size_t pos = 0;")
                line("size_t specLen = spec != nullptr ? spec->size() : 0;")
                line("if (!ret.empty()) ret[pos++] = duration;")
                line("if (ret.size() > 1) {")
                indent {
                    line("uint32_t wrapBit = hasWrap ? 1u : 0u;")
                    line("uint32_t initBit = hasInit ? 2u : 0u;")
                    line(
                        "uint32_t bits = static_cast<uint32_t>(type) | ((wrapBit | initBit) << 8) | (static_cast<uint32_t>(specLen) << 16);"
                    )
                    line("ret[pos++] = bitsToFloat(static_cast<int32_t>(bits));")
                }
                line("}")
                line(
                    "if (specLen > 0 && spec != nullptr) { for (float v : *spec) ret[pos++] = v; }"
                )
                line("if (hasInit) ret[pos++] = initialValue;")
                line("if (hasWrap) ret[pos] = wrap;")
                line("return ret;")
            }
            w.line()
        }

        schema.pathEffects?.let { pe ->
            w.block("struct PathEffect", close = "};") {
                for (t in pe.types.sortedBy { it.id }) {
                    line("static constexpr int32_t ${t.constName} = ${t.id};")
                }
                line("static constexpr size_t MAX_DASH_INTERVALS = ${pe.maxDashIntervals};")
                line("static constexpr int32_t MAX_NESTING_DEPTH = ${pe.maxNestingDepth};")
                line("struct Dash { float phase = 0.0f; std::vector<float> intervals; };")
                line("struct Discrete { float segmentLength = 0.0f; float deviation = 0.0f; };")
                line(
                    "struct PathDash { int32_t shapeId = 0; float advance = 0.0f; float phase = 0.0f; int32_t style = 0; };"
                )
                line(
                    "struct Sum { std::shared_ptr<PathEffect> first; std::shared_ptr<PathEffect> second; };"
                )
                line(
                    "struct Compose { std::shared_ptr<PathEffect> outer; std::shared_ptr<PathEffect> inner; };"
                )
                line("std::variant<Dash, Discrete, PathDash, Sum, Compose> value;")
                block("std::vector<float> encode() const") {
                    line("std::vector<float> out;")
                    line("if (auto* d = std::get_if<Dash>(&value)) {")
                    indent {
                        line(
                            "if (d->intervals.size() > MAX_DASH_INTERVALS) throw std::invalid_argument(\"Dash intervals exceed MAX_DASH_INTERVALS\");"
                        )
                        line(
                            "out.push_back(bitsToFloat(TYPE_DASH)); out.push_back(d->phase); out.push_back(bitsToFloat(static_cast<int32_t>(d->intervals.size())));"
                        )
                        line("out.insert(out.end(), d->intervals.begin(), d->intervals.end());")
                    }
                    line("} else if (auto* disc = std::get_if<Discrete>(&value)) {")
                    indent {
                        line(
                            "out = {bitsToFloat(TYPE_DISCRETE), disc->segmentLength, disc->deviation};"
                        )
                    }
                    line("} else if (auto* pd = std::get_if<PathDash>(&value)) {")
                    indent {
                        line(
                            "out = {bitsToFloat(TYPE_PATH_DASH), bitsToFloat(pd->shapeId), pd->advance, pd->phase, bitsToFloat(pd->style)};"
                        )
                    }
                    line("} else if (auto* s = std::get_if<Sum>(&value)) {")
                    indent {
                        line(
                            "if (!s->first || !s->second) throw std::invalid_argument(\"Null child in Sum PathEffect\");"
                        )
                        line("auto a = s->first->encode(); auto b = s->second->encode();")
                        line(
                            "out.push_back(bitsToFloat(TYPE_SUM)); out.insert(out.end(), a.begin(), a.end()); out.insert(out.end(), b.begin(), b.end());"
                        )
                    }
                    line("} else if (auto* c = std::get_if<Compose>(&value)) {")
                    indent {
                        line(
                            "if (!c->outer || !c->inner) throw std::invalid_argument(\"Null child in Compose PathEffect\");"
                        )
                        line("auto a = c->outer->encode(); auto b = c->inner->encode();")
                        line(
                            "out.push_back(bitsToFloat(TYPE_COMPOSE)); out.insert(out.end(), a.begin(), a.end()); out.insert(out.end(), b.begin(), b.end());"
                        )
                    }
                    line("}")
                    line("return out;")
                }
                block("static std::optional<PathEffect> decode(const std::vector<float>& data)") {
                    line("if (data.empty()) return std::nullopt;")
                    line("size_t cursor = 0;")
                    line("auto parsed = parseOne(data, cursor, 0);")
                    line("if (!parsed.has_value() || cursor != data.size()) return std::nullopt;")
                    line("return parsed;")
                }
                rawLine("private:")
                block(
                    "static std::optional<PathEffect> parseOne(const std::vector<float>& data, size_t& cursor, int32_t depth)"
                ) {
                    line(
                        "if (depth > MAX_NESTING_DEPTH || cursor >= data.size()) return std::nullopt;"
                    )
                    line("int32_t tag = floatToRawBits(data[cursor]);")
                    block("switch (tag)") {
                        line("case TYPE_DASH: {")
                        indent {
                            line("if (cursor + 3 > data.size()) return std::nullopt;")
                            line("float phase = data[cursor + 1];")
                            line("int32_t count = floatToRawBits(data[cursor + 2]);")
                            line(
                                "if (count < 0 || static_cast<size_t>(count) > MAX_DASH_INTERVALS || static_cast<size_t>(count) > data.size() - (cursor + 3)) return std::nullopt;"
                            )
                            line(
                                "std::vector<float> iv(data.begin() + cursor + 3, data.begin() + cursor + 3 + count);"
                            )
                            line("cursor += 3 + static_cast<size_t>(count);")
                            line("return PathEffect{Dash{phase, std::move(iv)}};")
                        }
                        line("}")
                        line("case TYPE_DISCRETE: {")
                        indent {
                            line("if (cursor + 3 > data.size()) return std::nullopt;")
                            line(
                                "Discrete d{data[cursor + 1], data[cursor + 2]}; cursor += 3; return PathEffect{d};"
                            )
                        }
                        line("}")
                        line("case TYPE_PATH_DASH: {")
                        indent {
                            line("if (cursor + 5 > data.size()) return std::nullopt;")
                            line(
                                "PathDash pd{floatToRawBits(data[cursor + 1]), data[cursor + 2], data[cursor + 3], floatToRawBits(data[cursor + 4])}; cursor += 5; return PathEffect{pd};"
                            )
                        }
                        line("}")
                        line("case TYPE_SUM: {")
                        indent {
                            line("cursor += 1;")
                            line(
                                "auto a = parseOne(data, cursor, depth + 1); if (!a) return std::nullopt;"
                            )
                            line(
                                "auto b = parseOne(data, cursor, depth + 1); if (!b) return std::nullopt;"
                            )
                            line(
                                "return PathEffect{Sum{std::make_shared<PathEffect>(std::move(*a)), std::make_shared<PathEffect>(std::move(*b))}};"
                            )
                        }
                        line("}")
                        line("case TYPE_COMPOSE: {")
                        indent {
                            line("cursor += 1;")
                            line(
                                "auto a = parseOne(data, cursor, depth + 1); if (!a) return std::nullopt;"
                            )
                            line(
                                "auto b = parseOne(data, cursor, depth + 1); if (!b) return std::nullopt;"
                            )
                            line(
                                "return PathEffect{Compose{std::make_shared<PathEffect>(std::move(*a)), std::make_shared<PathEffect>(std::move(*b))}};"
                            )
                        }
                        line("}")
                        line("default: return std::nullopt;")
                    }
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty() && schema.idEncoding != null) {
            w.block("struct PathCommand", close = "};") {
                for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    val vName = pathCmdVariantName(cmd)
                    if (cmd.fields.isEmpty()) {
                        line("struct $vName {};")
                    } else {
                        val fList = cmd.fields.joinToString(" ") { "float ${it.name} = 0.0f;" }
                        line("struct $vName { $fList };")
                    }
                }
                line("struct Unknown { int32_t verb = -1; };")
                val variants =
                    (schema.pathCommands.entries
                            .sortedBy { it.key }
                            .map { pathCmdVariantName(it.value) } + "Unknown")
                        .joinToString(", ")
                line("std::variant<$variants> value;")
            }
            w.line()
            w.block(
                "inline std::vector<PathCommand> decodePathCommands(const std::vector<float>& pathData)"
            ) {
                line("std::vector<PathCommand> result;")
                line("size_t i = 0; const size_t len = pathData.size();")
                block("while (i < len)") {
                    line("int32_t tag = IdEncoding::idFromNanBits(floatToRawBits(pathData[i]));")
                    block("switch (tag)") {
                        for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                            val tagConst = SchemaOperation.toScreamingSnakeCase(cmd.name)
                            val vName = pathCmdVariantName(cmd)
                            val total = 1 + cmd.leadingPaddingFloats + cmd.fields.size
                            line("case PathCommandTags::$tagConst: {")
                            indent {
                                if (cmd.fields.isEmpty()) {
                                    line("result.push_back(PathCommand{PathCommand::$vName{}});")
                                    if (cmd.name == "Done") {
                                        line("return result;")
                                    } else {
                                        line("i += 1; break;")
                                    }
                                } else {
                                    line(
                                        "if (i + ${total}u > len) { result.push_back(PathCommand{PathCommand::Unknown{tag}}); return result; }"
                                    )
                                    val args =
                                        cmd.fields.indices.joinToString(", ") { fIdx ->
                                            "pathData[i + ${1 + cmd.leadingPaddingFloats + fIdx}]"
                                        }
                                    line(
                                        "result.push_back(PathCommand{PathCommand::$vName{$args}});"
                                    )
                                    line("i += ${total}u; break;")
                                }
                            }
                            line("}")
                        }
                        line(
                            "default: result.push_back(PathCommand{PathCommand::Unknown{tag}}); return result;"
                        )
                    }
                }
                line("return result;")
            }
            w.line()
        }
    }

    fun emitCppOpExtras(w: CodeWriter, op: SchemaOperation) {
        if (op.name == "FloatExpression" && schema.animationSpec != null) {
            w.line(
                "bool isSpringAnimation() const { return isSpringAnimationSpec(animationSpec); }"
            )
            w.line(
                "FloatAnimationSpec unpackAnimationSpec() const { return androidx::compose::remote::unpackAnimationSpec(animationSpec); }"
            )
        }
        if (isBitmapDataReady(op)) {
            w.line(
                "int64_t uncompressedByteSize() const { int64_t w = getWidth(); int64_t h = getHeight(); if (w <= 0 || h <= 0) return 0; int64_t bpp = getImageType() == TYPE_PNG_ALPHA_8 ? 1LL : 4LL; if (w > std::numeric_limits<int64_t>::max() / bpp / h) return std::numeric_limits<int64_t>::max(); return w * h * bpp; }"
            )
        }
        if (
            (op.name == "PathData" || op.name == "PathAppend") &&
                schema.pathCommands.isNotEmpty() &&
                schema.idEncoding != null
        ) {
            w.line(
                "std::vector<PathCommand> decodePathCommands() const { return androidx::compose::remote::decodePathCommands(pathData); }"
            )
        }
        if (op.name == "ColorExpression" && op.fields.any { it.name == "flagsAndMode" }) {
            w.line(
                "int32_t alphaPayload() const { return static_cast<int32_t>((static_cast<uint32_t>(flagsAndMode) >> 16) & 0xFFu); }"
            )
        }
    }

    // =========================================================================================
    // Rust
    // =========================================================================================

    fun emitRustOpEnumConstants(w: CodeWriter, fields: List<SchemaField>) {
        val consts = collectEnumConstants(fields)
        if (consts.isEmpty()) return
        for ((k, v) in consts) {
            w.line("pub const $k: i32 = $v;")
        }
    }

    fun emitRustProtocol(w: CodeWriter) {
        schema.idEncoding?.let { enc ->
            w.block("pub mod id_encoding") {
                line("pub const NAN_PREFIX: i32 = ${enc.nanPrefix};")
                line("pub const NAN_EXPONENT: i32 = ${enc.nanExponent};")
                line("pub const PAYLOAD_MASK: i32 = ${enc.payloadMask};")
                line("pub const NO_ID: i32 = ${enc.noId};")
                line("pub const START_ID: i32 = ${enc.startId};")
                line("pub const REGION_MASK: i32 = ${enc.regionMask};")
                line("pub const REGION_SUB_ID_MASK: i32 = ${enc.regionSubIdMask};")
                line("pub const REGION_SYSTEM: i32 = ${enc.regionSystem};")
                line("pub const REGION_VARIABLE: i32 = ${enc.regionVariable};")
                line("pub const REGION_ARRAY: i32 = ${enc.regionArray};")
                line("pub const REGION_OPERATION: i32 = ${enc.regionOperation};")
                line("pub const START_ARRAY_ID: i32 = ${enc.regionArray + enc.startId};")
                line("pub const LONG_ID_OFFSET: i64 = ${enc.longIdOffset};")
                line("pub const PTR_DEREFERENCE: i32 = ${enc.ptrDereference};")
                line("pub const VALUE_MASK: i32 = ${enc.valueMask};")
                line("pub const MACRO_LOCAL_START_ID: i32 = ${enc.macroLocalStartId};")
                line("pub const MACRO_LOCAL_END_ID: i32 = ${enc.macroLocalEndId};")
                line(
                    "pub fn is_nan_bits(bits: i32) -> bool { (bits & NAN_EXPONENT) == NAN_EXPONENT && (bits & PAYLOAD_MASK) != 0 }"
                )
                line(
                    "pub fn id_from_nan_bits(bits: i32) -> i32 { if is_nan_bits(bits) { bits & PAYLOAD_MASK } else { NO_ID } }"
                )
                line("pub fn as_nan_bits(id: i32) -> i32 { (id & PAYLOAD_MASK) | NAN_PREFIX }")
                line("pub fn is_dereferenced(raw: i32) -> bool { (raw & PTR_DEREFERENCE) != 0 }")
                line(
                    "pub fn direct_id(raw: i32) -> Option<i32> { if is_dereferenced(raw) { None } else { Some(raw & VALUE_MASK) } }"
                )
                line(
                    "pub fn pointer_id(raw: i32) -> Option<i32> { if is_dereferenced(raw) { Some(raw & VALUE_MASK) } else { None } }"
                )
            }
            w.line()
        }

        if (schema.headerConstants.isNotEmpty()) {
            w.block("pub mod header_constants") {
                for ((_, group) in schema.headerConstants) {
                    for ((k, v) in group.entries.sortedBy { it.value }) {
                        line("pub const $k: i32 = $v;")
                    }
                }
            }
            w.line()
        }

        if (schema.paintCommands.isNotEmpty()) {
            w.block("pub mod paint_command_tags") {
                for ((id, cmd) in schema.paintCommands.entries.sortedBy { it.key }) {
                    line("pub const ${SchemaOperation.toScreamingSnakeCase(cmd.name)}: i32 = $id;")
                }
            }
            w.line()
            w.block("pub mod paint_constants") {
                for ((k, v) in collectPaintConstants()) {
                    line("pub const $k: i32 = $v;")
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty()) {
            w.block("pub mod path_command_tags") {
                for ((id, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    line("pub const ${SchemaOperation.toScreamingSnakeCase(cmd.name)}: i32 = $id;")
                }
            }
            w.line()
        }

        if (schema.systemVariables.isNotEmpty()) {
            val timeIds = schema.systemVariables.filter { it.isTime }.map { it.id }.sorted()
            w.block("pub mod remote_context_ids") {
                for (sv in schema.systemVariables) {
                    line("pub const ID_${sv.name}: i32 = ${sv.id};")
                }
                val expr =
                    if (timeIds.isEmpty()) "false"
                    else if (timeIds.size == timeIds.last() - timeIds.first() + 1)
                        "(${timeIds.first()}..=${timeIds.last()}).contains(&id)"
                    else "matches!(id, ${timeIds.joinToString(" | ")})"
                line("pub fn is_time(id: i32) -> bool { $expr }")
            }
            w.line()
        }

        schema.animationSpec?.let { anim ->
            w.block("pub mod animation_easing") {
                for (e in anim.easings.sortedBy { it.id }) {
                    line("pub const ${e.name}: i32 = ${e.id};")
                    for (a in e.aliases) {
                        line("pub const $a: i32 = ${e.id};")
                    }
                }
                block("pub fn cubic_control_points(easing_type: i32) -> Option<[f32; 4]>") {
                    block("match easing_type") {
                        for (e in anim.easings.sortedBy { it.id }) {
                            val pts = e.cubicControlPoints ?: continue
                            line(
                                "${e.name} => Some([${pts[0]}f32, ${pts[1]}f32, ${pts[2]}f32, ${pts[3]}f32]),"
                            )
                        }
                        line("_ => None,")
                    }
                }
            }
            w.line()
            w.line("#[derive(Debug, Clone)]")
            w.block("pub struct FloatAnimationSpec") {
                line("pub duration: f32,")
                line("pub easing_type: i32,")
                line("pub params: Vec<f32>,")
                line("pub initial_value: f32,")
                line("pub wrap: f32,")
                line("pub directional_snap: i32,")
                line("pub is_propagate: bool,")
            }
            w.line()
            w.block("impl PartialEq for FloatAnimationSpec") {
                block("fn eq(&self, other: &Self) -> bool") {
                    line("let feq = |a: f32, b: f32| (a.is_nan() && b.is_nan()) || a == b;")
                    line("feq(self.duration, other.duration)")
                    line("    && self.easing_type == other.easing_type")
                    line("    && self.params.len() == other.params.len()")
                    line(
                        "    && self.params.iter().zip(other.params.iter()).all(|(&a, &b)| feq(a, b))"
                    )
                    line("    && feq(self.initial_value, other.initial_value)")
                    line("    && feq(self.wrap, other.wrap)")
                    line("    && self.directional_snap == other.directional_snap")
                    line("    && self.is_propagate == other.is_propagate")
                }
            }
            w.line()
            w.line(
                "pub fn is_spring_animation_spec(data: &[f32]) -> bool { data.len() > 4 && data[0] == 0.0 }"
            )
            w.block(
                "pub fn unpack_animation_spec(data: &[f32]) -> Result<FloatAnimationSpec, String>"
            ) {
                line(
                    "if is_spring_animation_spec(data) { return Err(\"Spring animation specs cannot be unpacked with unpack_animation_spec\".to_string()); }"
                )
                line(
                    "let mut out = FloatAnimationSpec { duration: 1.0, easing_type: animation_easing::CUBIC_STANDARD, params: Vec::new(), initial_value: f32::NAN, wrap: f32::NAN, directional_snap: 0, is_propagate: false };"
                )
                line("if data.is_empty() { return Ok(out); }")
                line("out.duration = data[0];")
                line("if data.len() == 1 { return Ok(out); }")
                line("let bits = data[1].to_bits();")
                line("out.easing_type = (bits & 0xFF) as i32;")
                line("let has_wrap = ((bits >> 8) & 1) != 0;")
                line("let has_init = ((bits >> 9) & 1) != 0;")
                line("out.directional_snap = ((bits >> 10) & 3) as i32;")
                line("out.is_propagate = ((bits >> 12) & 1) != 0;")
                line("let param_len = ((bits >> 16) & 0xFFFF) as usize;")
                line(
                    "if 2 + param_len > data.len() { return Err(\"Truncated animation spec params\".to_string()); }"
                )
                line("out.params = data[2..2 + param_len].to_vec();")
                line("let mut off = 2 + param_len;")
                line(
                    "if has_init { if off >= data.len() { return Err(\"Truncated animation spec initial_value\".to_string()); } out.initial_value = data[off]; off += 1; }"
                )
                line(
                    "if has_wrap { if off >= data.len() { return Err(\"Truncated animation spec wrap\".to_string()); } out.wrap = data[off]; }"
                )
                line("Ok(out)")
            }
            w.line()
            w.block(
                "pub fn pack_animation_spec(duration: f32, easing_type: i32, spec: Option<&[f32]>, initial_value: f32, wrap: f32) -> Vec<f32>"
            ) {
                line("let mut count: usize = 0;")
                line("let has_init = !initial_value.is_nan();")
                line("let has_wrap = !wrap.is_nan();")
                line("if has_init { count += 1; }")
                line("if spec.is_some() { count += 1; }")
                line(
                    "if spec.is_some() || easing_type != animation_easing::CUBIC_STANDARD { count += 1 + spec.map_or(0, |s| s.len()); }"
                )
                line("if has_init { count += 1; }")
                line("if has_wrap { count += 1; }")
                line("if duration != 1.0 || count > 0 { count += 1; }")
                line("if has_wrap || has_init { count += 1; }")
                line("let mut ret = vec![0.0f32; count];")
                line("let mut pos: usize = 0;")
                line("let spec_len = spec.map_or(0, |s| s.len());")
                line("if !ret.is_empty() { ret[pos] = duration; pos += 1; }")
                line("if ret.len() > 1 {")
                indent {
                    line("let wrap_bit: u32 = if has_wrap { 1 } else { 0 };")
                    line("let init_bit: u32 = if has_init { 2 } else { 0 };")
                    line(
                        "let bits: u32 = (easing_type as u32) | ((wrap_bit | init_bit) << 8) | ((spec_len as u32) << 16);"
                    )
                    line("ret[pos] = f32::from_bits(bits); pos += 1;")
                }
                line("}")
                line("if let Some(s) = spec { for &v in s { ret[pos] = v; pos += 1; } }")
                line("if has_init { ret[pos] = initial_value; pos += 1; }")
                line("if has_wrap { ret[pos] = wrap; }")
                line("ret")
            }
            w.line()
        }

        schema.pathEffects?.let { pe ->
            w.line("#[derive(Debug, Clone, PartialEq)]")
            w.block("pub enum PathEffect") {
                line("Dash { phase: f32, intervals: Vec<f32> },")
                line("Discrete { segment_length: f32, deviation: f32 },")
                line("PathDash { shape_id: i32, advance: f32, phase: f32, style: i32 },")
                line("Sum(Box<PathEffect>, Box<PathEffect>),")
                line("Compose(Box<PathEffect>, Box<PathEffect>),")
            }
            w.line()
            w.block("impl PathEffect") {
                for (t in pe.types.sortedBy { it.id }) {
                    line("pub const ${t.constName}: i32 = ${t.id};")
                }
                line("pub const MAX_DASH_INTERVALS: usize = ${pe.maxDashIntervals};")
                line("pub const MAX_NESTING_DEPTH: usize = ${pe.maxNestingDepth};")
                block("pub fn encode(&self) -> Result<Vec<f32>, String>") {
                    block("match self") {
                        line(
                            "PathEffect::Dash { phase, intervals } => { if intervals.len() > Self::MAX_DASH_INTERVALS { return Err(format!(\"Dash interval count ({}) exceeds MAX_DASH_INTERVALS ({})\", intervals.len(), Self::MAX_DASH_INTERVALS)); } let mut out = Vec::with_capacity(intervals.len() + 3); out.push(f32::from_bits(Self::TYPE_DASH as u32)); out.push(*phase); out.push(f32::from_bits(intervals.len() as u32)); out.extend_from_slice(intervals); Ok(out) }"
                        )
                        line(
                            "PathEffect::Discrete { segment_length, deviation } => Ok(vec![f32::from_bits(Self::TYPE_DISCRETE as u32), *segment_length, *deviation]),"
                        )
                        line(
                            "PathEffect::PathDash { shape_id, advance, phase, style } => Ok(vec![f32::from_bits(Self::TYPE_PATH_DASH as u32), f32::from_bits(*shape_id as u32), *advance, *phase, f32::from_bits(*style as u32)]),"
                        )
                        line(
                            "PathEffect::Sum(a, b) => { let mut out = vec![f32::from_bits(Self::TYPE_SUM as u32)]; out.extend(a.encode()?); out.extend(b.encode()?); Ok(out) }"
                        )
                        line(
                            "PathEffect::Compose(a, b) => { let mut out = vec![f32::from_bits(Self::TYPE_COMPOSE as u32)]; out.extend(a.encode()?); out.extend(b.encode()?); Ok(out) }"
                        )
                    }
                }
                block("pub fn decode(data: &[f32]) -> Option<PathEffect>") {
                    line("if data.is_empty() { return None; }")
                    line("let mut cursor: usize = 0;")
                    line("let effect = Self::parse_one(data, &mut cursor, 0)?;")
                    line("if cursor == data.len() { Some(effect) } else { None }")
                }
                block(
                    "fn parse_one(data: &[f32], cursor: &mut usize, depth: usize) -> Option<PathEffect>"
                ) {
                    line(
                        "if depth > Self::MAX_NESTING_DEPTH || *cursor >= data.len() { return None; }"
                    )
                    line("let idx = *cursor;")
                    line("let tag = data[idx].to_bits() as i32;")
                    block("match tag") {
                        line(
                            "Self::TYPE_DASH => { if idx + 3 > data.len() { return None; } let phase = data[idx + 1]; let count = data[idx + 2].to_bits() as i32; if count < 0 || (count as usize) > Self::MAX_DASH_INTERVALS || (count as usize) > data.len() - (idx + 3) { return None; } let c = count as usize; *cursor = idx + 3 + c; Some(PathEffect::Dash { phase, intervals: data[idx + 3..idx + 3 + c].to_vec() }) }"
                        )
                        line(
                            "Self::TYPE_DISCRETE => { if idx + 3 > data.len() { return None; } *cursor = idx + 3; Some(PathEffect::Discrete { segment_length: data[idx + 1], deviation: data[idx + 2] }) }"
                        )
                        line(
                            "Self::TYPE_PATH_DASH => { if idx + 5 > data.len() { return None; } *cursor = idx + 5; Some(PathEffect::PathDash { shape_id: data[idx + 1].to_bits() as i32, advance: data[idx + 2], phase: data[idx + 3], style: data[idx + 4].to_bits() as i32 }) }"
                        )
                        line(
                            "Self::TYPE_SUM => { *cursor = idx + 1; let a = Self::parse_one(data, cursor, depth + 1)?; let b = Self::parse_one(data, cursor, depth + 1)?; Some(PathEffect::Sum(Box::new(a), Box::new(b))) }"
                        )
                        line(
                            "Self::TYPE_COMPOSE => { *cursor = idx + 1; let a = Self::parse_one(data, cursor, depth + 1)?; let b = Self::parse_one(data, cursor, depth + 1)?; Some(PathEffect::Compose(Box::new(a), Box::new(b))) }"
                        )
                        line("_ => None,")
                    }
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty() && schema.idEncoding != null) {
            w.line("#[derive(Debug, Clone, PartialEq)]")
            w.block("pub enum PathCommand") {
                for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    val vName = pathCmdVariantName(cmd)
                    if (cmd.fields.isEmpty()) {
                        line("$vName,")
                    } else {
                        val fList = cmd.fields.joinToString(", ") { "${it.name}: f32" }
                        line("$vName { $fList },")
                    }
                }
                line("Unknown(i32),")
            }
            w.line()
            w.block("pub fn decode_path_commands(path_data: &[f32]) -> Vec<PathCommand>") {
                line("let mut result = Vec::new();")
                line("let mut i: usize = 0;")
                line("let len = path_data.len();")
                block("while i < len") {
                    line("let tag = id_encoding::id_from_nan_bits(path_data[i].to_bits() as i32);")
                    block("match tag") {
                        for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                            val tagConst = SchemaOperation.toScreamingSnakeCase(cmd.name)
                            val vName = pathCmdVariantName(cmd)
                            val total = 1 + cmd.leadingPaddingFloats + cmd.fields.size
                            if (cmd.fields.isEmpty()) {
                                val nextStmt = if (cmd.name == "Done") "break;" else "i += 1;"
                                line(
                                    "path_command_tags::$tagConst => { result.push(PathCommand::$vName); $nextStmt }"
                                )
                            } else {
                                val args =
                                    cmd.fields.withIndex().joinToString(", ") { (fIdx, fl) ->
                                        "${fl.name}: path_data[i + ${1 + cmd.leadingPaddingFloats + fIdx}]"
                                    }
                                line(
                                    "path_command_tags::$tagConst => { if i + $total > len { result.push(PathCommand::Unknown(tag)); break; } result.push(PathCommand::$vName { $args }); i += $total; }"
                                )
                            }
                        }
                        line("_ => { result.push(PathCommand::Unknown(tag)); break; }")
                    }
                }
                line("result")
            }
            w.line()
        }
    }

    fun emitRustOpExtras(w: CodeWriter, op: SchemaOperation) {
        if (op.name == "FloatExpression" && schema.animationSpec != null) {
            w.line(
                "pub fn is_spring_animation(&self) -> bool { is_spring_animation_spec(&self.animation_spec) }"
            )
            w.line(
                "pub fn unpack_animation_spec(&self) -> Result<FloatAnimationSpec, String> { unpack_animation_spec(&self.animation_spec) }"
            )
        }
        if (isBitmapDataReady(op)) {
            w.line(
                "pub fn uncompressed_byte_size(&self) -> i64 { let w = self.width() as i64; let h = self.height() as i64; if w <= 0 || h <= 0 { return 0; } let bpp: i64 = if self.image_type() == Self::TYPE_PNG_ALPHA_8 { 1 } else { 4 }; w.saturating_mul(h).saturating_mul(bpp) }"
            )
        }
        if (
            (op.name == "PathData" || op.name == "PathAppend") &&
                schema.pathCommands.isNotEmpty() &&
                schema.idEncoding != null
        ) {
            w.line(
                "pub fn decode_path_commands(&self) -> Vec<PathCommand> { decode_path_commands(&self.path_data) }"
            )
        }
        if (op.name == "ColorExpression" && op.fields.any { it.name == "flagsAndMode" }) {
            w.line(
                "pub fn alpha_payload(&self) -> i32 { (((self.flags_and_mode as u32) >> 16) & 0xFF) as i32 }"
            )
        }
    }

    // =========================================================================================
    // Python 3
    // =========================================================================================

    fun emitPythonOpEnumConstants(w: CodeWriter, fields: List<SchemaField>) {
        val consts = collectEnumConstants(fields)
        if (consts.isEmpty()) return
        for ((k, v) in consts) {
            w.line("$k: ClassVar[int] = $v")
        }
    }

    fun emitPythonProtocol(w: CodeWriter) {
        w.line("def _float_to_raw_bits(v: float) -> int:")
        w.indent { w.line("return struct.unpack(\">i\", struct.pack(\">f\", v))[0]") }
        w.line()
        w.line("def _bits_to_float(bits: int) -> float:")
        w.indent {
            w.line("return struct.unpack(\">f\", struct.pack(\">I\", bits & 0xFFFFFFFF))[0]")
        }
        w.line()

        schema.idEncoding?.let { enc ->
            w.line("class IdEncoding:")
            w.indent {
                w.line("NAN_PREFIX = ${enc.nanPrefix}")
                w.line("NAN_EXPONENT = ${enc.nanExponent}")
                w.line("PAYLOAD_MASK = ${enc.payloadMask}")
                w.line("NO_ID = ${enc.noId}")
                w.line("START_ID = ${enc.startId}")
                w.line("REGION_MASK = ${enc.regionMask}")
                w.line("REGION_SUB_ID_MASK = ${enc.regionSubIdMask}")
                w.line("REGION_SYSTEM = ${enc.regionSystem}")
                w.line("REGION_VARIABLE = ${enc.regionVariable}")
                w.line("REGION_ARRAY = ${enc.regionArray}")
                w.line("REGION_OPERATION = ${enc.regionOperation}")
                w.line("START_ARRAY_ID = ${enc.regionArray + enc.startId}")
                w.line("LONG_ID_OFFSET = ${enc.longIdOffset}")
                w.line("PTR_DEREFERENCE = ${enc.ptrDereference}")
                w.line("VALUE_MASK = ${enc.valueMask}")
                w.line("MACRO_LOCAL_START_ID = ${enc.macroLocalStartId}")
                w.line("MACRO_LOCAL_END_ID = ${enc.macroLocalEndId}")
                w.line("@staticmethod")
                w.line("def is_nan_bits(bits: int) -> bool:")
                w.indent {
                    w.line(
                        "return ((bits & 0xFFFFFFFF) & IdEncoding.NAN_EXPONENT) == IdEncoding.NAN_EXPONENT and (bits & IdEncoding.PAYLOAD_MASK) != 0"
                    )
                }
                w.line("@staticmethod")
                w.line("def id_from_nan_bits(bits: int) -> int:")
                w.indent {
                    w.line(
                        "return (bits & IdEncoding.PAYLOAD_MASK) if IdEncoding.is_nan_bits(bits) else IdEncoding.NO_ID"
                    )
                }
                w.line("@staticmethod")
                w.line("def as_nan_bits(var_id: int) -> int:")
                w.indent {
                    w.line(
                        "val = (var_id & IdEncoding.PAYLOAD_MASK) | (IdEncoding.NAN_PREFIX & 0xFFFFFFFF)"
                    )
                    w.line("return val - 0x100000000 if val >= 0x80000000 else val")
                }
                w.line("@staticmethod")
                w.line("def is_dereferenced(raw: int) -> bool:")
                w.indent { w.line("return (raw & IdEncoding.PTR_DEREFERENCE) != 0") }
                w.line("@staticmethod")
                w.line("def direct_id(raw: int) -> Optional[int]:")
                w.indent {
                    w.line(
                        "return None if IdEncoding.is_dereferenced(raw) else (raw & IdEncoding.VALUE_MASK)"
                    )
                }
                w.line("@staticmethod")
                w.line("def pointer_id(raw: int) -> Optional[int]:")
                w.indent {
                    w.line(
                        "return (raw & IdEncoding.VALUE_MASK) if IdEncoding.is_dereferenced(raw) else None"
                    )
                }
            }
            w.line()
        }

        if (schema.headerConstants.isNotEmpty()) {
            w.line("class HeaderConstants:")
            w.indent {
                for ((_, group) in schema.headerConstants) {
                    for ((k, v) in group.entries.sortedBy { it.value }) {
                        w.line("$k = $v")
                    }
                }
            }
            w.line()
        }

        if (schema.paintCommands.isNotEmpty()) {
            w.line("class PaintCommandTags:")
            w.indent {
                for ((id, cmd) in schema.paintCommands.entries.sortedBy { it.key }) {
                    w.line("${SchemaOperation.toScreamingSnakeCase(cmd.name)} = $id")
                }
            }
            w.line()
            w.line("class PaintConstants:")
            w.indent {
                for ((k, v) in collectPaintConstants()) {
                    w.line("$k = $v")
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty()) {
            w.line("class PathCommandTags:")
            w.indent {
                for ((id, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    w.line("${SchemaOperation.toScreamingSnakeCase(cmd.name)} = $id")
                }
            }
            w.line()
        }

        if (schema.systemVariables.isNotEmpty()) {
            val timeIds = schema.systemVariables.filter { it.isTime }.map { it.id }.sorted()
            w.line("class RemoteContextIds:")
            w.indent {
                for (sv in schema.systemVariables) {
                    w.line("ID_${sv.name} = ${sv.id}")
                }
                w.line("@staticmethod")
                w.line("def is_time(var_id: int) -> bool:")
                val expr =
                    if (timeIds.isEmpty()) "False"
                    else if (timeIds.size == timeIds.last() - timeIds.first() + 1)
                        "${timeIds.first()} <= var_id <= ${timeIds.last()}"
                    else "var_id in {${timeIds.joinToString(", ")}}"
                w.indent { w.line("return $expr") }
            }
            w.line()
        }

        schema.animationSpec?.let { anim ->
            w.line("class AnimationEasing:")
            w.indent {
                for (e in anim.easings.sortedBy { it.id }) {
                    w.line("${e.name} = ${e.id}")
                    for (a in e.aliases) {
                        w.line("$a = ${e.id}")
                    }
                }
                w.line("_CUBIC_PRESETS = {")
                w.indent {
                    for (e in anim.easings.sortedBy { it.id }) {
                        val pts = e.cubicControlPoints ?: continue
                        w.line("${e.id}: (${pts[0]}, ${pts[1]}, ${pts[2]}, ${pts[3]}),")
                    }
                }
                w.line("}")
                w.line("@staticmethod")
                w.line("def cubic_control_points(easing_type: int) -> Optional[tuple]:")
                w.indent { w.line("return AnimationEasing._CUBIC_PRESETS.get(easing_type)") }
            }
            w.line()
            w.line("@dataclass")
            w.line("class FloatAnimationSpec:")
            w.indent {
                w.line("duration: float = 1.0")
                w.line("easing_type: int = AnimationEasing.CUBIC_STANDARD")
                w.line("params: List[float] = field(default_factory=list)")
                w.line("initial_value: float = float(\"nan\")")
                w.line("wrap: float = float(\"nan\")")
                w.line("directional_snap: int = 0")
                w.line("is_propagate: bool = False")
                w.line()
                w.line("def __eq__(self, other: object) -> bool:")
                w.indent {
                    w.line("if not isinstance(other, FloatAnimationSpec):")
                    w.indent { w.line("return NotImplemented") }
                    w.line("def _feq(a: float, b: float) -> bool:")
                    w.indent { w.line("return (math.isnan(a) and math.isnan(b)) or a == b") }
                    w.line("return (")
                    w.indent {
                        w.line("_feq(self.duration, other.duration)")
                        w.line("and self.easing_type == other.easing_type")
                        w.line("and len(self.params) == len(other.params)")
                        w.line("and all(_feq(a, b) for a, b in zip(self.params, other.params))")
                        w.line("and _feq(self.initial_value, other.initial_value)")
                        w.line("and _feq(self.wrap, other.wrap)")
                        w.line("and self.directional_snap == other.directional_snap")
                        w.line("and self.is_propagate == other.is_propagate")
                    }
                    w.line(")")
                }
            }
            w.line()
            w.line("def is_spring_animation_spec(data: List[float]) -> bool:")
            w.indent { w.line("return len(data) > 4 and data[0] == 0.0") }
            w.line()
            w.line("def unpack_animation_spec(data: List[float]) -> FloatAnimationSpec:")
            w.indent {
                w.line("if is_spring_animation_spec(data):")
                w.indent {
                    w.line(
                        "raise ValueError(\"Spring animation specs cannot be unpacked with unpack_animation_spec\")"
                    )
                }
                w.line("if not data:")
                w.indent { w.line("return FloatAnimationSpec()") }
                w.line("duration = data[0]")
                w.line("if len(data) == 1:")
                w.indent { w.line("return FloatAnimationSpec(duration=duration)") }
                w.line("bits = _float_to_raw_bits(data[1]) & 0xFFFFFFFF")
                w.line("easing_type = bits & 0xFF")
                w.line("has_wrap = ((bits >> 8) & 1) != 0")
                w.line("has_init = ((bits >> 9) & 1) != 0")
                w.line("directional_snap = (bits >> 10) & 3")
                w.line("is_propagate = ((bits >> 12) & 1) != 0")
                w.line("param_len = (bits >> 16) & 0xFFFF")
                w.line("if 2 + param_len > len(data):")
                w.indent { w.line("raise ValueError(\"Truncated animation spec params\")") }
                w.line("params = list(data[2:2 + param_len])")
                w.line("off = 2 + param_len")
                w.line("initial_value = float(\"nan\")")
                w.line("if has_init:")
                w.indent {
                    w.line("if off >= len(data):")
                    w.indent {
                        w.line("raise ValueError(\"Truncated animation spec initial_value\")")
                    }
                    w.line("initial_value = data[off]")
                    w.line("off += 1")
                }
                w.line("wrap = float(\"nan\")")
                w.line("if has_wrap:")
                w.indent {
                    w.line("if off >= len(data):")
                    w.indent { w.line("raise ValueError(\"Truncated animation spec wrap\")") }
                    w.line("wrap = data[off]")
                }
                w.line(
                    "return FloatAnimationSpec(duration=duration, easing_type=easing_type, params=params, initial_value=initial_value, wrap=wrap, directional_snap=directional_snap, is_propagate=is_propagate)"
                )
            }
            w.line()
            w.line(
                "def pack_animation_spec(duration: float = 1.0, easing_type: int = AnimationEasing.CUBIC_STANDARD, spec: Optional[List[float]] = None, initial_value: float = float(\"nan\"), wrap: float = float(\"nan\")) -> List[float]:"
            )
            w.indent {
                w.line("count = 0")
                w.line("has_init = not math.isnan(initial_value)")
                w.line("has_wrap = not math.isnan(wrap)")
                w.line("if has_init: count += 1")
                w.line("if spec is not None: count += 1")
                w.line("if spec is not None or easing_type != AnimationEasing.CUBIC_STANDARD:")
                w.indent { w.line("count += 1 + (len(spec) if spec is not None else 0)") }
                w.line("if has_init: count += 1")
                w.line("if has_wrap: count += 1")
                w.line("if duration != 1.0 or count > 0: count += 1")
                w.line("if has_wrap or has_init: count += 1")
                w.line("ret = [0.0] * count")
                w.line("pos = 0")
                w.line("spec_len = len(spec) if spec is not None else 0")
                w.line("if ret: ret[pos] = duration; pos += 1")
                w.line("if len(ret) > 1:")
                w.indent {
                    w.line("wrap_bit = 1 if has_wrap else 0")
                    w.line("init_bit = 2 if has_init else 0")
                    w.line("bits = easing_type | ((wrap_bit | init_bit) << 8) | (spec_len << 16)")
                    w.line("ret[pos] = _bits_to_float(bits); pos += 1")
                }
                w.line("if spec is not None:")
                w.indent { w.line("for v in spec: ret[pos] = v; pos += 1") }
                w.line("if has_init: ret[pos] = initial_value; pos += 1")
                w.line("if has_wrap: ret[pos] = wrap")
                w.line("return ret")
            }
            w.line()
        }

        schema.pathEffects?.let { pe ->
            w.line("class PathEffect:")
            w.indent {
                for (t in pe.types.sortedBy { it.id }) {
                    w.line("${t.constName} = ${t.id}")
                }
                w.line("MAX_DASH_INTERVALS = ${pe.maxDashIntervals}")
                w.line("MAX_NESTING_DEPTH = ${pe.maxNestingDepth}")
                w.line("def encode(self) -> List[float]:")
                w.indent { w.line("raise NotImplementedError()") }
                w.line("@staticmethod")
                w.line("def decode(data: List[float]) -> Optional[\"PathEffect\"]:")
                w.indent {
                    w.line("if not data: return None")
                    w.line("parsed = PathEffect._parse_one(data, 0, 0)")
                    w.line("if parsed is None or parsed[1] != len(data): return None")
                    w.line("return parsed[0]")
                }
                w.line("@staticmethod")
                w.line("def _parse_one(data: List[float], idx: int, depth: int):")
                w.indent {
                    w.line(
                        "if depth > PathEffect.MAX_NESTING_DEPTH or idx >= len(data): return None"
                    )
                    w.line("tag = _float_to_raw_bits(data[idx])")
                    w.line("if tag == PathEffect.TYPE_DASH:")
                    w.indent {
                        w.line("if idx + 3 > len(data): return None")
                        w.line("phase = data[idx + 1]")
                        w.line("count = _float_to_raw_bits(data[idx + 2])")
                        w.line(
                            "if count < 0 or count > PathEffect.MAX_DASH_INTERVALS or count > len(data) - (idx + 3): return None"
                        )
                        w.line(
                            "return (DashPathEffect(phase=phase, intervals=list(data[idx + 3:idx + 3 + count])), idx + 3 + count)"
                        )
                    }
                    w.line("if tag == PathEffect.TYPE_DISCRETE:")
                    w.indent {
                        w.line("if idx + 3 > len(data): return None")
                        w.line(
                            "return (DiscretePathEffect(segment_length=data[idx + 1], deviation=data[idx + 2]), idx + 3)"
                        )
                    }
                    w.line("if tag == PathEffect.TYPE_PATH_DASH:")
                    w.indent {
                        w.line("if idx + 5 > len(data): return None")
                        w.line(
                            "return (PathDashPathEffect(shape_id=_float_to_raw_bits(data[idx + 1]), advance=data[idx + 2], phase=data[idx + 3], style=_float_to_raw_bits(data[idx + 4])), idx + 5)"
                        )
                    }
                    w.line("if tag == PathEffect.TYPE_SUM:")
                    w.indent {
                        w.line("a = PathEffect._parse_one(data, idx + 1, depth + 1)")
                        w.line("if a is None: return None")
                        w.line("b = PathEffect._parse_one(data, a[1], depth + 1)")
                        w.line("if b is None: return None")
                        w.line("return (SumPathEffect(first=a[0], second=b[0]), b[1])")
                    }
                    w.line("if tag == PathEffect.TYPE_COMPOSE:")
                    w.indent {
                        w.line("a = PathEffect._parse_one(data, idx + 1, depth + 1)")
                        w.line("if a is None: return None")
                        w.line("b = PathEffect._parse_one(data, a[1], depth + 1)")
                        w.line("if b is None: return None")
                        w.line("return (ComposePathEffect(outer=a[0], inner=b[0]), b[1])")
                    }
                    w.line("return None")
                }
            }
            w.line()
            w.line("@dataclass")
            w.line("class DashPathEffect(PathEffect):")
            w.indent {
                w.line("phase: float")
                w.line("intervals: List[float]")
                w.line("def encode(self) -> List[float]:")
                w.indent {
                    w.line(
                        "if len(self.intervals) > PathEffect.MAX_DASH_INTERVALS: raise ValueError(\"Too many dash intervals\")"
                    )
                    w.line(
                        "return [_bits_to_float(PathEffect.TYPE_DASH), self.phase, _bits_to_float(len(self.intervals))] + list(self.intervals)"
                    )
                }
            }
            w.line()
            w.line("@dataclass")
            w.line("class DiscretePathEffect(PathEffect):")
            w.indent {
                w.line("segment_length: float")
                w.line("deviation: float")
                w.line("def encode(self) -> List[float]:")
                w.indent {
                    w.line(
                        "return [_bits_to_float(PathEffect.TYPE_DISCRETE), self.segment_length, self.deviation]"
                    )
                }
            }
            w.line()
            w.line("@dataclass")
            w.line("class PathDashPathEffect(PathEffect):")
            w.indent {
                w.line("shape_id: int")
                w.line("advance: float")
                w.line("phase: float")
                w.line("style: int")
                w.line("def encode(self) -> List[float]:")
                w.indent {
                    w.line(
                        "return [_bits_to_float(PathEffect.TYPE_PATH_DASH), _bits_to_float(self.shape_id), self.advance, self.phase, _bits_to_float(self.style)]"
                    )
                }
            }
            w.line()
            w.line("@dataclass")
            w.line("class SumPathEffect(PathEffect):")
            w.indent {
                w.line("first: PathEffect")
                w.line("second: PathEffect")
                w.line("def encode(self) -> List[float]:")
                w.indent {
                    w.line(
                        "return [_bits_to_float(PathEffect.TYPE_SUM)] + self.first.encode() + self.second.encode()"
                    )
                }
            }
            w.line()
            w.line("@dataclass")
            w.line("class ComposePathEffect(PathEffect):")
            w.indent {
                w.line("outer: PathEffect")
                w.line("inner: PathEffect")
                w.line("def encode(self) -> List[float]:")
                w.indent {
                    w.line(
                        "return [_bits_to_float(PathEffect.TYPE_COMPOSE)] + self.outer.encode() + self.inner.encode()"
                    )
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty() && schema.idEncoding != null) {
            w.line("class PathCommand:")
            w.indent { w.line("pass") }
            w.line()
            for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                val vName = pathCmdVariantName(cmd)
                w.line("@dataclass")
                w.line("class ${vName}PathCommand(PathCommand):")
                if (cmd.fields.isEmpty()) {
                    w.indent { w.line("pass") }
                } else {
                    w.indent {
                        for (fl in cmd.fields) {
                            w.line("${fl.name}: float")
                        }
                    }
                }
                w.line()
            }
            w.line("@dataclass")
            w.line("class UnknownPathCommand(PathCommand):")
            w.indent { w.line("verb: int") }
            w.line()
            w.line("def decode_path_commands(path_data: List[float]) -> List[PathCommand]:")
            w.indent {
                w.line("result: List[PathCommand] = []")
                w.line("i = 0")
                w.line("n = len(path_data)")
                w.line("while i < n:")
                w.indent {
                    w.line("tag = IdEncoding.id_from_nan_bits(_float_to_raw_bits(path_data[i]))")
                    var firstBranch = true
                    for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                        val kw = if (firstBranch) "if" else "elif"
                        firstBranch = false
                        val tagConst = SchemaOperation.toScreamingSnakeCase(cmd.name)
                        val vName = pathCmdVariantName(cmd)
                        val total = 1 + cmd.leadingPaddingFloats + cmd.fields.size
                        w.line("$kw tag == PathCommandTags.$tagConst:")
                        w.indent {
                            if (cmd.fields.isEmpty()) {
                                w.line("result.append(${vName}PathCommand())")
                                if (cmd.name == "Done") w.line("break") else w.line("i += 1")
                            } else {
                                w.line("if i + $total > n:")
                                w.indent {
                                    w.line("result.append(UnknownPathCommand(tag))")
                                    w.line("break")
                                }
                                val args =
                                    cmd.fields.withIndex().joinToString(", ") { (fIdx, fl) ->
                                        "${fl.name}=path_data[i + ${1 + cmd.leadingPaddingFloats + fIdx}]"
                                    }
                                w.line("result.append(${vName}PathCommand($args))")
                                w.line("i += $total")
                            }
                        }
                    }
                    w.line("else:")
                    w.indent {
                        w.line("result.append(UnknownPathCommand(tag))")
                        w.line("break")
                    }
                }
                w.line("return result")
            }
            w.line()
        }
    }

    fun emitPythonOpExtras(w: CodeWriter, op: SchemaOperation) {
        if (op.name == "FloatExpression" && schema.animationSpec != null) {
            w.line("@property")
            w.line("def is_spring_animation(self) -> bool:")
            w.indent { w.line("return is_spring_animation_spec(self.animation_spec)") }
            w.line()
            w.line("def unpack_animation_spec(self) -> FloatAnimationSpec:")
            w.indent { w.line("return unpack_animation_spec(self.animation_spec)") }
            w.line()
        }
        if (isBitmapDataReady(op)) {
            w.line("@property")
            w.line("def uncompressed_byte_size(self) -> int:")
            w.indent {
                w.line("w = self.width")
                w.line("h = self.height")
                w.line("if w <= 0 or h <= 0: return 0")
                w.line(
                    "return w * h * (1 if self.image_type == BitmapData.TYPE_PNG_ALPHA_8 else 4)"
                )
            }
            w.line()
        }
        if (
            (op.name == "PathData" || op.name == "PathAppend") &&
                schema.pathCommands.isNotEmpty() &&
                schema.idEncoding != null
        ) {
            w.line("def decode_path_commands(self) -> List[PathCommand]:")
            w.indent { w.line("return decode_path_commands(self.path_data)") }
            w.line()
        }
        if (op.name == "ColorExpression" && op.fields.any { it.name == "flagsAndMode" }) {
            w.line("@property")
            w.line("def alpha_payload(self) -> int:")
            w.indent { w.line("return (self.flags_and_mode >> 16) & 0xFF") }
            w.line()
        }
    }

    // =========================================================================================
    // Swift
    // =========================================================================================

    private fun toSwiftLowerCamel(name: String): String {
        val raw =
            if ('_' in name || name.all { !it.isLowerCase() }) {
                val parts = name.split('_').filter { it.isNotEmpty() }
                if (parts.isEmpty()) {
                    name.lowercase()
                } else {
                    parts[0].lowercase() +
                        parts.drop(1).joinToString("") { p ->
                            p[0].uppercaseChar() + p.substring(1).lowercase()
                        }
                }
            } else {
                name.replaceFirstChar { it.lowercaseChar() }
            }
        val safe = if (raw.isNotEmpty() && raw[0].isDigit()) "_$raw" else raw
        return if (safe in SWIFT_KEYWORDS) "`$safe`" else safe
    }

    fun emitSwiftOpEnumConstants(w: CodeWriter, fields: List<SchemaField>) {
        val consts = collectEnumConstants(fields)
        if (consts.isEmpty()) return
        for ((k, v) in consts) {
            w.line("public static let ${toSwiftLowerCamel(k)}: Int32 = $v")
        }
    }

    fun emitSwiftProtocol(w: CodeWriter) {
        schema.idEncoding?.let { enc ->
            w.block("public enum IdEncoding") {
                line("public static let nanPrefix: Int32 = ${enc.nanPrefix}")
                line("public static let nanExponent: Int32 = ${enc.nanExponent}")
                line("public static let payloadMask: Int32 = ${enc.payloadMask}")
                line("public static let noId: Int32 = ${enc.noId}")
                line("public static let startId: Int32 = ${enc.startId}")
                line("public static let regionMask: Int32 = ${enc.regionMask}")
                line("public static let regionSubIdMask: Int32 = ${enc.regionSubIdMask}")
                line("public static let regionSystem: Int32 = ${enc.regionSystem}")
                line("public static let regionVariable: Int32 = ${enc.regionVariable}")
                line("public static let regionArray: Int32 = ${enc.regionArray}")
                line("public static let regionOperation: Int32 = ${enc.regionOperation}")
                line("public static let startArrayId: Int32 = ${enc.regionArray + enc.startId}")
                line("public static let longIdOffset: Int64 = ${enc.longIdOffset}")
                line("public static let ptrDereference: Int32 = ${enc.ptrDereference}")
                line("public static let valueMask: Int32 = ${enc.valueMask}")
                line("public static let macroLocalStartId: Int32 = ${enc.macroLocalStartId}")
                line("public static let macroLocalEndId: Int32 = ${enc.macroLocalEndId}")
                line(
                    "public static func isNanBits(_ bits: Int32) -> Bool { (bits & nanExponent) == nanExponent && (bits & payloadMask) != 0 }"
                )
                line(
                    "public static func idFromNanBits(_ bits: Int32) -> Int32 { isNanBits(bits) ? (bits & payloadMask) : noId }"
                )
                line(
                    "public static func asNanBits(_ id: Int32) -> Int32 { (id & payloadMask) | nanPrefix }"
                )
                line(
                    "public static func isDereferenced(_ raw: Int32) -> Bool { (raw & ptrDereference) != 0 }"
                )
                line(
                    "public static func directId(_ raw: Int32) -> Int32? { isDereferenced(raw) ? nil : (raw & valueMask) }"
                )
                line(
                    "public static func pointerId(_ raw: Int32) -> Int32? { isDereferenced(raw) ? (raw & valueMask) : nil }"
                )
            }
            w.line()
        }

        if (schema.headerConstants.isNotEmpty()) {
            w.block("public enum HeaderConstants") {
                for ((_, group) in schema.headerConstants) {
                    for ((k, v) in group.entries.sortedBy { it.value }) {
                        line("public static let ${toSwiftLowerCamel(k)}: Int32 = $v")
                    }
                }
            }
            w.line()
        }

        if (schema.paintCommands.isNotEmpty()) {
            w.block("public enum PaintCommandTags") {
                for ((id, cmd) in schema.paintCommands.entries.sortedBy { it.key }) {
                    line(
                        "public static let ${toSwiftLowerCamel(SchemaOperation.toScreamingSnakeCase(cmd.name))}: Int32 = $id"
                    )
                }
            }
            w.line()
            w.block("public enum PaintConstants") {
                for ((k, v) in collectPaintConstants()) {
                    line("public static let ${toSwiftLowerCamel(k)}: Int32 = $v")
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty()) {
            w.block("public enum PathCommandTags") {
                for ((id, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    line(
                        "public static let ${toSwiftLowerCamel(SchemaOperation.toScreamingSnakeCase(cmd.name))}: Int32 = $id"
                    )
                }
            }
            w.line()
        }

        if (schema.systemVariables.isNotEmpty()) {
            val timeIds = schema.systemVariables.filter { it.isTime }.map { it.id }.sorted()
            w.block("public enum RemoteContextIds") {
                for (sv in schema.systemVariables) {
                    line(
                        "public static let ${toSwiftLowerCamel("ID_${sv.name}")}: Int32 = ${sv.id}"
                    )
                }
                val expr =
                    if (timeIds.isEmpty()) "false"
                    else if (timeIds.size == timeIds.last() - timeIds.first() + 1)
                        "id >= ${timeIds.first()} && id <= ${timeIds.last()}"
                    else timeIds.joinToString(" || ") { "id == $it" }
                line("public static func isTime(_ id: Int32) -> Bool { $expr }")
            }
            w.line()
        }

        schema.animationSpec?.let { anim ->
            w.block("public enum AnimationEasing") {
                for (e in anim.easings.sortedBy { it.id }) {
                    line("public static let ${toSwiftLowerCamel(e.name)}: Int32 = ${e.id}")
                    for (a in e.aliases) {
                        line("public static let ${toSwiftLowerCamel(a)}: Int32 = ${e.id}")
                    }
                }
                block("public static func cubicControlPoints(_ type: Int32) -> [Float]?") {
                    block("switch type") {
                        for (e in anim.easings.sortedBy { it.id }) {
                            val pts = e.cubicControlPoints ?: continue
                            line(
                                "case ${toSwiftLowerCamel(e.name)}: return [${pts[0]}, ${pts[1]}, ${pts[2]}, ${pts[3]}]"
                            )
                        }
                        line("default: return nil")
                    }
                }
            }
            w.line()
            w.block("public struct FloatAnimationSpec: Equatable, Hashable, Sendable") {
                line("public var duration: Float")
                line("public var type: Int32")
                line("public var params: [Float]")
                line("public var initialValue: Float")
                line("public var wrap: Float")
                line("public var directionalSnap: Int32")
                line("public var isPropagate: Bool")
                block(
                    "public init(duration: Float = 1.0, type: Int32 = AnimationEasing.cubicStandard, params: [Float] = [], initialValue: Float = .nan, wrap: Float = .nan, directionalSnap: Int32 = 0, isPropagate: Bool = false)"
                ) {
                    line("self.duration = duration")
                    line("self.type = type")
                    line("self.params = params")
                    line("self.initialValue = initialValue")
                    line("self.wrap = wrap")
                    line("self.directionalSnap = directionalSnap")
                    line("self.isPropagate = isPropagate")
                }
                block(
                    "public static func == (lhs: FloatAnimationSpec, rhs: FloatAnimationSpec) -> Bool"
                ) {
                    line(
                        "let feq: (Float, Float) -> Bool = { (\$0.isNaN && \$1.isNaN) || \$0 == \$1 }"
                    )
                    line("return feq(lhs.duration, rhs.duration)")
                    line("    && lhs.type == rhs.type")
                    line("    && lhs.params.count == rhs.params.count")
                    line("    && zip(lhs.params, rhs.params).allSatisfy(feq)")
                    line("    && feq(lhs.initialValue, rhs.initialValue)")
                    line("    && feq(lhs.wrap, rhs.wrap)")
                    line("    && lhs.directionalSnap == rhs.directionalSnap")
                    line("    && lhs.isPropagate == rhs.isPropagate")
                }
                block("public func hash(into hasher: inout Hasher)") {
                    line(
                        "let norm: (Float) -> UInt32 = { \$0.isNaN ? 0x7FC0_0000 : (\$0 == 0 ? 0 : \$0.bitPattern) }"
                    )
                    line("hasher.combine(norm(duration))")
                    line("hasher.combine(type)")
                    line("hasher.combine(params.count)")
                    line("for p in params { hasher.combine(norm(p)) }")
                    line("hasher.combine(norm(initialValue))")
                    line("hasher.combine(norm(wrap))")
                    line("hasher.combine(directionalSnap)")
                    line("hasher.combine(isPropagate)")
                }
                line(
                    "public static func isSpring(_ data: [Float]) -> Bool { data.count > 4 && data[0] == 0.0 }"
                )
                block(
                    "public static func unpack(_ data: [Float]) throws(WireError) -> FloatAnimationSpec"
                ) {
                    line(
                        "if isSpring(data) { throw WireError.invalidAnimationSpec(\"Spring animation specs cannot be unpacked with unpackAnimationSpec\") }"
                    )
                    line("var out = FloatAnimationSpec()")
                    line("if data.isEmpty { return out }")
                    line("out.duration = data[0]")
                    line("if data.count == 1 { return out }")
                    line("let bits = data[1].bitPattern")
                    line("out.type = Int32(bits & 0xFF)")
                    line("let hasWrap = ((bits >> 8) & 1) != 0")
                    line("let hasInit = ((bits >> 9) & 1) != 0")
                    line("out.directionalSnap = Int32((bits >> 10) & 3)")
                    line("out.isPropagate = ((bits >> 12) & 1) != 0")
                    line("let paramLen = Int((bits >> 16) & 0xFFFF)")
                    line(
                        "if 2 + paramLen > data.count { throw WireError.invalidAnimationSpec(\"Truncated animation spec params\") }"
                    )
                    line("out.params = Array(data[2..<(2 + paramLen)])")
                    line("var off = 2 + paramLen")
                    line(
                        "if hasInit { if off >= data.count { throw WireError.invalidAnimationSpec(\"Truncated animation spec initialValue\") }; out.initialValue = data[off]; off += 1 }"
                    )
                    line(
                        "if hasWrap { if off >= data.count { throw WireError.invalidAnimationSpec(\"Truncated animation spec wrap\") }; out.wrap = data[off] }"
                    )
                    line("return out")
                }
                block(
                    "public static func pack(duration: Float = 1.0, type: Int32 = AnimationEasing.cubicStandard, spec: [Float]? = nil, initialValue: Float = .nan, wrap: Float = .nan) -> [Float]"
                ) {
                    line("var count = 0")
                    line("let hasInit = !initialValue.isNaN")
                    line("let hasWrap = !wrap.isNaN")
                    line("if hasInit { count += 1 }")
                    line("if spec != nil { count += 1 }")
                    line(
                        "if spec != nil || type != AnimationEasing.cubicStandard { count += 1 + (spec?.count ?? 0) }"
                    )
                    line("if hasInit { count += 1 }")
                    line("if hasWrap { count += 1 }")
                    line("if duration != 1.0 || count > 0 { count += 1 }")
                    line("if hasWrap || hasInit { count += 1 }")
                    line("var ret = Array(repeating: Float(0.0), count: count)")
                    line("var pos = 0")
                    line("let specLen = spec?.count ?? 0")
                    line("if !ret.isEmpty { ret[pos] = duration; pos += 1 }")
                    line("if ret.count > 1 {")
                    indent {
                        line("let wrapBit: UInt32 = hasWrap ? 1 : 0")
                        line("let initBit: UInt32 = hasInit ? 2 : 0")
                        line(
                            "let bits: UInt32 = UInt32(bitPattern: type) | ((wrapBit | initBit) << 8) | (UInt32(specLen) << 16)"
                        )
                        line("ret[pos] = Float(bitPattern: bits); pos += 1")
                    }
                    line("}")
                    line("if let s = spec { for v in s { ret[pos] = v; pos += 1 } }")
                    line("if hasInit { ret[pos] = initialValue; pos += 1 }")
                    line("if hasWrap { ret[pos] = wrap }")
                    line("return ret")
                }
            }
            w.line()
            w.line(
                "public func isSpringAnimationSpec(_ data: [Float]) -> Bool { FloatAnimationSpec.isSpring(data) }"
            )
            w.line(
                "public func unpackAnimationSpec(_ data: [Float]) throws(WireError) -> FloatAnimationSpec { try FloatAnimationSpec.unpack(data) }"
            )
            w.line(
                "public func packAnimationSpec(duration: Float = 1.0, type: Int32 = AnimationEasing.cubicStandard, spec: [Float]? = nil, initialValue: Float = .nan, wrap: Float = .nan) -> [Float] { FloatAnimationSpec.pack(duration: duration, type: type, spec: spec, initialValue: initialValue, wrap: wrap) }"
            )
            w.line()
        }

        schema.pathEffects?.let { pe ->
            w.block("public indirect enum PathEffect: Equatable, Hashable, Sendable") {
                line("case dash(phase: Float, intervals: [Float])")
                line("case discrete(segmentLength: Float, deviation: Float)")
                line("case pathDash(shapeId: Int32, advance: Float, phase: Float, style: Int32)")
                line("case sum(PathEffect, PathEffect)")
                line("case compose(PathEffect, PathEffect)")
                for (t in pe.types.sortedBy { it.id }) {
                    line("public static let ${toSwiftLowerCamel(t.constName)}: Int32 = ${t.id}")
                }
                line("public static let maxDashIntervals: Int = ${pe.maxDashIntervals}")
                line("public static let maxNestingDepth: Int = ${pe.maxNestingDepth}")
                block("public func encode() throws(WireError) -> [Float]") {
                    block("switch self") {
                        line(
                            "case let .dash(phase, intervals): if intervals.count > Self.maxDashIntervals { throw WireError.invalidPathEffect(\"Dash interval count (\\(intervals.count)) exceeds maxDashIntervals (\\(Self.maxDashIntervals))\") }; return [Float(bitPattern: UInt32(bitPattern: Self.typeDash)), phase, Float(bitPattern: UInt32(intervals.count))] + intervals"
                        )
                        line(
                            "case let .discrete(segmentLength, deviation): return [Float(bitPattern: UInt32(bitPattern: Self.typeDiscrete)), segmentLength, deviation]"
                        )
                        line(
                            "case let .pathDash(shapeId, advance, phase, style): return [Float(bitPattern: UInt32(bitPattern: Self.typePathDash)), Float(bitPattern: UInt32(bitPattern: shapeId)), advance, phase, Float(bitPattern: UInt32(bitPattern: style))]"
                        )
                        line(
                            "case let .sum(a, b): return [Float(bitPattern: UInt32(bitPattern: Self.typeSum))] + (try a.encode()) + (try b.encode())"
                        )
                        line(
                            "case let .compose(a, b): return [Float(bitPattern: UInt32(bitPattern: Self.typeCompose))] + (try a.encode()) + (try b.encode())"
                        )
                    }
                }
                block("public static func decode(_ data: [Float]) -> PathEffect?") {
                    line("if data.isEmpty { return nil }")
                    line("var cursor = 0")
                    line(
                        "guard let effect = parseOne(data, &cursor, 0), cursor == data.count else { return nil }"
                    )
                    line("return effect")
                }
                block(
                    "private static func parseOne(_ data: [Float], _ cursor: inout Int, _ depth: Int) -> PathEffect?"
                ) {
                    line("if depth > maxNestingDepth || cursor >= data.count { return nil }")
                    line("let idx = cursor")
                    line("let tag = Int32(bitPattern: data[idx].bitPattern)")
                    block("switch tag") {
                        line(
                            "case typeDash: if idx + 3 > data.count { return nil }; let count = Int(Int32(bitPattern: data[idx + 2].bitPattern)); if count < 0 || count > maxDashIntervals || count > data.count - (idx + 3) { return nil }; cursor = idx + 3 + count; return .dash(phase: data[idx + 1], intervals: Array(data[(idx + 3)..<(idx + 3 + count)]))"
                        )
                        line(
                            "case typeDiscrete: if idx + 3 > data.count { return nil }; cursor = idx + 3; return .discrete(segmentLength: data[idx + 1], deviation: data[idx + 2])"
                        )
                        line(
                            "case typePathDash: if idx + 5 > data.count { return nil }; cursor = idx + 5; return .pathDash(shapeId: Int32(bitPattern: data[idx + 1].bitPattern), advance: data[idx + 2], phase: data[idx + 3], style: Int32(bitPattern: data[idx + 4].bitPattern))"
                        )
                        line(
                            "case typeSum: cursor = idx + 1; guard let a = parseOne(data, &cursor, depth + 1), let b = parseOne(data, &cursor, depth + 1) else { return nil }; return .sum(a, b)"
                        )
                        line(
                            "case typeCompose: cursor = idx + 1; guard let a = parseOne(data, &cursor, depth + 1), let b = parseOne(data, &cursor, depth + 1) else { return nil }; return .compose(a, b)"
                        )
                        line("default: return nil")
                    }
                }
                block("public static func == (lhs: PathEffect, rhs: PathEffect) -> Bool") {
                    block("switch (lhs, rhs)") {
                        line(
                            "case (.dash(let lp, let li), .dash(let rp, let ri)): return wireFloatEquals(lp, rp) && wireFloatEquals(li, ri)"
                        )
                        line(
                            "case (.discrete(let ls, let ld), .discrete(let rs, let rd)): return wireFloatEquals(ls, rs) && wireFloatEquals(ld, rd)"
                        )
                        line(
                            "case (.pathDash(let lShape, let lAdv, let lPhase, let lStyle), .pathDash(let rShape, let rAdv, let rPhase, let rStyle)): return lShape == rShape && wireFloatEquals(lAdv, rAdv) && wireFloatEquals(lPhase, rPhase) && lStyle == rStyle"
                        )
                        line(
                            "case (.sum(let la, let lb), .sum(let ra, let rb)): return la == ra && lb == rb"
                        )
                        line(
                            "case (.compose(let la, let lb), .compose(let ra, let rb)): return la == ra && lb == rb"
                        )
                        line("default: return false")
                    }
                }
                block("public func hash(into hasher: inout Hasher)") {
                    block("switch self") {
                        line("case .dash(let phase, let intervals):")
                        indent {
                            line("hasher.combine(0)")
                            line("wireFloatHash(phase, into: &hasher)")
                            line("wireFloatHash(intervals, into: &hasher)")
                        }
                        line("case .discrete(let segmentLength, let deviation):")
                        indent {
                            line("hasher.combine(1)")
                            line("wireFloatHash(segmentLength, into: &hasher)")
                            line("wireFloatHash(deviation, into: &hasher)")
                        }
                        line("case .pathDash(let shapeId, let advance, let phase, let style):")
                        indent {
                            line("hasher.combine(2)")
                            line("hasher.combine(shapeId)")
                            line("wireFloatHash(advance, into: &hasher)")
                            line("wireFloatHash(phase, into: &hasher)")
                            line("hasher.combine(style)")
                        }
                        line("case .sum(let a, let b):")
                        indent {
                            line("hasher.combine(3)")
                            line("hasher.combine(a)")
                            line("hasher.combine(b)")
                        }
                        line("case .compose(let a, let b):")
                        indent {
                            line("hasher.combine(4)")
                            line("hasher.combine(a)")
                            line("hasher.combine(b)")
                        }
                    }
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty() && schema.idEncoding != null) {
            val sortedPathCommands = schema.pathCommands.entries.sortedBy { it.key }
            w.block("public enum PathCommand: Equatable, Hashable, Sendable") {
                for ((_, cmd) in sortedPathCommands) {
                    val vName = pathCmdVariantName(cmd).replaceFirstChar { it.lowercaseChar() }
                    if (cmd.fields.isEmpty()) {
                        line("case $vName")
                    } else {
                        val fList = cmd.fields.joinToString(", ") { "${it.name}: Float" }
                        line("case $vName($fList)")
                    }
                }
                line("case unknown(verb: Int32)")
                block("public static func == (lhs: PathCommand, rhs: PathCommand) -> Bool") {
                    block("switch (lhs, rhs)") {
                        for ((_, cmd) in sortedPathCommands) {
                            val vName =
                                pathCmdVariantName(cmd).replaceFirstChar { it.lowercaseChar() }
                            if (cmd.fields.isEmpty()) {
                                line("case (.$vName, .$vName): return true")
                            } else {
                                val lhsBinds = cmd.fields.indices.joinToString(", ") { "let l$it" }
                                val rhsBinds = cmd.fields.indices.joinToString(", ") { "let r$it" }
                                val checks =
                                    cmd.fields.indices.joinToString(" && ") {
                                        "wireFloatEquals(l$it, r$it)"
                                    }
                                line(
                                    "case (.$vName($lhsBinds), .$vName($rhsBinds)): return $checks"
                                )
                            }
                        }
                        line("case (.unknown(let l), .unknown(let r)): return l == r")
                        line("default: return false")
                    }
                }
                block("public func hash(into hasher: inout Hasher)") {
                    block("switch self") {
                        for ((idx, entry) in sortedPathCommands.withIndex()) {
                            val cmd = entry.value
                            val vName =
                                pathCmdVariantName(cmd).replaceFirstChar { it.lowercaseChar() }
                            if (cmd.fields.isEmpty()) {
                                line("case .$vName: hasher.combine($idx)")
                            } else {
                                val binds = cmd.fields.indices.joinToString(", ") { "let v$it" }
                                line("case .$vName($binds):")
                                indent {
                                    line("hasher.combine($idx)")
                                    for (fIdx in cmd.fields.indices) {
                                        line("wireFloatHash(v$fIdx, into: &hasher)")
                                    }
                                }
                            }
                        }
                        line("case .unknown(let verb):")
                        indent {
                            line("hasher.combine(${sortedPathCommands.size})")
                            line("hasher.combine(verb)")
                        }
                    }
                }
                block("public static func decode(_ pathData: [Float]) -> [PathCommand]") {
                    line("var result: [PathCommand] = []")
                    line("var i = 0")
                    line("let n = pathData.count")
                    block("while i < n") {
                        line(
                            "let tag = IdEncoding.idFromNanBits(Int32(bitPattern: pathData[i].bitPattern))"
                        )
                        block("switch tag") {
                            for ((_, cmd) in sortedPathCommands) {
                                val tagConst =
                                    toSwiftLowerCamel(
                                        SchemaOperation.toScreamingSnakeCase(cmd.name)
                                    )
                                val vName =
                                    pathCmdVariantName(cmd).replaceFirstChar { it.lowercaseChar() }
                                val total = 1 + cmd.leadingPaddingFloats + cmd.fields.size
                                if (cmd.fields.isEmpty()) {
                                    val next = if (cmd.name == "Done") "return result" else "i += 1"
                                    line(
                                        "case PathCommandTags.$tagConst: result.append(.$vName); $next"
                                    )
                                } else {
                                    val args =
                                        cmd.fields.withIndex().joinToString(", ") { (fIdx, fl) ->
                                            "${fl.name}: pathData[i + ${1 + cmd.leadingPaddingFloats + fIdx}]"
                                        }
                                    line(
                                        "case PathCommandTags.$tagConst: if i + $total > n { result.append(.unknown(verb: tag)); return result }; result.append(.$vName($args)); i += $total"
                                    )
                                }
                            }
                            line("default: result.append(.unknown(verb: tag)); return result")
                        }
                    }
                    line("return result")
                }
            }
            w.line()
            w.line(
                "public func decodePathCommands(_ pathData: [Float]) -> [PathCommand] { PathCommand.decode(pathData) }"
            )
            w.line()
        }
    }

    fun emitSwiftOpExtras(w: CodeWriter, op: SchemaOperation) {
        if (op.name == "FloatExpression" && schema.animationSpec != null) {
            w.line(
                "public var isSpringAnimation: Bool { FloatAnimationSpec.isSpring(animationSpec) }"
            )
            w.line(
                "public func unpackAnimationSpec() throws(WireError) -> FloatAnimationSpec { try FloatAnimationSpec.unpack(animationSpec) }"
            )
        }
        if (isBitmapDataReady(op)) {
            w.line(
                "public var uncompressedByteSize: Int64 { let w = Int64(width); let h = Int64(height); if w <= 0 || h <= 0 { return 0 }; let bpp: Int64 = imageType == BitmapData.typePngAlpha8 ? 1 : 4; let (wh, o1) = w.multipliedReportingOverflow(by: h); if o1 { return Int64.max }; let (total, o2) = wh.multipliedReportingOverflow(by: bpp); return o2 ? Int64.max : total }"
            )
        }
        if (
            (op.name == "PathData" || op.name == "PathAppend") &&
                schema.pathCommands.isNotEmpty() &&
                schema.idEncoding != null
        ) {
            w.line(
                "public func decodePathCommands() -> [PathCommand] { PathCommand.decode(pathData) }"
            )
        }
        if (op.name == "ColorExpression" && op.fields.any { it.name == "flagsAndMode" }) {
            w.line(
                "public var alphaPayload: Int32 { Int32((UInt32(bitPattern: flagsAndMode) >> 16) & 0xFF) }"
            )
        }
    }

    // =========================================================================================
    // TypeScript
    // =========================================================================================

    fun emitTsOpEnumConstants(w: CodeWriter, fields: List<SchemaField>) {
        val consts = collectEnumConstants(fields)
        if (consts.isEmpty()) return
        for ((k, v) in consts) {
            w.line("static readonly $k: number = $v;")
        }
    }

    fun emitTsProtocol(w: CodeWriter) {
        val needsFloatBitHelpers =
            schema.animationSpec != null ||
                schema.pathEffects != null ||
                (schema.pathCommands.isNotEmpty() && schema.idEncoding != null)
        if (needsFloatBitHelpers) {
            w.line("const _f32Buf = new ArrayBuffer(4);")
            w.line("const _f32View = new DataView(_f32Buf);")
            w.line(
                "function floatToRawBits(v: number): number { _f32View.setFloat32(0, v, false); return _f32View.getInt32(0, false); }"
            )
            w.line(
                "function bitsToFloat(bits: number): number { _f32View.setInt32(0, bits | 0, false); return _f32View.getFloat32(0, false); }"
            )
            w.line(
                "function rawInt32View(data: ArrayLike<number>): Int32Array | null { return data instanceof Float32Array ? new Int32Array(data.buffer, data.byteOffset, data.length) : null; }"
            )
            w.line()
        }

        schema.idEncoding?.let { enc ->
            w.block("export const IdEncoding =", close = "} as const;") {
                line("NAN_PREFIX: ${enc.nanPrefix},")
                line("NAN_EXPONENT: ${enc.nanExponent},")
                line("PAYLOAD_MASK: ${enc.payloadMask},")
                line("NO_ID: ${enc.noId},")
                line("START_ID: ${enc.startId},")
                line("REGION_MASK: ${enc.regionMask},")
                line("REGION_SUB_ID_MASK: ${enc.regionSubIdMask},")
                line("REGION_SYSTEM: ${enc.regionSystem},")
                line("REGION_VARIABLE: ${enc.regionVariable},")
                line("REGION_ARRAY: ${enc.regionArray},")
                line("REGION_OPERATION: ${enc.regionOperation},")
                line("START_ARRAY_ID: ${enc.regionArray + enc.startId},")
                line("LONG_ID_OFFSET: ${enc.longIdOffset}n,")
                line("PTR_DEREFERENCE: ${enc.ptrDereference},")
                line("VALUE_MASK: ${enc.valueMask},")
                line("MACRO_LOCAL_START_ID: ${enc.macroLocalStartId},")
                line("MACRO_LOCAL_END_ID: ${enc.macroLocalEndId},")
                line(
                    "isNanBits(bits: number): boolean { return ((bits & ${enc.nanExponent}) | 0) === ${enc.nanExponent} && (bits & ${enc.payloadMask}) !== 0; },"
                )
                line(
                    "idFromNanBits(bits: number): number { return IdEncoding.isNanBits(bits) ? (bits & ${enc.payloadMask}) : ${enc.noId}; },"
                )
                line(
                    "asNanBits(id: number): number { return ((id & ${enc.payloadMask}) | ${enc.nanPrefix}) | 0; },"
                )
                line(
                    "isDereferenced(raw: number): boolean { return (raw & ${enc.ptrDereference}) !== 0; },"
                )
                line(
                    "directId(raw: number): number | null { return IdEncoding.isDereferenced(raw) ? null : (raw & ${enc.valueMask}); },"
                )
                line(
                    "pointerId(raw: number): number | null { return IdEncoding.isDereferenced(raw) ? (raw & ${enc.valueMask}) : null; },"
                )
            }
            w.line()
        }

        if (schema.headerConstants.isNotEmpty()) {
            w.block("export const HeaderConstants =", close = "} as const;") {
                for ((_, group) in schema.headerConstants) {
                    for ((k, v) in group.entries.sortedBy { it.value }) {
                        line("$k: $v,")
                    }
                }
            }
            w.line()
        }

        if (schema.paintCommands.isNotEmpty()) {
            w.block("export const PaintCommandTags =", close = "} as const;") {
                for ((id, cmd) in schema.paintCommands.entries.sortedBy { it.key }) {
                    line("${SchemaOperation.toScreamingSnakeCase(cmd.name)}: $id,")
                }
            }
            w.line()
            w.block("export const PaintConstants =", close = "} as const;") {
                for ((k, v) in collectPaintConstants()) {
                    line("$k: $v,")
                }
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty()) {
            w.block("export const PathCommandTags =", close = "} as const;") {
                for ((id, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    line("${SchemaOperation.toScreamingSnakeCase(cmd.name)}: $id,")
                }
            }
            w.line()
        }

        if (schema.systemVariables.isNotEmpty()) {
            val timeIds = schema.systemVariables.filter { it.isTime }.map { it.id }.sorted()
            w.block("export const RemoteContextIds =", close = "} as const;") {
                for (sv in schema.systemVariables) {
                    line("ID_${sv.name}: ${sv.id},")
                }
                val expr =
                    if (timeIds.isEmpty()) "false"
                    else if (timeIds.size == timeIds.last() - timeIds.first() + 1)
                        "id >= ${timeIds.first()} && id <= ${timeIds.last()}"
                    else timeIds.joinToString(" || ") { "id === $it" }
                line("isTime(id: number): boolean { return $expr; },")
            }
            w.line()
        }

        schema.animationSpec?.let { anim ->
            w.block("export const AnimationEasing =", close = "} as const;") {
                for (e in anim.easings.sortedBy { it.id }) {
                    line("${e.name}: ${e.id},")
                    for (a in e.aliases) {
                        line("$a: ${e.id},")
                    }
                }
                block(
                    "cubicControlPoints(type: number): readonly [number, number, number, number] | null"
                ) {
                    block("switch (type)") {
                        for (e in anim.easings.sortedBy { it.id }) {
                            val pts = e.cubicControlPoints ?: continue
                            line(
                                "case ${e.id}: return [${pts[0]}, ${pts[1]}, ${pts[2]}, ${pts[3]}];"
                            )
                        }
                        line("default: return null;")
                    }
                }
            }
            w.line()
            w.block("export interface FloatAnimationSpec") {
                line("duration: number;")
                line("type: number;")
                line("params: number[];")
                line("initialValue: number;")
                line("wrap: number;")
                line("directionalSnap: number;")
                line("isPropagate: boolean;")
            }
            w.line()
            w.line(
                "export function isSpringAnimationSpec(data: ArrayLike<number>): boolean { return data.length > 4 && data[0] === 0; }"
            )
            w.block(
                "export function unpackAnimationSpec(data: ArrayLike<number>): FloatAnimationSpec"
            ) {
                line(
                    "if (isSpringAnimationSpec(data)) throw new Error(\"Spring animation specs cannot be unpacked with unpackAnimationSpec\");"
                )
                line(
                    "const out: FloatAnimationSpec = { duration: 1, type: AnimationEasing.CUBIC_STANDARD, params: [], initialValue: NaN, wrap: NaN, directionalSnap: 0, isPropagate: false };"
                )
                line("if (data.length === 0) return out;")
                line("out.duration = data[0];")
                line("if (data.length === 1) return out;")
                line("const rawBits = rawInt32View(data);")
                line("const bits = (rawBits ? rawBits[1] : floatToRawBits(data[1])) >>> 0;")
                line("out.type = bits & 0xFF;")
                line("const hasWrap = ((bits >>> 8) & 1) !== 0;")
                line("const hasInit = ((bits >>> 9) & 1) !== 0;")
                line("out.directionalSnap = (bits >>> 10) & 3;")
                line("out.isPropagate = ((bits >>> 12) & 1) !== 0;")
                line("const paramLen = (bits >>> 16) & 0xFFFF;")
                line(
                    "if (2 + paramLen > data.length) throw new Error(\"Truncated animation spec params\");"
                )
                line("out.params = Array.from({ length: paramLen }, (_, k) => data[2 + k]);")
                line("let off = 2 + paramLen;")
                line(
                    "if (hasInit) { if (off >= data.length) throw new Error(\"Truncated animation spec initialValue\"); out.initialValue = data[off++]; }"
                )
                line(
                    "if (hasWrap) { if (off >= data.length) throw new Error(\"Truncated animation spec wrap\"); out.wrap = data[off++]; }"
                )
                line("return out;")
            }
            w.line()
            w.block(
                "export function packAnimationSpec(duration: number = 1, type: number = AnimationEasing.CUBIC_STANDARD, spec: ArrayLike<number> | null = null, initialValue: number = NaN, wrap: number = NaN): number[]"
            ) {
                line("let count = 0;")
                line("const hasInit = !Number.isNaN(initialValue);")
                line("const hasWrap = !Number.isNaN(wrap);")
                line("if (hasInit) count++;")
                line("if (spec !== null) count++;")
                line(
                    "if (spec !== null || type !== AnimationEasing.CUBIC_STANDARD) { count++; count += spec !== null ? spec.length : 0; }"
                )
                line("if (hasInit) count++;")
                line("if (hasWrap) count++;")
                line("if (duration !== 1 || count > 0) count++;")
                line("if (hasWrap || hasInit) count++;")
                line("const ret = new Array<number>(count).fill(0);")
                line("let pos = 0;")
                line("const specLen = spec !== null ? spec.length : 0;")
                line("if (ret.length > 0) ret[pos++] = duration;")
                line("if (ret.length > 1) {")
                indent {
                    line("const wrapBit = hasWrap ? 1 : 0;")
                    line("const initBit = hasInit ? 2 : 0;")
                    line("const bits = (type | ((wrapBit | initBit) << 8) | (specLen << 16)) | 0;")
                    line("ret[pos++] = bitsToFloat(bits);")
                }
                line("}")
                line(
                    "if (spec !== null) { for (let k = 0; k < spec.length; k++) ret[pos++] = spec[k]; }"
                )
                line("if (hasInit) ret[pos++] = initialValue;")
                line("if (hasWrap) ret[pos] = wrap;")
                line("return ret;")
            }
            w.line()
        }

        schema.pathEffects?.let { pe ->
            w.line("export type PathEffect =")
            w.indent {
                w.line("| { kind: \"dash\"; phase: number; intervals: number[] }")
                w.line("| { kind: \"discrete\"; segmentLength: number; deviation: number }")
                w.line(
                    "| { kind: \"pathDash\"; shapeId: number; advance: number; phase: number; style: number }"
                )
                w.line("| { kind: \"sum\"; first: PathEffect; second: PathEffect }")
                w.line("| { kind: \"compose\"; outer: PathEffect; inner: PathEffect };")
            }
            w.line()
            w.block("export const PathEffectConstants =", close = "} as const;") {
                for (t in pe.types.sortedBy { it.id }) {
                    line("${t.constName}: ${t.id},")
                }
                line("MAX_DASH_INTERVALS: ${pe.maxDashIntervals},")
                line("MAX_NESTING_DEPTH: ${pe.maxNestingDepth},")
            }
            w.line()
            w.block("export function encodePathEffect(effect: PathEffect): number[]") {
                block("switch (effect.kind)") {
                    line(
                        "case \"dash\": if (effect.intervals.length > PathEffectConstants.MAX_DASH_INTERVALS) throw new RangeError(\"Dash interval count exceeds MAX_DASH_INTERVALS\"); return [bitsToFloat(PathEffectConstants.TYPE_DASH), effect.phase, bitsToFloat(effect.intervals.length), ...effect.intervals];"
                    )
                    line(
                        "case \"discrete\": return [bitsToFloat(PathEffectConstants.TYPE_DISCRETE), effect.segmentLength, effect.deviation];"
                    )
                    line(
                        "case \"pathDash\": return [bitsToFloat(PathEffectConstants.TYPE_PATH_DASH), bitsToFloat(effect.shapeId), effect.advance, effect.phase, bitsToFloat(effect.style)];"
                    )
                    line(
                        "case \"sum\": return [bitsToFloat(PathEffectConstants.TYPE_SUM), ...encodePathEffect(effect.first), ...encodePathEffect(effect.second)];"
                    )
                    line(
                        "case \"compose\": return [bitsToFloat(PathEffectConstants.TYPE_COMPOSE), ...encodePathEffect(effect.outer), ...encodePathEffect(effect.inner)];"
                    )
                }
            }
            w.line()
            w.block(
                "export function decodePathEffect(data: ArrayLike<number>): PathEffect | null"
            ) {
                line("if (data.length === 0) return null;")
                line("const rawBits = rawInt32View(data);")
                line(
                    "const readBits = (index: number): number => rawBits ? rawBits[index] : floatToRawBits(data[index]);"
                )
                line("const state = { cursor: 0 };")
                block("function parseOne(depth: number): PathEffect | null") {
                    line(
                        "if (depth > PathEffectConstants.MAX_NESTING_DEPTH || state.cursor >= data.length) return null;"
                    )
                    line("const idx = state.cursor;")
                    line("const tag = readBits(idx);")
                    block("switch (tag)") {
                        line(
                            "case PathEffectConstants.TYPE_DASH: { if (idx + 3 > data.length) return null; const count = readBits(idx + 2); if (count < 0 || count > PathEffectConstants.MAX_DASH_INTERVALS || count > data.length - (idx + 3)) return null; state.cursor = idx + 3 + count; return { kind: \"dash\", phase: data[idx + 1], intervals: Array.from({ length: count }, (_, k) => data[idx + 3 + k]) }; }"
                        )
                        line(
                            "case PathEffectConstants.TYPE_DISCRETE: { if (idx + 3 > data.length) return null; state.cursor = idx + 3; return { kind: \"discrete\", segmentLength: data[idx + 1], deviation: data[idx + 2] }; }"
                        )
                        line(
                            "case PathEffectConstants.TYPE_PATH_DASH: { if (idx + 5 > data.length) return null; state.cursor = idx + 5; return { kind: \"pathDash\", shapeId: readBits(idx + 1), advance: data[idx + 2], phase: data[idx + 3], style: readBits(idx + 4) }; }"
                        )
                        line(
                            "case PathEffectConstants.TYPE_SUM: { state.cursor = idx + 1; const first = parseOne(depth + 1); if (!first) return null; const second = parseOne(depth + 1); if (!second) return null; return { kind: \"sum\", first, second }; }"
                        )
                        line(
                            "case PathEffectConstants.TYPE_COMPOSE: { state.cursor = idx + 1; const outer = parseOne(depth + 1); if (!outer) return null; const inner = parseOne(depth + 1); if (!inner) return null; return { kind: \"compose\", outer, inner }; }"
                        )
                        line("default: return null;")
                    }
                }
                line("const res = parseOne(0);")
                line("return res !== null && state.cursor === data.length ? res : null;")
            }
            w.line()
        }

        if (schema.pathCommands.isNotEmpty() && schema.idEncoding != null) {
            w.line("export type PathCommand =")
            w.indent {
                for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                    val vName = pathCmdVariantName(cmd)
                    val fieldsStr =
                        if (cmd.fields.isEmpty()) ""
                        else "; " + cmd.fields.joinToString("; ") { "${it.name}: number" }
                    w.line("| { kind: \"$vName\"$fieldsStr }")
                }
                w.line("| { kind: \"Unknown\"; verb: number };")
            }
            w.line()
            w.block(
                "export function decodePathCommands(pathData: ArrayLike<number>): PathCommand[]"
            ) {
                line("const result: PathCommand[] = [];")
                line("const rawBits = rawInt32View(pathData);")
                line("let i = 0; const n = pathData.length;")
                block("while (i < n)") {
                    line(
                        "const tag = IdEncoding.idFromNanBits(rawBits ? rawBits[i] : floatToRawBits(pathData[i]));"
                    )
                    block("switch (tag)") {
                        for ((_, cmd) in schema.pathCommands.entries.sortedBy { it.key }) {
                            val tagConst = SchemaOperation.toScreamingSnakeCase(cmd.name)
                            val vName = pathCmdVariantName(cmd)
                            val total = 1 + cmd.leadingPaddingFloats + cmd.fields.size
                            if (cmd.fields.isEmpty()) {
                                val next =
                                    if (cmd.name == "Done") "return result;" else "i += 1; break;"
                                line(
                                    "case PathCommandTags.$tagConst: result.push({ kind: \"$vName\" }); $next"
                                )
                            } else {
                                val args =
                                    cmd.fields.withIndex().joinToString(", ") { (fIdx, fl) ->
                                        "${fl.name}: pathData[i + ${1 + cmd.leadingPaddingFloats + fIdx}]"
                                    }
                                line(
                                    "case PathCommandTags.$tagConst: if (i + $total > n) { result.push({ kind: \"Unknown\", verb: tag }); return result; } result.push({ kind: \"$vName\", $args }); i += $total; break;"
                                )
                            }
                        }
                        line(
                            "default: result.push({ kind: \"Unknown\", verb: tag }); return result;"
                        )
                    }
                }
                line("return result;")
            }
            w.line()
        }
    }

    fun emitTsOpExtras(w: CodeWriter, op: SchemaOperation) {
        if (op.name == "FloatExpression" && schema.animationSpec != null) {
            w.line(
                "isSpringAnimation(): boolean { return isSpringAnimationSpec(this.animationSpec); }"
            )
            w.line(
                "unpackAnimationSpec(): FloatAnimationSpec { return unpackAnimationSpec(this.animationSpec); }"
            )
        }
        if (isBitmapDataReady(op)) {
            w.line(
                "getUncompressedByteSize(): number { const w = this.getWidth(); const h = this.getHeight(); if (w <= 0 || h <= 0) return 0; return w * h * (this.getImageType() === BitmapData.TYPE_PNG_ALPHA_8 ? 1 : 4); }"
            )
        }
        if (
            (op.name == "PathData" || op.name == "PathAppend") &&
                schema.pathCommands.isNotEmpty() &&
                schema.idEncoding != null
        ) {
            w.line(
                "decodePathCommands(): PathCommand[] { return decodePathCommands(this.pathData); }"
            )
        }
        if (op.name == "ColorExpression" && op.fields.any { it.name == "flagsAndMode" }) {
            w.line("getAlphaPayload(): number { return (this.flagsAndMode >>> 16) & 0xFF; }")
        }
    }

    private companion object {
        val SWIFT_KEYWORDS =
            setOf(
                "associatedtype",
                "class",
                "deinit",
                "enum",
                "extension",
                "fileprivate",
                "func",
                "import",
                "init",
                "inout",
                "internal",
                "let",
                "open",
                "operator",
                "private",
                "precedencegroup",
                "protocol",
                "public",
                "rethrows",
                "static",
                "struct",
                "subscript",
                "typealias",
                "var",
                "break",
                "case",
                "catch",
                "continue",
                "default",
                "defer",
                "do",
                "else",
                "fallthrough",
                "for",
                "guard",
                "if",
                "in",
                "repeat",
                "return",
                "throw",
                "switch",
                "where",
                "while",
                "as",
                "false",
                "is",
                "nil",
                "self",
                "Self",
                "super",
                "throws",
                "true",
                "try",
                "Any",
                "Type",
            )
    }
}
