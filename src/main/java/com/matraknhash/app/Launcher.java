package com.matraknhash.app;

/**
 * Non-JavaFX entry point used by the shaded fat-jar.
 * JavaFX cannot be the Main-Class of a shaded jar unless launched indirectly.
 */
public final class Launcher {
    public static void main(String[] args) { Main.main(args); }
}
