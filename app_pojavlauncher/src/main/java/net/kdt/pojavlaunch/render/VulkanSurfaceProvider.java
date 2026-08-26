package net.kdt.pojavlaunch.render;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import net.kdt.pojavlaunch.CallbackBridge;

@RequiresApi(api = 24)
public class VulkanSurfaceProvider implements SurfaceProvider {
    private VulkanSurfaceView mSurfaceView;

    @Override
    public View create(Context context, SurfaceCallback callback) {
        mSurfaceView = new VulkanSurfaceView(context, callback);
        return mSurfaceView;
    }

    @Override
    public void updateSize() {
        if (mSurfaceView != null) mSurfaceView.updateSize();
    }

    private static class VulkanSurfaceView extends SurfaceView {
        private final SurfaceCallback mCallback;

        VulkanSurfaceView(Context context, SurfaceCallback callback) {
            super(context);
            mCallback = callback;
            setEGLContextClientVersion(3);
            getHolder().addCallback(new CallbackAdapter());
            if (CallbackBridge.windowWidth != 0 && CallbackBridge.windowHeight != 0) {
                getHolder().setFixedSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
            }
        }

        void updateSize() {
            getHolder().setFixedSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
        }

        private class CallbackAdapter implements SurfaceHolder.Callback {
            @Override public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { mCallback.onSurfaceResized(); }
            @Override public void surfaceCreated(@NonNull SurfaceHolder holder) { mCallback.onSurfaceAvailable(holder.getSurface()); }
            @Override public void surfaceDestroyed(@NonNull SurfaceHolder holder) { mCallback.onSurfaceDestroyed(); }
        }
    }
}
