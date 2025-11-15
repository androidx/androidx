# AndroidX Library Analysis Progress

**Repository:** AndroidX (Jetpack Libraries)
**Total Libraries:** 911
**Last Updated:** 2025-11-15

## Status Legend

- `TODO` – biblioteka nie została jeszcze przeanalizowana
- `IN_PROGRESS` – aktualnie analizowana przez agenta
- `DONE` – analiza zakończona, raport znajduje się w `reports/`
- `SKIP` – świadomie pominięta (wyjaśnienie w kolumnie `notes`)

## Quick Stats

- **Module Types:**
  - java: 615
  - has-src: 100
  - kotlin-multiplatform: 83
  - kotlin: 66
  - unknown: 47

- **Special Categories:**
  - Compose-related: 218
  - Sample modules: 95
  - Test modules: 286

- **Top 10 Domains:**
  - compose: 138 modules
  - wear: 65 modules
  - camera: 40 modules
  - core: 40 modules
  - xr: 37 modules
  - lifecycle: 35 modules
  - navigation: 33 modules
  - benchmark: 25 modules
  - glance: 23 modules
  - privacysandbox: 22 modules

## Library Inventory

| id | library_name | path | type | status | assigned_to | report_file | notes |
|----|--------------|------|------|--------|-------------|-------------|-------|
| 1 | androidx | /home/user/androidx | unknown | TODO |  | reports/androidx.md | Domain:  |
| 2 | activity | activity/activity | java | TODO |  | reports/activity.md | Domain: activity |
| 3 | activity-compose | activity/activity-compose | java | TODO |  | reports/activity-compose.md | Compose, Domain: activity |
| 4 | activity-compose-lint | activity/activity-compose-lint | java | TODO |  | reports/activity-compose-lint.md | Compose, Domain: activity |
| 5 | benchmark | activity/activity-compose/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: activity |
| 6 | activity-demos | activity/activity-compose/integration-tests/activity-demos | unknown | TODO |  | reports/activity-demos.md | Compose, Test, Domain: activity |
| 7 | samples | activity/activity-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: activity |
| 8 | activity-ktx | activity/activity-ktx | unknown | TODO |  | reports/activity-ktx.md | Domain: activity |
| 9 | activity-lint | activity/activity-lint | java | TODO |  | reports/activity-lint.md | Domain: activity |
| 10 | baselineprofile | activity/integration-tests/baselineprofile | java | TODO |  | reports/baselineprofile.md | Test, Domain: activity |
| 11 | macrobenchmark | activity/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Test, Domain: activity |
| 12 | macrobenchmark-target | activity/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Test, Domain: activity |
| 13 | testapp | activity/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: activity |
| 14 | androidx-settings-plugins | androidx-settings-plugins | unknown | TODO |  | reports/androidx-settings-plugins.md | Domain: androidx-settings-plugins |
| 15 | hostTestFailureHandlerPlugin | androidx-settings-plugins/hostTestFailureHandlerPlugin | has-src | TODO |  | reports/hostTestFailureHandlerPlugin.md | Domain: androidx-settings-plugins |
| 16 | annotation | annotation/annotation | kotlin-multiplatform | TODO |  | reports/annotation.md | Domain: annotation |
| 17 | annotation-experimental | annotation/annotation-experimental | java | TODO |  | reports/annotation-experimental.md | Domain: annotation |
| 18 | annotation-experimental-lint | annotation/annotation-experimental-lint | java | TODO |  | reports/annotation-experimental-lint.md | Domain: annotation |
| 19 | integration-tests | annotation/annotation-experimental-lint/integration-tests | java | TODO |  | reports/integration-tests.md | Test, Domain: annotation |
| 20 | annotation-keep | annotation/annotation-keep | kotlin-multiplatform | TODO |  | reports/annotation-keep.md | Domain: annotation |
| 21 | annotation-keep-gradle-plugin | annotation/annotation-keep-gradle-plugin | kotlin | TODO |  | reports/annotation-keep-gradle-plugin.md | Domain: annotation |
| 22 | annotation-sampled | annotation/annotation-sampled | java | TODO |  | reports/annotation-sampled.md | Domain: annotation |
| 23 | appcompat | appcompat/appcompat | java | TODO |  | reports/appcompat.md | Domain: appcompat |
| 24 | appcompat-benchmark | appcompat/appcompat-benchmark | has-src | TODO |  | reports/appcompat-benchmark.md | Test, Domain: appcompat |
| 25 | appcompat-lint | appcompat/appcompat-lint | kotlin | TODO |  | reports/appcompat-lint.md | Domain: appcompat |
| 26 | integration-tests | appcompat/appcompat-lint/integration-tests | java | TODO |  | reports/integration-tests.md | Test, Domain: appcompat |
| 27 | appcompat-resources | appcompat/appcompat-resources | java | TODO |  | reports/appcompat-resources.md | Domain: appcompat |
| 28 | receive-content-testapp | appcompat/integration-tests/receive-content-testapp | java | TODO |  | reports/receive-content-testapp.md | Test, Domain: appcompat |
| 29 | appfunctions | appfunctions/appfunctions | java | TODO |  | reports/appfunctions.md | Domain: appfunctions |
| 30 | appfunctions-compiler | appfunctions/appfunctions-compiler | java | TODO |  | reports/appfunctions-compiler.md | Domain: appfunctions |
| 31 | appfunctions-service | appfunctions/appfunctions-service | java | TODO |  | reports/appfunctions-service.md | Domain: appfunctions |
| 32 | appfunctions-stubs | appfunctions/appfunctions-stubs | java | TODO |  | reports/appfunctions-stubs.md | Domain: appfunctions |
| 33 | appfunctions-testing | appfunctions/appfunctions-testing | java | TODO |  | reports/appfunctions-testing.md | Test, Domain: appfunctions |
| 34 | app | appfunctions/integration-tests/multi-modules-testapp/app | has-src | TODO |  | reports/app.md | Test, Domain: appfunctions |
| 35 | shared-library | appfunctions/integration-tests/multi-modules-testapp/shared-library | java | TODO |  | reports/shared-library.md | Test, Domain: appfunctions |
| 36 | appsearch | appsearch/appsearch | java | TODO |  | reports/appsearch.md | Domain: appsearch |
| 37 | appsearch-builtin-types | appsearch/appsearch-builtin-types | java | TODO |  | reports/appsearch-builtin-types.md | Domain: appsearch |
| 38 | appsearch-debug-view | appsearch/appsearch-debug-view | java | TODO |  | reports/appsearch-debug-view.md | Domain: appsearch |
| 39 | samples | appsearch/appsearch-debug-view/samples | java | TODO |  | reports/samples.md | Sample, Domain: appsearch |
| 40 | appsearch-external-protobuf | appsearch/appsearch-external-protobuf | unknown | TODO |  | reports/appsearch-external-protobuf.md | Domain: appsearch |
| 41 | appsearch-ktx | appsearch/appsearch-ktx | has-src | TODO |  | reports/appsearch-ktx.md | Domain: appsearch |
| 42 | appsearch-local-storage | appsearch/appsearch-local-storage | java | TODO |  | reports/appsearch-local-storage.md | Domain: appsearch |
| 43 | appsearch-platform-storage | appsearch/appsearch-platform-storage | java | TODO |  | reports/appsearch-platform-storage.md | Domain: appsearch |
| 44 | appsearch-play-services-storage | appsearch/appsearch-play-services-storage | java | TODO |  | reports/appsearch-play-services-storage.md | Domain: appsearch |
| 45 | appsearch-test-util | appsearch/appsearch-test-util | java | TODO |  | reports/appsearch-test-util.md | Test, Domain: appsearch |
| 46 | compiler | appsearch/compiler | java | TODO |  | reports/compiler.md | Domain: appsearch |
| 47 | core-common | arch/core/core-common | java | TODO |  | reports/core-common.md | Domain: arch |
| 48 | core-runtime | arch/core/core-runtime | java | TODO |  | reports/core-runtime.md | Domain: arch |
| 49 | core-testing | arch/core/core-testing | java | TODO |  | reports/core-testing.md | Test, Domain: arch |
| 50 | asynclayoutinflater | asynclayoutinflater/asynclayoutinflater | java | TODO |  | reports/asynclayoutinflater.md | Domain: asynclayoutinflater |
| 51 | asynclayoutinflater-appcompat | asynclayoutinflater/asynclayoutinflater-appcompat | java | TODO |  | reports/asynclayoutinflater-appcompat.md | Domain: asynclayoutinflater |
| 52 | autofill | autofill/autofill | java | TODO |  | reports/autofill.md | Domain: autofill |
| 53 | baseline-profile-gradle-plugin | benchmark/baseline-profile-gradle-plugin | kotlin | TODO |  | reports/baseline-profile-gradle-plugin.md | Test, Domain: benchmark |
| 54 | dependency | benchmark/baseline-profile-gradle-plugin/src/test/test-data/dependency | java | TODO |  | reports/dependency.md | Test, Domain: benchmark |
| 55 | benchmark | benchmark/benchmark | has-src | TODO |  | reports/benchmark.md | Test, Domain: benchmark |
| 56 | benchmark-common | benchmark/benchmark-common | java | TODO |  | reports/benchmark-common.md | Test, Domain: benchmark |
| 57 | benchmark-darwin | benchmark/benchmark-darwin | has-src | TODO |  | reports/benchmark-darwin.md | Test, Domain: benchmark |
| 58 | benchmark-darwin-core | benchmark/benchmark-darwin-core | has-src | TODO |  | reports/benchmark-darwin-core.md | Test, Domain: benchmark |
| 59 | benchmark-darwin-gradle-plugin | benchmark/benchmark-darwin-gradle-plugin | kotlin | TODO |  | reports/benchmark-darwin-gradle-plugin.md | Test, Domain: benchmark |
| 60 | benchmark-darwin-samples | benchmark/benchmark-darwin-samples | has-src | TODO |  | reports/benchmark-darwin-samples.md | Sample, Test, Domain: benchmark |
| 61 | benchmark-junit4 | benchmark/benchmark-junit4 | java | TODO |  | reports/benchmark-junit4.md | Test, Domain: benchmark |
| 62 | benchmark-macro | benchmark/benchmark-macro | java | TODO |  | reports/benchmark-macro.md | Test, Domain: benchmark |
| 63 | benchmark-macro-junit4 | benchmark/benchmark-macro-junit4 | java | TODO |  | reports/benchmark-macro-junit4.md | Test, Domain: benchmark |
| 64 | benchmark-traceprocessor | benchmark/benchmark-traceprocessor | kotlin-multiplatform | TODO |  | reports/benchmark-traceprocessor.md | Test, Domain: benchmark |
| 65 | gradle-plugin | benchmark/gradle-plugin | kotlin | TODO |  | reports/gradle-plugin.md | Test, Domain: benchmark |
| 66 | baselineprofile-consumer | benchmark/integration-tests/baselineprofile-consumer | java | TODO |  | reports/baselineprofile-consumer.md | Test, Domain: benchmark |
| 67 | baselineprofile-flavors-consumer | benchmark/integration-tests/baselineprofile-flavors-consumer | java | TODO |  | reports/baselineprofile-flavors-consumer.md | Test, Domain: benchmark |
| 68 | baselineprofile-flavors-producer | benchmark/integration-tests/baselineprofile-flavors-producer | has-src | TODO |  | reports/baselineprofile-flavors-producer.md | Test, Domain: benchmark |
| 69 | baselineprofile-library-app-target | benchmark/integration-tests/baselineprofile-library-app-target | java | TODO |  | reports/baselineprofile-library-app-target.md | Test, Domain: benchmark |
| 70 | baselineprofile-library-consumer | benchmark/integration-tests/baselineprofile-library-consumer | java | TODO |  | reports/baselineprofile-library-consumer.md | Test, Domain: benchmark |
| 71 | baselineprofile-library-producer | benchmark/integration-tests/baselineprofile-library-producer | java | TODO |  | reports/baselineprofile-library-producer.md | Test, Domain: benchmark |
| 72 | baselineprofile-producer | benchmark/integration-tests/baselineprofile-producer | java | TODO |  | reports/baselineprofile-producer.md | Test, Domain: benchmark |
| 73 | dry-run-benchmark | benchmark/integration-tests/dry-run-benchmark | has-src | TODO |  | reports/dry-run-benchmark.md | Test, Domain: benchmark |
| 74 | macrobenchmark | benchmark/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Test, Domain: benchmark |
| 75 | macrobenchmark-target | benchmark/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Test, Domain: benchmark |
| 76 | startup-benchmark | benchmark/integration-tests/startup-benchmark | has-src | TODO |  | reports/startup-benchmark.md | Test, Domain: benchmark |
| 77 | samples | benchmark/samples | java | TODO |  | reports/samples.md | Sample, Test, Domain: benchmark |
| 78 | binarycompatibilityvalidator | binarycompatibilityvalidator/binarycompatibilityvalidator | has-src | TODO |  | reports/binarycompatibilityvalidator.md | Domain: binarycompatibilityvalidator |
| 79 | biometric | biometric/biometric | java | TODO |  | reports/biometric.md | Domain: biometric |
| 80 | biometric-compose | biometric/biometric-compose | java | TODO |  | reports/biometric-compose.md | Compose, Domain: biometric |
| 81 | samples | biometric/biometric-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: biometric |
| 82 | samples | biometric/biometric/samples | java | TODO |  | reports/samples.md | Sample, Domain: biometric |
| 83 | testapp | biometric/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: biometric |
| 84 | testapp-compose | biometric/integration-tests/testapp-compose | java | TODO |  | reports/testapp-compose.md | Compose, Test, Domain: biometric |
| 85 | browser | browser/browser | java | TODO |  | reports/browser.md | Domain: browser |
| 86 | buildSrc | buildSrc | has-src | TODO |  | reports/buildSrc.md | Domain: buildSrc |
| 87 | buildSrc-tests | buildSrc-tests | has-src | TODO |  | reports/buildSrc-tests.md | Test, Domain: buildSrc-tests |
| 88 | buildSrc-tests-max-dep-versions-dep | buildSrc-tests/max-dep-versions/buildSrc-tests-max-dep-versions-dep | java | TODO |  | reports/buildSrc-tests-max-dep-versions-dep.md | Test, Domain: buildSrc-tests |
| 89 | buildSrc-tests-max-dep-versions-main | buildSrc-tests/max-dep-versions/buildSrc-tests-max-dep-versions-main | java | TODO |  | reports/buildSrc-tests-max-dep-versions-main.md | Test, Domain: buildSrc-tests |
| 90 | baseline-profile-gradle-plugin | buildSrc/imports/baseline-profile-gradle-plugin | unknown | TODO |  | reports/baseline-profile-gradle-plugin.md | Domain: buildSrc |
| 91 | benchmark-darwin-plugin | buildSrc/imports/benchmark-darwin-plugin | unknown | TODO |  | reports/benchmark-darwin-plugin.md | Test, Domain: buildSrc |
| 92 | benchmark-gradle-plugin | buildSrc/imports/benchmark-gradle-plugin | unknown | TODO |  | reports/benchmark-gradle-plugin.md | Test, Domain: buildSrc |
| 93 | binary-compatibility-validator | buildSrc/imports/binary-compatibility-validator | unknown | TODO |  | reports/binary-compatibility-validator.md | Domain: buildSrc |
| 94 | glance-layout-generator | buildSrc/imports/glance-layout-generator | unknown | TODO |  | reports/glance-layout-generator.md | Domain: buildSrc |
| 95 | inspection-gradle-plugin | buildSrc/imports/inspection-gradle-plugin | unknown | TODO |  | reports/inspection-gradle-plugin.md | Domain: buildSrc |
| 96 | privacysandbox-gradle-plugin | buildSrc/imports/privacysandbox-gradle-plugin | unknown | TODO |  | reports/privacysandbox-gradle-plugin.md | Domain: buildSrc |
| 97 | room-gradle-plugin | buildSrc/imports/room-gradle-plugin | unknown | TODO |  | reports/room-gradle-plugin.md | Domain: buildSrc |
| 98 | stableaidl-gradle-plugin | buildSrc/imports/stableaidl-gradle-plugin | unknown | TODO |  | reports/stableaidl-gradle-plugin.md | Domain: buildSrc |
| 99 | jetpad-integration | buildSrc/jetpad-integration | java | TODO |  | reports/jetpad-integration.md | Domain: buildSrc |
| 100 | plugins | buildSrc/plugins | kotlin | TODO |  | reports/plugins.md | Domain: buildSrc |
| 101 | private | buildSrc/private | kotlin | TODO |  | reports/private.md | Domain: buildSrc |
| 102 | public | buildSrc/public | kotlin | TODO |  | reports/public.md | Domain: buildSrc |
| 103 | camera-camera2 | camera/camera-camera2 | java | TODO |  | reports/camera-camera2.md | Domain: camera |
| 104 | camera-camera2-pipe | camera/camera-camera2-pipe | java | TODO |  | reports/camera-camera2-pipe.md | Domain: camera |
| 105 | camera-camera2-pipe-integration | camera/camera-camera2-pipe-integration | java | TODO |  | reports/camera-camera2-pipe-integration.md | Domain: camera |
| 106 | camera-camera2-pipe-testing | camera/camera-camera2-pipe-testing | java | TODO |  | reports/camera-camera2-pipe-testing.md | Test, Domain: camera |
| 107 | camera-compose | camera/camera-compose | java | TODO |  | reports/camera-compose.md | Compose, Domain: camera |
| 108 | samples | camera/camera-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: camera |
| 109 | camera-core | camera/camera-core | java | TODO |  | reports/camera-core.md | Domain: camera |
| 110 | samples | camera/camera-core/samples | java | TODO |  | reports/samples.md | Sample, Domain: camera |
| 111 | camera-effects | camera/camera-effects | java | TODO |  | reports/camera-effects.md | Domain: camera |
| 112 | camera-extensions | camera/camera-extensions | java | TODO |  | reports/camera-extensions.md | Domain: camera |
| 113 | camera-extensions-stub | camera/camera-extensions-stub | java | TODO |  | reports/camera-extensions-stub.md | Domain: camera |
| 114 | camera-lifecycle | camera/camera-lifecycle | java | TODO |  | reports/camera-lifecycle.md | Domain: camera |
| 115 | samples | camera/camera-lifecycle/samples | java | TODO |  | reports/samples.md | Sample, Domain: camera |
| 116 | camera-mlkit-vision | camera/camera-mlkit-vision | java | TODO |  | reports/camera-mlkit-vision.md | Domain: camera |
| 117 | camera-testing | camera/camera-testing | java | TODO |  | reports/camera-testing.md | Test, Domain: camera |
| 118 | camera-testlib-extensions | camera/camera-testlib-extensions | java | TODO |  | reports/camera-testlib-extensions.md | Domain: camera |
| 119 | camera-video | camera/camera-video | java | TODO |  | reports/camera-video.md | Domain: camera |
| 120 | samples | camera/camera-video/samples | java | TODO |  | reports/samples.md | Sample, Domain: camera |
| 121 | camera-view | camera/camera-view | java | TODO |  | reports/camera-view.md | Domain: camera |
| 122 | featurecombinationquery | camera/featurecombinationquery/featurecombinationquery | java | TODO |  | reports/featurecombinationquery.md | Domain: camera |
| 123 | featurecombinationquery-play-services | camera/featurecombinationquery/featurecombinationquery-play-services | java | TODO |  | reports/featurecombinationquery-play-services.md | Domain: camera |
| 124 | avsynctestapp | camera/integration-tests/avsynctestapp | java | TODO |  | reports/avsynctestapp.md | Test, Domain: camera |
| 125 | camera-benchmark | camera/integration-tests/camera-benchmark | has-src | TODO |  | reports/camera-benchmark.md | Test, Domain: camera |
| 126 | camera-macrobenchmark | camera/integration-tests/camera-macrobenchmark | java | TODO |  | reports/camera-macrobenchmark.md | Test, Domain: camera |
| 127 | camera-macrobenchmark-target | camera/integration-tests/camera-macrobenchmark-target | java | TODO |  | reports/camera-macrobenchmark-target.md | Test, Domain: camera |
| 128 | camerapipetestapp | camera/integration-tests/camerapipetestapp | java | TODO |  | reports/camerapipetestapp.md | Test, Domain: camera |
| 129 | coretestapp | camera/integration-tests/coretestapp | java | TODO |  | reports/coretestapp.md | Test, Domain: camera |
| 130 | diagnosetestapp | camera/integration-tests/diagnosetestapp | java | TODO |  | reports/diagnosetestapp.md | Test, Domain: camera |
| 131 | extensionstestapp | camera/integration-tests/extensionstestapp | java | TODO |  | reports/extensionstestapp.md | Test, Domain: camera |
| 132 | featurecombotestapp | camera/integration-tests/featurecombotestapp | java | TODO |  | reports/featurecombotestapp.md | Test, Domain: camera |
| 133 | testingtestapp | camera/integration-tests/testingtestapp | java | TODO |  | reports/testingtestapp.md | Test, Domain: camera |
| 134 | timingtestapp | camera/integration-tests/timingtestapp | java | TODO |  | reports/timingtestapp.md | Test, Domain: camera |
| 135 | uiwidgetstestapp | camera/integration-tests/uiwidgetstestapp | java | TODO |  | reports/uiwidgetstestapp.md | Test, Domain: camera |
| 136 | viewfindertestapp | camera/integration-tests/viewfindertestapp | java | TODO |  | reports/viewfindertestapp.md | Test, Domain: camera |
| 137 | viewtestapp | camera/integration-tests/viewtestapp | java | TODO |  | reports/viewtestapp.md | Test, Domain: camera |
| 138 | media3-effect | camera/media3/media3-effect | java | TODO |  | reports/media3-effect.md | Domain: camera |
| 139 | viewfinder-compose | camera/viewfinder/viewfinder-compose | java | TODO |  | reports/viewfinder-compose.md | Compose, Domain: camera |
| 140 | viewfinder-core | camera/viewfinder/viewfinder-core | java | TODO |  | reports/viewfinder-core.md | Domain: camera |
| 141 | samples | camera/viewfinder/viewfinder-core/samples | java | TODO |  | reports/samples.md | Sample, Domain: camera |
| 142 | viewfinder-view | camera/viewfinder/viewfinder-view | java | TODO |  | reports/viewfinder-view.md | Domain: camera |
| 143 | app | car/app/app | java | TODO |  | reports/app.md | Domain: car |
| 144 | app-automotive | car/app/app-automotive | java | TODO |  | reports/app-automotive.md | Domain: car |
| 145 | app-projected | car/app/app-projected | java | TODO |  | reports/app-projected.md | Domain: car |
| 146 | automotive | car/app/app-samples/navigation/automotive | has-src | TODO |  | reports/automotive.md | Sample, Domain: car |
| 147 | common | car/app/app-samples/navigation/common | java | TODO |  | reports/common.md | Sample, Domain: car |
| 148 | mobile | car/app/app-samples/navigation/mobile | has-src | TODO |  | reports/mobile.md | Sample, Domain: car |
| 149 | automotive | car/app/app-samples/showcase/automotive | java | TODO |  | reports/automotive.md | Sample, Domain: car |
| 150 | common | car/app/app-samples/showcase/common | java | TODO |  | reports/common.md | Sample, Domain: car |
| 151 | mobile | car/app/app-samples/showcase/mobile | has-src | TODO |  | reports/mobile.md | Sample, Domain: car |
| 152 | app-testing | car/app/app-testing | java | TODO |  | reports/app-testing.md | Test, Domain: car |
| 153 | cardview | cardview/cardview | java | TODO |  | reports/cardview.md | Domain: cardview |
| 154 | collection | collection/collection | kotlin-multiplatform | TODO |  | reports/collection.md | Domain: collection |
| 155 | collection-benchmark | collection/collection-benchmark | kotlin-multiplatform | TODO |  | reports/collection-benchmark.md | Test, Domain: collection |
| 156 | collection-benchmark-android | collection/collection-benchmark-android | has-src | TODO |  | reports/collection-benchmark-android.md | Test, Domain: collection |
| 157 | collection-benchmark-kmp | collection/collection-benchmark-kmp | kotlin-multiplatform | TODO |  | reports/collection-benchmark-kmp.md | Test, Domain: collection |
| 158 | collection-ktx | collection/collection-ktx | unknown | TODO |  | reports/collection-ktx.md | Domain: collection |
| 159 | testapp | collection/integration-tests/testapp | kotlin | TODO |  | reports/testapp.md | Test, Domain: collection |
| 160 | animation | compose/animation/animation | kotlin-multiplatform | TODO |  | reports/animation.md | Compose, Domain: compose |
| 161 | animation-core | compose/animation/animation-core | kotlin-multiplatform | TODO |  | reports/animation-core.md | Compose, Domain: compose |
| 162 | animation-core-lint | compose/animation/animation-core-lint | java | TODO |  | reports/animation-core-lint.md | Compose, Domain: compose |
| 163 | benchmark | compose/animation/animation-core/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 164 | samples | compose/animation/animation-core/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 165 | animation-graphics | compose/animation/animation-graphics | kotlin-multiplatform | TODO |  | reports/animation-graphics.md | Compose, Domain: compose |
| 166 | samples | compose/animation/animation-graphics/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 167 | animation-lint | compose/animation/animation-lint | java | TODO |  | reports/animation-lint.md | Compose, Domain: compose |
| 168 | animation-tooling-internal | compose/animation/animation-tooling-internal | java | TODO |  | reports/animation-tooling-internal.md | Compose, Domain: compose |
| 169 | animation-demos | compose/animation/animation/integration-tests/animation-demos | java | TODO |  | reports/animation-demos.md | Compose, Test, Domain: compose |
| 170 | samples | compose/animation/animation/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 171 | benchmark-utils | compose/benchmark-utils | java | TODO |  | reports/benchmark-utils.md | Compose, Test, Domain: compose |
| 172 | benchmark | compose/benchmark-utils/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 173 | foundation | compose/foundation/foundation | kotlin-multiplatform | TODO |  | reports/foundation.md | Compose, Domain: compose |
| 174 | foundation-layout | compose/foundation/foundation-layout | kotlin-multiplatform | TODO |  | reports/foundation-layout.md | Compose, Domain: compose |
| 175 | benchmark | compose/foundation/foundation-layout/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 176 | layout-demos | compose/foundation/foundation-layout/integration-tests/layout-demos | java | TODO |  | reports/layout-demos.md | Compose, Test, Domain: compose |
| 177 | samples | compose/foundation/foundation-layout/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 178 | foundation-lint | compose/foundation/foundation-lint | java | TODO |  | reports/foundation-lint.md | Compose, Domain: compose |
| 179 | benchmark | compose/foundation/foundation/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 180 | foundation-demos | compose/foundation/foundation/integration-tests/foundation-demos | java | TODO |  | reports/foundation-demos.md | Compose, Test, Domain: compose |
| 181 | lazy-tests | compose/foundation/foundation/integration-tests/lazy-tests | has-src | TODO |  | reports/lazy-tests.md | Compose, Test, Domain: compose |
| 182 | samples | compose/foundation/foundation/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 183 | demos | compose/integration-tests/demos | java | TODO |  | reports/demos.md | Compose, Test, Domain: compose |
| 184 | common | compose/integration-tests/demos/common | java | TODO |  | reports/common.md | Compose, Test, Domain: compose |
| 185 | docs-snippets | compose/integration-tests/docs-snippets | java | TODO |  | reports/docs-snippets.md | Compose, Test, Domain: compose |
| 186 | hero-common-implementation | compose/integration-tests/hero/hero-common/hero-common-implementation | java | TODO |  | reports/hero-common-implementation.md | Compose, Test, Domain: compose |
| 187 | hero-common-macrobenchmark | compose/integration-tests/hero/hero-common/hero-common-macrobenchmark | java | TODO |  | reports/hero-common-macrobenchmark.md | Compose, Test, Domain: compose |
| 188 | jetsnack-implementation | compose/integration-tests/hero/jetsnack/jetsnack-implementation | java | TODO |  | reports/jetsnack-implementation.md | Compose, Test, Domain: compose |
| 189 | jetsnack-macrobenchmark | compose/integration-tests/hero/jetsnack/jetsnack-macrobenchmark | java | TODO |  | reports/jetsnack-macrobenchmark.md | Compose, Test, Domain: compose |
| 190 | jetsnack-macrobenchmark-target | compose/integration-tests/hero/jetsnack/jetsnack-macrobenchmark-target | java | TODO |  | reports/jetsnack-macrobenchmark-target.md | Compose, Test, Domain: compose |
| 191 | jetsnack-microbenchmark | compose/integration-tests/hero/jetsnack/jetsnack-microbenchmark | has-src | TODO |  | reports/jetsnack-microbenchmark.md | Compose, Test, Domain: compose |
| 192 | pokedex-macrobenchmark | compose/integration-tests/hero/pokedex/pokedex-macrobenchmark | java | TODO |  | reports/pokedex-macrobenchmark.md | Compose, Test, Domain: compose |
| 193 | pokedex-macrobenchmark-target | compose/integration-tests/hero/pokedex/pokedex-macrobenchmark-target | java | TODO |  | reports/pokedex-macrobenchmark-target.md | Compose, Test, Domain: compose |
| 194 | macrobenchmark | compose/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Compose, Test, Domain: compose |
| 195 | macrobenchmark-target | compose/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Compose, Test, Domain: compose |
| 196 | material-catalog | compose/integration-tests/material-catalog | java | TODO |  | reports/material-catalog.md | Compose, Test, Domain: compose |
| 197 | common | compose/lint/common | java | TODO |  | reports/common.md | Compose, Domain: compose |
| 198 | common-test | compose/lint/common-test | java | TODO |  | reports/common-test.md | Compose, Domain: compose |
| 199 | internal-lint-checks | compose/lint/internal-lint-checks | java | TODO |  | reports/internal-lint-checks.md | Compose, Domain: compose |
| 200 | material | compose/material/material | kotlin-multiplatform | TODO |  | reports/material.md | Compose, Domain: compose |
| 201 | material-lint | compose/material/material-lint | java | TODO |  | reports/material-lint.md | Compose, Domain: compose |
| 202 | material-navigation | compose/material/material-navigation | java | TODO |  | reports/material-navigation.md | Compose, Domain: compose |
| 203 | samples | compose/material/material-navigation/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 204 | material-ripple | compose/material/material-ripple | kotlin-multiplatform | TODO |  | reports/material-ripple.md | Compose, Domain: compose |
| 205 | benchmark | compose/material/material-ripple/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 206 | benchmark | compose/material/material/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 207 | material-catalog | compose/material/material/integration-tests/material-catalog | java | TODO |  | reports/material-catalog.md | Compose, Test, Domain: compose |
| 208 | material-demos | compose/material/material/integration-tests/material-demos | java | TODO |  | reports/material-demos.md | Compose, Test, Domain: compose |
| 209 | samples | compose/material/material/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 210 | adaptive | compose/material3/adaptive/adaptive | kotlin-multiplatform | TODO |  | reports/adaptive.md | Compose, Domain: compose |
| 211 | adaptive-layout | compose/material3/adaptive/adaptive-layout | kotlin-multiplatform | TODO |  | reports/adaptive-layout.md | Compose, Domain: compose |
| 212 | adaptive-navigation | compose/material3/adaptive/adaptive-navigation | kotlin-multiplatform | TODO |  | reports/adaptive-navigation.md | Compose, Domain: compose |
| 213 | adaptive-navigation3 | compose/material3/adaptive/adaptive-navigation3 | kotlin-multiplatform | TODO |  | reports/adaptive-navigation3.md | Compose, Domain: compose |
| 214 | benchmark | compose/material3/adaptive/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 215 | samples | compose/material3/adaptive/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 216 | benchmark | compose/material3/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 217 | macrobenchmark | compose/material3/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Compose, Test, Domain: compose |
| 218 | macrobenchmark-target | compose/material3/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Compose, Test, Domain: compose |
| 219 | material3 | compose/material3/material3 | kotlin-multiplatform | TODO |  | reports/material3.md | Compose, Domain: compose |
| 220 | material3-adaptive-navigation-suite | compose/material3/material3-adaptive-navigation-suite | kotlin-multiplatform | TODO |  | reports/material3-adaptive-navigation-suite.md | Compose, Domain: compose |
| 221 | samples | compose/material3/material3-adaptive-navigation-suite/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 222 | material3-lint | compose/material3/material3-lint | java | TODO |  | reports/material3-lint.md | Compose, Domain: compose |
| 223 | material3-window-size-class | compose/material3/material3-window-size-class | kotlin-multiplatform | TODO |  | reports/material3-window-size-class.md | Compose, Domain: compose |
| 224 | samples | compose/material3/material3-window-size-class/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 225 | material3-catalog | compose/material3/material3/integration-tests/material3-catalog | java | TODO |  | reports/material3-catalog.md | Compose, Test, Domain: compose |
| 226 | material3-demos | compose/material3/material3/integration-tests/material3-demos | java | TODO |  | reports/material3-demos.md | Compose, Test, Domain: compose |
| 227 | samples | compose/material3/material3/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 228 | demos | compose/remote/integration-tests/demos | java | TODO |  | reports/demos.md | Compose, Test, Domain: compose |
| 229 | player-view-demos | compose/remote/integration-tests/player-view-demos | java | TODO |  | reports/player-view-demos.md | Compose, Test, Domain: compose |
| 230 | remote-core | compose/remote/remote-core | java | TODO |  | reports/remote-core.md | Compose, Domain: compose |
| 231 | remote-core-testutils | compose/remote/remote-core-testutils | java | TODO |  | reports/remote-core-testutils.md | Compose, Domain: compose |
| 232 | remote-creation | compose/remote/remote-creation | has-src | TODO |  | reports/remote-creation.md | Compose, Domain: compose |
| 233 | remote-creation-compose | compose/remote/remote-creation-compose | java | TODO |  | reports/remote-creation-compose.md | Compose, Domain: compose |
| 234 | remote-creation-core | compose/remote/remote-creation-core | java | TODO |  | reports/remote-creation-core.md | Compose, Domain: compose |
| 235 | remote-player-compose | compose/remote/remote-player-compose | java | TODO |  | reports/remote-player-compose.md | Compose, Domain: compose |
| 236 | remote-player-compose-testutils | compose/remote/remote-player-compose-testutils | java | TODO |  | reports/remote-player-compose-testutils.md | Compose, Domain: compose |
| 237 | remote-player-core | compose/remote/remote-player-core | java | TODO |  | reports/remote-player-core.md | Compose, Domain: compose |
| 238 | remote-player-view | compose/remote/remote-player-view | java | TODO |  | reports/remote-player-view.md | Compose, Domain: compose |
| 239 | remote-tooling-preview | compose/remote/remote-tooling-preview | kotlin | TODO |  | reports/remote-tooling-preview.md | Compose, Domain: compose |
| 240 | runtime | compose/runtime/runtime | kotlin-multiplatform | TODO |  | reports/runtime.md | Compose, Domain: compose |
| 241 | runtime-annotation | compose/runtime/runtime-annotation | kotlin-multiplatform | TODO |  | reports/runtime-annotation.md | Compose, Domain: compose |
| 242 | samples | compose/runtime/runtime-annotation/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 243 | runtime-lint | compose/runtime/runtime-lint | java | TODO |  | reports/runtime-lint.md | Compose, Domain: compose |
| 244 | runtime-livedata | compose/runtime/runtime-livedata | java | TODO |  | reports/runtime-livedata.md | Compose, Domain: compose |
| 245 | samples | compose/runtime/runtime-livedata/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 246 | runtime-retain | compose/runtime/runtime-retain | kotlin-multiplatform | TODO |  | reports/runtime-retain.md | Compose, Domain: compose |
| 247 | runtime-retain-lint | compose/runtime/runtime-retain-lint | java | TODO |  | reports/runtime-retain-lint.md | Compose, Domain: compose |
| 248 | samples | compose/runtime/runtime-retain/samples | kotlin | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 249 | runtime-rxjava2 | compose/runtime/runtime-rxjava2 | kotlin-multiplatform | TODO |  | reports/runtime-rxjava2.md | Compose, Domain: compose |
| 250 | samples | compose/runtime/runtime-rxjava2/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 251 | runtime-rxjava3 | compose/runtime/runtime-rxjava3 | kotlin-multiplatform | TODO |  | reports/runtime-rxjava3.md | Compose, Domain: compose |
| 252 | samples | compose/runtime/runtime-rxjava3/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 253 | runtime-saveable | compose/runtime/runtime-saveable | kotlin-multiplatform | TODO |  | reports/runtime-saveable.md | Compose, Domain: compose |
| 254 | runtime-saveable-lint | compose/runtime/runtime-saveable-lint | java | TODO |  | reports/runtime-saveable-lint.md | Compose, Domain: compose |
| 255 | samples | compose/runtime/runtime-saveable/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 256 | runtime-test-utils | compose/runtime/runtime-test-utils | kotlin-multiplatform | TODO |  | reports/runtime-test-utils.md | Compose, Test, Domain: compose |
| 257 | runtime-tracing | compose/runtime/runtime-tracing | java | TODO |  | reports/runtime-tracing.md | Compose, Domain: compose |
| 258 | compose-runtime-benchmark | compose/runtime/runtime/compose-runtime-benchmark | has-src | TODO |  | reports/compose-runtime-benchmark.md | Compose, Test, Domain: compose |
| 259 | integration-tests | compose/runtime/runtime/integration-tests | has-src | TODO |  | reports/integration-tests.md | Compose, Test, Domain: compose |
| 260 | samples | compose/runtime/runtime/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 261 | test-utils | compose/test-utils | kotlin-multiplatform | TODO |  | reports/test-utils.md | Compose, Test, Domain: compose |
| 262 | ui | compose/ui/ui | kotlin-multiplatform | TODO |  | reports/ui.md | Compose, Domain: compose |
| 263 | ui-android-stubs | compose/ui/ui-android-stubs | java | TODO |  | reports/ui-android-stubs.md | Compose, Domain: compose |
| 264 | ui-geometry | compose/ui/ui-geometry | kotlin-multiplatform | TODO |  | reports/ui-geometry.md | Compose, Domain: compose |
| 265 | ui-graphics | compose/ui/ui-graphics | kotlin-multiplatform | TODO |  | reports/ui-graphics.md | Compose, Domain: compose |
| 266 | ui-graphics-lint | compose/ui/ui-graphics-lint | java | TODO |  | reports/ui-graphics-lint.md | Compose, Domain: compose |
| 267 | benchmark | compose/ui/ui-graphics/benchmark | java | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 268 | test | compose/ui/ui-graphics/benchmark/test | has-src | TODO |  | reports/test.md | Compose, Test, Domain: compose |
| 269 | samples | compose/ui/ui-graphics/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 270 | ui-inspection | compose/ui/ui-inspection | java | TODO |  | reports/ui-inspection.md | Compose, Domain: compose |
| 271 | ui-lint | compose/ui/ui-lint | java | TODO |  | reports/ui-lint.md | Compose, Domain: compose |
| 272 | ui-test | compose/ui/ui-test | kotlin-multiplatform | TODO |  | reports/ui-test.md | Compose, Domain: compose |
| 273 | ui-test-accessibility | compose/ui/ui-test-accessibility | has-src | TODO |  | reports/ui-test-accessibility.md | Compose, Test, Domain: compose |
| 274 | samples | compose/ui/ui-test-accessibility/samples | java | TODO |  | reports/samples.md | Compose, Sample, Test, Domain: compose |
| 275 | ui-test-junit4 | compose/ui/ui-test-junit4 | has-src | TODO |  | reports/ui-test-junit4.md | Compose, Test, Domain: compose |
| 276 | ui-test-junit4-accessibility | compose/ui/ui-test-junit4-accessibility | has-src | TODO |  | reports/ui-test-junit4-accessibility.md | Compose, Test, Domain: compose |
| 277 | samples | compose/ui/ui-test-junit4-accessibility/samples | java | TODO |  | reports/samples.md | Compose, Sample, Test, Domain: compose |
| 278 | ui-test-manifest | compose/ui/ui-test-manifest | has-src | TODO |  | reports/ui-test-manifest.md | Compose, Test, Domain: compose |
| 279 | ui-test-manifest-lint | compose/ui/ui-test-manifest-lint | java | TODO |  | reports/ui-test-manifest-lint.md | Compose, Test, Domain: compose |
| 280 | testapp | compose/ui/ui-test-manifest/integration-tests/testapp | has-src | TODO |  | reports/testapp.md | Compose, Test, Domain: compose |
| 281 | samples | compose/ui/ui-test/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 282 | ui-text | compose/ui/ui-text | kotlin-multiplatform | TODO |  | reports/ui-text.md | Compose, Domain: compose |
| 283 | ui-text-google-fonts | compose/ui/ui-text-google-fonts | java | TODO |  | reports/ui-text-google-fonts.md | Compose, Domain: compose |
| 284 | ui-text-lint | compose/ui/ui-text-lint | java | TODO |  | reports/ui-text-lint.md | Compose, Domain: compose |
| 285 | benchmark | compose/ui/ui-text/benchmark | java | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 286 | samples | compose/ui/ui-text/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 287 | ui-tooling | compose/ui/ui-tooling | has-src | TODO |  | reports/ui-tooling.md | Compose, Domain: compose |
| 288 | ui-tooling-data | compose/ui/ui-tooling-data | has-src | TODO |  | reports/ui-tooling-data.md | Compose, Domain: compose |
| 289 | ui-tooling-preview | compose/ui/ui-tooling-preview | kotlin-multiplatform | TODO |  | reports/ui-tooling-preview.md | Compose, Domain: compose |
| 290 | ui-unit | compose/ui/ui-unit | kotlin-multiplatform | TODO |  | reports/ui-unit.md | Compose, Domain: compose |
| 291 | samples | compose/ui/ui-unit/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 292 | ui-util | compose/ui/ui-util | kotlin-multiplatform | TODO |  | reports/ui-util.md | Compose, Domain: compose |
| 293 | ui-viewbinding | compose/ui/ui-viewbinding | java | TODO |  | reports/ui-viewbinding.md | Compose, Domain: compose |
| 294 | samples | compose/ui/ui-viewbinding/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 295 | benchmark | compose/ui/ui/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: compose |
| 296 | ui-demos | compose/ui/ui/integration-tests/ui-demos | java | TODO |  | reports/ui-demos.md | Compose, Test, Domain: compose |
| 297 | samples | compose/ui/ui/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: compose |
| 298 | concurrent-futures | concurrent/concurrent-futures | java | TODO |  | reports/concurrent-futures.md | Domain: concurrent |
| 299 | concurrent-futures-ktx | concurrent/concurrent-futures-ktx | java | TODO |  | reports/concurrent-futures-ktx.md | Domain: concurrent |
| 300 | constraintlayout | constraintlayout/constraintlayout | java | TODO |  | reports/constraintlayout.md | Domain: constraintlayout |
| 301 | constraintlayout-compose | constraintlayout/constraintlayout-compose | has-src | TODO |  | reports/constraintlayout-compose.md | Compose, Domain: constraintlayout |
| 302 | constraintlayout-compose-lint | constraintlayout/constraintlayout-compose-lint | java | TODO |  | reports/constraintlayout-compose-lint.md | Compose, Domain: constraintlayout |
| 303 | compose-benchmark | constraintlayout/constraintlayout-compose/integration-tests/compose-benchmark | has-src | TODO |  | reports/compose-benchmark.md | Compose, Test, Domain: constraintlayout |
| 304 | demos | constraintlayout/constraintlayout-compose/integration-tests/demos | java | TODO |  | reports/demos.md | Compose, Test, Domain: constraintlayout |
| 305 | macrobenchmark | constraintlayout/constraintlayout-compose/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Compose, Test, Domain: constraintlayout |
| 306 | macrobenchmark-target | constraintlayout/constraintlayout-compose/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Compose, Test, Domain: constraintlayout |
| 307 | samples | constraintlayout/constraintlayout-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: constraintlayout |
| 308 | constraintlayout-core | constraintlayout/constraintlayout-core | java | TODO |  | reports/constraintlayout-core.md | Domain: constraintlayout |
| 309 | coordinatorlayout | coordinatorlayout/coordinatorlayout | java | TODO |  | reports/coordinatorlayout.md | Domain: coordinatorlayout |
| 310 | core | core/core | java | TODO |  | reports/core.md | Domain: core |
| 311 | core-animation | core/core-animation | java | TODO |  | reports/core-animation.md | Domain: core |
| 312 | testapp | core/core-animation-integration-tests/testapp | has-src | TODO |  | reports/testapp.md | Test, Domain: core |
| 313 | core-animation-testing | core/core-animation-testing | java | TODO |  | reports/core-animation-testing.md | Test, Domain: core |
| 314 | core-appdigest | core/core-appdigest | java | TODO |  | reports/core-appdigest.md | Domain: core |
| 315 | core-backported-fixes | core/core-backported-fixes | java | TODO |  | reports/core-backported-fixes.md | Domain: core |
| 316 | testapp | core/core-backported-fixes/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: core |
| 317 | samples | core/core-backported-fixes/samples | java | TODO |  | reports/samples.md | Sample, Domain: core |
| 318 | core-google-shortcuts | core/core-google-shortcuts | java | TODO |  | reports/core-google-shortcuts.md | Domain: core |
| 319 | testapp | core/core-graphics-integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: core |
| 320 | core-i18n | core/core-i18n | java | TODO |  | reports/core-i18n.md | Domain: core |
| 321 | core-ktx | core/core-ktx | java | TODO |  | reports/core-ktx.md | Domain: core |
| 322 | core-location-altitude | core/core-location-altitude | java | TODO |  | reports/core-location-altitude.md | Domain: core |
| 323 | core-location-altitude-external-protobuf | core/core-location-altitude-external-protobuf | unknown | TODO |  | reports/core-location-altitude-external-protobuf.md | Domain: core |
| 324 | core-location-altitude-proto | core/core-location-altitude-proto | has-src | TODO |  | reports/core-location-altitude-proto.md | Domain: core |
| 325 | core-performance | core/core-performance | java | TODO |  | reports/core-performance.md | Domain: core |
| 326 | core-performance-play-services | core/core-performance-play-services | java | TODO |  | reports/core-performance-play-services.md | Domain: core |
| 327 | testapp | core/core-performance-play-services/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: core |
| 328 | core-performance-testing | core/core-performance-testing | java | TODO |  | reports/core-performance-testing.md | Test, Domain: core |
| 329 | testapp | core/core-performance/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: core |
| 330 | testlib | core/core-performance/integration-tests/testlib | java | TODO |  | reports/testlib.md | Test, Domain: core |
| 331 | samples | core/core-performance/samples | java | TODO |  | reports/samples.md | Sample, Domain: core |
| 332 | core-remoteviews | core/core-remoteviews | java | TODO |  | reports/core-remoteviews.md | Domain: core |
| 333 | demos | core/core-remoteviews/integration-tests/demos | java | TODO |  | reports/demos.md | Test, Domain: core |
| 334 | core-role | core/core-role | java | TODO |  | reports/core-role.md | Domain: core |
| 335 | core-splashscreen | core/core-splashscreen | java | TODO |  | reports/core-splashscreen.md | Domain: core |
| 336 | samples | core/core-splashscreen/samples | java | TODO |  | reports/samples.md | Sample, Domain: core |
| 337 | core-telecom | core/core-telecom | java | TODO |  | reports/core-telecom.md | Domain: core |
| 338 | referenceapp | core/core-telecom/integration-tests/referenceapp | java | TODO |  | reports/referenceapp.md | Test, Domain: core |
| 339 | testapp | core/core-telecom/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: core |
| 340 | testicsapp | core/core-telecom/integration-tests/testicsapp | java | TODO |  | reports/testicsapp.md | Test, Domain: core |
| 341 | core-testing | core/core-testing | java | TODO |  | reports/core-testing.md | Test, Domain: core |
| 342 | core-viewtree | core/core-viewtree | java | TODO |  | reports/core-viewtree.md | Domain: core |
| 343 | publishing | core/core/integration-tests/publishing | has-src | TODO |  | reports/publishing.md | Test, Domain: core |
| 344 | samples | core/core/samples | java | TODO |  | reports/samples.md | Sample, Domain: core |
| 345 | haptics | core/haptics/haptics | java | TODO |  | reports/haptics.md | Domain: core |
| 346 | demos | core/haptics/haptics/integration-tests/demos | java | TODO |  | reports/demos.md | Test, Domain: core |
| 347 | samples | core/haptics/haptics/samples | java | TODO |  | reports/samples.md | Sample, Domain: core |
| 348 | uwb | core/uwb/uwb | java | TODO |  | reports/uwb.md | Domain: core |
| 349 | uwb-rxjava3 | core/uwb/uwb-rxjava3 | java | TODO |  | reports/uwb-rxjava3.md | Domain: core |
| 350 | credentials | credentials/credentials | java | TODO |  | reports/credentials.md | Domain: credentials |
| 351 | credentials-e2ee | credentials/credentials-e2ee | java | TODO |  | reports/credentials-e2ee.md | Domain: credentials |
| 352 | credentials-play-services-auth | credentials/credentials-play-services-auth | java | TODO |  | reports/credentials-play-services-auth.md | Domain: credentials |
| 353 | credentials-play-services-e2ee | credentials/credentials-play-services-e2ee | has-src | TODO |  | reports/credentials-play-services-e2ee.md | Domain: credentials |
| 354 | samples | credentials/credentials/samples | java | TODO |  | reports/samples.md | Sample, Domain: credentials |
| 355 | providerevents | credentials/providerevents/providerevents | java | TODO |  | reports/providerevents.md | Domain: credentials |
| 356 | providerevents-play-services | credentials/providerevents/providerevents-play-services | java | TODO |  | reports/providerevents-play-services.md | Domain: credentials |
| 357 | registry-digitalcredentials-mdoc | credentials/registry/registry-digitalcredentials-mdoc | java | TODO |  | reports/registry-digitalcredentials-mdoc.md | Domain: credentials |
| 358 | registry-digitalcredentials-openid | credentials/registry/registry-digitalcredentials-openid | java | TODO |  | reports/registry-digitalcredentials-openid.md | Domain: credentials |
| 359 | registry-digitalcredentials-sdjwtvc | credentials/registry/registry-digitalcredentials-sdjwtvc | java | TODO |  | reports/registry-digitalcredentials-sdjwtvc.md | Domain: credentials |
| 360 | registry-provider | credentials/registry/registry-provider | java | TODO |  | reports/registry-provider.md | Domain: credentials |
| 361 | registry-provider-play-services | credentials/registry/registry-provider-play-services | java | TODO |  | reports/registry-provider-play-services.md | Domain: credentials |
| 362 | cursoradapter | cursoradapter/cursoradapter | java | TODO |  | reports/cursoradapter.md | Domain: cursoradapter |
| 363 | customview | customview/customview | java | TODO |  | reports/customview.md | Domain: customview |
| 364 | customview-poolingcontainer | customview/customview-poolingcontainer | java | TODO |  | reports/customview-poolingcontainer.md | Domain: customview |
| 365 | datastore | datastore/datastore | kotlin-multiplatform | TODO |  | reports/datastore.md | Domain: datastore |
| 366 | datastore-benchmark | datastore/datastore-benchmark | has-src | TODO |  | reports/datastore-benchmark.md | Test, Domain: datastore |
| 367 | datastore-compose-samples | datastore/datastore-compose-samples | java | TODO |  | reports/datastore-compose-samples.md | Compose, Sample, Domain: datastore |
| 368 | datastore-core | datastore/datastore-core | kotlin-multiplatform | TODO |  | reports/datastore-core.md | Domain: datastore |
| 369 | datastore-core-okio | datastore/datastore-core-okio | kotlin-multiplatform | TODO |  | reports/datastore-core-okio.md | Domain: datastore |
| 370 | datastore-guava | datastore/datastore-guava | java | TODO |  | reports/datastore-guava.md | Domain: datastore |
| 371 | datastore-preferences | datastore/datastore-preferences | kotlin-multiplatform | TODO |  | reports/datastore-preferences.md | Domain: datastore |
| 372 | datastore-preferences-core | datastore/datastore-preferences-core | kotlin-multiplatform | TODO |  | reports/datastore-preferences-core.md | Domain: datastore |
| 373 | datastore-preferences-external-protobuf | datastore/datastore-preferences-external-protobuf | unknown | TODO |  | reports/datastore-preferences-external-protobuf.md | Domain: datastore |
| 374 | datastore-preferences-proto | datastore/datastore-preferences-proto | java | TODO |  | reports/datastore-preferences-proto.md | Domain: datastore |
| 375 | datastore-preferences-rxjava2 | datastore/datastore-preferences-rxjava2 | java | TODO |  | reports/datastore-preferences-rxjava2.md | Domain: datastore |
| 376 | datastore-preferences-rxjava3 | datastore/datastore-preferences-rxjava3 | java | TODO |  | reports/datastore-preferences-rxjava3.md | Domain: datastore |
| 377 | datastore-proto | datastore/datastore-proto | java | TODO |  | reports/datastore-proto.md | Domain: datastore |
| 378 | datastore-rxjava2 | datastore/datastore-rxjava2 | java | TODO |  | reports/datastore-rxjava2.md | Domain: datastore |
| 379 | datastore-rxjava3 | datastore/datastore-rxjava3 | java | TODO |  | reports/datastore-rxjava3.md | Domain: datastore |
| 380 | datastore-sampleapp | datastore/datastore-sampleapp | java | TODO |  | reports/datastore-sampleapp.md | Domain: datastore |
| 381 | testapp | datastore/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: datastore |
| 382 | aabReport | development/aabReport | kotlin | TODO |  | reports/aabReport.md | Domain: development |
| 383 | app | development/ab-benchmarking/app | kotlin | TODO |  | reports/app.md | Test, Domain: development |
| 384 | app | development/bench-flame-diff/app | kotlin | TODO |  | reports/app.md | Domain: development |
| 385 | importMaven | development/importMaven | kotlin | TODO |  | reports/importMaven.md | Domain: development |
| 386 | artifactId | development/project-creator/compose-template/groupId/artifactId | kotlin-multiplatform | TODO |  | reports/artifactId.md | Compose, Domain: development |
| 387 | artifactId | development/project-creator/java-template/groupId/artifactId | java | TODO |  | reports/artifactId.md | Domain: development |
| 388 | artifactId | development/project-creator/kotlin-template/groupId/artifactId | java | TODO |  | reports/artifactId.md | Domain: development |
| 389 | artifactId | development/project-creator/native-template/groupId/artifactId | java | TODO |  | reports/artifactId.md | Domain: development |
| 390 | splitBaselineProfiles | development/splitBaselineProfiles | java | TODO |  | reports/splitBaselineProfiles.md | Domain: development |
| 391 | docs-public | docs-public | unknown | TODO |  | reports/docs-public.md | Domain: docs-public |
| 392 | docs-tip-of-tree | docs-tip-of-tree | unknown | TODO |  | reports/docs-tip-of-tree.md | Domain: docs-tip-of-tree |
| 393 | documentfile | documentfile/documentfile | java | TODO |  | reports/documentfile.md | Domain: documentfile |
| 394 | draganddrop | draganddrop/draganddrop | java | TODO |  | reports/draganddrop.md | Domain: draganddrop |
| 395 | sampleapp | draganddrop/integration-tests/sampleapp | java | TODO |  | reports/sampleapp.md | Test, Domain: draganddrop |
| 396 | drawerlayout | drawerlayout/drawerlayout | java | TODO |  | reports/drawerlayout.md | Domain: drawerlayout |
| 397 | dynamicanimation | dynamicanimation/dynamicanimation | java | TODO |  | reports/dynamicanimation.md | Domain: dynamicanimation |
| 398 | dynamicanimation-ktx | dynamicanimation/dynamicanimation-ktx | java | TODO |  | reports/dynamicanimation-ktx.md | Domain: dynamicanimation |
| 399 | emoji | emoji/emoji | java | TODO |  | reports/emoji.md | Domain: emoji |
| 400 | emoji-appcompat | emoji/emoji-appcompat | java | TODO |  | reports/emoji-appcompat.md | Domain: emoji |
| 401 | emoji-bundled | emoji/emoji-bundled | java | TODO |  | reports/emoji-bundled.md | Domain: emoji |
| 402 | emoji2 | emoji2/emoji2 | java | TODO |  | reports/emoji2.md | Domain: emoji2 |
| 403 | emoji2-benchmark | emoji2/emoji2-benchmark | has-src | TODO |  | reports/emoji2-benchmark.md | Test, Domain: emoji2 |
| 404 | emoji2-bundled | emoji2/emoji2-bundled | java | TODO |  | reports/emoji2-bundled.md | Domain: emoji2 |
| 405 | emoji2-emojipicker | emoji2/emoji2-emojipicker | java | TODO |  | reports/emoji2-emojipicker.md | Domain: emoji2 |
| 406 | samples | emoji2/emoji2-emojipicker/samples | java | TODO |  | reports/samples.md | Sample, Domain: emoji2 |
| 407 | emoji2-views | emoji2/emoji2-views | java | TODO |  | reports/emoji2-views.md | Domain: emoji2 |
| 408 | emoji2-views-helper | emoji2/emoji2-views-helper | java | TODO |  | reports/emoji2-views-helper.md | Domain: emoji2 |
| 409 | init-disabled-macrobenchmark | emoji2/integration-tests/init-disabled-macrobenchmark | java | TODO |  | reports/init-disabled-macrobenchmark.md | Test, Domain: emoji2 |
| 410 | init-disabled-macrobenchmark-target | emoji2/integration-tests/init-disabled-macrobenchmark-target | java | TODO |  | reports/init-disabled-macrobenchmark-target.md | Test, Domain: emoji2 |
| 411 | init-enabled-macrobenchmark | emoji2/integration-tests/init-enabled-macrobenchmark | java | TODO |  | reports/init-enabled-macrobenchmark.md | Test, Domain: emoji2 |
| 412 | init-enabled-macrobenchmark-target | emoji2/integration-tests/init-enabled-macrobenchmark-target | java | TODO |  | reports/init-enabled-macrobenchmark-target.md | Test, Domain: emoji2 |
| 413 | enterprise-feedback | enterprise/enterprise-feedback | java | TODO |  | reports/enterprise-feedback.md | Domain: enterprise |
| 414 | enterprise-feedback-testing | enterprise/enterprise-feedback-testing | java | TODO |  | reports/enterprise-feedback-testing.md | Test, Domain: enterprise |
| 415 | exifinterface | exifinterface/exifinterface | java | TODO |  | reports/exifinterface.md | Domain: exifinterface |
| 416 | libyuv | external/libyuv | unknown | TODO |  | reports/libyuv.md | Domain: external |
| 417 | fragment | fragment/fragment | java | TODO |  | reports/fragment.md | Domain: fragment |
| 418 | fragment-compose | fragment/fragment-compose | java | TODO |  | reports/fragment-compose.md | Compose, Domain: fragment |
| 419 | samples | fragment/fragment-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: fragment |
| 420 | fragment-ktx | fragment/fragment-ktx | java | TODO |  | reports/fragment-ktx.md | Domain: fragment |
| 421 | fragment-lint | fragment/fragment-lint | java | TODO |  | reports/fragment-lint.md | Domain: fragment |
| 422 | fragment-testing | fragment/fragment-testing | java | TODO |  | reports/fragment-testing.md | Test, Domain: fragment |
| 423 | fragment-testing-lint | fragment/fragment-testing-lint | java | TODO |  | reports/fragment-testing-lint.md | Test, Domain: fragment |
| 424 | fragment-testing-manifest | fragment/fragment-testing-manifest | java | TODO |  | reports/fragment-testing-manifest.md | Test, Domain: fragment |
| 425 | fragment-testing-manifest-lint | fragment/fragment-testing-manifest-lint | java | TODO |  | reports/fragment-testing-manifest-lint.md | Test, Domain: fragment |
| 426 | fragment-truth | fragment/fragment-truth | java | TODO |  | reports/fragment-truth.md | Domain: fragment |
| 427 | testapp | fragment/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: fragment |
| 428 | glance | glance/glance | java | TODO |  | reports/glance.md | Domain: glance |
| 429 | glance-appwidget | glance/glance-appwidget | java | TODO |  | reports/glance-appwidget.md | Domain: glance |
| 430 | glance-appwidget-external-protobuf | glance/glance-appwidget-external-protobuf | unknown | TODO |  | reports/glance-appwidget-external-protobuf.md | Domain: glance |
| 431 | glance-appwidget-multiprocess | glance/glance-appwidget-multiprocess | kotlin | TODO |  | reports/glance-appwidget-multiprocess.md | Domain: glance |
| 432 | testapp | glance/glance-appwidget-multiprocess/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: glance |
| 433 | glance-appwidget-preview | glance/glance-appwidget-preview | java | TODO |  | reports/glance-appwidget-preview.md | Domain: glance |
| 434 | glance-appwidget-proto | glance/glance-appwidget-proto | java | TODO |  | reports/glance-appwidget-proto.md | Domain: glance |
| 435 | glance-appwidget-testing | glance/glance-appwidget-testing | java | TODO |  | reports/glance-appwidget-testing.md | Test, Domain: glance |
| 436 | samples | glance/glance-appwidget-testing/samples | java | TODO |  | reports/samples.md | Sample, Test, Domain: glance |
| 437 | glance-layout-generator | glance/glance-appwidget/glance-layout-generator | kotlin | TODO |  | reports/glance-layout-generator.md | Domain: glance |
| 438 | demos | glance/glance-appwidget/integration-tests/demos | java | TODO |  | reports/demos.md | Test, Domain: glance |
| 439 | macrobenchmark | glance/glance-appwidget/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Test, Domain: glance |
| 440 | macrobenchmark-target | glance/glance-appwidget/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Test, Domain: glance |
| 441 | samples | glance/glance-appwidget/samples | java | TODO |  | reports/samples.md | Sample, Domain: glance |
| 442 | glance-material | glance/glance-material | java | TODO |  | reports/glance-material.md | Domain: glance |
| 443 | glance-material3 | glance/glance-material3 | java | TODO |  | reports/glance-material3.md | Domain: glance |
| 444 | glance-preview | glance/glance-preview | java | TODO |  | reports/glance-preview.md | Domain: glance |
| 445 | glance-testing | glance/glance-testing | java | TODO |  | reports/glance-testing.md | Test, Domain: glance |
| 446 | glance-wear-tiles | glance/glance-wear-tiles | java | TODO |  | reports/glance-wear-tiles.md | Domain: glance |
| 447 | demos | glance/glance-wear-tiles/integration-tests/demos | java | TODO |  | reports/demos.md | Test, Domain: glance |
| 448 | template-demos | glance/glance-wear-tiles/integration-tests/template-demos | java | TODO |  | reports/template-demos.md | Test, Domain: glance |
| 449 | wear | glance/wear/wear | java | TODO |  | reports/wear.md | Domain: glance |
| 450 | wear-core | glance/wear/wear-core | java | TODO |  | reports/wear-core.md | Domain: glance |
| 451 | filters | graphics/filters/filters | java | TODO |  | reports/filters.md | Domain: graphics |
| 452 | graphics-core | graphics/graphics-core | java | TODO |  | reports/graphics-core.md | Domain: graphics |
| 453 | samples | graphics/graphics-core/samples | java | TODO |  | reports/samples.md | Sample, Domain: graphics |
| 454 | graphics-path | graphics/graphics-path | java | TODO |  | reports/graphics-path.md | Domain: graphics |
| 455 | graphics-shapes | graphics/graphics-shapes | kotlin-multiplatform | TODO |  | reports/graphics-shapes.md | Domain: graphics |
| 456 | testapp | graphics/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: graphics |
| 457 | testapp-compose | graphics/integration-tests/testapp-compose | java | TODO |  | reports/testapp-compose.md | Compose, Test, Domain: graphics |
| 458 | gridlayout | gridlayout/gridlayout | java | TODO |  | reports/gridlayout.md | Domain: gridlayout |
| 459 | connect-client | health/connect/connect-client | java | TODO |  | reports/connect-client.md | Domain: health |
| 460 | connect-client-external-protobuf | health/connect/connect-client-external-protobuf | unknown | TODO |  | reports/connect-client-external-protobuf.md | Domain: health |
| 461 | connect-client-proto | health/connect/connect-client-proto | has-src | TODO |  | reports/connect-client-proto.md | Domain: health |
| 462 | samples | health/connect/connect-client/samples | java | TODO |  | reports/samples.md | Sample, Domain: health |
| 463 | connect-testing | health/connect/connect-testing | java | TODO |  | reports/connect-testing.md | Test, Domain: health |
| 464 | samples | health/connect/connect-testing/samples | java | TODO |  | reports/samples.md | Sample, Test, Domain: health |
| 465 | health-services-client | health/health-services-client | java | TODO |  | reports/health-services-client.md | Domain: health |
| 466 | health-services-client-external-protobuf | health/health-services-client-external-protobuf | unknown | TODO |  | reports/health-services-client-external-protobuf.md | Domain: health |
| 467 | health-services-client-proto | health/health-services-client-proto | java | TODO |  | reports/health-services-client-proto.md | Domain: health |
| 468 | heifwriter | heifwriter/heifwriter | java | TODO |  | reports/heifwriter.md | Domain: heifwriter |
| 469 | hilt-common | hilt/hilt-common | java | TODO |  | reports/hilt-common.md | Domain: hilt |
| 470 | hilt-compiler | hilt/hilt-compiler | kotlin | TODO |  | reports/hilt-compiler.md | Domain: hilt |
| 471 | hilt-lifecycle-viewmodel | hilt/hilt-lifecycle-viewmodel | kotlin | TODO |  | reports/hilt-lifecycle-viewmodel.md | Domain: hilt |
| 472 | hilt-lifecycle-viewmodel-compose | hilt/hilt-lifecycle-viewmodel-compose | kotlin | TODO |  | reports/hilt-lifecycle-viewmodel-compose.md | Compose, Domain: hilt |
| 473 | hilt-navigation | hilt/hilt-navigation | java | TODO |  | reports/hilt-navigation.md | Domain: hilt |
| 474 | hilt-navigation-compose | hilt/hilt-navigation-compose | java | TODO |  | reports/hilt-navigation-compose.md | Compose, Domain: hilt |
| 475 | samples | hilt/hilt-navigation-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: hilt |
| 476 | hilt-navigation-fragment | hilt/hilt-navigation-fragment | java | TODO |  | reports/hilt-navigation-fragment.md | Domain: hilt |
| 477 | hilt-work | hilt/hilt-work | java | TODO |  | reports/hilt-work.md | Domain: hilt |
| 478 | viewmodelapp | hilt/integration-tests/viewmodelapp | java | TODO |  | reports/viewmodelapp.md | Test, Domain: hilt |
| 479 | workerapp | hilt/integration-tests/workerapp | java | TODO |  | reports/workerapp.md | Test, Domain: hilt |
| 480 | ink-authoring | ink/ink-authoring | has-src | TODO |  | reports/ink-authoring.md | Domain: ink |
| 481 | ink-authoring-compose | ink/ink-authoring-compose | has-src | TODO |  | reports/ink-authoring-compose.md | Compose, Domain: ink |
| 482 | ink-brush | ink/ink-brush | has-src | TODO |  | reports/ink-brush.md | Domain: ink |
| 483 | ink-brush-compose | ink/ink-brush-compose | has-src | TODO |  | reports/ink-brush-compose.md | Compose, Domain: ink |
| 484 | ink-geometry | ink/ink-geometry | has-src | TODO |  | reports/ink-geometry.md | Domain: ink |
| 485 | ink-geometry-compose | ink/ink-geometry-compose | has-src | TODO |  | reports/ink-geometry-compose.md | Compose, Domain: ink |
| 486 | ink-nativeloader | ink/ink-nativeloader | kotlin-multiplatform | TODO |  | reports/ink-nativeloader.md | Domain: ink |
| 487 | ink-rendering | ink/ink-rendering | has-src | TODO |  | reports/ink-rendering.md | Domain: ink |
| 488 | ink-storage | ink/ink-storage | has-src | TODO |  | reports/ink-storage.md | Domain: ink |
| 489 | ink-strokes | ink/ink-strokes | has-src | TODO |  | reports/ink-strokes.md | Domain: ink |
| 490 | input-motionprediction | input/input-motionprediction | java | TODO |  | reports/input-motionprediction.md | Domain: input |
| 491 | inspection | inspection/inspection | java | TODO |  | reports/inspection.md | Domain: inspection |
| 492 | inspection-gradle-plugin | inspection/inspection-gradle-plugin | kotlin | TODO |  | reports/inspection-gradle-plugin.md | Domain: inspection |
| 493 | inspection-testing | inspection/inspection-testing | java | TODO |  | reports/inspection-testing.md | Test, Domain: inspection |
| 494 | interpolator | interpolator/interpolator | java | TODO |  | reports/interpolator.md | Domain: interpolator |
| 495 | testapp | javascriptengine/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: javascriptengine |
| 496 | javascriptengine | javascriptengine/javascriptengine | java | TODO |  | reports/javascriptengine.md | Domain: javascriptengine |
| 497 | kruth | kruth/kruth | kotlin-multiplatform | TODO |  | reports/kruth.md | Domain: kruth |
| 498 | leanback | leanback/leanback | java | TODO |  | reports/leanback.md | Domain: leanback |
| 499 | leanback-grid | leanback/leanback-grid | java | TODO |  | reports/leanback-grid.md | Domain: leanback |
| 500 | leanback-paging | leanback/leanback-paging | java | TODO |  | reports/leanback-paging.md | Domain: leanback |
| 501 | leanback-preference | leanback/leanback-preference | java | TODO |  | reports/leanback-preference.md | Domain: leanback |
| 502 | leanback-tab | leanback/leanback-tab | java | TODO |  | reports/leanback-tab.md | Domain: leanback |
| 503 | incrementality | lifecycle/integration-tests/incrementality | has-src | TODO |  | reports/incrementality.md | Test, Domain: lifecycle |
| 504 | kotlintestapp | lifecycle/integration-tests/kotlintestapp | java | TODO |  | reports/kotlintestapp.md | Test, Domain: lifecycle |
| 505 | testapp | lifecycle/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: lifecycle |
| 506 | lifecycle-common | lifecycle/lifecycle-common | kotlin-multiplatform | TODO |  | reports/lifecycle-common.md | Domain: lifecycle |
| 507 | lifecycle-common-java8 | lifecycle/lifecycle-common-java8 | unknown | TODO |  | reports/lifecycle-common-java8.md | Domain: lifecycle |
| 508 | lifecycle-compiler | lifecycle/lifecycle-compiler | kotlin | TODO |  | reports/lifecycle-compiler.md | Domain: lifecycle |
| 509 | lifecycle-livedata | lifecycle/lifecycle-livedata | java | TODO |  | reports/lifecycle-livedata.md | Domain: lifecycle |
| 510 | lifecycle-livedata-core | lifecycle/lifecycle-livedata-core | java | TODO |  | reports/lifecycle-livedata-core.md | Domain: lifecycle |
| 511 | lifecycle-livedata-core-ktx | lifecycle/lifecycle-livedata-core-ktx | unknown | TODO |  | reports/lifecycle-livedata-core-ktx.md | Domain: lifecycle |
| 512 | lifecycle-livedata-core-lint | lifecycle/lifecycle-livedata-core-lint | java | TODO |  | reports/lifecycle-livedata-core-lint.md | Domain: lifecycle |
| 513 | lifecycle-livedata-core-truth | lifecycle/lifecycle-livedata-core-truth | java | TODO |  | reports/lifecycle-livedata-core-truth.md | Domain: lifecycle |
| 514 | lifecycle-livedata-ktx | lifecycle/lifecycle-livedata-ktx | unknown | TODO |  | reports/lifecycle-livedata-ktx.md | Domain: lifecycle |
| 515 | lifecycle-process | lifecycle/lifecycle-process | java | TODO |  | reports/lifecycle-process.md | Domain: lifecycle |
| 516 | lifecycle-reactivestreams | lifecycle/lifecycle-reactivestreams | java | TODO |  | reports/lifecycle-reactivestreams.md | Domain: lifecycle |
| 517 | lifecycle-reactivestreams-ktx | lifecycle/lifecycle-reactivestreams-ktx | unknown | TODO |  | reports/lifecycle-reactivestreams-ktx.md | Domain: lifecycle |
| 518 | lifecycle-runtime | lifecycle/lifecycle-runtime | kotlin-multiplatform | TODO |  | reports/lifecycle-runtime.md | Domain: lifecycle |
| 519 | lifecycle-runtime-compose | lifecycle/lifecycle-runtime-compose | kotlin-multiplatform | TODO |  | reports/lifecycle-runtime-compose.md | Compose, Domain: lifecycle |
| 520 | lifecycle-runtime-compose-lint | lifecycle/lifecycle-runtime-compose-lint | java | TODO |  | reports/lifecycle-runtime-compose-lint.md | Compose, Domain: lifecycle |
| 521 | lifecycle-runtime-compose-demos | lifecycle/lifecycle-runtime-compose/integration-tests/lifecycle-runtime-compose-demos | unknown | TODO |  | reports/lifecycle-runtime-compose-demos.md | Compose, Test, Domain: lifecycle |
| 522 | samples | lifecycle/lifecycle-runtime-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: lifecycle |
| 523 | lifecycle-runtime-ktx | lifecycle/lifecycle-runtime-ktx | unknown | TODO |  | reports/lifecycle-runtime-ktx.md | Domain: lifecycle |
| 524 | lifecycle-runtime-lint | lifecycle/lifecycle-runtime-lint | java | TODO |  | reports/lifecycle-runtime-lint.md | Domain: lifecycle |
| 525 | lifecycle-runtime-testing | lifecycle/lifecycle-runtime-testing | kotlin-multiplatform | TODO |  | reports/lifecycle-runtime-testing.md | Test, Domain: lifecycle |
| 526 | lifecycle-runtime-testing-lint | lifecycle/lifecycle-runtime-testing-lint | java | TODO |  | reports/lifecycle-runtime-testing-lint.md | Test, Domain: lifecycle |
| 527 | lifecycle-service | lifecycle/lifecycle-service | java | TODO |  | reports/lifecycle-service.md | Domain: lifecycle |
| 528 | lifecycle-viewmodel | lifecycle/lifecycle-viewmodel | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel.md | Domain: lifecycle |
| 529 | lifecycle-viewmodel-compose | lifecycle/lifecycle-viewmodel-compose | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-compose.md | Compose, Domain: lifecycle |
| 530 | lifecycle-viewmodel-compose-lint | lifecycle/lifecycle-viewmodel-compose-lint | java | TODO |  | reports/lifecycle-viewmodel-compose-lint.md | Compose, Domain: lifecycle |
| 531 | lifecycle-viewmodel-demos | lifecycle/lifecycle-viewmodel-compose/integration-tests/lifecycle-viewmodel-demos | unknown | TODO |  | reports/lifecycle-viewmodel-demos.md | Compose, Test, Domain: lifecycle |
| 532 | samples | lifecycle/lifecycle-viewmodel-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: lifecycle |
| 533 | lifecycle-viewmodel-ktx | lifecycle/lifecycle-viewmodel-ktx | unknown | TODO |  | reports/lifecycle-viewmodel-ktx.md | Domain: lifecycle |
| 534 | lifecycle-viewmodel-navigation3 | lifecycle/lifecycle-viewmodel-navigation3 | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-navigation3.md | Domain: lifecycle |
| 535 | lifecycle-viewmodel-savedstate | lifecycle/lifecycle-viewmodel-savedstate | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-savedstate.md | Domain: lifecycle |
| 536 | lifecycle-viewmodel-savedstate-samples | lifecycle/lifecycle-viewmodel-savedstate-samples | java | TODO |  | reports/lifecycle-viewmodel-savedstate-samples.md | Sample, Domain: lifecycle |
| 537 | lifecycle-viewmodel-testing | lifecycle/lifecycle-viewmodel-testing | kotlin-multiplatform | TODO |  | reports/lifecycle-viewmodel-testing.md | Test, Domain: lifecycle |
| 538 | lint-checks | lint-checks | java | TODO |  | reports/lint-checks.md | Domain: lint-checks |
| 539 | integration-tests | lint-checks/integration-tests | java | TODO |  | reports/integration-tests.md | Test, Domain: lint-checks |
| 540 | lint-gradle | lint/lint-gradle | java | TODO |  | reports/lint-gradle.md | Domain: lint |
| 541 | loader | loader/loader | java | TODO |  | reports/loader.md | Domain: loader |
| 542 | loader-ktx | loader/loader-ktx | java | TODO |  | reports/loader-ktx.md | Domain: loader |
| 543 | media | media/media | java | TODO |  | reports/media.md | Domain: media |
| 544 | mediarouter | mediarouter/mediarouter | java | TODO |  | reports/mediarouter.md | Domain: mediarouter |
| 545 | mediarouter-testing | mediarouter/mediarouter-testing | java | TODO |  | reports/mediarouter-testing.md | Test, Domain: mediarouter |
| 546 | integration-tests | metrics/integration-tests | unknown | TODO |  | reports/integration-tests.md | Test, Domain: metrics |
| 547 | janktest | metrics/integration-tests/janktest | java | TODO |  | reports/janktest.md | Test, Domain: metrics |
| 548 | metrics-benchmark | metrics/metrics-benchmark | has-src | TODO |  | reports/metrics-benchmark.md | Test, Domain: metrics |
| 549 | metrics-performance | metrics/metrics-performance | java | TODO |  | reports/metrics-performance.md | Domain: metrics |
| 550 | integration-tests | navigation/integration-tests | unknown | TODO |  | reports/integration-tests.md | Test, Domain: navigation |
| 551 | safeargs-testapp | navigation/integration-tests/safeargs-testapp | java | TODO |  | reports/safeargs-testapp.md | Test, Domain: navigation |
| 552 | buildSrc | navigation/integration-tests/safeargs-testapp/buildSrc | unknown | TODO |  | reports/buildSrc.md | Test, Domain: navigation |
| 553 | testapp | navigation/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: navigation |
| 554 | common | navigation/lint/common | java | TODO |  | reports/common.md | Domain: navigation |
| 555 | common-test | navigation/lint/common-test | java | TODO |  | reports/common-test.md | Domain: navigation |
| 556 | navigation-benchmark | navigation/navigation-benchmark | has-src | TODO |  | reports/navigation-benchmark.md | Test, Domain: navigation |
| 557 | navigation-common | navigation/navigation-common | kotlin-multiplatform | TODO |  | reports/navigation-common.md | Domain: navigation |
| 558 | navigation-common-ktx | navigation/navigation-common-ktx | has-src | TODO |  | reports/navigation-common-ktx.md | Domain: navigation |
| 559 | navigation-common-lint | navigation/navigation-common-lint | java | TODO |  | reports/navigation-common-lint.md | Domain: navigation |
| 560 | navigation-compose | navigation/navigation-compose | kotlin-multiplatform | TODO |  | reports/navigation-compose.md | Compose, Domain: navigation |
| 561 | navigation-compose-lint | navigation/navigation-compose-lint | java | TODO |  | reports/navigation-compose-lint.md | Compose, Domain: navigation |
| 562 | navigation-demos | navigation/navigation-compose/integration-tests/navigation-demos | java | TODO |  | reports/navigation-demos.md | Compose, Test, Domain: navigation |
| 563 | samples | navigation/navigation-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: navigation |
| 564 | navigation-dynamic-features-fragment | navigation/navigation-dynamic-features-fragment | java | TODO |  | reports/navigation-dynamic-features-fragment.md | Domain: navigation |
| 565 | navigation-dynamic-features-runtime | navigation/navigation-dynamic-features-runtime | java | TODO |  | reports/navigation-dynamic-features-runtime.md | Domain: navigation |
| 566 | navigation-fragment | navigation/navigation-fragment | java | TODO |  | reports/navigation-fragment.md | Domain: navigation |
| 567 | navigation-fragment-compose | navigation/navigation-fragment-compose | java | TODO |  | reports/navigation-fragment-compose.md | Compose, Domain: navigation |
| 568 | navigation-fragment-ktx | navigation/navigation-fragment-ktx | unknown | TODO |  | reports/navigation-fragment-ktx.md | Domain: navigation |
| 569 | navigation-runtime | navigation/navigation-runtime | kotlin-multiplatform | TODO |  | reports/navigation-runtime.md | Domain: navigation |
| 570 | navigation-runtime-ktx | navigation/navigation-runtime-ktx | unknown | TODO |  | reports/navigation-runtime-ktx.md | Domain: navigation |
| 571 | navigation-runtime-lint | navigation/navigation-runtime-lint | java | TODO |  | reports/navigation-runtime-lint.md | Domain: navigation |
| 572 | navigation-safe-args-generator | navigation/navigation-safe-args-generator | kotlin | TODO |  | reports/navigation-safe-args-generator.md | Domain: navigation |
| 573 | navigation-safe-args-gradle-plugin | navigation/navigation-safe-args-gradle-plugin | kotlin | TODO |  | reports/navigation-safe-args-gradle-plugin.md | Domain: navigation |
| 574 | app | navigation/navigation-safe-args-gradle-plugin/src/test/test-data/multimodule-project/app | has-src | TODO |  | reports/app.md | Test, Domain: navigation |
| 575 | base | navigation/navigation-safe-args-gradle-plugin/src/test/test-data/multimodule-project/base | has-src | TODO |  | reports/base.md | Test, Domain: navigation |
| 576 | dynamic_feature | navigation/navigation-safe-args-gradle-plugin/src/test/test-data/multimodule-project/dynamic_feature | has-src | TODO |  | reports/dynamic_feature.md | Test, Domain: navigation |
| 577 | feature | navigation/navigation-safe-args-gradle-plugin/src/test/test-data/multimodule-project/feature | has-src | TODO |  | reports/feature.md | Test, Domain: navigation |
| 578 | instantapp | navigation/navigation-safe-args-gradle-plugin/src/test/test-data/multimodule-project/instantapp | unknown | TODO |  | reports/instantapp.md | Test, Domain: navigation |
| 579 | library | navigation/navigation-safe-args-gradle-plugin/src/test/test-data/multimodule-project/library | has-src | TODO |  | reports/library.md | Test, Domain: navigation |
| 580 | navigation-testing | navigation/navigation-testing | kotlin-multiplatform | TODO |  | reports/navigation-testing.md | Test, Domain: navigation |
| 581 | navigation-ui | navigation/navigation-ui | java | TODO |  | reports/navigation-ui.md | Domain: navigation |
| 582 | navigation-ui-ktx | navigation/navigation-ui-ktx | unknown | TODO |  | reports/navigation-ui-ktx.md | Domain: navigation |
| 583 | navigation3-benchmark | navigation3/navigation3-benchmark | has-src | TODO |  | reports/navigation3-benchmark.md | Test, Domain: navigation3 |
| 584 | navigation3-runtime | navigation3/navigation3-runtime | kotlin-multiplatform | TODO |  | reports/navigation3-runtime.md | Domain: navigation3 |
| 585 | samples | navigation3/navigation3-runtime/samples | kotlin | TODO |  | reports/samples.md | Sample, Domain: navigation3 |
| 586 | navigation3-ui | navigation3/navigation3-ui | kotlin-multiplatform | TODO |  | reports/navigation3-ui.md | Domain: navigation3 |
| 587 | navigation3-demos | navigation3/navigation3-ui/integration-tests/navigation3-demos | kotlin | TODO |  | reports/navigation3-demos.md | Test, Domain: navigation3 |
| 588 | samples | navigation3/navigation3-ui/samples | kotlin | TODO |  | reports/samples.md | Sample, Domain: navigation3 |
| 589 | navigationevent | navigationevent/navigationevent | kotlin-multiplatform | TODO |  | reports/navigationevent.md | Domain: navigationevent |
| 590 | navigationevent-compose | navigationevent/navigationevent-compose | kotlin-multiplatform | TODO |  | reports/navigationevent-compose.md | Compose, Domain: navigationevent |
| 591 | navigationevent-samples | navigationevent/navigationevent-samples | kotlin | TODO |  | reports/navigationevent-samples.md | Sample, Domain: navigationevent |
| 592 | navigationevent-testing | navigationevent/navigationevent-testing | kotlin-multiplatform | TODO |  | reports/navigationevent-testing.md | Test, Domain: navigationevent |
| 593 | testapp | paging/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: paging |
| 594 | paging-common | paging/paging-common | kotlin-multiplatform | TODO |  | reports/paging-common.md | Domain: paging |
| 595 | paging-common-ktx | paging/paging-common-ktx | unknown | TODO |  | reports/paging-common-ktx.md | Domain: paging |
| 596 | paging-compose | paging/paging-compose | kotlin-multiplatform | TODO |  | reports/paging-compose.md | Compose, Domain: paging |
| 597 | paging-demos | paging/paging-compose/integration-tests/paging-demos | java | TODO |  | reports/paging-demos.md | Compose, Test, Domain: paging |
| 598 | samples | paging/paging-compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: paging |
| 599 | paging-guava | paging/paging-guava | java | TODO |  | reports/paging-guava.md | Domain: paging |
| 600 | paging-runtime | paging/paging-runtime | java | TODO |  | reports/paging-runtime.md | Domain: paging |
| 601 | paging-runtime-ktx | paging/paging-runtime-ktx | unknown | TODO |  | reports/paging-runtime-ktx.md | Domain: paging |
| 602 | paging-rxjava2 | paging/paging-rxjava2 | java | TODO |  | reports/paging-rxjava2.md | Domain: paging |
| 603 | paging-rxjava2-ktx | paging/paging-rxjava2-ktx | unknown | TODO |  | reports/paging-rxjava2-ktx.md | Domain: paging |
| 604 | paging-rxjava3 | paging/paging-rxjava3 | java | TODO |  | reports/paging-rxjava3.md | Domain: paging |
| 605 | paging-testing | paging/paging-testing | kotlin-multiplatform | TODO |  | reports/paging-testing.md | Test, Domain: paging |
| 606 | samples | paging/samples | java | TODO |  | reports/samples.md | Sample, Domain: paging |
| 607 | palette | palette/palette | java | TODO |  | reports/palette.md | Domain: palette |
| 608 | palette-ktx | palette/palette-ktx | java | TODO |  | reports/palette-ktx.md | Domain: palette |
| 609 | testapp | pdf/integration-tests/testapp | kotlin | TODO |  | reports/testapp.md | Test, Domain: pdf |
| 610 | pdf-compose | pdf/pdf-compose | java | TODO |  | reports/pdf-compose.md | Compose, Domain: pdf |
| 611 | pdf-document-service | pdf/pdf-document-service | kotlin | TODO |  | reports/pdf-document-service.md | Domain: pdf |
| 612 | pdf-ink | pdf/pdf-ink | kotlin | TODO |  | reports/pdf-ink.md | Domain: pdf |
| 613 | pdf-viewer | pdf/pdf-viewer | kotlin | TODO |  | reports/pdf-viewer.md | Domain: pdf |
| 614 | pdf-viewer-fragment | pdf/pdf-viewer-fragment | kotlin | TODO |  | reports/pdf-viewer-fragment.md | Domain: pdf |
| 615 | percentlayout | percentlayout/percentlayout | java | TODO |  | reports/percentlayout.md | Domain: percentlayout |
| 616 | photopicker | photopicker/photopicker | kotlin | TODO |  | reports/photopicker.md | Domain: photopicker |
| 617 | photopicker-compose | photopicker/photopicker-compose | kotlin | TODO |  | reports/photopicker-compose.md | Compose, Domain: photopicker |
| 618 | photopicker-testing | photopicker/photopicker-testing | kotlin | TODO |  | reports/photopicker-testing.md | Test, Domain: photopicker |
| 619 | placeholder | placeholder | unknown | TODO |  | reports/placeholder.md | Domain: placeholder |
| 620 | placeholder-tests | placeholder-tests | has-src | TODO |  | reports/placeholder-tests.md | Test, Domain: placeholder-tests |
| 621 | playground-plugin | playground-common/playground-plugin | kotlin | TODO |  | reports/playground-plugin.md | Domain: playground-common |
| 622 | shared | playground-common/playground-plugin/shared | has-src | TODO |  | reports/shared.md | Domain: playground-common |
| 623 | preference | preference/preference | java | TODO |  | reports/preference.md | Domain: preference |
| 624 | preference-ktx | preference/preference-ktx | java | TODO |  | reports/preference-ktx.md | Domain: preference |
| 625 | print | print/print | java | TODO |  | reports/print.md | Domain: print |
| 626 | activity-client | privacysandbox/activity/activity-client | java | TODO |  | reports/activity-client.md | Domain: privacysandbox |
| 627 | activity-core | privacysandbox/activity/activity-core | java | TODO |  | reports/activity-core.md | Domain: privacysandbox |
| 628 | activity-provider | privacysandbox/activity/activity-provider | java | TODO |  | reports/activity-provider.md | Domain: privacysandbox |
| 629 | ads-adservices | privacysandbox/ads/ads-adservices | java | TODO |  | reports/ads-adservices.md | Domain: privacysandbox |
| 630 | ads-adservices-java | privacysandbox/ads/ads-adservices-java | java | TODO |  | reports/ads-adservices-java.md | Domain: privacysandbox |
| 631 | plugins-privacysandbox-library | privacysandbox/plugins/plugins-privacysandbox-library | java | TODO |  | reports/plugins-privacysandbox-library.md | Domain: privacysandbox |
| 632 | sdkruntime-client | privacysandbox/sdkruntime/sdkruntime-client | java | TODO |  | reports/sdkruntime-client.md | Domain: privacysandbox |
| 633 | sdkruntime-core | privacysandbox/sdkruntime/sdkruntime-core | java | TODO |  | reports/sdkruntime-core.md | Domain: privacysandbox |
| 634 | sdkruntime-provider | privacysandbox/sdkruntime/sdkruntime-provider | java | TODO |  | reports/sdkruntime-provider.md | Domain: privacysandbox |
| 635 | tools | privacysandbox/tools/tools | java | TODO |  | reports/tools.md | Domain: privacysandbox |
| 636 | tools-apicompiler | privacysandbox/tools/tools-apicompiler | java | TODO |  | reports/tools-apicompiler.md | Domain: privacysandbox |
| 637 | tools-apigenerator | privacysandbox/tools/tools-apigenerator | java | TODO |  | reports/tools-apigenerator.md | Domain: privacysandbox |
| 638 | tools-apipackager | privacysandbox/tools/tools-apipackager | java | TODO |  | reports/tools-apipackager.md | Domain: privacysandbox |
| 639 | tools-core | privacysandbox/tools/tools-core | java | TODO |  | reports/tools-core.md | Domain: privacysandbox |
| 640 | tools-core-external-protobuf | privacysandbox/tools/tools-core-external-protobuf | unknown | TODO |  | reports/tools-core-external-protobuf.md | Domain: privacysandbox |
| 641 | tools-testing | privacysandbox/tools/tools-testing | java | TODO |  | reports/tools-testing.md | Test, Domain: privacysandbox |
| 642 | testingutils | privacysandbox/ui/integration-tests/testingutils | java | TODO |  | reports/testingutils.md | Test, Domain: privacysandbox |
| 643 | ui-client | privacysandbox/ui/ui-client | java | TODO |  | reports/ui-client.md | Domain: privacysandbox |
| 644 | ui-client-compose | privacysandbox/ui/ui-client-compose | java | TODO |  | reports/ui-client-compose.md | Compose, Domain: privacysandbox |
| 645 | ui-core | privacysandbox/ui/ui-core | java | TODO |  | reports/ui-core.md | Domain: privacysandbox |
| 646 | ui-provider | privacysandbox/ui/ui-provider | java | TODO |  | reports/ui-provider.md | Domain: privacysandbox |
| 647 | ui-tests | privacysandbox/ui/ui-tests | java | TODO |  | reports/ui-tests.md | Test, Domain: privacysandbox |
| 648 | init-macrobenchmark | profileinstaller/integration-tests/init-macrobenchmark | java | TODO |  | reports/init-macrobenchmark.md | Test, Domain: profileinstaller |
| 649 | init-macrobenchmark-target | profileinstaller/integration-tests/init-macrobenchmark-target | java | TODO |  | reports/init-macrobenchmark-target.md | Test, Domain: profileinstaller |
| 650 | profile-verification | profileinstaller/integration-tests/profile-verification | has-src | TODO |  | reports/profile-verification.md | Test, Domain: profileinstaller |
| 651 | profile-verification-sample | profileinstaller/integration-tests/profile-verification-sample | java | TODO |  | reports/profile-verification-sample.md | Test, Domain: profileinstaller |
| 652 | profile-verification-sample-no-initializer | profileinstaller/integration-tests/profile-verification-sample-no-initializer | java | TODO |  | reports/profile-verification-sample-no-initializer.md | Test, Domain: profileinstaller |
| 653 | profileinstaller | profileinstaller/profileinstaller | java | TODO |  | reports/profileinstaller.md | Domain: profileinstaller |
| 654 | profileinstaller-benchmark | profileinstaller/profileinstaller-benchmark | has-src | TODO |  | reports/profileinstaller-benchmark.md | Test, Domain: profileinstaller |
| 655 | recommendation | recommendation/recommendation | java | TODO |  | reports/recommendation.md | Domain: recommendation |
| 656 | recyclerview | recyclerview/recyclerview | java | TODO |  | reports/recyclerview.md | Domain: recyclerview |
| 657 | recyclerview-benchmark | recyclerview/recyclerview-benchmark | has-src | TODO |  | reports/recyclerview-benchmark.md | Test, Domain: recyclerview |
| 658 | recyclerview-lint | recyclerview/recyclerview-lint | java | TODO |  | reports/recyclerview-lint.md | Domain: recyclerview |
| 659 | recyclerview-selection | recyclerview/recyclerview-selection | java | TODO |  | reports/recyclerview-selection.md | Domain: recyclerview |
| 660 | resourceinspection-annotation | resourceinspection/resourceinspection-annotation | java | TODO |  | reports/resourceinspection-annotation.md | Domain: resourceinspection |
| 661 | resourceinspection-processor | resourceinspection/resourceinspection-processor | kotlin | TODO |  | reports/resourceinspection-processor.md | Domain: resourceinspection |
| 662 | benchmark | room3/benchmark | has-src | TODO |  | reports/benchmark.md | Test, Domain: room3 |
| 663 | autovaluetestapp | room3/integration-tests/autovaluetestapp | has-src | TODO |  | reports/autovaluetestapp.md | Test, Domain: room3 |
| 664 | incremental-annotation-processing | room3/integration-tests/incremental-annotation-processing | kotlin | TODO |  | reports/incremental-annotation-processing.md | Test, Domain: room3 |
| 665 | kotlintestapp | room3/integration-tests/kotlintestapp | java | TODO |  | reports/kotlintestapp.md | Test, Domain: room3 |
| 666 | multiplatformtestapp | room3/integration-tests/multiplatformtestapp | has-src | TODO |  | reports/multiplatformtestapp.md | Test, Domain: room3 |
| 667 | room3-common | room3/room3-common | kotlin-multiplatform | TODO |  | reports/room3-common.md | Domain: room3 |
| 668 | room3-compiler | room3/room3-compiler | kotlin | TODO |  | reports/room3-compiler.md | Domain: room3 |
| 669 | room3-compiler-processing | room3/room3-compiler-processing | java | TODO |  | reports/room3-compiler-processing.md | Domain: room3 |
| 670 | room3-compiler-processing-testing | room3/room3-compiler-processing-testing | java | TODO |  | reports/room3-compiler-processing-testing.md | Test, Domain: room3 |
| 671 | room3-external-antlr | room3/room3-external-antlr | unknown | TODO |  | reports/room3-external-antlr.md | Domain: room3 |
| 672 | room3-gradle-plugin | room3/room3-gradle-plugin | java | TODO |  | reports/room3-gradle-plugin.md | Domain: room3 |
| 673 | room3-guava | room3/room3-guava | java | TODO |  | reports/room3-guava.md | Domain: room3 |
| 674 | room3-migration | room3/room3-migration | kotlin-multiplatform | TODO |  | reports/room3-migration.md | Domain: room3 |
| 675 | room3-paging | room3/room3-paging | kotlin-multiplatform | TODO |  | reports/room3-paging.md | Domain: room3 |
| 676 | room3-paging-guava | room3/room3-paging-guava | java | TODO |  | reports/room3-paging-guava.md | Domain: room3 |
| 677 | room3-paging-rxjava3 | room3/room3-paging-rxjava3 | java | TODO |  | reports/room3-paging-rxjava3.md | Domain: room3 |
| 678 | room3-runtime | room3/room3-runtime | kotlin-multiplatform | TODO |  | reports/room3-runtime.md | Domain: room3 |
| 679 | room3-rxjava3 | room3/room3-rxjava3 | java | TODO |  | reports/room3-rxjava3.md | Domain: room3 |
| 680 | room3-sqlite-wrapper | room3/room3-sqlite-wrapper | kotlin | TODO |  | reports/room3-sqlite-wrapper.md | Domain: room3 |
| 681 | room3-testing | room3/room3-testing | kotlin-multiplatform | TODO |  | reports/room3-testing.md | Test, Domain: room3 |
| 682 | safeparcel | safeparcel/safeparcel | java | TODO |  | reports/safeparcel.md | Domain: safeparcel |
| 683 | safeparcel-processor | safeparcel/safeparcel-processor | java | TODO |  | reports/safeparcel-processor.md | Domain: safeparcel |
| 684 | AndroidXDemos | samples/AndroidXDemos | java | TODO |  | reports/AndroidXDemos.md | Sample, Domain: samples |
| 685 | MediaRoutingDemo | samples/MediaRoutingDemo | java | TODO |  | reports/MediaRoutingDemo.md | Sample, Domain: samples |
| 686 | Support4Demos | samples/Support4Demos | java | TODO |  | reports/Support4Demos.md | Sample, Domain: samples |
| 687 | SupportAnimationDemos | samples/SupportAnimationDemos | java | TODO |  | reports/SupportAnimationDemos.md | Sample, Domain: samples |
| 688 | SupportEmojiDemos | samples/SupportEmojiDemos | java | TODO |  | reports/SupportEmojiDemos.md | Sample, Domain: samples |
| 689 | SupportLeanbackDemos | samples/SupportLeanbackDemos | java | TODO |  | reports/SupportLeanbackDemos.md | Sample, Domain: samples |
| 690 | SupportPreferenceDemos | samples/SupportPreferenceDemos | java | TODO |  | reports/SupportPreferenceDemos.md | Sample, Domain: samples |
| 691 | SupportTransitionDemos | samples/SupportTransitionDemos | java | TODO |  | reports/SupportTransitionDemos.md | Sample, Domain: samples |
| 692 | SupportWearDemos | samples/SupportWearDemos | java | TODO |  | reports/SupportWearDemos.md | Sample, Domain: samples |
| 693 | savedstate | savedstate/savedstate | kotlin-multiplatform | TODO |  | reports/savedstate.md | Domain: savedstate |
| 694 | savedstate-benchmark | savedstate/savedstate-benchmark | has-src | TODO |  | reports/savedstate-benchmark.md | Test, Domain: savedstate |
| 695 | savedstate-compose | savedstate/savedstate-compose | kotlin-multiplatform | TODO |  | reports/savedstate-compose.md | Compose, Domain: savedstate |
| 696 | savedstate-ktx | savedstate/savedstate-ktx | unknown | TODO |  | reports/savedstate-ktx.md | Domain: savedstate |
| 697 | savedstate-samples | savedstate/savedstate-samples | java | TODO |  | reports/savedstate-samples.md | Sample, Domain: savedstate |
| 698 | savedstate-testing | savedstate/savedstate-testing | kotlin-multiplatform | TODO |  | reports/savedstate-testing.md | Test, Domain: savedstate |
| 699 | security-app-authenticator | security/security-app-authenticator | java | TODO |  | reports/security-app-authenticator.md | Domain: security |
| 700 | security-app-authenticator-testing | security/security-app-authenticator-testing | java | TODO |  | reports/security-app-authenticator-testing.md | Test, Domain: security |
| 701 | security-crypto | security/security-crypto | java | TODO |  | reports/security-crypto.md | Domain: security |
| 702 | security-crypto-ktx | security/security-crypto-ktx | java | TODO |  | reports/security-crypto-ktx.md | Domain: security |
| 703 | security-mls | security/security-mls | java | TODO |  | reports/security-mls.md | Domain: security |
| 704 | security-state | security/security-state | java | TODO |  | reports/security-state.md | Domain: security |
| 705 | security-state-provider | security/security-state-provider | java | TODO |  | reports/security-state-provider.md | Domain: security |
| 706 | testapp | sharetarget/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: sharetarget |
| 707 | sharetarget | sharetarget/sharetarget | java | TODO |  | reports/sharetarget.md | Domain: sharetarget |
| 708 | slidingpanelayout | slidingpanelayout/slidingpanelayout | java | TODO |  | reports/slidingpanelayout.md | Domain: slidingpanelayout |
| 709 | slidingpanelayout-testapp | slidingpanelayout/slidingpanelayout-testapp | java | TODO |  | reports/slidingpanelayout-testapp.md | Domain: slidingpanelayout |
| 710 | driver-conformance-test | sqlite/integration-tests/driver-conformance-test | has-src | TODO |  | reports/driver-conformance-test.md | Test, Domain: sqlite |
| 711 | inspection-room-testapp | sqlite/integration-tests/inspection-room-testapp | has-src | TODO |  | reports/inspection-room-testapp.md | Test, Domain: sqlite |
| 712 | inspection-sqldelight-testapp | sqlite/integration-tests/inspection-sqldelight-testapp | has-src | TODO |  | reports/inspection-sqldelight-testapp.md | Test, Domain: sqlite |
| 713 | sqlite | sqlite/sqlite | kotlin-multiplatform | TODO |  | reports/sqlite.md | Domain: sqlite |
| 714 | sqlite-bundled | sqlite/sqlite-bundled | kotlin-multiplatform | TODO |  | reports/sqlite-bundled.md | Domain: sqlite |
| 715 | sqlite-framework | sqlite/sqlite-framework | has-src | TODO |  | reports/sqlite-framework.md | Domain: sqlite |
| 716 | sqlite-inspection | sqlite/sqlite-inspection | java | TODO |  | reports/sqlite-inspection.md | Domain: sqlite |
| 717 | sqlite-ktx | sqlite/sqlite-ktx | java | TODO |  | reports/sqlite-ktx.md | Domain: sqlite |
| 718 | stableaidl-gradle-plugin | stableaidl/stableaidl-gradle-plugin | java | TODO |  | reports/stableaidl-gradle-plugin.md | Domain: stableaidl |
| 719 | first-library | startup/integration-tests/first-library | java | TODO |  | reports/first-library.md | Test, Domain: startup |
| 720 | second-library | startup/integration-tests/second-library | java | TODO |  | reports/second-library.md | Test, Domain: startup |
| 721 | test-app | startup/integration-tests/test-app | java | TODO |  | reports/test-app.md | Test, Domain: startup |
| 722 | startup-runtime | startup/startup-runtime | java | TODO |  | reports/startup-runtime.md | Domain: startup |
| 723 | startup-runtime-lint | startup/startup-runtime-lint | java | TODO |  | reports/startup-runtime-lint.md | Domain: startup |
| 724 | swiperefreshlayout | swiperefreshlayout/swiperefreshlayout | java | TODO |  | reports/swiperefreshlayout.md | Domain: swiperefreshlayout |
| 725 | junit-gtest | test/ext/junit-gtest | java | TODO |  | reports/junit-gtest.md | Domain: test |
| 726 | junit-gtest-test | test/integration-tests/junit-gtest-test | has-src | TODO |  | reports/junit-gtest-test.md | Test, Domain: test |
| 727 | screenshot | test/screenshot/screenshot | java | TODO |  | reports/screenshot.md | Domain: test |
| 728 | screenshot-proto | test/screenshot/screenshot-proto | has-src | TODO |  | reports/screenshot-proto.md | Domain: test |
| 729 | testapp | test/uiautomator/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: test |
| 730 | uiautomator | test/uiautomator/uiautomator | java | TODO |  | reports/uiautomator.md | Domain: test |
| 731 | uiautomator-lint | test/uiautomator/uiautomator-lint | java | TODO |  | reports/uiautomator-lint.md | Domain: test |
| 732 | uiautomator-shell | test/uiautomator/uiautomator-shell | has-src | TODO |  | reports/uiautomator-shell.md | Domain: test |
| 733 | testutils-appcompat | testutils/testutils-appcompat | java | TODO |  | reports/testutils-appcompat.md | Domain: testutils |
| 734 | testutils-appfunctions | testutils/testutils-appfunctions | java | TODO |  | reports/testutils-appfunctions.md | Domain: testutils |
| 735 | testutils-benchmark-macro | testutils/testutils-benchmark-macro | java | TODO |  | reports/testutils-benchmark-macro.md | Test, Domain: testutils |
| 736 | testutils-common | testutils/testutils-common | java | TODO |  | reports/testutils-common.md | Domain: testutils |
| 737 | testutils-datastore | testutils/testutils-datastore | has-src | TODO |  | reports/testutils-datastore.md | Domain: testutils |
| 738 | testutils-espresso | testutils/testutils-espresso | java | TODO |  | reports/testutils-espresso.md | Domain: testutils |
| 739 | testutils-fonts | testutils/testutils-fonts | has-src | TODO |  | reports/testutils-fonts.md | Domain: testutils |
| 740 | testutils-gradle-plugin | testutils/testutils-gradle-plugin | java | TODO |  | reports/testutils-gradle-plugin.md | Domain: testutils |
| 741 | testutils-ktx | testutils/testutils-ktx | kotlin-multiplatform | TODO |  | reports/testutils-ktx.md | Domain: testutils |
| 742 | testutils-lifecycle | testutils/testutils-lifecycle | kotlin-multiplatform | TODO |  | reports/testutils-lifecycle.md | Domain: testutils |
| 743 | testutils-mockito | testutils/testutils-mockito | java | TODO |  | reports/testutils-mockito.md | Domain: testutils |
| 744 | testutils-navigation | testutils/testutils-navigation | java | TODO |  | reports/testutils-navigation.md | Domain: testutils |
| 745 | testutils-paging | testutils/testutils-paging | kotlin-multiplatform | TODO |  | reports/testutils-paging.md | Domain: testutils |
| 746 | testutils-runtime | testutils/testutils-runtime | java | TODO |  | reports/testutils-runtime.md | Domain: testutils |
| 747 | testutils-truth | testutils/testutils-truth | java | TODO |  | reports/testutils-truth.md | Domain: testutils |
| 748 | text-vertical | text/text-vertical | java | TODO |  | reports/text-vertical.md | Domain: text |
| 749 | testapp | text/text-vertical/testapp | java | TODO |  | reports/testapp.md | Domain: text |
| 750 | benchmark | tracing/benchmark | has-src | TODO |  | reports/benchmark.md | Test, Domain: tracing |
| 751 | tracing | tracing/tracing | kotlin-multiplatform | TODO |  | reports/tracing.md | Domain: tracing |
| 752 | tracing-driver | tracing/tracing-driver | kotlin-multiplatform | TODO |  | reports/tracing-driver.md | Domain: tracing |
| 753 | tracing-driver-wire | tracing/tracing-driver-wire | kotlin-multiplatform | TODO |  | reports/tracing-driver-wire.md | Domain: tracing |
| 754 | tracing-ktx | tracing/tracing-ktx | has-src | TODO |  | reports/tracing-ktx.md | Domain: tracing |
| 755 | tracing-perfetto | tracing/tracing-perfetto | java | TODO |  | reports/tracing-perfetto.md | Domain: tracing |
| 756 | tracing-perfetto-binary | tracing/tracing-perfetto-binary | has-src | TODO |  | reports/tracing-perfetto-binary.md | Domain: tracing |
| 757 | tracing-perfetto-handshake | tracing/tracing-perfetto-handshake | java | TODO |  | reports/tracing-perfetto-handshake.md | Domain: tracing |
| 758 | transition | transition/transition | java | TODO |  | reports/transition.md | Domain: transition |
| 759 | transition-ktx | transition/transition-ktx | java | TODO |  | reports/transition-ktx.md | Domain: transition |
| 760 | macrobenchmark | tv/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Test, Domain: tv |
| 761 | macrobenchmark-target | tv/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Test, Domain: tv |
| 762 | playground | tv/integration-tests/playground | java | TODO |  | reports/playground.md | Test, Domain: tv |
| 763 | tv-foundation | tv/tv-foundation | java | TODO |  | reports/tv-foundation.md | Domain: tv |
| 764 | tv-material | tv/tv-material | java | TODO |  | reports/tv-material.md | Domain: tv |
| 765 | samples | tv/tv-material/samples | java | TODO |  | reports/samples.md | Sample, Domain: tv |
| 766 | tvprovider | tvprovider/tvprovider | java | TODO |  | reports/tvprovider.md | Domain: tvprovider |
| 767 | testapp | vectordrawable/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: vectordrawable |
| 768 | vectordrawable | vectordrawable/vectordrawable | java | TODO |  | reports/vectordrawable.md | Domain: vectordrawable |
| 769 | vectordrawable-animated | vectordrawable/vectordrawable-animated | java | TODO |  | reports/vectordrawable-animated.md | Domain: vectordrawable |
| 770 | vectordrawable-seekable | vectordrawable/vectordrawable-seekable | java | TODO |  | reports/vectordrawable-seekable.md | Domain: vectordrawable |
| 771 | versionedparcelable | versionedparcelable/versionedparcelable | java | TODO |  | reports/versionedparcelable.md | Domain: versionedparcelable |
| 772 | versionedparcelable-compiler | versionedparcelable/versionedparcelable-compiler | java | TODO |  | reports/versionedparcelable-compiler.md | Domain: versionedparcelable |
| 773 | viewpager | viewpager/viewpager | java | TODO |  | reports/viewpager.md | Domain: viewpager |
| 774 | targetsdk-tests | viewpager2/integration-tests/targetsdk-tests | has-src | TODO |  | reports/targetsdk-tests.md | Test, Domain: viewpager2 |
| 775 | testapp | viewpager2/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: viewpager2 |
| 776 | viewpager2 | viewpager2/viewpager2 | java | TODO |  | reports/viewpager2.md | Domain: viewpager2 |
| 777 | macrobenchmark | wear/benchmark/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Test, Domain: wear |
| 778 | macrobenchmark-target | wear/benchmark/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Test, Domain: wear |
| 779 | compose-foundation | wear/compose/compose-foundation | java | TODO |  | reports/compose-foundation.md | Compose, Domain: wear |
| 780 | benchmark | wear/compose/compose-foundation/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: wear |
| 781 | samples | wear/compose/compose-foundation/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: wear |
| 782 | compose-material | wear/compose/compose-material | java | TODO |  | reports/compose-material.md | Compose, Domain: wear |
| 783 | compose-material-core | wear/compose/compose-material-core | java | TODO |  | reports/compose-material-core.md | Compose, Domain: wear |
| 784 | benchmark | wear/compose/compose-material/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: wear |
| 785 | samples | wear/compose/compose-material/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: wear |
| 786 | compose-material3 | wear/compose/compose-material3 | java | TODO |  | reports/compose-material3.md | Compose, Domain: wear |
| 787 | benchmark | wear/compose/compose-material3/benchmark | has-src | TODO |  | reports/benchmark.md | Compose, Test, Domain: wear |
| 788 | integration-tests | wear/compose/compose-material3/integration-tests | java | TODO |  | reports/integration-tests.md | Compose, Test, Domain: wear |
| 789 | macrobenchmark | wear/compose/compose-material3/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Compose, Test, Domain: wear |
| 790 | macrobenchmark-common | wear/compose/compose-material3/macrobenchmark-common | java | TODO |  | reports/macrobenchmark-common.md | Compose, Test, Domain: wear |
| 791 | macrobenchmark-target | wear/compose/compose-material3/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Compose, Test, Domain: wear |
| 792 | samples | wear/compose/compose-material3/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: wear |
| 793 | compose-navigation | wear/compose/compose-navigation | java | TODO |  | reports/compose-navigation.md | Compose, Domain: wear |
| 794 | samples | wear/compose/compose-navigation/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: wear |
| 795 | compose-ui-tooling | wear/compose/compose-ui-tooling | java | TODO |  | reports/compose-ui-tooling.md | Compose, Domain: wear |
| 796 | demos | wear/compose/integration-tests/demos | java | TODO |  | reports/demos.md | Compose, Test, Domain: wear |
| 797 | common | wear/compose/integration-tests/demos/common | java | TODO |  | reports/common.md | Compose, Test, Domain: wear |
| 798 | macrobenchmark | wear/compose/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Compose, Test, Domain: wear |
| 799 | macrobenchmark-target | wear/compose/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Compose, Test, Domain: wear |
| 800 | navigation | wear/compose/integration-tests/navigation | java | TODO |  | reports/navigation.md | Compose, Test, Domain: wear |
| 801 | profileparser | wear/compose/integration-tests/profileparser | java | TODO |  | reports/profileparser.md | Compose, Test, Domain: wear |
| 802 | remote-material3 | wear/compose/remote/remote-material3 | java | TODO |  | reports/remote-material3.md | Compose, Domain: wear |
| 803 | protolayout | wear/protolayout/protolayout | java | TODO |  | reports/protolayout.md | Domain: wear |
| 804 | protolayout-expression | wear/protolayout/protolayout-expression | java | TODO |  | reports/protolayout-expression.md | Domain: wear |
| 805 | protolayout-expression-pipeline | wear/protolayout/protolayout-expression-pipeline | java | TODO |  | reports/protolayout-expression-pipeline.md | Domain: wear |
| 806 | protolayout-external-protobuf | wear/protolayout/protolayout-external-protobuf | unknown | TODO |  | reports/protolayout-external-protobuf.md | Domain: wear |
| 807 | protolayout-lint | wear/protolayout/protolayout-lint | java | TODO |  | reports/protolayout-lint.md | Domain: wear |
| 808 | protolayout-material | wear/protolayout/protolayout-material | java | TODO |  | reports/protolayout-material.md | Domain: wear |
| 809 | protolayout-material-core | wear/protolayout/protolayout-material-core | java | TODO |  | reports/protolayout-material-core.md | Domain: wear |
| 810 | protolayout-material3 | wear/protolayout/protolayout-material3 | java | TODO |  | reports/protolayout-material3.md | Domain: wear |
| 811 | samples | wear/protolayout/protolayout-material3/samples | java | TODO |  | reports/samples.md | Sample, Domain: wear |
| 812 | protolayout-proto | wear/protolayout/protolayout-proto | java | TODO |  | reports/protolayout-proto.md | Domain: wear |
| 813 | protolayout-renderer | wear/protolayout/protolayout-renderer | java | TODO |  | reports/protolayout-renderer.md | Domain: wear |
| 814 | protolayout-testing | wear/protolayout/protolayout-testing | java | TODO |  | reports/protolayout-testing.md | Test, Domain: wear |
| 815 | tiles | wear/tiles/tiles | java | TODO |  | reports/tiles.md | Domain: wear |
| 816 | tiles-material | wear/tiles/tiles-material | java | TODO |  | reports/tiles-material.md | Domain: wear |
| 817 | tiles-proto | wear/tiles/tiles-proto | java | TODO |  | reports/tiles-proto.md | Domain: wear |
| 818 | tiles-renderer | wear/tiles/tiles-renderer | java | TODO |  | reports/tiles-renderer.md | Domain: wear |
| 819 | tiles-samples | wear/tiles/tiles-samples | java | TODO |  | reports/tiles-samples.md | Sample, Domain: wear |
| 820 | tiles-testing | wear/tiles/tiles-testing | java | TODO |  | reports/tiles-testing.md | Test, Domain: wear |
| 821 | tiles-tooling | wear/tiles/tiles-tooling | java | TODO |  | reports/tiles-tooling.md | Domain: wear |
| 822 | tiles-tooling-preview | wear/tiles/tiles-tooling-preview | java | TODO |  | reports/tiles-tooling-preview.md | Domain: wear |
| 823 | watchface-complications | wear/watchface/watchface-complications | java | TODO |  | reports/watchface-complications.md | Domain: wear |
| 824 | watchface-complications-data | wear/watchface/watchface-complications-data | java | TODO |  | reports/watchface-complications-data.md | Domain: wear |
| 825 | watchface-complications-data-source | wear/watchface/watchface-complications-data-source | java | TODO |  | reports/watchface-complications-data-source.md | Domain: wear |
| 826 | watchface-complications-data-source-ktx | wear/watchface/watchface-complications-data-source-ktx | java | TODO |  | reports/watchface-complications-data-source-ktx.md | Domain: wear |
| 827 | watchface-complications-data-source-samples | wear/watchface/watchface-complications-data-source-samples | java | TODO |  | reports/watchface-complications-data-source-samples.md | Sample, Domain: wear |
| 828 | watchface-complications-permission-dialogs-sample | wear/watchface/watchface-complications-permission-dialogs-sample | java | TODO |  | reports/watchface-complications-permission-dialogs-sample.md | Domain: wear |
| 829 | watchfacepush | wear/watchfacepush/watchfacepush | java | TODO |  | reports/watchfacepush.md | Domain: wear |
| 830 | wear | wear/wear | java | TODO |  | reports/wear.md | Domain: wear |
| 831 | wear-core | wear/wear-core | java | TODO |  | reports/wear-core.md | Domain: wear |
| 832 | wear-input | wear/wear-input | java | TODO |  | reports/wear-input.md | Domain: wear |
| 833 | wear-input-testing | wear/wear-input-testing | java | TODO |  | reports/wear-input-testing.md | Test, Domain: wear |
| 834 | samples | wear/wear-input/samples | java | TODO |  | reports/samples.md | Sample, Domain: wear |
| 835 | wear-ongoing | wear/wear-ongoing | java | TODO |  | reports/wear-ongoing.md | Domain: wear |
| 836 | wear-phone-interactions | wear/wear-phone-interactions | java | TODO |  | reports/wear-phone-interactions.md | Domain: wear |
| 837 | samples | wear/wear-phone-interactions/samples | java | TODO |  | reports/samples.md | Sample, Domain: wear |
| 838 | wear-remote-interactions | wear/wear-remote-interactions | java | TODO |  | reports/wear-remote-interactions.md | Domain: wear |
| 839 | samples | wear/wear-remote-interactions/samples | java | TODO |  | reports/samples.md | Sample, Domain: wear |
| 840 | wear-samples-ambient | wear/wear-samples-ambient | java | TODO |  | reports/wear-samples-ambient.md | Sample, Domain: wear |
| 841 | wear-tooling-preview | wear/wear-tooling-preview | java | TODO |  | reports/wear-tooling-preview.md | Domain: wear |
| 842 | instrumentation | webkit/chips-enabled-integration-tests/instrumentation | has-src | TODO |  | reports/instrumentation.md | Test, Domain: webkit |
| 843 | common | webkit/integration-tests/common | java | TODO |  | reports/common.md | Test, Domain: webkit |
| 844 | instrumentation | webkit/integration-tests/instrumentation | has-src | TODO |  | reports/instrumentation.md | Test, Domain: webkit |
| 845 | testapp | webkit/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: webkit |
| 846 | webkit | webkit/webkit | java | TODO |  | reports/webkit.md | Domain: webkit |
| 847 | core | window/extensions/core/core | java | TODO |  | reports/core.md | Domain: window |
| 848 | extensions | window/extensions/extensions | java | TODO |  | reports/extensions.md | Domain: window |
| 849 | configuration-change-tests | window/integration-tests/configuration-change-tests | java | TODO |  | reports/configuration-change-tests.md | Test, Domain: window |
| 850 | macrobenchmark | window/integration-tests/macrobenchmark | java | TODO |  | reports/macrobenchmark.md | Test, Domain: window |
| 851 | macrobenchmark-target | window/integration-tests/macrobenchmark-target | java | TODO |  | reports/macrobenchmark-target.md | Test, Domain: window |
| 852 | sidecar | window/sidecar/sidecar | java | TODO |  | reports/sidecar.md | Domain: window |
| 853 | window | window/window | java | TODO |  | reports/window.md | Domain: window |
| 854 | window-core | window/window-core | kotlin-multiplatform | TODO |  | reports/window-core.md | Domain: window |
| 855 | samples | window/window-core/samples | java | TODO |  | reports/samples.md | Sample, Domain: window |
| 856 | demo | window/window-demos/demo | java | TODO |  | reports/demo.md | Domain: window |
| 857 | demo-common | window/window-demos/demo-common | java | TODO |  | reports/demo-common.md | Domain: window |
| 858 | demo-second-app | window/window-demos/demo-second-app | java | TODO |  | reports/demo-second-app.md | Domain: window |
| 859 | window-java | window/window-java | java | TODO |  | reports/window-java.md | Domain: window |
| 860 | window-rxjava2 | window/window-rxjava2 | java | TODO |  | reports/window-rxjava2.md | Domain: window |
| 861 | window-rxjava3 | window/window-rxjava3 | java | TODO |  | reports/window-rxjava3.md | Domain: window |
| 862 | window-testing | window/window-testing | java | TODO |  | reports/window-testing.md | Test, Domain: window |
| 863 | samples | window/window/samples | java | TODO |  | reports/samples.md | Sample, Domain: window |
| 864 | testapp | work/integration-tests/testapp | java | TODO |  | reports/testapp.md | Test, Domain: work |
| 865 | work-benchmark | work/work-benchmark | has-src | TODO |  | reports/work-benchmark.md | Test, Domain: work |
| 866 | work-gcm | work/work-gcm | java | TODO |  | reports/work-gcm.md | Domain: work |
| 867 | work-inspection | work/work-inspection | java | TODO |  | reports/work-inspection.md | Domain: work |
| 868 | work-lint | work/work-lint | java | TODO |  | reports/work-lint.md | Domain: work |
| 869 | work-multiprocess | work/work-multiprocess | java | TODO |  | reports/work-multiprocess.md | Domain: work |
| 870 | work-runtime | work/work-runtime | java | TODO |  | reports/work-runtime.md | Domain: work |
| 871 | work-runtime-ktx | work/work-runtime-ktx | has-src | TODO |  | reports/work-runtime-ktx.md | Domain: work |
| 872 | work-rxjava2 | work/work-rxjava2 | java | TODO |  | reports/work-rxjava2.md | Domain: work |
| 873 | work-rxjava3 | work/work-rxjava3 | java | TODO |  | reports/work-rxjava3.md | Domain: work |
| 874 | work-testing | work/work-testing | java | TODO |  | reports/work-testing.md | Test, Domain: work |
| 875 | arcore | xr/arcore/arcore | kotlin | TODO |  | reports/arcore.md | Domain: xr |
| 876 | arcore-guava | xr/arcore/arcore-guava | kotlin | TODO |  | reports/arcore-guava.md | Domain: xr |
| 877 | arcore-openxr | xr/arcore/arcore-openxr | kotlin | TODO |  | reports/arcore-openxr.md | Domain: xr |
| 878 | arcore-play-services | xr/arcore/arcore-play-services | kotlin | TODO |  | reports/arcore-play-services.md | Domain: xr |
| 879 | arcore-projected | xr/arcore/arcore-projected | kotlin | TODO |  | reports/arcore-projected.md | Domain: xr |
| 880 | arcore-runtime | xr/arcore/arcore-runtime | kotlin | TODO |  | reports/arcore-runtime.md | Domain: xr |
| 881 | arcore-rxjava3 | xr/arcore/arcore-rxjava3 | kotlin | TODO |  | reports/arcore-rxjava3.md | Domain: xr |
| 882 | arcore-samples | xr/arcore/arcore-samples | kotlin | TODO |  | reports/arcore-samples.md | Sample, Domain: xr |
| 883 | arcore-testing | xr/arcore/arcore-testing | kotlin | TODO |  | reports/arcore-testing.md | Test, Domain: xr |
| 884 | projected-testapp | xr/arcore/integration-tests/projected-testapp | kotlin | TODO |  | reports/projected-testapp.md | Test, Domain: xr |
| 885 | testapp | xr/arcore/integration-tests/testapp | kotlin | TODO |  | reports/testapp.md | Test, Domain: xr |
| 886 | whitebox-mobile | xr/arcore/integration-tests/whitebox-mobile | kotlin | TODO |  | reports/whitebox-mobile.md | Test, Domain: xr |
| 887 | assets | xr/assets | unknown | TODO |  | reports/assets.md | Domain: xr |
| 888 | compose | xr/compose/compose | kotlin | TODO |  | reports/compose.md | Compose, Domain: xr |
| 889 | compose-testing | xr/compose/compose-testing | kotlin | TODO |  | reports/compose-testing.md | Compose, Test, Domain: xr |
| 890 | samples | xr/compose/compose/samples | java | TODO |  | reports/samples.md | Compose, Sample, Domain: xr |
| 891 | testapp | xr/compose/integration-tests/testapp | kotlin | TODO |  | reports/testapp.md | Compose, Test, Domain: xr |
| 892 | testapp | xr/compose/material3/integration-tests/testapp | java | TODO |  | reports/testapp.md | Compose, Test, Domain: xr |
| 893 | material3 | xr/compose/material3/material3 | java | TODO |  | reports/material3.md | Compose, Domain: xr |
| 894 | benchmark | xr/glimmer/benchmark | has-src | TODO |  | reports/benchmark.md | Test, Domain: xr |
| 895 | glimmer | xr/glimmer/glimmer | java | TODO |  | reports/glimmer.md | Domain: xr |
| 896 | samples | xr/glimmer/glimmer/samples | java | TODO |  | reports/samples.md | Sample, Domain: xr |
| 897 | demos | xr/glimmer/integration-tests/demos | java | TODO |  | reports/demos.md | Test, Domain: xr |
| 898 | testapp | xr/projected/integration-tests/testapp | kotlin | TODO |  | reports/testapp.md | Test, Domain: xr |
| 899 | projected | xr/projected/projected | kotlin | TODO |  | reports/projected.md | Domain: xr |
| 900 | runtime | xr/runtime/runtime | kotlin | TODO |  | reports/runtime.md | Domain: xr |
| 901 | runtime-manifest | xr/runtime/runtime-manifest | kotlin | TODO |  | reports/runtime-manifest.md | Domain: xr |
| 902 | runtime-rxjava3 | xr/runtime/runtime-rxjava3 | kotlin | TODO |  | reports/runtime-rxjava3.md | Domain: xr |
| 903 | runtime-testing | xr/runtime/runtime-testing | kotlin | TODO |  | reports/runtime-testing.md | Test, Domain: xr |
| 904 | testapp | xr/scenecore/integration-tests/testapp | kotlin | TODO |  | reports/testapp.md | Test, Domain: xr |
| 905 | videoplayerdrmtest | xr/scenecore/integration-tests/videoplayerdrmtest | kotlin | TODO |  | reports/videoplayerdrmtest.md | Test, Domain: xr |
| 906 | scenecore | xr/scenecore/scenecore | java | TODO |  | reports/scenecore.md | Domain: xr |
| 907 | scenecore-guava | xr/scenecore/scenecore-guava | java | TODO |  | reports/scenecore-guava.md | Domain: xr |
| 908 | scenecore-runtime | xr/scenecore/scenecore-runtime | kotlin | TODO |  | reports/scenecore-runtime.md | Domain: xr |
| 909 | scenecore-spatial-core | xr/scenecore/scenecore-spatial-core | java | TODO |  | reports/scenecore-spatial-core.md | Domain: xr |
| 910 | scenecore-spatial-rendering | xr/scenecore/scenecore-spatial-rendering | java | TODO |  | reports/scenecore-spatial-rendering.md | Domain: xr |
| 911 | scenecore-testing | xr/scenecore/scenecore-testing | kotlin | TODO |  | reports/scenecore-testing.md | Test, Domain: xr |