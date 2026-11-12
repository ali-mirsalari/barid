#!/usr/bin/env bash
# Builds the signed release APK for a tag, the way F-Droid can reproduce it.
#
#   scripts/release-apk.sh 1.0.3            -> dist/barid-1.0.3.apk
#   scripts/release-apk.sh 1.0.3 --upload   -> also attaches it to the GitHub release v1.0.3
#
# The tag is built from a clean clone (never from the working tree) in the README's Docker image.
# The release key stays where it is: keystore.properties and the keystore's folder are mounted
# read-only into the container, nothing is copied. Set BARID_KEYSTORE_PROPERTIES to use another
# file (default ~/.config/barid/keystore.properties); set GOOGLE_MAVEN_MIRROR="" outside Iran.
set -euo pipefail

version=${1:?usage: scripts/release-apk.sh <versionName> [--upload]}
upload=${2:-}
tag="v$version"
image=cimg/android:2026.10.1
mirror=${GOOGLE_MAVEN_MIRROR-https://maven.myket.ir}
props=${BARID_KEYSTORE_PROPERTIES:-$HOME/.config/barid/keystore.properties}
cert=a7e745ed02870a86d8261b174ae5e48ebe5ed6374793ca9b3775dfde5c01a82d
repo=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
work=/tmp/barid-release-$version
cache=/tmp/barid-gradle
out=$repo/dist/barid-$version.apk

[[ -r $props ]] || { echo "cannot read $props" >&2; exit 1; }
store=$(sed -n 's/^storeFile=//p' "$props")
[[ -r $store ]] || { echo "keystore $store (storeFile in $props) is not readable" >&2; exit 1; }

git -C "$repo" fetch -q --tags origin
commit=$(git -C "$repo" rev-parse "$tag^{commit}")
[[ $(git -C "$repo" branch -r --contains "$commit" origin/main) ]] ||
  { echo "$tag ($commit) is not on origin/main; tag the merged commit" >&2; exit 1; }

echo "building $tag ($commit) in $work"
[[ -d $work ]] && rm -r -- "$work"
git clone -q "$repo" "$work"
git -C "$work" checkout -q "$commit"
chmod -R a+rwX "$work"
mkdir -p "$cache" && chmod a+rwX "$cache"

# Root in the container: the image's SDK belongs to its own user, and the key files are 0600.
mirror_arg=${mirror:+-PgoogleMavenMirror=$mirror}
docker run --rm --network host --user 0 \
  -e GRADLE_USER_HOME=/g -v "$cache:/g" -v "$work:/w" -w /w \
  -v "$props:/w/keystore.properties:ro" -v "$(dirname "$store"):$(dirname "$store"):ro" \
  "$image" bash -c "umask 000; ./gradlew --no-daemon -q $mirror_arg assembleRelease"

apk=$work/app/build/outputs/apk/release/app-release.apk
[[ -f $apk ]] || { echo "no signed APK at $apk (is storeFile set in $props?)" >&2; exit 1; }

signer=$(docker run --rm --user 0 -v "$apk:/a.apk:ro" "$image" \
  /home/circleci/android-sdk/build-tools/35.0.0/apksigner verify --print-certs /a.apk |
  sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
[[ $signer == "$cert" ]] || { echo "signed with $signer, expected Barid's release key $cert" >&2; exit 1; }
if unzip -l "$apk" | grep -q version-control-info; then
  echo "warning: APK contains version-control-info.textproto (built before vcsInfo was turned off)" >&2
fi

mkdir -p "$(dirname "$out")"
cp "$apk" "$out"
echo "$out"
sha256sum "$out"

if [[ $upload == --upload ]]; then
  gh release upload "$tag" "$out" --clobber --repo ali-mirsalari/barid
  echo "attached to https://github.com/ali-mirsalari/barid/releases/tag/$tag"
fi
