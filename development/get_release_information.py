#!/usr/bin/env python3

"""
A script that takes in a maven coordinate of an androidx library, such as
androidx.collection:collection:1.5.0 and returns two links, one to the build page and another to the
source tree at the state of the release.
"""

import sys
import requests

def get_artifact_links(coordinate):
    try:
        group, artifact, version = coordinate.split(':')

        # 1. Get Module Metadata URL and fetch JSON
        base_url = "https://dl.google.com/android/maven2"
        module_url = f"{base_url}/{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.module"
        module_data = requests.get(module_url).json()

        # 2. Extract buildId
        # Note: Uses .get() to handle potential 'buildId' vs 'buildId:' variations
        gradle_metadata = module_data.get('createdBy', {}).get('gradle', {})
        build_id = gradle_metadata.get('buildId') or gradle_metadata.get('buildId:')

        if not build_id:
            print(f"Error: Could not find buildId for {coordinate}. Only relatively new AndroidX artifact embed build id.")
            return

        build_link = f"https://ci.android.com/builds/submitted/{build_id}/androidx/latest"

        # 3. Fetch BUILD_INFO and extract SHA
        build_info_url = f"{build_link}/raw/BUILD_INFO"
        build_info = requests.get(build_info_url).json()
        sha = build_info.get('repo-dict', {}).get('platform/frameworks/support')

        if not sha:
             print(f"Build Link: {build_link}")
             print("Error: Could not find platform/frameworks/support SHA")
             return

        source_link = f"https://cs.android.com/androidx/platform/frameworks/support/+/{sha}:"

        print(f"Build Page: {build_link}")
        print(f"Source Tree: {source_link}")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        get_artifact_links(sys.argv[1])
    else:
        print("Usage: get_release_information.py <maven_coordinate>")
