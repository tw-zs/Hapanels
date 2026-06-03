#include <android/log.h>
#include <cerrno>
#include <fcntl.h>
#include <jni.h>
#include <linux/input.h>
#include <poll.h>
#include <cstring>
#include <sys/ioctl.h>
#include <unistd.h>

#include <atomic>
#include <string>
#include <thread>
#include <vector>

namespace {
std::atomic<bool> running{false};
std::thread worker;
JavaVM* jvm = nullptr;
jobject callback_ref = nullptr;
jmethodID callback_method = nullptr;

struct InputDevice {
    pollfd poll;
    std::string path;
    std::string name;
};

void close_fds(std::vector<InputDevice>& devices) {
    for (auto& device : devices) {
        if (device.poll.fd >= 0) close(device.poll.fd);
        device.poll.fd = -1;
    }
}

void input_loop(std::vector<std::string> paths) {
    JNIEnv* env = nullptr;
    if (jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) return;

    std::vector<InputDevice> devices;
    for (const auto& path : paths) {
        int fd = open(path.c_str(), O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (fd >= 0) {
            char name[128] = {0};
            if (ioctl(fd, EVIOCGNAME(sizeof(name)), name) < 0) {
                strcpy(name, "unknown");
            }
            devices.push_back({{fd, POLLIN, 0}, path, name});
            __android_log_print(ANDROID_LOG_INFO, "HapanelsInput", "opened %s name=%s", path.c_str(), name);
        } else {
            __android_log_print(
                ANDROID_LOG_WARN,
                "HapanelsInput",
                "open failed %s errno=%d %s",
                path.c_str(),
                errno,
                strerror(errno)
            );
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "HapanelsInput", "monitoring %zu input devices", devices.size());

    while (running.load()) {
        if (devices.empty()) {
            usleep(250000);
            continue;
        }
        std::vector<pollfd> poll_fds;
        poll_fds.reserve(devices.size());
        for (const auto& device : devices) poll_fds.push_back(device.poll);
        int ready = poll(poll_fds.data(), poll_fds.size(), 250);
        if (ready <= 0) continue;
        for (size_t i = 0; i < devices.size(); ++i) {
            devices[i].poll.revents = poll_fds[i].revents;
            if ((devices[i].poll.revents & POLLIN) == 0) continue;
            input_event event{};
            while (read(devices[i].poll.fd, &event, sizeof(event)) == sizeof(event)) {
                if (event.type == EV_KEY && callback_ref != nullptr && callback_method != nullptr) {
                    __android_log_print(
                        ANDROID_LOG_INFO,
                        "HapanelsInput",
                        "key path=%s name=%s code=%d value=%d",
                        devices[i].path.c_str(),
                        devices[i].name.c_str(),
                        event.code,
                        event.value
                    );
                    env->CallVoidMethod(callback_ref, callback_method, event.code, event.value, 0);
                    if (env->ExceptionCheck()) env->ExceptionClear();
                }
            }
        }
    }

    close_fds(devices);
    jvm->DetachCurrentThread();
}
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    jvm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_github_itskenny0_r1ha_core_hardware_ShellyInputMonitor_nativeStart(
    JNIEnv* env,
    jobject,
    jobject callback,
    jobjectArray paths
) {
    if (running.exchange(true)) return JNI_TRUE;

    callback_ref = env->NewGlobalRef(callback);
    jclass callback_class = env->GetObjectClass(callback);
    callback_method = env->GetMethodID(callback_class, "onHardwareKey", "(III)V");
    if (callback_method == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "HapanelsInput", "callback onHardwareKey(III)V not found");
        running.store(false);
        return JNI_FALSE;
    }

    std::vector<std::string> path_list;
    jsize count = env->GetArrayLength(paths);
    for (jsize i = 0; i < count; ++i) {
        auto item = static_cast<jstring>(env->GetObjectArrayElement(paths, i));
        const char* chars = env->GetStringUTFChars(item, nullptr);
        path_list.emplace_back(chars);
        env->ReleaseStringUTFChars(item, chars);
        env->DeleteLocalRef(item);
    }

    worker = std::thread(input_loop, path_list);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_github_itskenny0_r1ha_core_hardware_ShellyInputMonitor_nativeStop(JNIEnv* env, jobject) {
    running.store(false);
    if (worker.joinable()) worker.join();
    if (callback_ref != nullptr) {
        env->DeleteGlobalRef(callback_ref);
        callback_ref = nullptr;
    }
    callback_method = nullptr;
}
