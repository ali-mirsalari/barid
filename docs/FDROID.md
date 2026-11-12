# Publishing on F-Droid

F-Droid builds Barid from this repository's source on its own servers (outside Iran, so no Maven
mirror is needed). Builds are reproducible: F-Droid checks that its build is identical to the signed APK
on the GitHub release and then ships that APK's signature (Barid's release key), so installs from
F-Droid and from GitHub update each other. Store texts, screenshots and changelogs come from
`fastlane/metadata/android/` (`en-US`, `fa`); a new release needs `changelogs/<versionCode>.txt` in both.

## Recipe

`metadata/com.darakav.barid.yml` in https://gitlab.com/fdroid/fdroiddata (first submitted in merge
request !56871):

```yaml
Categories:
  - Finance Manager
  - Phone & SMS
License: GPL-3.0-only
AuthorName: Darakav
WebSite: https://darakav.com
SourceCode: https://github.com/ali-mirsalari/barid
IssueTracker: https://github.com/ali-mirsalari/barid/issues
Changelog: https://github.com/ali-mirsalari/barid/releases

AutoName: Barid

RepoType: git
Repo: https://github.com/ali-mirsalari/barid.git
Binaries: https://github.com/ali-mirsalari/barid/releases/download/v%v/barid-%v.apk

Builds:
  - versionName: 1.0.2
    versionCode: 3
    commit: fd4cb587cc02aa3c722f3d1471d2a1f4ac56590b
    subdir: app
    gradle:
      - yes
    prebuild: sed -i -e '/googleMavenMirror/d' ../settings.gradle.kts

AllowedAPKSigningKeys: a7e745ed02870a86d8261b174ae5e48ebe5ed6374793ca9b3775dfde5c01a82d

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.2
CurrentVersionCode: 3
```

- `commit` is the tag's full commit hash (`git rev-parse v1.0.2^{commit}`); fdroiddata does not accept
  tag names there. Before a new app is merged, only its current build is listed.
- `prebuild` removes the optional Maven mirror (`maven(it)` in `settings.gradle.kts`, used only for
  builds from Iran), which F-Droid's source scanner rejects as an unknown repository.
- Categories must come from fdroiddata's list (`schemas/metadata.json`), sorted. The file ends with a
  newline. `fdroid lint` and `fdroid rewritemeta` in the merge request's pipeline check both.
- `Binaries` + `AllowedAPKSigningKeys` turn on reproducible builds. F-Droid's build job downloads the
  GitHub APK, copies its signature onto its own build and fails if that does not verify.

## Releases

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts` and add the changelogs in both
   locales. Merge the pull request.
2. Tag the merged commit on `main` (`v<versionName>`) and push the tag. Release tags (`v*`) are protected
   by a GitHub ruleset: they cannot be moved or deleted, because F-Droid builds exactly the tagged commit.
3. Create the GitHub release for the tag, then run `scripts/release-apk.sh <versionName> --upload`. It
   builds the tag from a clean clone in the README's Docker image, signs it with Gradle and the release
   key (mounted read-only, never copied), checks the certificate and attaches `dist/barid-<versionName>.apk`
   to the release. The file name must match `Binaries`.
4. F-Droid's update checker picks up the tag (`UpdateCheckMode: Tags`), builds it and compares the
   result with that APK. Until the APK is attached, F-Droid's build of that version fails.

Until merge request !56871 is merged, step 4 does not happen on its own: the merge request names one
exact build. After attaching the APK, replace that build in the fork's branch (`alimirsalari/fdroiddata`,
`com.darakav.barid`) with the new versionName, versionCode and full commit hash, update
`CurrentVersion`/`CurrentVersionCode`, push, and wait for the pipeline to verify the signature. Then update
the recipe above to match.

The script's APK for v1.0.2 was checked byte for byte: it is F-Droid's own build plus the signature block.
Release APKs also leave out the commit hash (`vcsInfo.include = false`, from 1.0.3), so even a build from
the release branch before its squash merge would match. Do not re-sign an APK by hand with `apksigner`:
it re-aligns entries unless given `--alignment-preserved`, and then F-Droid's copy of the signature does
not verify.

## Notes for reviewers

- SMS permissions (`RECEIVE_SMS`, `READ_SMS`) are the app's purpose: messages are filtered on the phone
  and only bank transactions go to the server the owner connected. No tracking, ads or proprietary
  libraries (AndroidX, Jetpack Compose, CameraX, ZXing, Haze).
- `NonFreeNet` does not apply: the app sends only to a server the owner chooses; Darakav is one option.
- GitLab blocks Iranian IP addresses; reach it (and its API, for example with `glab`) through a proxy.
  If GitLab asks for a phone number or card to run a fork's pipeline, do not give one: leave a note on
  the merge request and the reviewers trigger it.
