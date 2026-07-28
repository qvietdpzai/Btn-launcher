package net.kdt.pojavlaunch.zerotier;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZeroTierManager {
    public static final String ZEROTIER_NETWORK_ID = "b103a835d2a2c7b5";
    private static final String ZT_PACKAGE = "com.zerotier.one";
    private static final String ZT_PLAY_STORE = "market://details?id=" + ZT_PACKAGE;

    private static ZeroTierManager sInstance;
    private boolean mConnected = false;
    private Context mContext;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public interface ConnectionCallback {
        void onConnected(String networkId);
        void onDisconnected(String networkId);
        void onError(String error);
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

    private boolean isZeroTierInstalled() {
        try {
            mContext.getPackageManager().getPackageInfo(ZT_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void connect() {
        if (mConnected) {
            disconnect();
            return;
        }

        if (!isZeroTierInstalled()) {
            mMainHandler.post(() -> {
                if (mCallback != null) mCallback.onError("ZeroTier app not installed");
            });
            return;
        }

        mExecutor.execute(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("zerotier://network/" + ZEROTIER_NETWORK_ID));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);

                mConnected = true;
                mMainHandler.post(() -> {
                    if (mCallback != null) mCallback.onConnected(ZEROTIER_NETWORK_ID);
                });
            } catch (Exception e) {
                mMainHandler.post(() -> {
                    if (mCallback != null) mCallback.onError("Failed to open ZeroTier: " + e.getMessage());
                });
            }
        });
    }

    public void disconnect() {
        mConnected = false;
        mMainHandler.post(() -> {
            if (mCallback != null) mCallback.onDisconnected(ZEROTIER_NETWORK_ID);
        });
    }

    public void openPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ZT_PLAY_STORE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + ZT_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    public boolean isConnected() { return mConnected; }
    public String getNetworkId() { return ZEROTIER_NETWORK_ID; }
}
