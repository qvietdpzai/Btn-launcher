package net.kdt.pojavlaunch.zerotier;

import android.content.Context;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * ZeroTier auto-connect manager.
 * Uses ZeroTier SDK (AAR) to join network automatically.
 * Place zerotier-one.aar in app_pojavlauncher/libs/ folder.
 */
public class ZeroTierManager {
    private static final String TAG = "ZeroTierManager";
    public static final String ZEROTIER_NETWORK_ID = "b103a835d2a2c7b5";
    private static ZeroTierManager sInstance;
    private boolean mInitialized = false;
    private boolean mConnected = false;
    private Context mContext;

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

    /**
     * Initialize and auto-join the ZeroTier network.
     * Requires ZeroTier AAR in libs/ and VPN permission.
     */
    public void autoConnect() {
        if (mInitialized) return;
        mInitialized = true;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Check if ZeroTier SDK is available
                Class.forName("com.zerotier.sdk.ZeroTierService");
                Log.i(TAG, "ZeroTier SDK found, joining network: " + ZEROTIER_NETWORK_ID);
                joinNetwork(ZEROTIER_NETWORK_ID);
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "ZeroTier SDK not found. Add zerotier-one.aar to libs/ folder.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize ZeroTier", e);
                if (mCallback != null) mCallback.onError(e.getMessage());
            }
        });
    }

    private void joinNetwork(String networkId) {
        try {
            // Use ZeroTier SDK API via reflection for flexibility
            Class<?> ztNodeClass = Class.forName("com.zerotier.sdk.ZeroTierNode");
            Object node = ztNodeClass.getConstructor().newInstance();

            // Start the node
            Method startMethod = ztNodeClass.getMethod("start");
            startMethod.invoke(node);

            // Join network
            Method joinMethod = ztNodeClass.getMethod("join", long.class);
            long networkIdLong = Long.parseUnsignedLong(networkId, 16);
            joinMethod.invoke(node, networkIdLong);

            mConnected = true;
            Log.i(TAG, "Joined ZeroTier network: " + networkId);
            if (mCallback != null) mCallback.onConnected(networkId);

        } catch (ClassNotFoundException e) {
            Log.w(TAG, "ZeroTier native class not found", e);
            fallbackJoin(networkId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to join network via SDK, trying fallback", e);
            fallbackJoin(networkId);
        }
    }

    /**
     * Fallback: use ZeroTier CLI if SDK reflection fails
     */
    private void fallbackJoin(String networkId) {
        try {
            Runtime.getRuntime().exec(new String[]{
                "zerotier-cli", "join", networkId
            });
            mConnected = true;
            Log.i(TAG, "Joined ZeroTier network via CLI: " + networkId);
            if (mCallback != null) mCallback.onConnected(networkId);
        } catch (Exception e) {
            Log.e(TAG, "ZeroTier CLI fallback also failed", e);
            if (mCallback != null) mCallback.onError("ZeroTier not available: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            Class<?> ztNodeClass = Class.forName("com.zerotier.sdk.ZeroTierNode");
            Object node = ztNodeClass.getConstructor().newInstance();
            Method stopMethod = ztNodeClass.getMethod("stop");
            stopMethod.invoke(node);
            mConnected = false;
            if (mCallback != null) mCallback.onDisconnected(ZEROTIER_NETWORK_ID);
        } catch (Exception e) {
            Log.e(TAG, "Failed to disconnect", e);
        }
    }

    public boolean isConnected() {
        return mConnected;
    }

    public String getNetworkId() {
        return ZEROTIER_NETWORK_ID;
    }
}
