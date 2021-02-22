#!/usr/bin/python3

import argparse
import os
import shutil



SOONG_SUFFIX = '/android_common/javac'
VERSION = '0.1.0'


def main():
    parser = argparse.ArgumentParser(description='Copy Crystalball Prebuilts')
    parser.add_argument('soong_path', action="store", help='Looks like "/usr/local/google/home/rahulrav/Workspace/internal_android/out/soong/.intermediates"')
    parser.add_argument('prebuilts_directory', action="store", help='Looks like "/mnt/Android/Flatfoot/androidx_main/prebuilts/androidx/external/com/android/"')
    parser.add_argument('--verbose', action="store_true", default=False)
    parse_result = parser.parse_args()

    soong_path = parse_result.soong_path
    soong_output_directory = f'{soong_path}/platform_testing/libraries/'
    prebuilts_directory = parse_result.prebuilts_directory

    mapping = dict([
        # Device Collectors
        (f'{soong_output_directory}/device-collectors/src/main/collector-device-lib/{SOONG_SUFFIX}/collector-device-lib.jar', f'{prebuilts_directory}/collector-device-lib/{VERSION}/collector-device-lib-{VERSION}.jar'),
        # Collectors Device Lib Platform
        (f'{soong_output_directory}/device-collectors/src/main/platform-collectors/collector-device-lib-platform/{SOONG_SUFFIX}/collector-device-lib-platform.jar', f'{prebuilts_directory}/collector-device-lib-platform/{VERSION}/collector-device-lib-platform-{VERSION}.jar'),
        # Collector Helpers
        (f'{soong_output_directory}/collectors-helper/utilities/collector-helper-utilities/{SOONG_SUFFIX}/collector-helper-utilities.jar', f'{prebuilts_directory}/collector-helper-utilities/{VERSION}/collector-helper-utilities-{VERSION}.jar'),
        (f'{soong_output_directory}/collectors-helper/jank/jank-helper/{SOONG_SUFFIX}/jank-helper.jar', f'{prebuilts_directory}/jank-helper/{VERSION}/jank-helper-{VERSION}.jar'),
        (f'{soong_output_directory}/collectors-helper/memory/memory-helper/{SOONG_SUFFIX}/memory-helper.jar', f'{prebuilts_directory}/memory-helper/{VERSION}/memory-helper-{VERSION}.jar'),
        # Microbenchmarks
        (f'{soong_output_directory}/health/runners/microbenchmark/microbenchmark-device-lib/{SOONG_SUFFIX}/microbenchmark-device-lib.jar', f'{prebuilts_directory}/microbenchmark-device-lib/{VERSION}/microbenchmark-device-lib-{VERSION}.jar'),
        # Perfetto Helper
        (f'{soong_output_directory}/collectors-helper/perfetto/perfetto-helper/{SOONG_SUFFIX}/perfetto-helper.jar', f'{prebuilts_directory}/perfetto-helper/{VERSION}/perfetto-helper-{VERSION}.jar'),
        # Platform Protos
        (f'{soong_path}/frameworks/base/platformprotoslite/{SOONG_SUFFIX}/platformprotoslite.jar', f'{prebuilts_directory}/platformprotoslite/{VERSION}/platformprotoslite-{VERSION}.jar'),
        (f'{soong_path}/frameworks/base/platformprotosnano/{SOONG_SUFFIX}/platformprotosnano.jar', f'{prebuilts_directory}/platformprotosnano/{VERSION}/platformprotosnano-{VERSION}.jar'),
        # Platform Test Composers
        (f'{soong_output_directory}/health/composers/platform/platform-test-composers/{SOONG_SUFFIX}/platform-test-composers.jar', f'{prebuilts_directory}/platform-test-composers/{VERSION}/platform-test-composers-{VERSION}.jar'),
        # Platform Test Rules
        (f'{soong_output_directory}/health/rules/platform-test-rules/{SOONG_SUFFIX}/platform-test-rules.jar', f'{prebuilts_directory}/platform-test-rules/{VERSION}/platform-test-rules-{VERSION}.jar'),
        # Power Helper
        (f'{soong_output_directory}/collectors-helper/power/power-helper/{SOONG_SUFFIX}/power-helper.jar', f'{prebuilts_directory}/power-helper/{VERSION}/power-helper-{VERSION}.jar'),
        # Simple Perf Helper
        (f'{soong_output_directory}/collectors-helper/simpleperf/simpleperf-helper/{SOONG_SUFFIX}/simpleperf-helper.jar', f'{prebuilts_directory}/simpleperf-helper/{VERSION}/simpleperf-helper-{VERSION}.jar'),
        # Statsd Helper
        (f'{soong_output_directory}/collectors-helper/statsd/statsd-helper/{SOONG_SUFFIX}/statsd-helper.jar', f'{prebuilts_directory}/statsd-helper/{VERSION}/statsd-helper-{VERSION}.jar'),
        # Statsd Protos
        (f'{soong_path}/frameworks/base/cmds/statsd/statsdprotonano/{SOONG_SUFFIX}/statsdprotonano.jar', f'{prebuilts_directory}/statsdprotonano/{VERSION}/statsdprotonano-{VERSION}.jar'),
        # System Metric Helpers
        (f'{soong_output_directory}/collectors-helper/system/system-metric-helper/{SOONG_SUFFIX}/system-metric-helper.jar', f'{prebuilts_directory}/system-metric-helper/{VERSION}/system-metric-helper-{VERSION}.jar'),
        # Test Composers
        (f'{soong_output_directory}/health/composers/host/test-composers/{SOONG_SUFFIX}/test-composers.jar', f'{prebuilts_directory}/test-composers/{VERSION}/test-composers-{VERSION}.jar'),
    ])

    size = len(mapping)
    if parse_result.verbose:
        print(f'Total number of entries = {size}')

    for key in mapping:
        if not os.path.exists(key):
            print(f'Artifact at {key} does not exist. You may have forgotten to build the necessary artifacts.')

        if os.path.exists(mapping[key]) and parse_result.verbose:
            print(f'Artifact at {mapping[key]} will be overwritten')

        shutil.copyfile(key, mapping[key])

        if parse_result.verbose:
            print(f'Copied from {key} to {mapping[key]}\n')

    print('All done.')

if __name__ == "__main__":
    main()
