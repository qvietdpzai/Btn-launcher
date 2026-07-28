package net.kdt.pojavlaunch.aiturbo;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AiTurboManager {
    private static AiTurboManager sInstance;
    private boolean mEnabled = false;
    private boolean mMonitoring = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private DeviceProfile mDeviceProfile;

    private static final String TAG = "AiTurbo";

    public interface TurboCallback {
        void onOptimizationApplied(String message);
        void onPerformanceWarning(String warning);
    }

    private TurboCallback mCallback;

    public static synchronized AiTurboManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AiTurboManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private AiTurboManager(Context context) {
        mContext = context;
    }

    public void setCallback(TurboCallback callback) {
        mCallback = callback;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public static class DeviceProfile {
        public final int totalRamMB;
        public final int freeRamMB;
        public final int cpuCores;
        public final int minFreqMhz;
        public final int maxFreqMhz;
        public final int screenWidth;
        public final int screenHeight;
        public final boolean is64Bit;
        public final String cpuName;
        public final int deviceTier; // 1=low, 2=mid, 3=high

        public DeviceProfile(Context ctx) {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);

            totalRamMB = (int) (memInfo.totalMem / (1024 * 1024));
            freeRamMB = (int) (memInfo.availMem / (1024 * 1024));
            cpuCores = Runtime.getRuntime().availableProcessors();
            is64Bit = Build.SOC_MODEL != null || Build.CPU_ABI.contains("64");

            DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;

            cpuName = getCpuName();
            minFreqMhz = getCpuFreq(0);
            maxFreqMhz = getCpuMaxFreq(cpuCores - 1);

            deviceTier = calculateTier();
        }

        private String getCpuName() {
            try {
                BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("model name") || line.contains("Hardware")) {
                        br.close();
                        return line.split(":\\s*", 2).length > 1 ? line.split(":\\s*", 2)[1].trim() : "Unknown";
                    }
                }
                br.close();
            } catch (IOException ignored) {}
            return Build.HARDWARE;
        }

        private int getCpuFreq(int core) {
            try {
                String freq = Tools.read("/sys/devices/system/cpu/cpu" + core + "/cpufreq/cpuinfo_min_freq");
                return Integer.parseInt(freq.trim()) / 1000;
            } catch (Exception e) {
                return 0;
            }
        }

        private int getCpuMaxFreq(int core) {
            try {
                String freq = Tools.read("/sys/devices/system/cpu/cpu" + core + "/cpufreq/cpuinfo_max_freq");
                return Integer.parseInt(freq.trim()) / 1000;
            } catch (Exception e) {
                return 1500;
            }
        }

        private int calculateTier() {
            int score = 0;
            if (totalRamMB >= 6144) score += 3;
            else if (totalRamMB >= 4096) score += 2;
            else if (totalRamMB >= 2048) score += 1;

            if (cpuCores >= 8) score += 3;
            else if (cpuCores >= 6) score += 2;
            else if (cpuCores >= 4) score += 1;

            if (maxFreqMhz >= 2400) score += 3;
            else if (maxFreqMhz >= 1800) score += 2;
            else if (maxFreqMhz >= 1200) score += 1;

            int minSide = Math.min(screenWidth, screenHeight);
            if (minSide >= 1080) score += 2;
            else if (minSide >= 720) score += 1;

            if (is64Bit) score += 1;

            if (score >= 10) return 3;
            if (score >= 6) return 2;
            return 1;
        }

        public String getTierName() {
            switch (deviceTier) {
                case 3: return "High";
                case 2: return "Mid";
                default: return "Low";
            }
        }
    }

    public void analyze() {
        mDeviceProfile = new DeviceProfile(mContext);
    }

    public void applyOptimizations(Instance instance) {
        if (!mEnabled || mDeviceProfile == null) return;

        // 1. Optimize RAM allocation
        optimizeRam();

        // 2. Optimize resolution
        optimizeResolution();

        // 3. Optimize renderer
        optimizeRenderer(instance);

        // 4. Apply GC & JVM args
        applyJvmOptimizations(instance);

        // 5. Enable big core affinity
        LauncherPreferences.PREF_BIG_CORE_AFFINITY = true;
    }

    private void optimizeRam() {
        int deviceTier = mDeviceProfile.deviceTier;
        int totalRam = mDeviceProfile.totalRamMB;

        int recommendedRam;
        if (deviceTier == 3) {
            // High-end: use up to 4GB
            recommendedRam = Math.min(4096, totalRam / 3);
        } else if (deviceTier == 2) {
            // Mid-range: use up to 3GB
            recommendedRam = Math.min(3072, totalRam / 3);
        } else {
            // Low-end: use up to 2GB
            recommendedRam = Math.min(2048, totalRam / 4);
        }

        // Don't allocate more than 60% of free RAM
        int maxAlloc = (int) (mDeviceProfile.freeRamMB * 0.6);
        recommendedRam = Math.min(recommendedRam, maxAlloc);
        recommendedRam = Math.max(recommendedRam, 512);

        LauncherPreferences.DEFAULT_PREF.edit()
            .putInt("allocation", recommendedRam)
            .apply();
        LauncherPreferences.PREF_RAM_ALLOCATION = recommendedRam;

        notify("RAM: " + recommendedRam + "MB (Tier " + deviceTier + ")");
    }

    private void optimizeResolution() {
        int minSide = Math.min(mDeviceProfile.screenWidth, mDeviceProfile.screenHeight);
        int deviceTier = mDeviceProfile.deviceTier;

        int targetResolution;
        if (deviceTier == 3) {
            targetResolution = 100; // Full resolution
        } else if (deviceTier == 2) {
            if (minSide >= 1440) targetResolution = 50; // QHD -> 720p
            else if (minSide >= 1080) targetResolution = 67; // FHD -> 720p
            else targetResolution = 100;
        } else {
            if (minSide >= 1440) targetResolution = 38; // QHD -> 540p
            else if (minSide >= 1080) targetResolution = 50; // FHD -> 540p
            else if (minSide >= 720) targetResolution = 75; // HD -> slight scale
            else targetResolution = 100;
        }

        LauncherPreferences.DEFAULT_PREF.edit()
            .putInt("resolutionRatio", targetResolution)
            .apply();
        LauncherPreferences.PREF_SCALE_FACTOR = targetResolution / 100f;

        notify("Resolution: " + targetResolution + "%");
    }

    private void optimizeRenderer(Instance instance) {
        // Let GameRunner's switchLtw handle renderer selection
        // Only ensure GL4ES for very low-end devices
        if (mDeviceProfile.deviceTier == 1 && mDeviceProfile.totalRamMB < 2048) {
            instance.renderer = "opengles2";
        }
    }

    private void applyJvmOptimizations(Instance instance) {
        StringBuilder args = new StringBuilder();
        int tier = mDeviceProfile.deviceTier;

        // G1GC for better GC performance
        args.append("-XX:+UseG1GC ");
        if (tier == 3) {
            args.append("-XX:MaxGCPauseMillis=50 ");
            args.append("-XX:+AlwaysPreTouch ");
        } else if (tier == 2) {
            args.append("-XX:MaxGCPauseMillis=100 ");
        } else {
            args.append("-XX:MaxGCPauseMillis=200 ");
            args.append("-XX:+UseStringDeduplication ");
        }

        // Thread optimization
        args.append("-XX:ActiveProcessorCount=").append(Math.min(mDeviceProfile.cpuCores, 4)).append(" ");

        LauncherPreferences.DEFAULT_PREF.edit()
            .putString("javaArgs", args.toString())
            .apply();
        LauncherPreferences.PREF_CUSTOM_JAVA_ARGS = args.toString();

        notify("JVM: G1GC optimized (Tier " + tier + ")");
    }

    public void startMonitoring() {
        if (mMonitoring || !mEnabled) return;
        mMonitoring = true;
    }

    public void stopMonitoring() {
        mMonitoring = false;
        mHandler.removeCallbacksAndMessages(null);
    }

    public String getOptimizationReport() {
        if (mDeviceProfile == null) return "Not analyzed yet";

        StringBuilder sb = new StringBuilder();
        sb.append("=== AI Turbo Report ===\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("CPU: ").append(mDeviceProfile.cpuName).append("\n");
        sb.append("Cores: ").append(mDeviceProfile.cpuCores).append("\n");
        sb.append("Max Freq: ").append(mDeviceProfile.maxFreqMhz).append(" MHz\n");
        sb.append("RAM: ").append(mDeviceProfile.totalRamMB).append("MB total, ");
        sb.append(mDeviceProfile.freeRamMB).append("MB free\n");
        sb.append("Display: ").append(mDeviceProfile.screenWidth).append("x");
        sb.append(mDeviceProfile.screenHeight).append("\n");
        sb.append("Arch: ").append(mDeviceProfile.is64Bit ? "64-bit" : "32-bit").append("\n");
        sb.append("Tier: ").append(mDeviceProfile.getTierName()).append("\n");
        sb.append("Allocated RAM: ").append(LauncherPreferences.PREF_RAM_ALLOCATION).append("MB\n");
        sb.append("Resolution: ").append((int)(LauncherPreferences.PREF_SCALE_FACTOR * 100)).append("%\n");
        return sb.toString();
    }

    private void notify(String message) {
        if (mCallback != null) mCallback.onOptimizationApplied(message);
    }
}
