#!/usr/bin/env python3
"""Exercise stable publishing with fake CLI commands; never contact GitHub."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

SCRIPT = Path(__file__).with_name("publish_stable.sh")
FAKE_CLI = '''#!/usr/bin/env python3
import json, os, pathlib, sys
args = sys.argv[1:]
with open(os.environ["CALL_LOG"], "a") as log:
    log.write(json.dumps([pathlib.Path(sys.argv[0]).name] + args) + "\\n")
if args[:1] == ["api"]:
    print(os.environ["MAIN_SHA"])
if args[:2] == ["release", "view"]:
    sys.exit(0 if os.environ["EXISTS"] == "1" else 1)
if args[:2] == ["release", "upload"] and os.environ["FAIL_UPLOAD"] == "1":
    sys.exit(1)
'''

def run_case(exists=False, stale=False, fail_upload=False, missing=False):
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        for tool in ("gh", "git"):
            path = root / tool
            path.write_text(FAKE_CLI)
            path.chmod(0o755)
        apk = root / "test.apk"
        if not missing:
            apk.write_bytes(b"test APK")
        log = root / "calls.jsonl"
        env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                   CALL_LOG=str(log), GITHUB_REPOSITORY="test/remote", GITHUB_SHA="current",
                   MAIN_SHA="newer" if stale else "current", EXISTS=str(int(exists)),
                   FAIL_UPLOAD=str(int(fail_upload)))
        result = subprocess.run(["bash", str(SCRIPT), str(apk)], cwd=root,
                                env=env, capture_output=True, text=True)
        calls = [json.loads(line) for line in log.read_text().splitlines()] if log.exists() else []
        if missing:
            assert result.returncode != 0 and not calls
        elif stale:
            assert result.returncode == 0 and len(calls) == 1
        else:
            assert (result.returncode != 0) == fail_upload
            assert ["git", "tag", "--force", "stable", "current"] in calls
            assert ["git", "push", "--force", "origin", "refs/tags/stable"] in calls
            assert (root / "build/release/Quiet-Remote.apk").read_bytes() == apk.read_bytes()
            releases = [c for c in calls if c[:2] == ["gh", "release"]]
            assert [c[2] for c in releases] == (
                ["view", "upload"] if fail_upload else ["view", "upload", "edit"] if exists
                else ["view", "create"])
            assert "--clobber" in releases[1] if exists else "--verify-tag" in releases[1]
            if not fail_upload:
                assert "--latest" in releases[-1]
                if exists:
                    assert "--draft=false" in releases[-1] and "--prerelease=false" in releases[-1]

if __name__ == "__main__":
    run_case()
    run_case(exists=True)
    run_case(stale=True)
    run_case(exists=True, fail_upload=True)
    run_case(missing=True)
    print("PASS: stable release creation, replacement, stale-build skip and failure handling")
