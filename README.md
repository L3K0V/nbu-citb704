# CITB704

This is course project for the NBU CITB704 - Operation systems for mobile devices with Android.

## Requirements

- Java/Kotlin Android app ✅
- Good coding practices ✅
- No external libraries ✅
- Two Activities communicating between using Intents ⚠️
- Fragments ⛔️
- Consuming API ✅
- At least one service ✅
- Threads for non-UI actions ✅
- SQLite persistence ✅
- applicationId should contains student id ⛔️

## Project description

Simple Android Auto application for fetching and displaying public buffered paid parking inside 
Blue and Green parking zones in Sofia using Sofia Traffic public API. All parking places are saved
into SQLite database and updated on user refresh. Selecting a parking user can navigate to or see
its details.

<img width="912" alt="image" src="https://user-images.githubusercontent.com/2267270/146090753-726fd584-e7f2-45b6-8dd7-ec755dd22978.png">

<img width="912" alt="image" src="https://user-images.githubusercontent.com/2267270/146090781-28f84317-d29c-4d8f-a35d-b982cc227bb4.png">


## Getting started

You will need:

- Latest version of Android Studio
- [Android Auto DHU 2.0.0-rc02](https://developer.android.com/training/cars/testing#test-auto)
- Phone or emulator with Android 10 or above
- Installed latest [Android Auto](https://www.apkmirror.com/apk/google-inc/android-auto/)

When you download the setup the project you be able to run it normally and check the result

## Start the app for macOS

```shell
cd $ANDROID_SDK_ROOT/extras/google/auto/
adb forward tcp:5277 tcp:5277
./desktop-head-unit
```