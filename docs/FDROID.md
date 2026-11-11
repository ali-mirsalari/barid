# Publishing on F-Droid

F-Droid builds Barid from this repository's source on its own servers (outside Iran, so no Maven
mirror is needed) and signs it with F-Droid's key. Store texts, screenshots and changelogs come from
`fastlane/metadata/android/` (`en-US`, `fa`); a new release needs `changelogs/<versionCode>.txt` in both.

## First submission

1. The repository must be public and tagged `v<versionName>` (for example `v1.0.0`).
2. Fork https://gitlab.com/fdroid/fdroiddata, add `metadata/com.darakav.barid.yml` with the content
   below, and open a merge request (title: "New app: Barid"). F-Droid's CI builds it; reviewers may ask
   questions on the merge request.
3. After it is merged, the app appears within a few days at https://f-droid.org/packages/com.darakav.barid/

```yaml
Categories:
  - Money
  - Connectivity
License: GPL-3.0-only
AuthorName: Darakav
WebSite: https://darakav.com
SourceCode: https://github.com/ali-mirsalari/barid
IssueTracker: https://github.com/ali-mirsalari/barid/issues
Changelog: https://github.com/ali-mirsalari/barid/releases

AutoName: Barid

RepoType: git
Repo: https://github.com/ali-mirsalari/barid.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0

  - versionName: 1.0.1
    versionCode: 2
    commit: v1.0.1
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.1
CurrentVersionCode: 2
```

## Later releases

Increase `versionCode` and `versionName` in `app/build.gradle.kts`, add the changelogs, commit, and
push a tag `v<versionName>`. F-Droid's update checker picks up new tags (`UpdateCheckMode: Tags`).

## Notes for reviewers

- SMS permissions (`RECEIVE_SMS`, `READ_SMS`) are the app's purpose: messages are filtered on the phone
  and only bank transactions go to the server the owner connected. No tracking, ads or proprietary
  libraries (AndroidX, Jetpack Compose, CameraX, ZXing, Haze).
- `NonFreeNet` does not apply: the app sends only to a server the owner chooses; Darakav is one option.
- Installs from F-Droid (F-Droid's key) and from GitHub releases (Barid's own key) cannot update each
  other; switching means uninstalling first.
