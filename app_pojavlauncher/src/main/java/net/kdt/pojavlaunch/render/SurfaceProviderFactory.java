package net.kdt.pojavlaunch.render;

import android.content.Context;
import android.os.Build;

import net.kdt.pojavlaunch.Tools;

public class SurfaceProviderFactory {
    public enum RendererType { VULKAN, OPENGLES_SURFACEVIEW, OPENGLES_TEXTUREVIEW }

    public static SurfaceProvider createBest(Context context, SurfaceProvider.SurfaceCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { if (isVulkanAvailable()) { Tools.log("Using Vulkan renderer"); return new VulkanSurfaceProvider().create(context, callback); } }
            catch (Exception e) { Tools.log("Vulkan not available: " + e.getMessage()); }
        }
        Tools.log("Using SurfaceView renderer");
        return new SurfaceViewSurfaceProvider().create(context, callback);
    }

    public static SurfaceProvider create(Context context, SurfaceProvider.SurfaceCallback callback, RendererType type) {
        switch (type) {
            case VULKAN:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isVulkanAvailable()) return new VulkanSurfaceProvider().create(context, callback);
            case OPENGLES_SURFACEVIEW:
                return new SurfaceViewSurfaceProvider().create(context, callback);
            case OPENGLES_TEXTUREVIEW:
                return new TextureViewSurfaceProvider().create(context, callback);
            default:
                return createBest(context, callback);
        }
    }

    private static boolean isVulkanAvailable() {
        try { Class.forName("android.graphics.Vulkan"); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
}
