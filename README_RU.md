<H1 align="center">BtnLauncher</H1>

<a href="./README.md">Readme in English</a>

<img src="./app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="150" height="150" alt="BtnLauncher logo">

[![Android CI](https://github.com/qvietdpzai/Btn-launcher/actions/workflows/build-apk.yml/badge.svg)](https://github.com/qvietdpzai/Btn-launcher/actions)

* BtnLauncher это лаунчер, основанный на [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher), позволяющий играть в Minecraft: Java Edition на устройствах Android!

* Он может запускать почти любую версию Minecraft, позволяя устанваливать через .jar загрузчики модов такие как [Forge](https://files.minecraftforge.net/) и [Fabric](http://fabricmc.net/), и моды по типу [OptiFine](https://optifine.net).

## Сборка  
* Скомпилируйте лаунчер (все необходимые компоненты скачаются автоматически)
```
./gradlew :app_pojavlauncher:assembleDebug
```
(Замените `./gradlew` на `.\gradlew.bat` если вы компилируете на Windows).

## Лицензия
- BtnLauncher лицензирован под [GNU LGPLv3](https://github.com/qvietdpzai/Btn-launcher/blob/main/LICENSE).

## Благодарности & Сторонние компоненты и их лицензии (если таковые имеются)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Лаунчер): Неизвестная Лицензия/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) или GNU GPLv2.
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt).
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE).<br>
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html).<br>
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license
- [LWJGL3](https://github.com/LWJGL/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md).
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html).
- [pro-grade](https://github.com/pro-grade/pro-grade) (Менеджер контейнеризации Java): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt).
- [bhook](https://github.com/bytedance/bhook) (Используется для получения кода ошибки): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE).
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Используется для авторизации через ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE).
- [alsoft](https://github.com/kcat/openal-soft/) (Библиотека вывода звука): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) и [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft).
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE).
- Отдельная благодарность к [Mineskin](https://mineskin.eu/) за предоставление аватаров Minecraft.
