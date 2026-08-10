#!/usr/bin/env python3
from __future__ import annotations

import dataclasses
import json
import os
from pathlib import Path
import sys
from typing import Any

import common
from common import WorkspaceContext


def log(msg: str) -> None:
    """Logs diagnostic messages for session setup to stderr."""
    common.logger.info(f"[cl_session_setup] {msg}")


def get_default_support_root() -> Path:
    """Returns absolute path to frameworks/support relative to this script."""
    return Path(__file__).resolve().parent.parent.parent


def handle_sync_subcommand(
    pos_args: list[str],
    provider: common.WorkspaceProvider,
    env: common.HostEnvironment,
) -> None:
    """Handles positionals for 'sync' subcommand."""
    clean = False
    if "--clean" in pos_args:
        clean = True
        pos_args = [arg for arg in pos_args if arg != "--clean"]

    if not pos_args:
        sys.stderr.write("Usage: cl_session_setup.py sync [--clean] <target_ref|conversation_id> [conversation_id] [workspace_root]\n")
        sys.exit(1)

    default_ws = get_default_support_root()

    if len(pos_args) == 1:
        target_ref, conv_id, ws_root = "HEAD", pos_args[0], default_ws
    elif len(pos_args) == 2:
        if Path(pos_args[1]).is_dir():
            target_ref, conv_id, ws_root = "HEAD", pos_args[0], Path(pos_args[1])
        else:
            target_ref, conv_id, ws_root = pos_args[0], pos_args[1], default_ws
    else:
        target_ref, conv_id, ws_root = pos_args[0], pos_args[1], Path(pos_args[2])

    ctx = WorkspaceContext.from_root(ws_root, conv_id, env)
    success = provider.sync_workspace_to_ref(ctx, ws_root, target_ref, clean=clean)
    sys.exit(0 if success else 1)


def handle_cleanup_subcommand(
    conv_id: str,
    provider: common.WorkspaceProvider,
    env: common.HostEnvironment,
) -> None:
    """Handles 'cleanup' subcommand."""
    root = get_default_support_root()
    ctx = WorkspaceContext.from_root(root, conv_id, env)
    provider.cleanup_workspace(ctx)


def _make_success_response(provider: common.WorkspaceProvider, ctx: WorkspaceContext, workspace_root: Path) -> dict[str, Any]:
    """Formats the harness step injection response for a successfully created workspace."""
    cl_dir = provider.get_cl_dir(ctx)
    msg = (
        f"An isolated workspace has been created for this session at {cl_dir}. "
        "You MUST perform all file operations, builds, and tests in this new workspace path "
        f"instead of the default one ({workspace_root})."
    )
    return {"injectSteps": [{"systemMessage": {"systemMessage": msg}}]}


def _provision_and_sync(
    provider: common.WorkspaceProvider,
    ctx: WorkspaceContext,
    workspace_root: Path,
    target_sha: str,
) -> bool:
    """Helper to check/create and sync workspace for a provider and context."""
    if provider.workspace_exists(ctx) or provider.create_workspace(ctx, target_sha):
        log(f"Workspace ready at {provider.get_cl_dir(ctx)}")
        provider.sync_workspace_to_ref(ctx, workspace_root, target_sha)
        return True
    return False


def process_session_request(
    input_data: dict[str, Any],
    provider: common.WorkspaceProvider,
    env: common.HostEnvironment,
) -> dict[str, Any]:
    """Pure, dependency-injected core logic for session setup requests."""
    conversation_id = input_data.get("conversationId", "unknown")
    workspace_paths = input_data.get("workspacePaths", [])

    if not workspace_paths:
        log("No workspace paths provided.")
        return {}

    workspace_root = Path(workspace_paths[0])
    ctx = WorkspaceContext.from_root(workspace_root, conversation_id, env)
    active_provider = provider if provider is not None else common.get_workspace_provider(workspace_root, env)

    if common.is_already_isolated_workspace(ctx.support_root):
        log("Already in an isolated workspace.")
        active_provider.ensure_workspace_ready(ctx)
        return {}

    target_sha = (
        input_data.get("baseRef")
        or input_data.get("baseCommit")
        or (common.DEFAULT_TARGET_BRANCH if input_data.get("freshUpstream") or input_data.get("clean") else "HEAD")
    )

    if _provision_and_sync(active_provider, ctx, workspace_root, target_sha):
        return _make_success_response(active_provider, ctx, workspace_root)

    if isinstance(active_provider, common.CogWorkspaceProvider):
        log("CoG workspace creation failed. Falling back to LocalWorktreeProvider...")
        fallback_provider = common.LocalWorktreeProvider()
        fallback_ctx = dataclasses.replace(ctx, is_cog_workspace=False)
        if _provision_and_sync(fallback_provider, fallback_ctx, workspace_root, target_sha):
            return _make_success_response(fallback_provider, fallback_ctx, workspace_root)

    log("Failed to create workspace.")
    return {"error": "Failed to create workspace"}


def main(
    stdin: TextIO = sys.stdin,
    stdout: TextIO = sys.stdout,
    environ: Mapping[str, str] = os.environ,
) -> None:
    if len(sys.argv) > 1:
        cmd = sys.argv[1]
        default_root = get_default_support_root()
        env = common.HostEnvironment.detect(default_root, environ=environ)
        provider = common.get_workspace_provider(default_root, env)
        if cmd == "sync":
            handle_sync_subcommand(sys.argv[2:], provider=provider, env=env)
            return
        elif cmd == "cleanup":
            if len(sys.argv) > 2:
                handle_cleanup_subcommand(sys.argv[2], provider=provider, env=env)
                return
            else:
                sys.stderr.write("Usage: cl_session_setup.py cleanup <conversation_id>\n")
                sys.exit(1)

    try:
        input_data = json.load(stdin)
    except (json.JSONDecodeError, OSError, KeyError, TypeError) as e:
        log(f"Failed to parse JSON input: {e}")
        stdout.write(json.dumps({}) + "\n")
        return

    workspace_paths = input_data.get("workspacePaths", [])
    root = Path(workspace_paths[0]) if workspace_paths else get_default_support_root()
    env = common.HostEnvironment.detect(root, environ=environ)
    provider = common.get_workspace_provider(root, env)

    result = process_session_request(input_data, provider=provider, env=env)
    if "error" in result:
        stdout.write(json.dumps(result) + "\n")
        sys.exit(1)
    stdout.write(json.dumps(result) + "\n")


if __name__ == "__main__":
    main()
