package net.kdt.pojavlaunch.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import git.artdeell.mojo.R;

public class RendererCompatUtil {
    private static RenderersList sCompatibleRenderers;

    public static boolean checkVulkanSupport(PackageManager packageManager) {
        if(SDK_INT >= Build.VERSION_CODES.N) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION);
        }
        return false;
    }

    /** Return all renderers - let user choose freely */
    public static RenderersList getCompatibleRenderers(Context context) {
        if(sCompatibleRenderers != null) return sCompatibleRenderers;
        Resources resources = context.getResources();
        String[] defaultRenderers = resources.getStringArray(R.array.renderer_values);
        String[] defaultRendererNames = resources.getStringArray(R.array.renderer);
        List<String> rendererIds = new ArrayList<>(defaultRenderers.length);
        for(int i = 0; i < defaultRenderers.length; i++) {
            rendererIds.add(defaultRenderers[i]);
        }
        sCompatibleRenderers = new RenderersList(rendererIds, defaultRendererNames);
        return sCompatibleRenderers;
    }

    /** Always return true - let user pick any renderer */
    public static boolean checkRendererCompatible(Context context, String rendererName) {
         return true;
    }

    /** Releases the cache of compatible renderers. */
    public static void releaseRenderersCache() {
        sCompatibleRenderers = null;
        System.gc();
    }

    public static class RenderersList {
        public final List<String> rendererIds;
        public final String[] rendererDisplayNames;

        public RenderersList(List<String> rendererIds, String[] rendererDisplayNames) {
            this.rendererIds = rendererIds;
            this.rendererDisplayNames = rendererDisplayNames;
        }
    }
}
