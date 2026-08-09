#include <jni.h>
#include "memory.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_winlator_MemoryNative_initialize(JNIEnv *, jclass) {
    WinlatorMemory::initialize();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_MemoryNative_getConfiguredRAM(JNIEnv *, jclass) {
    return static_cast<jlong>(
        WinlatorMemory::getConfiguredRAM()
    );
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_MemoryNative_getPhysicalRAM(JNIEnv *, jclass) {
    return static_cast<jlong>(
        WinlatorMemory::getPhysicalRAM()
    );
}
