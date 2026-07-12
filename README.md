# Drebin451

[![Download latest Drebin451 APK](https://img.shields.io/badge/Download-latest%20Drebin451%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Commit451/Drebin451/releases/latest/download/Drebin451.apk)

Ship Android apps privately. Uploading apps to the Play Store for testing purposes is possible, 
but there are lots of steps and hurdles to do so. Drebin451 fills the gap and lets you easily share
and distribute your apps while getting them ready for prime time.

## Uploading

There are a few ways you can upload your app to Drebin451:

### Manual upload

Go to [Drebin451.com](https://drebin451.com) to create an account and upload an APK to install or
share.

### API upload

After creating an account, you can create an API key in the Settings and use that to deploy your APK
in any workflow you'd like. One such way is via GitHub Actions:
https://github.com/Commit451/drebin451-release

```
- name: Publish APK to Drebin451
  uses: Commit451/drebin451-release@v1
  with:
    api-key: ${{ secrets.DREBIN_API_KEY }}
    apk-path: app/androidApp/build/outputs/apk/release/androidApp-release.apk
    note: ${{ github.event.head_commit.message }}
```

## License

Drebin451 is available under the MIT license. See the LICENSE file for more info.

\ ゜o゜)ノ
