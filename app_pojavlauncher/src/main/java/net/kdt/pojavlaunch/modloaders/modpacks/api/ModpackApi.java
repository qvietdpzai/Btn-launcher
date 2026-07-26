package net.kdt.pojavlaunch.modloaders.modpacks.api;


import android.content.Context;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.PojavApplication;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail;
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface ModpackApi {

    SearchResult searchMod(SearchFilters searchFilters, SearchResult previousPageResult);

    default SearchResult searchMod(SearchFilters searchFilters) {
        return searchMod(searchFilters, null);
    }

    ModDetail getModDetails(ModItem item);

    default void handleModpackInstallation(Context context, ModDetail modDetail, int selectedVersion) {
        if (modDetail == null) {
            Tools.showErrorRemote("No mod selected", new IOException("null modDetail"));
            return;
        }
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.global_waiting);
        PojavApplication.sExecutorService.execute(() -> {
            try {
                if (modDetail.contentType == Constants.CONTENT_TYPE_MOD ||
                    modDetail.contentType == Constants.CONTENT_TYPE_SHADER ||
                    modDetail.contentType == Constants.CONTENT_TYPE_RESOURCE_PACK) {
                    installSingleContent(context, modDetail, selectedVersion);
                } else {
                    installModpack(modDetail, selectedVersion);
                }
            }catch (IOException e) {
                Tools.showErrorRemote(context, R.string.modpack_install_download_failed, e);
            } finally {
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            }
        });
    }

    static void installSingleContent(Context context, ModDetail modDetail, int selectedVersion) throws IOException {
        if (modDetail == null || modDetail.versionUrls == null) {
            throw new IOException("Invalid mod detail");
        }
        if (selectedVersion < 0 || selectedVersion >= modDetail.versionUrls.length) {
            selectedVersion = 0;
        }
        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            throw new IOException(context.getString(R.string.no_instance));
        }
        File gameDir = instance.getGameDirectory();
        String folderName;
        switch (modDetail.contentType) {
            case Constants.CONTENT_TYPE_SHADER:
                folderName = "shaderpacks";
                break;
            case Constants.CONTENT_TYPE_RESOURCE_PACK:
                folderName = "resourcepacks";
                break;
            default:
                folderName = "mods";
                break;
        }
        File targetDir = new File(gameDir, folderName);
        targetDir.mkdirs();

        String fileUrl = modDetail.versionUrls[selectedVersion];
        String hash = (modDetail.versionHashes != null && selectedVersion < modDetail.versionHashes.length)
                ? modDetail.versionHashes[selectedVersion] : null;
        String fileName = extractFileNameFromUrl(fileUrl);
        File targetFile = new File(targetDir, fileName);

        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0,
                "Downloading " + modDetail.title);

        downloadFile(fileUrl, targetFile);

        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 100,
                "Installed to " + folderName);
    }

    static String extractFileNameFromUrl(String url) {
        String path = url.split("\\?")[0];
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return "downloaded_file";
    }

    static void downloadFile(String fileUrl, File targetFile) throws IOException {
        try (InputStream in = new URL(fileUrl).openStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    ModLoader installLocalModpack(String modpackName, File modpackFile, String icon) throws IOException;

    ModLoader installModpack(ModDetail modDetail, int selectedVersion) throws IOException;
}
