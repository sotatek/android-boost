# Simple Chat Room - Demo of Boost for Android

## Introduction
- Purpose: a simple application that demonstrates how to integrate and use C++ Boost lib in an Android project.
- Function:
    - User chooses a name and login with that name
    - Type something there and see the result: all messages people put are shown in a text view, anyone that enters the room can see the texts.
- The main important parts of the integration will be explained below.

## Prepare tools and environment
- Download Android Stutio IDE and SDK bundles from [offical page](https://developer.android.com/sdk/index.html) (We were using Android Studio 1.3.1 for this demo).
- Update SDK platform packages (For this demo we set minSdkVersion=14 and targetSdkVersion=22).
- Download [CrystaX NDK](https://www.crystax.net/en/download) which is a replacement for Google NDK, that strongly supports building applications with NDK and Boost C++ library (We were using CrytaX NDK 10.2.1 for this demo).

## Create an demo project
#### Create a new Android project by Android Studio.
- Select Android 4.0.3 for target version.
- Select blank activity for main activity.
- Set MainActivity as name for main activity.

#### Prepare native part
- Create `jni` folder under `main` source set on the Android project (just create folder `app/src/main/jni`).
- Create file `Application.mk` in the above `jni` folder with content:
```
# Application.mk
APP_ABI := armeabi-v7a
```
- Create file `Android.mk` in the `jni` folder with content:
```
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

BOOST_VERSION   := 1_58
LOCAL_MODULE    := test-boost
LOCAL_SRC_FILES := funcs_async.cpp
LOCAL_STATIC_LIBRARIES := boost_serialization_static boost_system_static boost_thread_static
LOCAL_LDLIBS    := -llog

include $(BUILD_SHARED_LIBRARY)

$(call import-module,boost/1.58.0)
```
- Create file `test_async_client.hpp` in the `jni` folder with content like [this](https://raw.githubusercontent.com/sotatek/android-boost/master/app/src/main/jni/test_async_client.hpp)
- Create file `funcs_async.cpp` in the `jni` folder with content like [this](https://raw.githubusercontent.com/sotatek/android-boost/master/app/src/main/jni/funcs_async.cpp)

#### Prepare Java part
- Create new class `RequestFactory`
- Add declaration of native method into `RequestFactory` class:
```Java
    private native boolean requestAsync(String rootPath);
```
- Also add loading of native library to the static initialization block:
```Java
    static {
        System.loadLibrary("test-boost");
    }
```
- Create static function that will be called from native side:
```Java
    public static void onJNICallback(int id, String data) {
        Log.d(Const.APP_TAG, "RequestFactory onJNICallback: id=" + id + ", data=" + data);
        try {
            JSONObject json = new JSONObject(data);
            Request request = getInstance().getRequest(id);
            getInstance().getCurrentActivity().onRequestFinish(request, new RequestResult(json.getInt("code"), json.getString("msg"), json.optJSONObject("data")));
        }
        catch (Exception e) {
            e .printStackTrace();
        }
    }
```
- See all content and some other support classes and functions [here](https://github.com/sotatek/android-boost/tree/master/app/src/main/java/com/sotatek/androidboost)

#### Prepare build scripts
- Add config for ndk in file `local.properties`:
```
ndk.dir=[Write absolute path to the CrystaX NDK folder you downloaded here]
```
- Define new tasks on gradle build script: `app/build.gradle`, see full content [here](https://github.com/sotatek/android-boost/blob/master/app/build.gradle). Below is the additional part:
```
    sourceSets.main.jni.srcDirs = []
    sourceSets.main.jniLibs.srcDir 'src/main/libs'

    task ndkBuild(type: Exec) {
        workingDir file('src/main')
        commandLine getNdkBuildCmd()
    }

    tasks.withType(JavaCompile) {
        compileTask -> compileTask.dependsOn ndkBuild
    }

    task cleanNative(type: Exec) {
        workingDir file('src/main')
        commandLine getNdkBuildCmd(), 'clean'
    }
```
```
def getNdkDir() {
    if (System.env.ANDROID_NDK_ROOT != null)
        return System.env.ANDROID_NDK_ROOT

    Properties properties = new Properties()
    properties.load(project.rootProject.file('local.properties').newDataInputStream())
    def ndkdir = properties.getProperty('ndk.dir', null)
    if (ndkdir == null)
        throw new GradleException("NDK location not found. Define location with ndk.dir in the local.properties file or with an ANDROID_NDK_ROOT environment variable.")

    return ndkdir
}

def getNdkBuildCmd() {
    def ndkbuild = getNdkDir() + "/ndk-build"
    if (Os.isFamily(Os.FAMILY_WINDOWS))
        ndkbuild += ".cmd"

    return ndkbuild
}
```
## How it works
- When `Login` or `Chat` button is pressed, request factory's native function `requestAsync` will be called from Java side.
- The bridge function `Java_com_sotatek_androidboost_RequestFactory_requestAsync` (that is defined in `funcs_async.cpp`) is called corresponding in C++ side.
- The bridge function create request service/object/thread and handle the connection with server, using Boost Asio library.
- When server response is returned, C++ side will call function `OnServerResponse`, it calls function `onJNICallback` of `RequestFactory` next and result will be handled and display in Java side.