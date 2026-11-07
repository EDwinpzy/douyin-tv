# Third-Party Notices

本项目使用或改编了以下开源项目。各项目仍归其原作者所有，并适用各自的许可证。

## abogus

- Upstream: <https://github.com/sobird/abogus>
- Package: `abogus@2.1.1`
- Copyright: Copyright (c) 2024 sobird
- License: MIT
- Local use: `app/src/main/java/com/dongpiandi/douyintv/network/AbogusSigner.kt`
  is a Kotlin adaptation of the upstream signing implementation.

```text
MIT License

Copyright (c) 2024 sobird

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Runtime dependencies

以下依赖通过 Gradle/Maven 获取，仓库本身不包含其二进制文件：

| Dependency | License |
| --- | --- |
| Kotlin and kotlinx libraries | Apache-2.0 |
| AndroidX / Jetpack Compose | Apache-2.0 |
| AndroidX Media3 | Apache-2.0 |
| OkHttp | Apache-2.0 |
| Coil | Apache-2.0 |

具体版权声明与许可证正文以各依赖上游发布物为准。公开发布 APK 前，应根据实际
打包依赖重新生成并附带完整的第三方通知。
