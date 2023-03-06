"""
Helper script to upload metrics files from a given build ID to skia perf. Must be run on gLinux
because it relies on the 'fetch_artifact' tool.
"""


import subprocess
import sys
import os
import shutil
import requests
import json

FETCH_ARTIFACT = "/google/data/ro/projects/android/fetch_artifact"


def fetch_artifacts(build_id, branch, target, output_dir, file_path):
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


def prep_staging_dir(staging_path):
  current_dir = os.path.dirname(os.path.realpath(__file__))
  staging_dir = current_dir + staging_path
  if os.path.isdir(staging_dir):
    shutil.rmtree(staging_dir)
  os.makedirs(staging_dir, exist_ok=True)
  return staging_dir


def post_json_to_endpoint(contents, endpoint):
  requests.post(endpoint, json=contents)


def mutate_metrics(contents, build_id, branch):
  contents["git_hash"] = build_id
  contents["key"]["branch"] = branch


def upload_metrics(download_staging_dir, build_id, branch, endpoint):
  for file_name in os.listdir(download_staging_dir):
    full_path = os.path.join(download_staging_dir, file_name)
    with open(full_path) as src_file:
      contents = json.load(src_file)
      mutate_metrics(contents, build_id, branch)
      post_json_to_endpoint(contents, endpoint)


def main(args):
  if (len(args) < 2):
    raise Exception("Please provide a build ID")
  if (len(args) < 3):
    raise Exception("Please provide an endpoint")
  build_id = args[1]
  endpoint = args[2]
  branch = "androidx-main"
  target = "androidx_multiplatform_mac_host_tests"
  file_path = "librarymetrics/**/*.json"
  download_staging_dir = prep_staging_dir("/download_staging")
  fetch_artifacts(
      build_id,
      branch,
      target,
      download_staging_dir,
      file_path
  )
  upload_metrics(download_staging_dir, build_id, branch, endpoint)


if __name__ == "__main__":
  main(sys.argv)
