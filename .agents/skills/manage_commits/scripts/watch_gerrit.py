#!/usr/bin/env python3
"""
Watch Gerrit CL presubmits.

This script will:
1. Resolve the Change-Id from your local HEAD commit log (or positional CL argument).
2. Query Gerrit for the CL details using gob-curl (or standard curl as a fallback).
3. Detect if presubmits are running. If they are not running and `Presubmit-Ready` is not set:
   - Prompts the user interactively to trigger presubmits if in a TTY.
   - Automatically triggers them if --trigger is passed.
   - Skips triggering if --no-trigger is passed or in a non-TTY environment.
4. Poll the build status periodically and display progress.
5. Exit with status code 0 on success (Presubmit-Verified+1) or 1 on failure (Presubmit-Verified-1).
6. Terminate with status code 2 if a new patchset is uploaded to the CL (superseded).
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time

# Cache for resolved curl command prefix to avoid running subprocess on every request
_curl_command_cache = None


def get_curl_command():
    global _curl_command_cache
    if _curl_command_cache is not None:
        return _curl_command_cache

    # 1. Check if gob-curl is available
    if shutil.which("gob-curl"):
        _curl_command_cache = ["gob-curl"]
        return _curl_command_cache

    # 2. Fall back to standard curl
    print(
        "[WARNING] 'gob-curl' not found. Falling back to standard 'curl' with git config / netrc auth...",
        file=sys.stderr,
    )
    cmd = ["curl", "--netrc"]

    # Try to load cookie file from git config
    try:
        cookie_file = subprocess.run(
            ["git", "config", "--get", "http.cookiefile"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        ).stdout.strip()
        if cookie_file:
            cookie_file = os.path.expanduser(cookie_file)
            cmd.extend(["--cookie", cookie_file])
    except Exception:
        pass

    _curl_command_cache = cmd
    return _curl_command_cache


def run_gob_curl(url, method="GET", data=None):
    curl_base = get_curl_command()
    cmd = list(curl_base) + ["-s", "-X", method]

    if data:
        cmd.extend(
            ["-H", "Content-Type: application/json", "-d", json.dumps(data)]
        )
    cmd.append(url)

    result = subprocess.run(
        cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
    )
    if result.returncode != 0:
        print(f"Error running curl: {result.stderr}", file=sys.stderr)
        return None

    output = result.stdout
    # Strip Gerrit magic prefix
    if output.startswith(")]}'\n"):
        output = output[5:]
    elif output.startswith(")]}'"):
        output = output[4:]

    try:
        return json.loads(output)
    except json.JSONDecodeError:
        if (
            "Authentication Required" in output
            or "Sign In" in output
            or "Unauthorized" in output
        ):
            print(
                "[ERROR] Authentication failed. If gob-curl is missing, ensure you are authenticated in git or ~/.netrc.",
                file=sys.stderr,
            )
        else:
            print(
                f"Failed to parse JSON response. Raw output:\n{output}",
                file=sys.stderr,
            )
        return None


def get_change_id_from_git():
    try:
        result = subprocess.run(
            ["git", "log", "-n", "1"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True,
        )
        match = re.search(r"Change-Id:\s*(I[a-f0-9]+)", result.stdout)
        if match:
            return match.group(1)
    except Exception as e:
        print(f"Failed to read Change-Id from git log: {e}", file=sys.stderr)
    return None


def resolve_cl_number(change_id):
    url = f"https://android-review.googlesource.com/changes/?q=change:{change_id}"
    res = run_gob_curl(url)
    if res and len(res) > 0:
        return res[0].get("_number")
    return None


def ask_yes_no(prompt, default="no"):
    valid = {"yes": True, "y": True, "ye": True, "no": False, "n": False}
    if default == "yes":
        prompt_suffix = " [Y/n] "
    else:
        prompt_suffix = " [y/N] "

    while True:
        sys.stdout.write(prompt + prompt_suffix)
        sys.stdout.flush()
        choice = sys.stdin.readline().strip().lower()
        if choice == "":
            return valid[default]
        elif choice in valid:
            return valid[choice]
        else:
            sys.stdout.write(
                "Please respond with 'yes' or 'no' (or 'y' or 'n').\n"
            )


def trigger_presubmit(cl_number):
    print(
        f"Triggering presubmit for CL {cl_number} (setting Presubmit-Ready+1)..."
    )
    url = f"https://android-review.googlesource.com/a/changes/{cl_number}/revisions/current/review"
    res = run_gob_curl(
        url, method="POST", data={"labels": {"Presubmit-Ready": 1}}
    )
    if res:
        print("Successfully triggered presubmits.")
        return True
    else:
        print("Failed to trigger presubmits.", file=sys.stderr)
        return False


def monitor_cl(
    cl_number,
    target_revision=None,
    poll_interval=180,
    auto_trigger=False,
    no_trigger=False,
    watch_comments=True,
):
    print(f"Starting monitor for CL {cl_number}...")

    triggered_this_run = False
    seen_messages = set()
    is_first_run = True
    finished_printed_for_revisions = set()

    while True:
        url = f"https://android-review.googlesource.com/changes/{cl_number}/detail"
        data = run_gob_curl(url)
        if not data:
            print(
                "Failed to retrieve CL details. Retrying in next loop...",
                file=sys.stderr,
            )
            time.sleep(poll_interval)
            continue

        current_rev = data.get("current_revision_number")
        if not current_rev:
            print("Could not determine current revision. Exiting.", file=sys.stderr)
            sys.exit(1)

        if target_revision is None:
            target_revision = current_rev
            print(
                f"No target revision specified. Watching latest Patch Set: {target_revision}"
            )

        if current_rev > target_revision:
            if watch_comments:
                print(
                    f"\n[{time.strftime('%H:%M:%S')}] [INFO] New Patch Set {current_rev} detected (superseding Patch Set {target_revision})."
                )
                print(f"Now watching Patch Set {current_rev} for comments and presubmits.")
                target_revision = current_rev
                triggered_this_run = False
            else:
                print(
                    f"\n[WARNING] Superseded! Current Patch Set is {current_rev}, but we were watching {target_revision}."
                )
                print("Exiting watch.")
                sys.exit(2)

        messages = data.get("messages", [])

        # Fetch inline comments from /comments endpoint
        comments_url = (
            f"https://android-review.googlesource.com/changes/{cl_number}/comments"
        )
        inline_comments_data = run_gob_curl(comments_url) or {}
        inline_comments_list = []
        for file_path, comment_objs in inline_comments_data.items():
            for c_obj in comment_objs:
                c_id = c_obj.get("id")
                if c_id:
                    author_info = c_obj.get("author", {})
                    author = (
                        author_info.get("display_name")
                        or author_info.get("name")
                        or "Reviewer"
                    )
                    date = c_obj.get("updated") or c_obj.get("written")
                    line = c_obj.get("line", 0)
                    msg_text = c_obj.get("message", "")
                    line_str = f":L{line}" if line else ""
                    inline_comments_list.append({
                        "id": c_id,
                        "date": date,
                        "author": author,
                        "message": (
                            f"[Inline comment on {file_path}{line_str}]\n"
                            f"  {msg_text}"
                        ),
                    })

        combined_items = []
        for m in messages:
            msg_id = m.get("id")
            if msg_id:
                author_info = m.get("author", {})
                author = (
                    author_info.get("display_name")
                    or author_info.get("name")
                    or "System"
                )
                date = m.get("date")
                text = m.get("message", "")
                combined_items.append({
                    "id": msg_id,
                    "date": date,
                    "author": author,
                    "message": text,
                })

        for ic in inline_comments_list:
            combined_items.append(ic)

        combined_items.sort(key=lambda x: x.get("date") or "")

        # Print new messages and inline comments
        if is_first_run:
            if watch_comments:
                print("\n--- Existing Comments/Messages on CL ---")
                for item in combined_items:
                    item_id = item.get("id")
                    if item_id:
                        author = item.get("author")
                        date = item.get("date")
                        text = item.get("message", "")
                        print(f"\n[{date}] {author}:")
                        for line in text.splitlines():
                            print(f"  {line}")
                        print("-" * 50)
                        seen_messages.add(item_id)
            else:
                seen_messages = {
                    item.get("id") for item in combined_items if item.get("id")
                }
            is_first_run = False
        else:
            for item in combined_items:
                item_id = item.get("id")
                if item_id and item_id not in seen_messages:
                    author = item.get("author")
                    date = item.get("date")
                    text = item.get("message", "")
                    print(f"\n[{date}] {author}:")
                    for line in text.splitlines():
                        print(f"  {line}")
                    print("-" * 50)
                    seen_messages.add(item_id)

                    print(f"\n[{time.strftime('%H:%M:%S')}] New message from {author} ({date}):")
                    for line in text.splitlines():
                        print(f"  {line}")
                    print("-" * 60)

                    seen_messages.add(msg_id)

        # Scan for presubmit started and finished comments for this revision
        started_msg = None
        finished_msg = None

        for m in messages:
            rev = m.get("_revision_number")
            if rev != target_revision:
                continue

            msg_text = m.get("message", "")
            if "## Presubmit started:" in msg_text:
                started_msg = msg_text
            elif "## Presubmit finished:" in msg_text:
                finished_msg = msg_text

        # Check label value
        labels = data.get("labels", {})
        presubmit_verified = labels.get("Presubmit-Verified", {})
        # Note: Presubmit-Verified might contain votes, check if approved (+1) or rejected (-1)
        # It has "approved" (account detail) or "rejected"
        verified_value = 0
        if "approved" in presubmit_verified:
            verified_value = 1
        elif "rejected" in presubmit_verified:
            verified_value = -1

        presubmit_ready = labels.get("Presubmit-Ready", {})
        # Presubmit-Ready+1 trigger state
        ready_value = 0
        if "approved" in presubmit_ready:
            ready_value = 1

        # Parse Workplan / details from started message
        workplan_id = "Unknown"
        if started_msg:
            wp_match = re.search(r"Presubmit started:\s*(\S+)", started_msg)
            if wp_match:
                workplan_id = wp_match.group(1)

        # Decision matrix
        if target_revision in finished_printed_for_revisions:
            pass
        elif finished_msg:
            print(f"\nPresubmits finished for Patch Set {target_revision}!")
            summary_lines = [
                line.strip() for line in finished_msg.split("\n") if line.strip()
            ]
            for line in summary_lines[:3]:  # print first few lines of summary
                print(f"  {line}")

            if verified_value == 1:
                print("\nSTATUS: SUCCESS (Presubmit-Verified+1)")
            elif verified_value == -1:
                print("\nSTATUS: FAILED (Presubmit-Verified-1)")
            else:
                print(
                    f"\nSTATUS: FINISHED (No Verified vote cast yet, check results in Gerrit)"
                )

            finished_printed_for_revisions.add(target_revision)

            if not watch_comments:
                if verified_value == 1:
                    sys.exit(0)
                elif verified_value == -1:
                    sys.exit(1)
                else:
                    sys.exit(0)

        elif started_msg:
            print(
                f"[{time.strftime('%H:%M:%S')}] Presubmits are running... (Workplan: {workplan_id})"
            )

        else:
            # Not started and not finished
            if ready_value == 1:
                print(
                    f"[{time.strftime('%H:%M:%S')}] Presubmit-Ready label is set. Waiting for builders to start..."
                )
            else:
                if not triggered_this_run:
                    print(
                        f"[{time.strftime('%H:%M:%S')}] Presubmit not running and Presubmit-Ready is not set."
                    )

                    should_trigger = False
                    if auto_trigger:
                        should_trigger = True
                    elif no_trigger:
                        should_trigger = False
                    elif sys.stdin.isatty():
                        should_trigger = ask_yes_no(
                            "Would you like to trigger presubmits now?"
                        )
                    else:
                        print(
                            f"[{time.strftime('%H:%M:%S')}] Non-interactive shell and auto-trigger not enabled. Not triggering."
                        )

                    if should_trigger:
                        success = trigger_presubmit(cl_number)
                        if success:
                            triggered_this_run = True
                    else:
                        # Mark as triggered_this_run to avoid prompting again in subsequent loops
                        triggered_this_run = True
                else:
                    print(
                        f"[{time.strftime('%H:%M:%S')}] Waiting for presubmit trigger to be processed..."
                    )

        status = data.get("status")
        if status in ("MERGED", "ABANDONED"):
            print(f"\n[{time.strftime('%H:%M:%S')}] CL status is {status}. Exiting.")
            sys.exit(0)

        sys.stdout.flush()
        time.sleep(poll_interval)


def main():
    parser = argparse.ArgumentParser(description="Watch Gerrit CL presubmits.")
    parser.add_argument(
        "cl",
        nargs="?",
        help="Numeric CL number or Change-Id (defaults to HEAD commit's Change-Id)",
    )
    parser.add_argument(
        "-p", "--patchset", type=int, help="Target Patch Set to watch (defaults to current)"
    )
    parser.add_argument(
        "-i",
        "--interval",
        type=int,
        default=180,
        help="Polling interval in seconds (default 180)",
    )
    parser.add_argument(
        "--trigger",
        action="store_true",
        help="Automatically trigger presubmits if not running",
    )
    parser.add_argument(
        "--no-trigger",
        action="store_true",
        help="Never trigger presubmits, only monitor",
    )
    parser.add_argument(
        "-c",
        "--comments",
        action="store_true",
        default=True,
        help="Watch for review comments (enabled by default)",
    )
    parser.add_argument(
        "--no-comments",
        action="store_false",
        dest="comments",
        help="Disable watching review comments",
    )
    args = parser.parse_args()

    cl_input = args.cl
    if not cl_input:
        print("No CL or Change-Id provided. Reading Change-Id from HEAD commit log...")
        cl_input = get_change_id_from_git()
        if not cl_input:
            print(
                "Error: Could not find Change-Id in git history. Please specify CL number.",
                file=sys.stderr,
            )
            sys.exit(1)
        print(f"Found Change-Id: {cl_input}")

    # Resolve to CL number if Change-Id is passed
    if isinstance(cl_input, str) and cl_input.startswith("I"):
        cl_number = resolve_cl_number(cl_input)
        if not cl_number:
            print(
                f"Error: Could not resolve Change-Id {cl_input} to a Gerrit CL number.",
                file=sys.stderr,
            )
            sys.exit(1)
        print(f"Resolved to CL number: {cl_number}")
    else:
        try:
            cl_number = int(cl_input)
        except ValueError:
            print(f"Error: Invalid CL input {cl_input}", file=sys.stderr)
            sys.exit(1)

    try:
        monitor_cl(
            cl_number,
            target_revision=args.patchset,
            poll_interval=args.interval,
            auto_trigger=args.trigger,
            no_trigger=args.no_trigger,
            watch_comments=args.comments,
        )
    except KeyboardInterrupt:
        print("\nMonitoring stopped by user.")
        sys.exit(0)


if __name__ == "__main__":
    main()
