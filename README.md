<H1 align="center">BtnLauncher</H1>

<img src="./app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="150" height="150" alt="BtnLauncher logo">

[![Android CI](https://github.com/qvietdpzai/Btn-launcher/actions/workflows/build-apk.yml/badge.svg)](https://github.com/qvietdpzai/Btn-launcher/actions)

* BtnLauncher is a launcher, based on [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher), that allows you to play Minecraft: Java Edition on your Android device!

* It can run almost every version of Minecraft, allowing you to use .jar only installers to install modloaders such as [Forge](https://files.minecraftforge.net/) and [Fabric](http://fabricmc.net/) and mods like [OptiFine](https://optifine.net).

## Building   
* Build the launcher (it will automatically download all required components)
```
./gradlew :app_pojavlauncher:assembleDebug
```
(Replace `./gradlew` with `.\gradlew.bat` if you are building on Windows).

## License
- BtnLauncher is licensed under [GNU LGPLv3](https://github.com/qvietdpzai/Btn-launcher/blob/main/LICENSE).

## Third party components, licenses and sources (when applicable)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or GNU GPLv2.
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt).
- [Holy GL4ES](https://github.com/artdeell/gl4es_extra_extra/): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE).<br>
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html).<br>
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license
- [LWJGL3](https://github.com/LWJGL/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md).
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html).
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt).
- [bhook](https://github.com/bytedance/bhook) (Used for exit code trapping): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE).
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Used for authorisation via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE).
- [alsoft](https://github.com/kcat/openal-soft/) (Audio output library): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) and [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft).
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE).
- Thanks to [Mineskin](https://mineskin.eu/) for providing Minecraft avatars.
