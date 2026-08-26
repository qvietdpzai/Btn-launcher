#include <vulkan/vulkan.h>
#include <android/native_window.h>
#include <android/log.h>
#include "pojavexec.h"
#define LOG_TAG "VulkanBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static VkInstance g_vulkanInstance = VK_NULL_HANDLE;
static VkSurfaceKHR g_vulkanSurface = VK_NULL_HANDLE;
static VkDevice g_vulkanDevice = VK_NULL_HANDLE;
static VkSwapchainKHR g_vulkanSwapchain = VK_NULL_HANDLE;
static VkQueue g_graphicsQueue = VK_NULL_HANDLE;
static uint32_t g_graphicsQueueFamilyIndex = UINT32_MAX;

JNIEXPORT jboolean JNICALL Java_net_kdt_pojavlaunch_render_VulkanSurfaceProvider_nativeInitVulkan(JNIEnv* env, jclass clazz, jobject surface) {
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (!window) { LOGE("Failed to get ANativeWindow"); return JNI_FALSE; }
    VkApplicationInfo appInfo = { .sType = VK_STRUCTURE_TYPE_APPLICATION_INFO, .pApplicationName = "BtnLauncher", .applicationVersion = VK_MAKE_VERSION(1,0,0), .pEngineName = "PoJaV", .engineVersion = VK_MAKE_VERSION(1,0,0), .apiVersion = VK_API_VERSION_1_0 };
    const char* enabledExtensions[] = { "VK_KHR_surface", "VK_KHR_android_surface" };
    VkInstanceCreateInfo createInfo = { .sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO, .pApplicationInfo = &appInfo, .enabledExtensionCount = 2, .ppEnabledExtensionNames = enabledExtensions };
    VkResult result = vkCreateInstance(&createInfo, NULL, &g_vulkanInstance);
    if (result != VK_SUCCESS) { LOGE("Failed to create Vulkan instance: %d", result); return JNI_FALSE; }
    VkAndroidSurfaceCreateInfoKHR surfaceCreateInfo = { .sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR, .window = window };
    PFN_vkCreateAndroidSurfaceKHR vkCreateAndroidSurfaceKHR = (PFN_vkCreateAndroidSurfaceKHR)vkGetInstanceProcAddr(g_vulkanInstance, "vkCreateAndroidSurfaceKHR");
    if (!vkCreateAndroidSurfaceKHR) { LOGE("vkCreateAndroidSurfaceKHR not available"); return JNI_FALSE; }
    result = vkCreateAndroidSurfaceKHR(g_vulkanInstance, &surfaceCreateInfo, NULL, &g_vulkanSurface);
    if (result != VK_SUCCESS) { LOGE("Failed to create Vulkan surface: %d", result); return JNI_FALSE; }
    LOGI("Vulkan initialized successfully"); return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_net_kdt_pojavlaunch_render_VulkanSurfaceProvider_nativeCreateSwapchain(JNIEnv* env, jclass clazz, jint width, jint height) {
    uint32_t deviceCount = 0; vkEnumeratePhysicalDevices(g_vulkanInstance, &deviceCount, NULL);
    if (deviceCount == 0) { LOGE("No Vulkan physical devices"); return JNI_FALSE; }
    VkPhysicalDevice physicalDevices[deviceCount]; vkEnumeratePhysicalDevices(g_vulkanInstance, &deviceCount, physicalDevices);
    VkPhysicalDevice physicalDevice = physicalDevices[0];
    uint32_t queueFamilyCount = 0; vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, &queueFamilyCount, NULL);
    VkQueueFamilyProperties queueFamilies[queueFamilyCount]; vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, &queueFamilyCount, queueFamilies);
    for (uint32_t i = 0; i < queueFamilyCount; i++) {
        if (queueFamilies[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) {
            VkBool32 surfaceSupport = VK_FALSE; vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, g_vulkanSurface, &surfaceSupport);
            if (surfaceSupport) { g_graphicsQueueFamilyIndex = i; break; }
        }
    }
    if (g_graphicsQueueFamilyIndex == UINT32_MAX) { LOGE("No suitable graphics queue"); return JNI_FALSE; }
    float queuePriority = 1.0f;
    VkDeviceQueueCreateInfo queueCreateInfo = { .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO, .queueFamilyIndex = g_graphicsQueueFamilyIndex, .queueCount = 1, .pQueuePriorities = &queuePriority };
    const char* deviceExtensions[] = { "VK_KHR_swapchain" };
    VkDeviceCreateInfo deviceCreateInfo = { .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO, .queueCreateInfoCount = 1, .pQueueCreateInfos = &queueCreateInfo, .enabledExtensionCount = 1, .ppEnabledExtensionNames = deviceExtensions };
    VkResult result = vkCreateDevice(physicalDevice, &deviceCreateInfo, NULL, &g_vulkanDevice);
    if (result != VK_SUCCESS) { LOGE("Failed to create Vulkan device: %d", result); return JNI_FALSE; }
    vkGetDeviceQueue(g_vulkanDevice, g_graphicsQueueFamilyIndex, 0, &g_graphicsQueue);
    VkSurfaceCapabilitiesKHR capabilities; vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, g_vulkanSurface, &capabilities);
    VkSurfaceFormatKHR surfaceFormat = { .format = VK_FORMAT_B8G8R8A8_SRGB, .colorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR };
    VkSwapchainCreateInfoKHR swapchainCreateInfo = { .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR, .surface = g_vulkanSurface, .minImageCount = capabilities.minImageCount + 1, .imageFormat = surfaceFormat.format, .imageColorSpace = surfaceFormat.colorSpace, .imageExtent = { .width = (uint32_t)width, .height = (uint32_t)height }, .imageArrayLayers = 1, .imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, .imageSharingMode = VK_SHARING_MODE_EXCLUSIVE, .preTransform = capabilities.currentTransform, .compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR, .presentMode = VK_PRESENT_MODE_FIFO_KHR, .clipped = VK_TRUE, .oldSwapchain = VK_NULL_HANDLE };
    PFN_vkCreateSwapchainKHR vkCreateSwapchainKHR = (PFN_vkCreateSwapchainKHR)vkGetInstanceProcAddr(g_vulkanInstance, "vkCreateSwapchainKHR");
    if (!vkCreateSwapchainKHR) { LOGE("vkCreateSwapchainKHR not available"); return JNI_FALSE; }
    result = vkCreateSwapchainKHR(g_vulkanDevice, &swapchainCreateInfo, NULL, &g_vulkanSwapchain);
    if (result != VK_SUCCESS) { LOGE("Failed to create swapchain: %d", result); return JNI_FALSE; }
    LOGI("Vulkan swapchain created: %dx%d", width, height); return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_render_VulkanSurfaceProvider_nativeRenderFrame(JNIEnv* env, jclass clazz) {
    if (!g_vulkanSwapchain || !g_vulkanDevice) return;
    uint32_t imageIndex = 0;
    PFN_vkAcquireNextImageKHR vkAcquireNextImageKHR = (PFN_vkAcquireNextImageKHR)vkGetInstanceProcAddr(g_vulkanInstance, "vkAcquireNextImageKHR");
    VkResult result = vkAcquireNextImageKHR(g_vulkanDevice, g_vulkanSwapchain, UINT64_MAX, VK_NULL_HANDLE, VK_NULL_HANDLE, &imageIndex);
    if (result != VK_SUCCESS) { LOGE("Failed to acquire next image: %d", result); return; }
    PFN_vkQueuePresentKHR vkQueuePresentKHR = (PFN_vkQueuePresentKHR)vkGetInstanceProcAddr(g_vulkanInstance, "vkQueuePresentKHR");
    VkPresentInfoKHR presentInfo = { .sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR, .swapchainCount = 1, .pSwapchains = &g_vulkanSwapchain, .pImageIndices = &imageIndex };
    vkQueuePresentKHR(g_graphicsQueue, &presentInfo);
}

JNIEXPORT void JNICALL Java_net_kdt_pojavlaunch_render_VulkanSurfaceProvider_nativeCleanup(JNIEnv* env, jclass clazz) {
    if (g_vulkanSwapchain) { PFN_vkDestroySwapchainKHR vkDestroySwapchainKHR = (PFN_vkDestroySwapchainKHR)vkGetInstanceProcAddr(g_vulkanInstance, "vkDestroySwapchainKHR"); vkDestroySwapchainKHR(g_vulkanDevice, g_vulkanSwapchain, NULL); g_vulkanSwapchain = VK_NULL_HANDLE; }
    if (g_vulkanDevice) { vkDestroyDevice(g_vulkanDevice, NULL); g_vulkanDevice = VK_NULL_HANDLE; }
    if (g_vulkanSurface) { vkDestroySurfaceKHR(g_vulkanInstance, g_vulkanSurface, NULL); g_vulkanSurface = VK_NULL_HANDLE; }
    if (g_vulkanInstance) { vkDestroyInstance(g_vulkanInstance, NULL); g_vulkanInstance = VK_NULL_HANDLE; }
    LOGI("Vulkan cleanup complete");
}
