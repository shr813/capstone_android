#include <jni.h>
#include <string>
#include "net.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

ncnn::Net yolov8;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_aiguideapp_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
    std::string hello = "Hello from NCNN";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_aiguideapp_MainActivity_initYolov8(JNIEnv* env, jobject /* this */, jobject assetManager) {
AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

yolov8.load_param(mgr, "yolov8n.param");
yolov8.load_model(mgr, "yolov8n.bin");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_aiguideapp_MainActivity_detectImage(JNIEnv *env, jobject thiz,
                                                     jbyteArray image_data, jint width,
                                                     jint height) {
    // TODO: implement detectImage()
}