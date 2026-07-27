package net.kdt.pojavlaunch.zerotier;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZeroTierManager {
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

    public void autoConnect() {
        if (mInitialized) return;
        mInitialized = true;

        mExecutor.execute(() -> {
            try {
                File ztBinary = getZeroTierBinary();
                if (!ztBinary.exists()) {
                    downloadZeroTier();
                    ztBinary = getZeroTierBinary();
                }

                if (ztBinary.exists()) {
                    joinNetwork(ZEROTIER_NETWORK_ID);
                } else {
                    mMainHandler.post(() -> {
                        if (mCallback != null) mCallback.onError("ZeroTier download failed");
                    });
                }
            } catch (Exception e) {
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
            if (responseCode != 200) return;

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

            if (outputFile.exists()) outputFile.delete();
            tempFile.renameTo(outputFile);
            outputFile.setExecutable(true, false);
            outputFile.setReadable(true, false);
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
            mDownloading = false;
        }
    }

    private void joinNetwork(String networkId) {
        try {
            Class.forName("com.zerotier.sdk.ZeroTierService");
            joinViaSDK(networkId);
        } catch (ClassNotFoundException e) {
            joinViaBinary(networkId);
        } catch (Exception e) {
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
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onConnected(networkId);
            });
        } catch (Exception e) {
            joinViaBinary(networkId);
        }
    }

    private void joinViaBinary(String networkId) {
        try {
            File ztBinary = getZeroTierBinary();
            if (!ztBinary.exists()) return;

            ProcessBuilder pb = new ProcessBuilder(
                ztBinary.getAbsolutePath(), "-q", "join", networkId
            );
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                mConnected = true;
                mMainHandler.post(() -> {
                    if (mCallback != null) mCallback.onConnected(networkId);
                });
            } else {
                startZeroTierDaemon();
                Thread.sleep(2000);
                joinViaCLI(networkId);
            }
        } catch (Exception e) {
            joinViaCLI(networkId);
        }
    }

    private void startZeroTierDaemon() {
        try {
            File ztBinary = getZeroTierBinary();
            ProcessBuilder pb = new ProcessBuilder(ztBinary.getAbsolutePath());
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);
            pb.start();
        } catch (Exception ignored) {
        }
    }

    private void joinViaCLI(String networkId) {
        try {
            File ztBinary = getZeroTierBinary();
            ProcessBuilder pb = new ProcessBuilder(
                ztBinary.getAbsolutePath(), "join", networkId
            );
            pb.directory(mContext.getFilesDir());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            mConnected = (exitCode == 0);
            mMainHandler.post(() -> {
                if (mConnected) {
                    if (mCallback != null) mCallback.onConnected(networkId);
                } else {
                    if (mCallback != null) mCallback.onError("ZeroTier join failed");
                }
            });
        } catch (Exception e) {
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onError("ZeroTier not available");
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
        } catch (Exception ignored) {
        }
    }

    public boolean isConnected() { return mConnected; }
    public boolean isDownloading() { return mDownloading; }
    public String getNetworkId() { return ZEROTIER_NETWORK_ID; }
}
