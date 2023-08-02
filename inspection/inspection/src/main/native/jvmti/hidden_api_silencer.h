#ifndef HIDDEN_API_SILENCER_H
#define HIDDEN_API_SILENCER_H

#include <jvmti.h>

namespace androidx_inspection {

class HiddenApiSilencer {
 public:
  explicit HiddenApiSilencer(jvmtiEnv* jvmti);
  ~HiddenApiSilencer();

 private:
  jint policy_ = 0;
  jvmtiEnv* jvmti_ = nullptr;
  bool supported_ = false;

  bool Setup();

  jvmtiExtensionFunction DisableHiddenApiEnforcementPolicy = nullptr;
  jvmtiExtensionFunction GetHiddenApiEnforcementPolicy = nullptr;
  jvmtiExtensionFunction SetHiddenApiEnforcementPolicy = nullptr;

  void Free(void* obj);
};

}  // namespace androidx_inspection

#endif  // HIDDEN_API_SILENCER_H
