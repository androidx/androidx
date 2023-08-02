/*
 * Copyright (C) 2019 The Android Open Source Project
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
#include "array_params_entry_hook.h"

namespace androidx_inspection {

namespace {

struct BytecodeConvertingVisitor : public lir::Visitor {
  lir::Bytecode* out = nullptr;
  bool Visit(lir::Bytecode* bytecode) {
    out = bytecode;
    return true;
  }
};

void BoxValue(lir::Bytecode* bytecode, lir::CodeIr* code_ir, ir::Type* type,
              dex::u4 src_reg, dex::u4 dst_reg) {
  bool is_wide = false;
  const char* boxed_type_name = nullptr;
  switch (*(type->descriptor)->c_str()) {
    case 'Z':
      boxed_type_name = "Ljava/lang/Boolean;";
      break;
    case 'B':
      boxed_type_name = "Ljava/lang/Byte;";
      break;
    case 'C':
      boxed_type_name = "Ljava/lang/Character;";
      break;
    case 'S':
      boxed_type_name = "Ljava/lang/Short;";
      break;
    case 'I':
      boxed_type_name = "Ljava/lang/Integer;";
      break;
    case 'J':
      is_wide = true;
      boxed_type_name = "Ljava/lang/Long;";
      break;
    case 'F':
      boxed_type_name = "Ljava/lang/Float;";
      break;
    case 'D':
      is_wide = true;
      boxed_type_name = "Ljava/lang/Double;";
      break;
  }
  SLICER_CHECK(boxed_type_name != nullptr);

  ir::Builder builder(code_ir->dex_ir);
  std::vector<ir::Type*> param_types;
  param_types.push_back(type);

  auto boxed_type = builder.GetType(boxed_type_name);
  auto ir_proto =
      builder.GetProto(boxed_type, builder.GetTypeList(param_types));

  auto ir_method_decl = builder.GetMethodDecl(builder.GetAsciiString("valueOf"),
                                              ir_proto, boxed_type);

  auto boxing_method =
      code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);

  auto args = code_ir->Alloc<lir::VRegRange>(src_reg, 1 + is_wide);
  auto boxing_invoke = code_ir->Alloc<lir::Bytecode>();
  boxing_invoke->opcode = dex::OP_INVOKE_STATIC_RANGE;
  boxing_invoke->operands.push_back(args);
  boxing_invoke->operands.push_back(boxing_method);
  code_ir->instructions.InsertBefore(bytecode, boxing_invoke);

  auto move_result = code_ir->Alloc<lir::Bytecode>();
  move_result->opcode = dex::OP_MOVE_RESULT_OBJECT;
  move_result->operands.push_back(code_ir->Alloc<lir::VReg>(dst_reg));
  code_ir->instructions.InsertBefore(bytecode, move_result);
}

std::string MethodLabel(ir::EncodedMethod* ir_method) {
  auto signature_str = ir_method->decl->prototype->Signature();
  return ir_method->decl->parent->Decl() + "->" +
         ir_method->decl->name->c_str() + signature_str;
}

void GenerateShiftParamsCode(lir::CodeIr* code_ir, lir::Instruction* position,
                             dex::u4 shift) {
  const auto ir_method = code_ir->ir_method;
  SLICER_CHECK(ir_method->code->ins_count > 0);

  // build a param list with the explicit "this" argument for non-static methods
  std::vector<ir::Type*> param_types;
  if ((ir_method->access_flags & dex::kAccStatic) == 0) {
    param_types.push_back(ir_method->decl->parent);
  }
  if (ir_method->decl->prototype->param_types != nullptr) {
    const auto& orig_param_types =
        ir_method->decl->prototype->param_types->types;
    param_types.insert(param_types.end(), orig_param_types.begin(),
                       orig_param_types.end());
  }

  const dex::u4 regs = ir_method->code->registers;
  const dex::u4 ins_count = ir_method->code->ins_count;
  SLICER_CHECK(regs >= ins_count);

  // generate the args "relocation" instructions
  dex::u4 reg = regs - ins_count;
  for (const auto& type : param_types) {
    auto move = code_ir->Alloc<lir::Bytecode>();
    switch (type->GetCategory()) {
      case ir::Type::Category::Reference:
        move->opcode = dex::OP_MOVE_OBJECT_16;
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg - shift));
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
        reg += 1;
        break;
      case ir::Type::Category::Scalar:
        move->opcode = dex::OP_MOVE_16;
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg - shift));
        move->operands.push_back(code_ir->Alloc<lir::VReg>(reg));
        reg += 1;
        break;
      case ir::Type::Category::WideScalar:
        move->opcode = dex::OP_MOVE_WIDE_16;
        move->operands.push_back(code_ir->Alloc<lir::VRegPair>(reg - shift));
        move->operands.push_back(code_ir->Alloc<lir::VRegPair>(reg));
        reg += 2;
        break;
      case ir::Type::Category::Void:
        SLICER_FATAL("void parameter type");
    }
    code_ir->instructions.InsertBefore(position, move);
  }
}

}  // namespace

bool ArrayParamsEntryHook::Apply(lir::CodeIr* code_ir) {
  lir::Bytecode* bytecode = nullptr;
  // find the first bytecode in the method body to insert the hook before it
  for (auto instr : code_ir->instructions) {
    BytecodeConvertingVisitor visitor;
    instr->Accept(&visitor);
    bytecode = visitor.out;
    if (bytecode != nullptr) {
      break;
    }
  }
  if (bytecode == nullptr) {
    return false;
  }

  ir::Builder builder(code_ir->dex_ir);
  const auto ir_method = code_ir->ir_method;
  auto param_types_list = ir_method->decl->prototype->param_types;
  auto param_types = param_types_list != nullptr ? param_types_list->types
                                                 : std::vector<ir::Type*>();
  bool is_static = (ir_method->access_flags & dex::kAccStatic) != 0;

  // number of registers that we need to operate
  dex::u2 regs_count = 3;
  auto non_param_regs = ir_method->code->registers - ir_method->code->ins_count;

  // do we have enough registers to operate?
  bool needsExtraRegs = non_param_regs < regs_count;
  if (needsExtraRegs) {
    // we don't have enough registers, so we allocate more, we will shift
    // params to their original registers later.
    code_ir->ir_method->code->registers += regs_count - non_param_regs;
  }

  // use three first registers:
  // all three are needed when we "aput" a string/boxed-value (1) into an array
  // (2) at an index (3)

  // register that will store size of during allocation
  // later will be reused to store index when do "aput"
  dex::u4 array_size_reg = 0;
  // register that will store an array that will be passed
  // as a parameter in entry hook
  dex::u4 array_reg = 1;
  // stores result of boxing (if it's needed); also stores the method signature
  // string
  dex::u4 value_reg = 2;
  // array size bytecode
  auto const_size_op = code_ir->Alloc<lir::Bytecode>();
  const_size_op->opcode = dex::OP_CONST;
  const_size_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_size_reg));
  const_size_op->operands.push_back(code_ir->Alloc<lir::Const32>(
      2 + param_types.size()));  // method signature + params + "this" object
  code_ir->instructions.InsertBefore(bytecode, const_size_op);

  // allocate array
  const auto obj_array_type = builder.GetType("[Ljava/lang/Object;");
  auto allocate_array_op = code_ir->Alloc<lir::Bytecode>();
  allocate_array_op->opcode = dex::OP_NEW_ARRAY;
  allocate_array_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_reg));
  allocate_array_op->operands.push_back(
      code_ir->Alloc<lir::VReg>(array_size_reg));
  allocate_array_op->operands.push_back(
      code_ir->Alloc<lir::Type>(obj_array_type, obj_array_type->orig_index));
  code_ir->instructions.InsertBefore(bytecode, allocate_array_op);

  // fill the array with parameters passed into function

  std::vector<ir::Type*> types;
  types.push_back(
      builder.GetType("Ljava/lang/String;"));  // method signature string
  if (!is_static) {
    types.push_back(ir_method->decl->parent);  // "this" object
  }

  types.insert(types.end(), param_types.begin(),
               param_types.end());  // parameters

  // register where params start
  dex::u4 current_reg = ir_method->code->registers - ir_method->code->ins_count;
  // reuse not needed anymore register to store indexes
  dex::u4 array_index_reg = array_size_reg;
  int i = 0;
  for (auto type : types) {
    dex::u4 src_reg = 0;
    if (i == 0) {  // method signature string
      // e.g. const-string v2, "(I[Ljava/lang/String;)Ljava/lang/String;"
      // for (int, String[]) -> String
      auto const_str_op = code_ir->Alloc<lir::Bytecode>();
      const_str_op->opcode = dex::OP_CONST_STRING;
      const_str_op->operands.push_back(
          code_ir->Alloc<lir::VReg>(value_reg));  // dst
      auto method_label =
          builder.GetAsciiString(MethodLabel(ir_method).c_str());
      const_str_op->operands.push_back(code_ir->Alloc<lir::String>(
          method_label, method_label->orig_index));  // src
      code_ir->instructions.InsertBefore(bytecode, const_str_op);
      src_reg = value_reg;
    } else if (type->GetCategory() != ir::Type::Category::Reference) {
      BoxValue(bytecode, code_ir, type, current_reg, value_reg);
      src_reg = value_reg;
      current_reg +=
          1 + (type->GetCategory() == ir::Type::Category::WideScalar);
    } else {
      src_reg = current_reg;
      current_reg++;
    }

    auto index_const_op = code_ir->Alloc<lir::Bytecode>();
    index_const_op->opcode = dex::OP_CONST;
    index_const_op->operands.push_back(
        code_ir->Alloc<lir::VReg>(array_index_reg));
    index_const_op->operands.push_back(code_ir->Alloc<lir::Const32>(i++));
    code_ir->instructions.InsertBefore(bytecode, index_const_op);

    auto aput_op = code_ir->Alloc<lir::Bytecode>();
    aput_op->opcode = dex::OP_APUT_OBJECT;
    aput_op->operands.push_back(code_ir->Alloc<lir::VReg>(src_reg));
    aput_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_reg));
    aput_op->operands.push_back(code_ir->Alloc<lir::VReg>(array_index_reg));
    code_ir->instructions.InsertBefore(bytecode, aput_op);

    // if function is static, then jumping over index 1
    //  since null should be be passed in this case
    if (i == 1 && is_static) i++;
  }

  std::vector<ir::Type*> hook_param_types;
  hook_param_types.push_back(obj_array_type);

  auto ir_proto = builder.GetProto(builder.GetType("V"),
                                   builder.GetTypeList(hook_param_types));

  auto ir_method_decl = builder.GetMethodDecl(
      builder.GetAsciiString(hook_method_id_.method_name), ir_proto,
      builder.GetType(hook_method_id_.class_descriptor));

  auto hook_method =
      code_ir->Alloc<lir::Method>(ir_method_decl, ir_method_decl->orig_index);
  auto args = code_ir->Alloc<lir::VRegRange>(array_reg, 1);
  auto hook_invoke = code_ir->Alloc<lir::Bytecode>();
  hook_invoke->opcode = dex::OP_INVOKE_STATIC_RANGE;
  hook_invoke->operands.push_back(args);
  hook_invoke->operands.push_back(hook_method);
  code_ir->instructions.InsertBefore(bytecode, hook_invoke);

  // clean up registries used by us
  // registers are assigned to a marker value 0xFE_FE_FE_FE (decimal
  // value: -16843010) to help identify use of uninitialized registers.
  for (dex::u2 i = 0; i < regs_count; ++i) {
    auto cleanup = code_ir->Alloc<lir::Bytecode>();
    cleanup->opcode = dex::OP_CONST;
    cleanup->operands.push_back(code_ir->Alloc<lir::VReg>(i));
    cleanup->operands.push_back(code_ir->Alloc<lir::Const32>(0xFEFEFEFE));
    code_ir->instructions.InsertBefore(bytecode, cleanup);
  }

  // now we have to shift params to their original registers
  if (needsExtraRegs) {
    GenerateShiftParamsCode(code_ir, bytecode, regs_count - non_param_regs);
  }
  return true;
}

}  // namespace androidx_inspection
