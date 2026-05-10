package com.matraknhash.app;

import com.matraknhash.model.User;

/** Holds the currently logged-in user. Single mutable global is OK for desktop app. */
public final class Session {
    private static User current;
    private Session() {}
    public static User current() { return current; }
    public static void set(User u) { current = u; }
    public static void clear() { current = null; }
}
