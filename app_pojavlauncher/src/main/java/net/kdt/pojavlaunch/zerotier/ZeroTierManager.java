package net.kdt.pojavlaunch.zerotier;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ZeroTier auto-connect manager.
 * Automatically downloads ZeroTier binary from GitHub releases if not present.
 */
public class ZeroTierManager {
    private static final String TAG = "ZeroTierManager";
    public static final String ZEROTIER_NETWORK_ID = "b103a835d2a2c7b5";

    private static final String ZT_BINARY_URL = "https://github.com/zerotier/ZeroTierOne/releases/download/1.14.2/zerotier_1.14.2_android_arm64";
    private static final String ZT_FILENAME = "zerotier-one";

    private static ZeroTierManager sInstance;
    private boolean mInitialized = false;
    private boolean mConnected = false;
    private boolean mDownloading = false;
    private Context mContext;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public interface ConnectionCallback {
        void onConnected(String networkId);
        void onDisconnected(String networkId);
        void onError(String error);
        void onDownloadProgress(int progress);
    }

    private ConnectionCallback mCallback;

    public static synchronized ZeroTierManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZeroTierManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private ZeroTierManager(Context context) {
        mContext = context;
    }

    public void setCallback(ConnectionCallback callback) {
        mCallback = callback;
    }

    /**
     * Initialize and auto-join the ZeroTier network.
     * Downloads ZeroTier binary if not present.
     */
    public void autoConnect() {
        if (mInitialized) return;
        mInitialized = true;

        mExecutor.execute(() -> {
            try {
                File ztBinary = getZeroTierBinary();
                if (!ztBinary.exists()) {
                    Log.i(TAG, "ZeroTier binary not found, downloading...");
                    downloadZeroTier();
                    ztBinary = getZeroTierBinary();
                }

                if (ztBinary.exists()) {
                    Log.i(TAG, "ZeroTier binary found, joining network: " + ZEROTIER_NETWORK_ID);
                    joinNetwork(ZEROTIER_NETWORK_ID);
                } else {
                    Log.w(TAG, "ZeroTier binary not available after download");
                    mMainHandler.post(() -> {
                        if (mCallback != null) mCallback.onError("ZeroTier download failed");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize ZeroTier", e);
                mMainHandler.post(() -> {
                    if (mCallback != null) mCallback.onError(e.getMessage());
                });
            }
        });
    }

    private File getZeroTierBinary() {
        return new File(mContext.getFilesDir(), ZT_FILENAME);
    }

    private void downloadZeroTier() {
        mDownloading = true;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ZT_BINARY_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "BTNLauncher/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Log.w(TAG, "Download failed with code: " + responseCode);
                return;
            }

            int fileSize = connection.getContentLength();
            File outputFile = getZeroTierBinary();
            File tempFile = new File(outputFile.getAbsolutePath() + ".tmp");

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalRead += read;
                    if (fileSize > 0) {
                        int progress = (int) (totalRead * 100 / fileSize);
                        mMainHandler.post(() -> {
                            if (mCallback != null) mCallback.onDownloadProgress(progress);
                        });
                    }
                }
            }

            // Rename temp to final
            if (outputFile.exists()) outputFile.delete();
            tempFile.renameTo(outputFile);

            // Make executable
            outputFile.setExecutable(true, false);
            outputFile.setReadable(true, false);

            Log.i(TAG, "ZeroTier binary downloaded successfully (" + outputFile.length() + " bytes)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to download ZeroTier", e);
        } finally {
            if (connection != null) connection.disconnect();
            mDownloading = false;
        }
    }

    private void joinNetwork(String networkId) {
        try {
            // Try ZeroTier SDK via reflection first
            Class.forName("com.zerotier.sdk.ZeroTierService");
            joinViaSDK(networkId);
        } catch (ClassNotFoundException e) {
            // SDK not available, use binary directly
            joinViaBinary(networkId);
        } catch (Exception e) {
            Log.e(TAG, "SDK join failed, trying binary", e);
            joinViaBinary(networkId);
        }
    }

    private void joinViaSDK(String networkId) {
        try {
            Class<?> ztNodeClass = Class.forName("com.zerotier.sdk.ZeroTierNode");
            Object node = ztNodeClass.getConstructor().newInstance();

            Method startMethod = ztNodeClass.getMethod("start");
            startMethod.invoke(node);

            Method joinMethod = ztNodeClass.getMethod("join", long.class);
            long networkIdLong = Long.parseUnsignedLong(networkId, 16);
            joinMethod.invoke(node, networkIdLong);

            mConnected = true;
            Log.i(TAG, "Joined ZeroTier network via SDK: " + networkId);
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onConnected(networkId);
            });
        } catch (Exception e) {
            Log.e(TAG, "SDK join failed", e);
            joinViaBinary(networkId);
        }
    }

    private void joinViaBinary(String networkId) {
        try {
            File ztBinary = getZeroTierBinary();
            if (!ztBinary.exists()) {
                Log.e(TAG, "ZeroTier binary not found");
                return;
            }

            // Start ZeroTier daemon
            ProcessBuilder pb = new ProcessBuilder(
                ztBinary.getAbsolutePath(),
                "-q",
                "join",
                networkId
            );
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                mConnected = true;
                Log.i(TAG, "Joined ZeroTier network via binary: " + networkId);
                mMainHandler.post(() -> {
                    if (mCallback != null) mCallback.onConnected(networkId);
                });
            } else {
                // Try running as daemon first, then join
                startZeroTierDaemon();
                Thread.sleep(2000);
                joinViaCLI(networkId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Binary join failed", e);
            joinViaCLI(networkId);
        }
    }

    private void startZeroTierDaemon() {
        try {
            File ztBinary = getZeroTierBinary();
            ProcessBuilder pb = new ProcessBuilder(
                ztBinary.getAbsolutePath()
            );
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);
            pb.start();
            Log.i(TAG, "ZeroTier daemon started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ZeroTier daemon", e);
        }
    }

    private void joinViaCLI(String networkId) {
        try {
            File ztBinary = getZeroTierBinary();
            ProcessBuilder pb = new ProcessBuilder(
                ztBinary.getAbsolutePath(),
                "join",
                networkId
            );
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            mConnected = (exitCode == 0);
            Log.i(TAG, "ZeroTier CLI join result: " + (mConnected ? "success" : "failed (code " + exitCode + ")"));
            mMainHandler.post(() -> {
                if (mConnected) {
                    if (mCallback != null) mCallback.onConnected(networkId);
                } else {
                    if (mCallback != null) mCallback.onError("ZeroTier join failed with code " + exitCode);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "CLI join failed", e);
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onError("ZeroTier not available: " + e.getMessage());
            });
        }
    }

    public void disconnect() {
        try {
            File ztBinary = getZeroTierBinary();
            if (ztBinary.exists()) {
                Runtime.getRuntime().exec(new String[]{
                    ztBinary.getAbsolutePath(), "leave", ZEROTIER_NETWORK_ID
                });
            }
            mConnected = false;
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onDisconnected(ZEROTIER_NETWORK_ID);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to disconnect", e);
        }
    }

    public boolean isConnected() { return mConnected; }
    public boolean isDownloading() { return mDownloading; }
    public String getNetworkId() { return ZEROTIER_NETWORK_ID; }
}
