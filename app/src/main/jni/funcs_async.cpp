#include <jni.h>

#include <string.h>
#include <stdlib.h>

#include <string>
#include <exception>
#include <sstream>
#include <iostream>

#include <boost/array.hpp>
#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/chrono.hpp>

#include <android/log.h>

#include "test_async_client.hpp"

#define LOG(fmt, ...) __android_log_print(ANDROID_LOG_INFO, "TEST-BOOST", fmt, ##__VA_ARGS__)

using boost::asio::ip::tcp;

static JavaVM* gVM = NULL;
static jobject gObj = NULL;
static jmethodID gMethodId = NULL;


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* aReserved)
{
    LOG("[BOOST] JNI_Onload");
    gVM = vm;

    return JNI_VERSION_1_4;
}

void OnServerResponse(int id, std::string response)
{
    try {

        JNIEnv * local_env;
        int getEnvStat = gVM->GetEnv((void **)&local_env, JNI_VERSION_1_4);
        int attachResult = gVM->AttachCurrentThread(&local_env, NULL);

        jstring jstr1 = local_env->NewStringUTF(response.c_str());
        jclass clazz = local_env->GetObjectClass(gObj);
        local_env->CallStaticVoidMethod(clazz, gMethodId, id, jstr1);

        gVM->DetachCurrentThread();

    } catch (std::exception& e) {
        LOG("[BOOST] OnServerResponse ERROR: %s", e.what());
    }
}

bool getServerDataAsync(int id, std::string const &host, int port, std::string const &data) {
    LOG("getServerDataAsync: %s", data.c_str());
    boost::thread t([id, host, port, data](){
        boost::asio::io_service io_service;
        client c(io_service, data, host, port);
        c.start();
        io_service.run();
        OnServerResponse(id, c.get_response());
    });

    return true;
}

extern "C"
jboolean
Java_com_sotatek_androidboost_RequestFactory_requestAsync( JNIEnv* env, jobject thiz, jint id, jstring jhost, jint jport, jstring jdata)
{
    try {
        gObj = env->NewGlobalRef(thiz);
        /*
         * Dont know why this doesn't work
         * jclass clazz = env->FindClass("com/sotatek/androidboost/RequestFactory");
         */
        jclass clazz = env->GetObjectClass(gObj);
        gMethodId = env->GetStaticMethodID(clazz, "onJNICallback", "(ILjava/lang/String;)V");

        const char *s = env->GetStringUTFChars(jdata, 0);
        std::string data(s);
        env->ReleaseStringUTFChars(jdata, s);

        const char *ss = env->GetStringUTFChars(jhost, 0);
        std::string host(ss);
        env->ReleaseStringUTFChars(jhost, ss);

        getServerDataAsync((int) id, host, (int) jport, data);
        return true;
    }
    catch (std::exception &e) {
        LOG("[BOOST] Error: e = %s", e.what());
        abort();
    }
    catch (...) {
        LOG("[BOOST] Unkown error.");
        abort();
    }
    return false;
}