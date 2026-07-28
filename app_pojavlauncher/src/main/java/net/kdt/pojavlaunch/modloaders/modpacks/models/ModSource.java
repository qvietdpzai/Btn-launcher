package net.kdt.pojavlaunch.modloaders.modpacks.models;

public class ModSource {
    public int apiSource;
    public int contentType;
    public boolean isModpack;

    public boolean isModpack() {
        return contentType == Constants.CONTENT_TYPE_MODPACK;
    }
}
