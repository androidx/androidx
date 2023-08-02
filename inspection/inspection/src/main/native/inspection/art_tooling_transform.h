/*
 * Copyright (C) 2022 The Android Open Source Project
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
#ifndef ART_TOOLING_TRANSFORM_H
#define ART_TOOLING_TRANSFORM_H

#include "array_params_entry_hook.h"
#include "slicer/dex_ir.h"
#include "slicer/dex_ir_builder.h"
#include "slicer/instrumentation.h"
#include "logger.h"

namespace androidx_inspection {

    class ArtToolingTransform {
    public:
        ArtToolingTransform(const char* class_name) : class_name_(class_name){};

        void AddTransform(const char* class_name, const char* method_name,
                          const char* signature, bool isEntry) {
            transforms.push_back(
                    TransformDescription(class_name, method_name, signature, isEntry));
        }

        void Apply(std::shared_ptr<ir::DexFile> dex_ir) {
            for (auto transform : transforms) {
                slicer::MethodInstrumenter mi(dex_ir);
                if (transform.isEntry()) {
                    mi.AddTransformation<ArrayParamsEntryHook>(ir::MethodId(
                            "Landroidx/inspection/ArtToolingImpl;",
                            "onEntry"));
                } else {
                    auto tweak = transform.HasPrimitiveOrVoidReturnType()
                                 ? slicer::ExitHook::Tweak::None
                                 : slicer::ExitHook::Tweak::ReturnAsObject;
                    tweak = tweak | slicer::ExitHook::Tweak::PassMethodSignature;
                    mi.AddTransformation<slicer::ExitHook>(
                            ir::MethodId(
                                    "Landroidx/inspection/ArtToolingImpl;",
                                    "onExit"),
                            tweak);
                }

                if (!mi.InstrumentMethod(ir::MethodId(transform.GetClassName(),
                                                      transform.GetMethod(),
                                                      transform.GetSignature()))) {
                    LOGE("Error enter instrumenting %s\n", GetClassName());
                }
            }
        }

        const char* GetClassName() { return class_name_.c_str(); }

    private:
        class TransformDescription {
        public:
            TransformDescription(const char* class_name, const char* method_name,
                                 const char* signature, bool isEntry)
                    : class_name_(class_name),
                      method_name_(method_name),
                      signature_(signature),
                      isEntry_(isEntry) {}

            const char* GetClassName() { return class_name_.c_str(); }

            const char* GetMethod() { return method_name_.c_str(); }

            const char* GetSignature() { return signature_.c_str(); }

            bool HasPrimitiveOrVoidReturnType() { return signature_.back() != ';'; }

            bool isEntry() { return isEntry_; }

        private:
            std::string class_name_;
            std::string method_name_;
            std::string signature_;
            bool isEntry_;
        };

        std::string class_name_;
        std::list<TransformDescription> transforms;
    };

}  // namespace androidx_inspection

#endif  // ART_TOOLING_TRANSFORM_H
