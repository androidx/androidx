# Copyright 2022, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import os
import subprocess
import shutil
import re
import requests
import zipfile
import sys

FETCH_ARTIFACT = "/google/data/ro/projects/android/fetch_artifact"
BASS = "/google/data/ro/projects/android/bass"
DEFAULT_CLONE_DEPTH = 100 # chosen arbitrarily, may need to be adjusted
DEFAULT_BUILD_ARTIFACT_SEARCH_TIME_SPAN_IN_DAYS = 7 # chosen arbitrarily, may need to be adjusted
GIT_LOG_URL = "https://android.googlesource.com/platform/frameworks/support/+log"
IGNORE_PATHS = [
 # includes timestamps
  "*.xml",
   # are different because the xml files include timestamps
  "*.xml.*",
  "*.sha*",
  "*.md5",
  # is different because it references the .sha* files.
  "*.module"
]


def main(build_id):
  """
  This is a script to take a given build_id, and search for a build of the previous commit on ab/
  If a commit is found, we download the top-of-tree-m2repository-all-{build_id}.zip file from both
  builds and diff the contents. If an .aar / .jar is different, we will unzip those and diff the
  contents as well.
  """
  staging_dir = prep_staging_dir()
  build_info_file_path = fetch_build_info(build_id, staging_dir)

  # presubmit BUILD_INFO files include the commit being built as well as the and parent commit
  if is_presubmit_build(build_id):
    previous_revision = get_previous_revision_from_build_info(build_info_file_path)
  # other builds only include the commit being built (as far as I can tell),  we need to
  # get the previous commit from git log
  else:
    current_revision = get_current_revision(build_info_file_path)
    previous_revision = get_previous_revision_from_git_history(current_revision, staging_dir)
  previous_build_id = get_previous_build_id(previous_revision)
  (before, after) = download_and_unzip_repos(staging_dir, build_id, previous_build_id)
  diff_repos(before, after, staging_dir)

def prep_staging_dir():
  """
  remove and recreate the ./download_staging which is located as a sibling of this script.
  """
  current_dir = os.path.dirname(os.path.realpath(__file__))
  staging_dir = current_dir + "/download_staging"
  if os.path.isdir(staging_dir):
    shutil.rmtree(staging_dir)
  os.makedirs(staging_dir, exist_ok=True)
  return staging_dir


def fetch_m2repo(build_id, staging_dir):
  file_path = f"top-of-tree-m2repository-all-{build_id}.zip"
  if is_presubmit_build(build_id):
    file_path = f"incremental/{file_path}"
  return fetch_artifact(build_id, staging_dir, file_path)


def fetch_build_info(build_id, staging_dir):
  return fetch_artifact(build_id, staging_dir, "BUILD_INFO")


def fetch_artifact(build_id, output_dir, file_path):
  file_name = file_path.split("/")[-1]
  print(f"fetching {file_name}")

  if is_presubmit_build(build_id):
    target = "androidx_incremental"
  else:
    target = "androidx"
  return FetchArtifactService().fetch_artifact(build_id, "aosp-androidx-main", target, output_dir, file_path)


def get_current_revision(build_info_file_path):
  print("Getting current revision from BUILD_INFO")
  with open(build_info_file_path) as f:
    build_info = json.load(f)
    support_project = next(
        project for project in build_info["parsed_manifest"]["projects"] if
        project["name"] == "platform/frameworks/support")
    current_revision = support_project["revision"]
    print(f"Found revision: {current_revision}")
    return current_revision


def get_previous_revision_from_build_info(build_info_file_path):
  print("Getting previous revision from BUILD_INFO")
  with open(build_info_file_path) as f:
    build_info = json.load(f)
    revision = build_info["git-pull"][0]["revisions"][0]["commit"]["parents"][0]["commitId"]
    print(f"Found previous revision: {revision}")
    return revision

def get_previous_revision_from_git_history(current_revision, staging_dir):
  """
  Gets previous revision from git log endpoint for androidx-main.
  """
  response = requests.get(git_log_url(current_revision))
  # endpoint returns  some junk in the first line making it invalid json
  text_with_first_line_removed = "\n".join(response.text.split("\n")[1:])
  response_json = json.loads(text_with_first_line_removed)
  previous_revision = response_json["log"][0]["parents"][0]
  print(f"Found previous revision: {previous_revision}")
  return previous_revision


def get_previous_build_id(previous_revision):
  print("Searching Android Build server for build matching previous revision")
  output = BassService().search_builds(
    DEFAULT_BUILD_ARTIFACT_SEARCH_TIME_SPAN_IN_DAYS,
    "aosp-androidx-main",
    "androidx",
    "BUILD_INFO",
    previous_revision
  )
  match = re.search("BuildID\: (\d+)", output.stdout)
  if match is None:
    raise Exception(f"Couldn't find previous build ID for revision {previous_revision}")

  previous_build_id = match.group(1)
  print(f"Found build matching previous revision: {previous_build_id}")
  return previous_build_id

def download_and_unzip_repos(staging_dir, build_id, previous_build_id):
  before_dir = staging_dir + "/before"
  after_dir = staging_dir + "/after"
  os.makedirs(before_dir)
  os.makedirs(after_dir)
  after_zip = fetch_m2repo(build_id, staging_dir)
  before_zip = fetch_m2repo(previous_build_id, staging_dir)
  return (unzip(before_zip, before_dir), unzip(after_zip, after_dir))

def diff_repos(before, after, staging_dir):
  output = DiffService().diff(before, after, IGNORE_PATHS)
  for line in output.stdout.splitlines():
    if line.startswith("Binary files "):
      for (before_file, after_file) in re.findall("Binary files (.+) and (.+) differ", line):
        diff_binary(before_file, after_file, staging_dir)
    else:
      print(line)


def diff_binary(before, after, staging_dir):
  file_name = before.split("/")[-1]
  if is_unzippable(before) and is_unzippable(after):
    before_contents = unzip(before, staging_dir + "/" + file_name + "-before")
    after_contents = unzip(after, staging_dir + "/" + file_name + "-after")
    output = DiffService().diff(before_contents, after_contents)
    # sometimes the binary is "different" but the contents are identical.
    # It might be interesting to add diff the metadata, but for now just ignore it.
    if output.stdout.strip() != "":
      print(output.stdout)
  else:
    print(f"Binary files {before} and {after} differ")

def is_unzippable(filename):
  return filename.endswith(".zip") or filename.endswith(".aar") or filename.endswith(".jar")

def unzip(file, destination):
  with zipfile.ZipFile(file, 'r') as zip:
    zip.extractall(destination)
    return destination

def is_presubmit_build(build_id):
    return build_id.startswith("P")

def git_log_url(revision):
    return f"{GIT_LOG_URL}/{revision}?format=JSON"

class DiffService():
    @staticmethod
    def diff(before_dir, after_dir, exclude=[]):
      args = ["diff", "-r"]
      for pattern in exclude:
        args.extend(["-x", pattern])
      args.extend([before_dir, after_dir])
      return subprocess.run(args, text=True, capture_output=True)


class FetchArtifactService():
    @staticmethod
    def fetch_artifact(build_id, branch, target, output_dir, file_path):
      file_name = file_path.split("/")[-1]
      subprocess.run(
        [
          FETCH_ARTIFACT,
          "--bid",
          build_id,
          "--branch",
          branch,
          "--target",
          target,
          file_path,
        ],
        cwd=output_dir,
        capture_output=True,
        check=True
      )
      return f"{output_dir}/{file_name}"

class BassService():
    @staticmethod
    def search_builds(days, branch, target, file_name, query):
     return subprocess.run([
          BASS,
          "--days",
          str(days),
          "--successful",
          "true",
          "--branch",
          branch,
          "--target",
          target,
          "--filename",
          file_name,
          "--query",
          query
      ],
          capture_output=True,
          text=True,
          check=True
      )

if __name__ == "__main__":
    main(sys.argv[1])