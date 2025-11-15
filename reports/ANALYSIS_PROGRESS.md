# AndroidX Library Analysis Progress

**Repository:** AndroidX (Jetpack Libraries)
**Total Libraries:** 83 (Filtered: Kotlin Multiplatform only)
**Last Updated:** 2025-11-15

## Status Legend

- `TODO` – biblioteka nie została jeszcze przeanalizowana
- `IN_PROGRESS` – aktualnie analizowana przez agenta
- `DONE` – analiza zakończona, raport znajduje się w `reports/`
- `SKIP` – świadomie pominięta (wyjaśnienie w kolumnie `notes`)

## Filter Applied

**Current Filter:** Kotlin Multiplatform modules only

This list contains only libraries that use Kotlin Multiplatform (KMP), enabling cross-platform code sharing across Android, iOS, Desktop, and Web.

## Quick Stats

- **Module Type:** kotlin-multiplatform (all 83 modules)

- **Special Categories:**
  - Compose-related: 37
  - Sample modules: 0
  - Test modules: 12

- **Domains (20 total):**
  - compose: 30 modules
  - lifecycle: 9 modules
  - datastore: 5 modules
  - room3: 5 modules
  - navigation: 4 modules
  - collection: 3 modules
  - navigationevent: 3 modules
  - paging: 3 modules
  - savedstate: 3 modules
  - testutils: 3 modules
  - tracing: 3 modules
  - annotation: 2 modules
  - navigation3: 2 modules
  - sqlite: 2 modules
  - benchmark: 1 modules
  - development: 1 modules
  - graphics: 1 modules
  - ink: 1 modules
  - kruth: 1 modules
  - window: 1 modules

## Library Inventory

| id | library_name | path | type | status | assigned_to | report_file | notes |
|----|--------------|------|------|--------|-------------|-------------|-------|
| 1 | annotation | annotation/annotation | kotlin-multiplatform | TODO |  | reports/annotation.md | Domain: annotation |
| 2 | annotation-keep | annotation/annotation-keep | kotlin-multiplatform | TODO |  | reports/annotation-keep.md | Domain: annotation |
| 3 | benchmark-traceprocessor | benchmark/benchmark-traceprocessor | kotlin-multiplatform | TODO |  | reports/benchmark-traceprocessor.md | Test, Domain: benchmark |
| 4 | collection | collection/collection | kotlin-multiplatform | TODO |  | reports/collection.md | Domain: collection |
| 5 | collection-benchmark | collection/collection-benchmark | kotlin-multiplatform | TODO |  | reports/collection-benchmark.md | Test, Domain: collection |
| 6 | collection-benchmark-kmp | collection/collection-benchmark-kmp | kotlin-multiplatform | TODO |  | reports/collection-benchmark-kmp.md | Test, Domain: collection |
| 7 | animation | compose/animation/animation | kotlin-multiplatform | TODO |  | reports/animation.md | Compose, Domain: compose |
| 8 | animation-core | compose/animation/animation-core | kotlin-multiplatform | TODO |  | reports/animation-core.md | Compose, Domain: compose |
| 9 | animation-graphics | compose/animation/animation-graphics | kotlin-multiplatform | TODO |  | reports/animation-graphics.md | Compose, Domain: compose |
| 10 | foundation | compose/foundation/foundation | kotlin-multiplatform | TODO |  | reports/foundation.md | Compose, Domain: compose |
| 11 | foundation-layout | compose/foundation/foundation-layout | kotlin-multiplatform | TODO |  | reports/foundation-layout.md | Compose, Domain: compose |
| 12 | material | compose/material/material | kotlin-multiplatform | TODO |  | reports/material.md | Compose, Domain: compose |
| 13 | material-ripple | compose/material/material-ripple | kotlin-multiplatform | TODO |  | reports/material-ripple.md | Compose, Domain: compose |
| 14 | adaptive | compose/material3/adaptive/adaptive | kotlin-multiplatform | TODO |  | reports/adaptive.md | Compose, Domain: compose |
| 15 | adaptive-layout | compose/material3/adaptive/adaptive-layout | kotlin-multiplatform | TODO |  | reports/adaptive-layout.md | Compose, Domain: compose |
| 16 | adaptive-navigation | compose/material3/adaptive/adaptive-navigation | kotlin-multiplatform | TODO |  | reports/adaptive-navigation.md | Compose, Domain: compose |
| 17 | adaptive-navigation3 | compose/material3/adaptive/adaptive-navigation3 | kotlin-multiplatform | TODO |  | reports/adaptive-navigation3.md | Compose, Domain: compose |
| 18 | material3 | compose/material3/material3 | kotlin-multiplatform | TODO |  | reports/material3.md | Compose, Domain: compose |
| 19 | material3-adaptive-navigation-suite | compose/material3/material3-adaptive-navigation-suite | kotlin-multiplatform | TODO |  | reports/material3-adaptive-navigation-suite.md | Compose, Domain: compose |
| 20 | material3-window-size-class | compose/material3/material3-window-size-class | kotlin-multiplatform | TODO |  | reports/material3-window-size-class.md | Compose, Domain: compose |
| 21 | runtime | compose/runtime/runtime | kotlin-multiplatform | TODO |  | reports/runtime.md | Compose, Domain: compose |
| 22 | runtime-annotation | compose/runtime/runtime-annotation | kotlin-multiplatform | TODO |  | reports/runtime-annotation.md | Compose, Domain: compose |
| 23 | runtime-retain | compose/runtime/runtime-retain | kotlin-multiplatform | TODO |  | reports/runtime-retain.md | Compose, Domain: compose |
| 24 | runtime-rxjava2 | compose/runtime/runtime-rxjava2 | kotlin-multiplatform | TODO |  | reports/runtime-rxjava2.md | Compose, Domain: compose |
| 25 | runtime-rxjava3 | compose/runtime/runtime-rxjava3 | kotlin-multiplatform | TODO |  | reports/runtime-rxjava3.md | Compose, Domain: compose |
| 26 | runtime-saveable | compose/runtime/runtime-saveable | kotlin-multiplatform | TODO |  | reports/runtime-saveable.md | Compose, Domain: compose |
| 27 | runtime-test-utils | compose/runtime/runtime-test-utils | kotlin-multiplatform | TODO |  | reports/runtime-test-utils.md | Compose, Test, Domain: compose |
| 28 | test-utils | compose/test-utils | kotlin-multiplatform | TODO |  | reports/test-utils.md | Compose, Test, Domain: compose |
| 29 | ui | compose/ui/ui | kotlin-multiplatform | TODO |  | reports/ui.md | Compose, Domain: compose |
| 30 | ui-geometry | compose/ui/ui-geometry | kotlin-multiplatform | TODO |  | reports/ui-geometry.md | Compose, Domain: compose |
| 31 | ui-graphics | compose/ui/ui-graphics | kotlin-multiplatform | TODO |  | reports/ui-graphics.md | Compose, Domain: compose |
| 32 | ui-test | compose/ui/ui-test | kotlin-multiplatform | TODO |  | reports/ui-test.md | Compose, Domain: compose |
| 33 | ui-text | compose/ui/ui-text | kotlin-multiplatform | TODO |  | reports/ui-text.md | Compose, Domain: compose |
| 34 | ui-tooling-preview | compose/ui/ui-tooling-preview | kotlin-multiplatform | TODO |  | reports/ui-tooling-preview.md | Compose, Domain: compose |
| 35 | ui-unit | compose/ui/ui-unit | kotlin-multiplatform | TODO |  | reports/ui-unit.md | Compose, Domain: compose |
| 36 | ui-util | compose/ui/ui-util | kotlin-multiplatform | TODO |  | reports/ui-util.md | Compose, Domain: compose |
| 37 | datastore | datastore/datastore | kotlin-multiplatform | TODO |  | reports/datastore.md | Domain: datastore |
| 38 | datastore-core | datastore/datastore-core | kotlin-multiplatform | TODO |  | reports/datastore-core.md | Domain: datastore |
| 39 | datastore-core-okio | datastore/datastore-core-okio | kotlin-multiplatform | TODO |  | reports/datastore-core-okio.md | Domain: datastore |
| 40 | datastore-preferences | datastore/datastore-preferences | kotlin-multiplatform | TODO |  | reports/datastore-preferences.md | Domain: datastore |
| 41 | datastore-preferences-core | datastore/datastore-preferences-core | kotlin-multiplatform | TODO |  | reports/datastore-preferences-core.md | Domain: datastore |
| 42 | artifactId | development/project-creator/compose-template/groupId/artifactId | kotlin-multiplatform | TODO |  | reports/artifactId.md | Compose, Domain: development |
| 43 | graphics-shapes | graphics/graphics-shapes | kotlin-multiplatform | TODO |  | reports/graphics-shapes.md | Domain: graphics |
| 44 | ink-nativeloader | ink/ink-nativeloader | kotlin-multiplatform | TODO |  | reports/ink-nativeloader.md | Domain: ink |
| 45 | kruth | kruth/kruth | kotlin-multiplatform | TODO |  | reports/kruth.md | Domain: kruth |
| 46 | lifecycle-common | lifecycle/lifecycle-common | kotlin-multiplatform | TODO |  | reports/lifecycle-common.md | Domain: lifecycle |
| 47 | lifecycle-runtime | lifecycle/lifecycle-runtime | kotlin-multiplatform | TODO |  | reports/lifecycle-runtime.md | Domain: lifecycle |
| 48 | lifecycle-runtime-compose | lifecycle/lifecycle-runtime-compose | kotlin-multiplatform | TODO |  | reports/lifecycle-runtime-compose.md | Compose, Domain: lifecycle |
| 49 | lifecycle-runtime-testing | lifecycle/lifecycle-runtime-testing | kotlin-multiplatform | TODO |  | reports/lifecycle-runtime-testing.md | Test, Domain: lifecycle |
| 50 | lifecycle-viewmodel | lifecycle/lifecycle-viewmodel | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel.md | Domain: lifecycle |
| 51 | lifecycle-viewmodel-compose | lifecycle/lifecycle-viewmodel-compose | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-compose.md | Compose, Domain: lifecycle |
| 52 | lifecycle-viewmodel-navigation3 | lifecycle/lifecycle-viewmodel-navigation3 | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-navigation3.md | Domain: lifecycle |
| 53 | lifecycle-viewmodel-savedstate | lifecycle/lifecycle-viewmodel-savedstate | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-savedstate.md | Domain: lifecycle |
| 54 | lifecycle-viewmodel-testing | lifecycle/lifecycle-viewmodel-testing | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-testing.md | Test, Domain: lifecycle |
| 55 | navigation-common | navigation/navigation-common | kotlin-multiplatform | TODO |  | reports/navigation-common.md | Domain: navigation |
| 56 | navigation-compose | navigation/navigation-compose | kotlin-multiplatform | TODO |  | reports/navigation-compose.md | Compose, Domain: navigation |
| 57 | navigation-runtime | navigation/navigation-runtime | kotlin-multiplatform | TODO |  | reports/navigation-runtime.md | Domain: navigation |
| 58 | navigation-testing | navigation/navigation-testing | kotlin-multiplatform | TODO |  | reports/navigation-testing.md | Test, Domain: navigation |
| 59 | navigation3-runtime | navigation3/navigation3-runtime | kotlin-multiplatform | TODO |  | reports/navigation3-runtime.md | Domain: navigation3 |
| 60 | navigation3-ui | navigation3/navigation3-ui | kotlin-multiplatform | TODO |  | reports/navigation3-ui.md | Domain: navigation3 |
| 61 | navigationevent | navigationevent/navigationevent | kotlin-multiplatform | TODO |  | reports/navigationevent.md | Domain: navigationevent |
| 62 | navigationevent-compose | navigationevent/navigationevent-compose | kotlin-multiplatform | TODO |  | reports/navigationevent-compose.md | Compose, Domain: navigationevent |
| 63 | navigationevent-testing | navigationevent/navigationevent-testing | kotlin-multiplatform | TODO |  | reports/navigationevent-testing.md | Test, Domain: navigationevent |
| 64 | paging-common | paging/paging-common | kotlin-multiplatform | TODO |  | reports/paging-common.md | Domain: paging |
| 65 | paging-compose | paging/paging-compose | kotlin-multiplatform | TODO |  | reports/paging-compose.md | Compose, Domain: paging |
| 66 | paging-testing | paging/paging-testing | kotlin-multiplatform | TODO |  | reports/paging-testing.md | Test, Domain: paging |
| 67 | room3-common | room3/room3-common | kotlin-multiplatform | TODO |  | reports/room3-common.md | Domain: room3 |
| 68 | room3-migration | room3/room3-migration | kotlin-multiplatform | TODO |  | reports/room3-migration.md | Domain: room3 |
| 69 | room3-paging | room3/room3-paging | kotlin-multiplatform | TODO |  | reports/room3-paging.md | Domain: room3 |
| 70 | room3-runtime | room3/room3-runtime | kotlin-multiplatform | TODO |  | reports/room3-runtime.md | Domain: room3 |
| 71 | room3-testing | room3/room3-testing | kotlin-multiplatform | TODO |  | reports/room3-testing.md | Test, Domain: room3 |
| 72 | savedstate | savedstate/savedstate | kotlin-multiplatform | TODO |  | reports/savedstate.md | Domain: savedstate |
| 73 | savedstate-compose | savedstate/savedstate-compose | kotlin-multiplatform | TODO |  | reports/savedstate-compose.md | Compose, Domain: savedstate |
| 74 | savedstate-testing | savedstate/savedstate-testing | kotlin-multiplatform | TODO |  | reports/savedstate-testing.md | Test, Domain: savedstate |
| 75 | sqlite | sqlite/sqlite | kotlin-multiplatform | TODO |  | reports/sqlite.md | Domain: sqlite |
| 76 | sqlite-bundled | sqlite/sqlite-bundled | kotlin-multiplatform | TODO |  | reports/sqlite-bundled.md | Domain: sqlite |
| 77 | testutils-ktx | testutils/testutils-ktx | kotlin-multiplatform | TODO |  | reports/testutils-ktx.md | Domain: testutils |
| 78 | testutils-lifecycle | testutils/testutils-lifecycle | kotlin-multiplatform | TODO |  | reports/testutils-lifecycle.md | Domain: testutils |
| 79 | testutils-paging | testutils/testutils-paging | kotlin-multiplatform | TODO |  | reports/testutils-paging.md | Domain: testutils |
| 80 | tracing | tracing/tracing | kotlin-multiplatform | TODO |  | reports/tracing.md | Domain: tracing |
| 81 | tracing-driver | tracing/tracing-driver | kotlin-multiplatform | TODO |  | reports/tracing-driver.md | Domain: tracing |
| 82 | tracing-driver-wire | tracing/tracing-driver-wire | kotlin-multiplatform | TODO |  | reports/tracing-driver-wire.md | Domain: tracing |
| 83 | window-core | window/window-core | kotlin-multiplatform | TODO |  | reports/window-core.md | Domain: window |