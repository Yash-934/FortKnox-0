#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <android/log.h>
#include <cstdlib>
#include <cstring>

#define TAG "FortKnoxNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

static const char* ROOT_PATHS[] = {
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/system/sd/xbin/su",
    "/system/bin/failsafe/su",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/data/local/su",
    "/data/adb/magisk",
    "/data/adb/ksu",
    "/data/adb/ap",
    "/system/app/Superuser.apk",
    "/sbin/ext/su",
    "/system/usr/we-need-root/su",
    "/system/bin/magisk",
    "/system/xbin/magisk",
    "/system/bin/ksu",
    "/su/bin/su",
    "/su/xbin/su",
    "/magisk/.core/bin/su",
    "/dev/com.koushikdutta.superuser.daemon/",
    "/cache/su",
    "/custom/bin/su",
    "/system/xbin/daemonsu",
    "/system/etc/init.d/99SuperSUDaemon",
    "/system/bin/.ext/.su"
};

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    // Disable core dumps for memory protection against forensic dumping
    prctl(PR_SET_DUMPABLE, 0);
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_security_NativeCore_isRootDetected(JNIEnv* env, jobject /* this */) {
    // Check root file paths
    size_t count = sizeof(ROOT_PATHS) / sizeof(ROOT_PATHS[0]);
    for (size_t i = 0; i < count; ++i) {
        if (access(ROOT_PATHS[i], F_OK) == 0) {
            return JNI_TRUE;
        }
    }
    
    // Check /proc/mounts for dangerous mounts
    std::ifstream mounts("/proc/mounts");
    if (mounts.is_open()) {
        std::string line;
        while (std::getline(mounts, line)) {
            if (line.find("magisk") != std::string::npos ||
                line.find("core/mirror") != std::string::npos ||
                line.find("core/img") != std::string::npos) {
                return JNI_TRUE;
            }
        }
    }

    return JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_security_NativeCore_isDebuggerOrHookDetected(JNIEnv* env, jobject /* this */) {
    // 1. Check TracerPid in /proc/self/status
    std::ifstream status("/proc/self/status");
    if (status.is_open()) {
        std::string line;
        while (std::getline(status, line)) {
            if (line.find("TracerPid:") != std::string::npos) {
                int tracerPid = 0;
                if (sscanf(line.c_str(), "TracerPid:\t%d", &tracerPid) == 1) {
                    if (tracerPid > 0) {
                        return JNI_TRUE;
                    }
                }
            }
        }
    }

    // 2. Scan /proc/self/maps for Frida, Xposed, Substrate
    std::ifstream maps("/proc/self/maps");
    if (maps.is_open()) {
        std::string line;
        while (std::getline(maps, line)) {
            if (line.find("frida") != std::string::npos ||
                line.find("xposed") != std::string::npos ||
                line.find("substrate") != std::string::npos ||
                line.find("edxposed") != std::string::npos ||
                line.find("lsposed") != std::string::npos) {
                return JNI_TRUE;
            }
        }
    }

    // 3. Check for standard Frida server listening port (27042)
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock >= 0) {
        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port = htons(27042);
        addr.sin_addr.s_addr = inet_addr("127.0.0.1");
        
        // Non-blocking quick check
        struct timeval tv;
        tv.tv_sec = 0;
        tv.tv_usec = 10000;
        setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof tv);
        setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof tv);

        if (connect(sock, (struct sockaddr*)&addr, sizeof(addr)) == 0) {
            close(sock);
            return JNI_TRUE; // Frida port is open!
        }
        close(sock);
    }

    return JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_security_NativeCore_enforceKillSwitch(JNIEnv* env, jobject /* this */) {
    kill(getpid(), SIGKILL);
    _exit(1);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_security_NativeCore_lockMemory(JNIEnv* env, jobject /* this */, jbyteArray array) {
    if (array == nullptr) return JNI_FALSE;
    jsize len = env->GetArrayLength(array);
    jbyte* bytes = env->GetByteArrayElements(array, nullptr);
    if (bytes == nullptr) return JNI_FALSE;
    
    int res = mlock(bytes, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(array, bytes, JNI_ABORT);
    return res == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_security_NativeCore_unlockMemory(JNIEnv* env, jobject /* this */, jbyteArray array) {
    if (array == nullptr) return JNI_FALSE;
    jsize len = env->GetArrayLength(array);
    jbyte* bytes = env->GetByteArrayElements(array, nullptr);
    if (bytes == nullptr) return JNI_FALSE;
    
    int res = munlock(bytes, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(array, bytes, JNI_ABORT);
    return res == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_security_NativeCore_secureWipe(JNIEnv* env, jobject /* this */, jbyteArray array) {
    if (array == nullptr) return;
    jsize len = env->GetArrayLength(array);
    jbyte* bytes = env->GetByteArrayElements(array, nullptr);
    if (bytes == nullptr) return;

    // Overwrite with 0xAA, then 0x55, then 0x00 for DoD 5220.22-M sanitization
    memset(bytes, 0xAA, static_cast<size_t>(len));
    memset(bytes, 0x55, static_cast<size_t>(len));
    memset(bytes, 0x00, static_cast<size_t>(len));

    env->ReleaseByteArrayElements(array, bytes, 0); // commit back to Java array
}
