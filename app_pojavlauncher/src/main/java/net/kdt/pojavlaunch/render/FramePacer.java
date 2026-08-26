package net.kdt.pojavlaunch.render;

import android.view.Choreographer;

public class FramePacer {
    private final Choreographer mChoreographer;
    private final FrameCallback mCallback;
    private long mFrameIntervalNanos, mLastFrameTimeNanos;
    private boolean mRunning = false;

    public interface FrameCallback { void onFrame(long frameTimeNanos); }

    public FramePacer(FrameCallback callback) {
        mChoreographer = Choreographer.getInstance();
        mCallback = callback;
        mFrameIntervalNanos = (long)(1_000_000_000.0 / 60.0);
    }

    public void start() { if (mRunning) return; mRunning = true; mLastFrameTimeNanos = System.nanoTime(); postFrame(); }
    public void stop() { mRunning = false; mChoreographer.removeFrameCallback(mFrameCallback); }

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override public void doFrame(long frameTimeNanos) {
            if (!mRunning) return;
            long elapsed = frameTimeNanos - mLastFrameTimeNanos;
            if (elapsed >= mFrameIntervalNanos) { mLastFrameTimeNanos = frameTimeNanos; mCallback.onFrame(frameTimeNanos); }
            postFrame();
        }
    };
    private void postFrame() { mChoreographer.postFrameCallback(mFrameCallback); }
    public void setTargetFrameRate(float fps) { mFrameIntervalNanos = (long)(1_000_000_000.0 / fps); }
}
