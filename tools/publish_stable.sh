#!/usr/bin/env bash
set -euo pipefail

apk="${1:?APK path required}"
test -s "$apk"

# Ignore reruns of old commits after main has advanced.
latest_commit=$(gh api "repos/$GITHUB_REPOSITORY/git/ref/heads/main" --jq '.object.sha')
if [[ "$latest_commit" != "$GITHUB_SHA" ]]; then
  echo "Skipping stable publication: this build is no longer the current main commit."
  exit 0
fi

mkdir -p build/release
cp "$apk" build/release/Quiet-Remote.apk
cat > build/release/notes.md <<EOF
Latest successful build from main.

Commit: $GITHUB_SHA

Download Quiet-Remote.apk and install it over your existing Quiet Remote app.
EOF

git tag --force stable "$GITHUB_SHA"
git push --force origin refs/tags/stable

if gh release view stable --repo "$GITHUB_REPOSITORY" > /dev/null 2>&1; then
  gh release upload stable build/release/Quiet-Remote.apk --repo "$GITHUB_REPOSITORY" --clobber
  gh release edit stable --repo "$GITHUB_REPOSITORY" --title "Quiet Remote Stable" \
    --notes-file build/release/notes.md --latest --draft=false --prerelease=false
else
  gh release create stable build/release/Quiet-Remote.apk --repo "$GITHUB_REPOSITORY" \
    --verify-tag --title "Quiet Remote Stable" --notes-file build/release/notes.md --latest
fi
