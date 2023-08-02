#include "hidden_api_silencer.h"

#include <cstring>
#include <string>

namespace androidx_inspection {

HiddenApiSilencer::HiddenApiSilencer(jvmtiEnv* jvmti) : jvmti_(jvmti) {
  supported_ = Setup();
  if (!supported_) {
    return;
  }

  GetHiddenApiEnforcementPolicy(jvmti_, &policy_);
  DisableHiddenApiEnforcementPolicy(jvmti_);
}

HiddenApiSilencer::~HiddenApiSilencer() {
  if (!supported_) {
    return;
  }

  SetHiddenApiEnforcementPolicy(jvmti_, policy_);
}

void HiddenApiSilencer::Free(void* obj) {
  jvmti_->Deallocate((unsigned char*)obj);
}

bool HiddenApiSilencer::Setup() {
  jint count = 0;
  jvmtiExtensionFunctionInfo* extensions = NULL;

  if (jvmti_->GetExtensionFunctions(&count, &extensions) != JVMTI_ERROR_NONE) {
    return false;
  }

  /* Find the JVMTI extension event we want */
  jvmtiExtensionFunctionInfo* extension = extensions;
  for (jint i = 0; i < count; i++, extension++) {
    if (strcmp("com.android.art.misc.get_hidden_api_enforcement_policy",
               extension->id) == 0) {
      GetHiddenApiEnforcementPolicy = extension->func;
    } else if (strcmp("com.android.art.misc.set_hidden_api_enforcement_policy",
                      extension->id) == 0) {
      SetHiddenApiEnforcementPolicy = extension->func;
    } else if (strcmp(
                   "com.android.art.misc.disable_hidden_api_enforcement_policy",
                   extension->id) == 0) {
      DisableHiddenApiEnforcementPolicy = extension->func;
    }
  }

  // Clean up
  extension = extensions;
  for (jint i = 0; i < count; i++, extension++) {
    for (auto j = 0; j < extension->param_count; j++) {
      Free(extension->params[j].name);
    }
    Free(extension->short_description);
    Free(extension->errors);
    Free(extension->id);
    Free(extension->params);
  }
  Free(extensions);

  return SetHiddenApiEnforcementPolicy && GetHiddenApiEnforcementPolicy &&
         DisableHiddenApiEnforcementPolicy;
}

}  // namespace androidx_inspection
