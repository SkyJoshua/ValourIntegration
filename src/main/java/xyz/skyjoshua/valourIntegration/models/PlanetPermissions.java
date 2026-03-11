package xyz.skyjoshua.valourIntegration.models;

public class PlanetPermissions {
    public static final long FullControl    = -1L;
    public static final long View           = 1L;
    public static final long Invite         = 2L;
    public static final long DisplayRole    = 4L;
    public static final long Manage         = 8L;
    public static final long Kick           = 16L;
    public static final long Ban            = 32L;
    public static final long CreateChannels = 64L;
    public static final long ManageRoles    = 128L;
    public static final long UseEconomy     = 256L;
    public static final long ManageCurrency = 512L;
    public static final long ManageEcoAccounts  = 1024L;
    public static final long ForceTransactions  = 2048L;
    public static final long MentionAll     = 4096L;
    public static final long BypassAutomod  = 8192L;
    public static final long UseCustomEmojis = 16384L;

    // Mirrors Valour's Default: View | UseEconomy | UseCustomEmojis
    public static final long Default = View | UseEconomy | UseCustomEmojis;

    // Check if a combined permission value includes a specific permission
    public static boolean Has(long combinedPermissions, long permission) {
        if (combinedPermissions == FullControl) return true;
        return (combinedPermissions & permission) == permission;
    }
}