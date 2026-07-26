package net.kdt.pojavlaunch.modloaders.modpacks.models;

import org.jetbrains.annotations.Nullable;

/**
 * Search filters, passed to APIs
 */
public class SearchFilters {
    public boolean isModpack;
    public int contentType = Constants.CONTENT_TYPE_MODPACK;
    public String name;
    @Nullable public String mcVersion;

}
