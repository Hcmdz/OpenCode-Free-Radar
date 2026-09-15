# T001 scaffold — project-specific keep rules go here (F6+ features).

# Release hardening: strip all log calls (no Log.* ships in release builds).
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
