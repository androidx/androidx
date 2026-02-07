PLEASE IMPLEMENT THIS PLAN:
**Title: Navigation3 Predictive Back Commit Handoff Fix (Core) + Wear OS Direction Summary**

**Summary**
- Track 1 (Bug): `predictivePopTransitionSpec` → `popTransitionSpec`의 “commit handoff”가 매끄럽게 일어나지 않는 문제는 **Navigation3 core(`NavDisplay`)의 predictive 종료 처리 로직**에서 발생할 가능성이 높습니다. 현재는 predictive 종료 시점에 “seek/snap로 마무리”하는 경로가 있어 **popTransitionSpec이 시작될 기회를 차단**합니다.
- Track 2 (방향성 요약): Wear OS에서는 **`wear.compose.navigation3`(1.6.0-alpha07+) + `SwipeDismissableSceneStrategy`**가 공식 방향이며, **Navigation3의 transition hooks를 통해 커스텀 전환을 구현**하는 것이 권장 흐름입니다. 기존 `SwipeDismissableNavHost` 대비 **전환 커스터마이즈 지원이 강화**된 것이 핵심입니다. (요청 본문에 포함된 Google 답변 내용 기준)

**Public API Changes**
- 없음 (내부 로직 수정만).

**Implementation Plan (Core Fix Only)**
1. **Predictive 종료 상태 추적 추가**
   - `NavDisplay` 내부에 `wasInPredictiveBack` 상태를 `remember`로 유지하고, `wasInPredictiveBack && !inPredictiveBack` 시점을 “predictive 종료”로 판단합니다.
   - 종료 시점에 `isPop`(백스택 감소 여부)를 함께 평가해 **commit vs cancel**을 구분합니다.
   - 파일: `navigation3/navigation3-ui/src/commonMain/kotlin/androidx/navigation3/ui/NavDisplay.kt`

2. **Predictive 종료 시 “seek/snap 마무리” 경로를 commit에는 사용하지 않도록 분기**
   - 현재 `else { /* Predictive Back has either been completed or cancelled */ ... seekTo+snapTo }` 블록은 **popTransitionSpec을 적용할 기회를 막습니다.**
   - 수정 방향:
     - **commit(isPop=true)**: `transitionState.animateTo(scene)`를 반드시 수행하도록 분기하고, `seekTo+snapTo` 경로는 타지 않게 처리합니다. 이렇게 하면 `contentTransform` 분기가 `popTransitionSpec`을 선택할 수 있습니다.
     - **cancel(isPop=false)**: 기존 `seekTo+snapTo` 로직을 유지하거나, `transitionState.animateTo(scene)`로 원상복귀를 검토합니다(이 경우 `transitionSpec` 사용).
   - 목표: commit 시 **predictive 상태에서 이어져 popTransitionSpec이 마무리**되도록 경로를 보장.

3. **commit 시 popTransitionSpec 선택 보장**
   - `contentTransform` 선택 로직이 `isPop`을 기반으로 pop spec을 고르게 되어 있으므로, commit 분기에서 **`isPop`이 true로 유지되는지** 확인하고, 필요 시 `wasInPredictiveBack` 기반의 **보조 플래그**를 도입합니다.
   - 예: `val finishingPredictivePop = predictiveEnded && isPop` 등을 만들어 `contentTransform` 분기에 반영.

4. **보조 대응(필요 시)**
   - 만약 commit 직전 `transitionState.currentState`가 이미 `scene`으로 바뀌어 `animateTo`가 실행되지 않는 상황이 재현된다면, **예측 종료 직전 fraction을 캡처**하고, commit 시점에 그 fraction에서 `animateTo(scene)`가 수행되도록 보완 로직을 추가합니다.
   - 이 부분은 실제 재현/로깅으로 확인 후 적용.

**Test Cases / Scenarios**
- 요청에 따라 “기존 테스트만” 기준으로 **수동 검증 시나리오**를 제시합니다.
1. **Predictive Commit Handoff**
   - `predictivePopTransitionSpec`: `scaleOut(targetScale = 0.9f)`
   - `popTransitionSpec`: `scaleOut(targetScale = 0f)`
   - 예측 제스처 커밋 시 **0.9 → 0.0으로 부드럽게 이어지는지** 확인.
2. **Predictive Cancel**
   - 제스처 중 취소 시 원래 화면으로 **자연스럽게 복귀**하는지 확인.
3. **일반 Pop (non-predictive)**
   - 기존 pop 동작이 regression 없이 유지되는지 확인.
4. **Wear OS 경로**
   - `SwipeDismissableSceneStrategy`(API 36+) 경로에서 동일 증상 개선 여부 확인.

**Assumptions and Defaults**
- 공개 API 변경은 하지 않음.
- 수정 범위는 **Navigation3 core**(NavDisplay)로 제한.
- 새로운 자동 테스트는 추가하지 않음(수동 검증으로 충분하다고 가정).
- Wear OS의 공식 방향성은 요청 본문에 포함된 Google 답변 내용을 사실로 사용.

아래는 변경 내용을 **한 단계씩 자세히** 풀어쓴 설명입니다. 핵심 목적은 “Predictive Back 커밋 직후 `popTransitionSpec`이 실제로 이어지도록 보장”하는 것입니다.

---

**변경 파일**
- `navigation3/navigation3-ui/src/commonMain/kotlin/androidx/navigation3/ui/NavDisplay.kt`

---

## 1) 기존 동작 흐름과 문제점

### 기존 흐름 요약
1. Predictive Back 제스처 중(`inPredictiveBack == true`)에는  
   `transitionState.seekTo(progress, previousScene)`로 **진행률에 맞춰 시킹**합니다.
2. 제스처가 끝나면(`inPredictiveBack == false`), `LaunchedEffect(scene)`에서:
   - `transitionState.currentState != scene`이면 `animateTo(scene)`로 정상 전환.
   - 그렇지 않으면 **“predictive 완료/취소”로 간주하고**  
     `seekTo + snapTo` 기반의 마무리를 합니다.

### 문제 발생 지점
- Predictive가 **커밋된 경우**에도 `transitionState.currentState == scene` 상태가 될 수 있습니다.
- 이 경우 `animateTo(scene)`가 실행되지 않고,  
  **seek/snap 로직이 실행되어 popTransitionSpec이 타지 않습니다.**
- 그 결과:
  - predictive 단계에서 0.9까지 간 스케일이
  - pop 단계의 0.0으로 이어지지 않고
  - **바로 사라지는 느낌**이 발생합니다.

즉, **커밋 상황인데도 “predictive 마무리 로직”으로 처리되어 pop spec이 실행되지 않는 구조**였습니다.

---

## 2) 변경 사항의 핵심 아이디어

### 의도
- Predictive → Pop의 **handoff**가 반드시 일어나도록 만든다.
- 커밋일 때는 **무조건 `animateTo(scene)`를 타게 해서 popTransitionSpec이 적용**되게 한다.

### 구현 전략
1. **Predictive가 끝난 순간**을 탐지한다.
2. 그 순간이 **커밋인지(= pop)**를 판단한다.
3. 커밋이라면 **popTransitionSpec이 시작될 수 있게 `animateTo(scene)`로 강제한다.**

---

## 3) 실제 코드 변경 설명

### (A) Predictive 종료 시점 추적 추가

```kotlin
val wasInPredictiveBack = remember { mutableStateOf(false) }
val predictiveEnded = wasInPredictiveBack.value && !inPredictiveBack
val finishingPredictivePop = predictiveEnded && (isPop || transition.targetState == scene)

SideEffect { wasInPredictiveBack.value = inPredictiveBack }
```

**설명**
- `wasInPredictiveBack`는 **직전 프레임 상태**를 기억합니다.
- `predictiveEnded`는 “바로 직전엔 predictive였는데 지금은 아닌 상태”입니다.
- `finishingPredictivePop`은 predictive 종료가 **커밋(= pop)**인지 판단하기 위한 플래그입니다.
  - `isPop` 또는 `transition.targetState == scene`를 조건으로 추가해서  
    commit 판단이 흔들리는 순간에도 안전하게 pop으로 보게 했습니다.
- `SideEffect`는 매 프레임 `wasInPredictiveBack`을 갱신합니다.

---

### (B) Predictive 종료 로직 분기 수정

기존:
```kotlin
LaunchedEffect(scene) {
    if (transitionState.currentState != scene) {
        transitionState.animateTo(scene)
    } else {
        // predictive 완료/취소: seekTo + snapTo
    }
}
```

변경:
```kotlin
LaunchedEffect(scene, finishingPredictivePop) {
    if (transitionState.currentState != scene || finishingPredictivePop) {
        transitionState.animateTo(scene)
    } else {
        // predictive 완료/취소: seekTo + snapTo (기존 유지)
    }
}
```

**설명**
- `finishingPredictivePop == true`이면 **무조건 animateTo(scene)** 실행합니다.
- 즉, predictive 종료가 “커밋”인 경우 **popTransitionSpec이 반드시 구동되는 경로로 강제**합니다.
- cancel은 기존 seek/snap 로직을 유지해 원상복귀 자연스럽게 합니다.

---

### (C) Transition 선택 분기 강화

기존:
```kotlin
when {
    inPredictiveBack -> predictivePopTransitionSpec
    isPop -> popTransitionSpec
    else -> transitionSpec
}
```

변경:
```kotlin
when {
    inPredictiveBack -> predictivePopTransitionSpec
    finishingPredictivePop || isPop -> popTransitionSpec
    else -> transitionSpec
}
```

**설명**
- predictive 종료 직후에는 `isPop` 판정이 프레임 타이밍에 따라 흔들릴 수 있습니다.
- 그래서 `finishingPredictivePop`를 추가해 **handoff 순간에도 pop spec이 확실히 선택되도록** 했습니다.

---

## 4) 기대되는 효과

### 커밋 시
- predictive spec 진행 → 커밋 후 pop spec이 이어서 작동
- 예: scale 1.0 → 0.9 → 0.0 **부드럽게 마무리**

### 취소 시
- 기존 seek/snap 방식 유지
- 사용자가 제스처를 취소하면 **원상 복귀** (기존 동작 유지)

### 일반 pop (non predictive)
- 기존 pop 경로 그대로 (`isPop == true`)

---

## 5) 검증 포인트

1. Predictive commit handoff  
   - `predictivePopTransitionSpec: scaleOut(0.9f)`  
   - `popTransitionSpec: scaleOut(0.0f)`  
   - 커밋 후 0.9 → 0.0 이어지는지 확인
2. Predictive cancel  
   - 드래그 후 취소 시 자연스럽게 원래 화면으로 돌아오는지 확인
3. 일반 pop  
   - 기존 pop 동작에 regression 없는지 확인
4. Wear OS path  
   - `SwipeDismissableSceneStrategy` 사용 시 동일 증상 해결 확인

---

## 6) 추가 참고
- 이 수정은 **Navigation3 core에서만 진행**합니다.
- API 변경 없음.
- 테스트는 추가하지 않았습니다(요청 기준).

---

원하면 현재 변경이 어떤 경우에 `transitionState.currentState == scene`이 되는지
실제 타임라인 예시로 더 상세히 풀어줄 수도 있습니다.