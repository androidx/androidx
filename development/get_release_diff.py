#!/usr/bin/env python3

"""
A script that takes in a maven coordinate of an androidx library, such as
androidx.collection:collection and two versions such as 1.5.0 1.6.0-alpha01 and returns two links,
one to the source diff between the two releases and another to the list of commits between the two
releases.
"""

import sys
import requests

def get_json(url):
    response = requests.get(url)
    response.raise_for_status()
    return response.json()

def main(coord, version_1, version_2):
    group, artifact = coord.split(':')

    # 1. Get Build IDs from Gradle Metadata
    def get_build_id(v):
        base_url = "https://dl.google.com/android/maven2"
        url = f"{base_url}/{group.replace('.', '/')}/{artifact}/{v}/{artifact}-{v}.module"
        module_data = get_json(url)
        gradle_metadata = module_data.get('createdBy', {}).get('gradle', {})
        build_id = gradle_metadata.get('buildId') or gradle_metadata.get('buildId:')
        if not build_id:
            print(f"Error: Could not find buildId for {coord}:{v}. Only relatively new AndroidX artifact embed build id.")
            sys.exit(1)
        return build_id

    build_id_1 = get_build_id(version_1)
    build_id_2 = get_build_id(version_2)

    # 2. Get Commit SHAs from BUILD_INFO
    def get_sha(bid):
        url = f"https://ci.android.com/builds/submitted/{bid}/androidx/latest/raw/BUILD_INFO"
        return get_json(url)['repo-dict']['platform/frameworks/support']

    sha1 = get_sha(build_id_1)
    sha2 = get_sha(build_id_2)

    # 3. Construct Link
    # Drops "androidx.", replaces all "." and ":" with "/"
    suffix = coord.replace('androidx.', '').replace('.', '/').replace(':', '/')
    print(f"Diff: https://android.googlesource.com/platform/frameworks/support/+/{sha1}..{sha2}/{suffix}")
    print(f"Commits: https://android.googlesource.com/platform/frameworks/support/+log/{sha1}..{sha2}/{suffix}")

if __name__ == '__main__':
    if len(sys.argv) != 4:
        print("Usage: get_release_diff.py <coordinate> <version1> <version2>")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2], sys.argv[3])