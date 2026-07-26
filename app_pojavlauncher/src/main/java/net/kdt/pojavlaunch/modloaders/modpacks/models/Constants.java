package net.kdt.pojavlaunch.modloaders.modpacks.models;

public class Constants {
    private Constants(){}

    /** Types of modpack apis */
    public static final int SOURCE_MODRINTH = 0x0;
    public static final int SOURCE_CURSEFORGE = 0x1;
    public static final int SOURCE_TECHNIC = 0x2;

    /** Modrinth api, file environments */
    public static final String MODRINTH_FILE_ENV_REQUIRED = "required";
    public static final String MODRINTH_FILE_ENV_OPTIONAL = "optional";
    public static final String MODRINTH_FILE_ENV_UNSUPPORTED = "unsupported";

    /** Content types for search/install */
    public static final int CONTENT_TYPE_MODPACK = 0;
    public static final int CONTENT_TYPE_MOD = 1;
    public static final int CONTENT_TYPE_SHADER = 2;
    public static final int CONTENT_TYPE_RESOURCE_PACK = 3;

}
