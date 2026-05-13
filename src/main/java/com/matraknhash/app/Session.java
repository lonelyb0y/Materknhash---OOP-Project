package com.matraknhash.app;

import com.matraknhash.model.User;

/** Holds the currently logged-in user + their cart. Resets at logout. */
public final class Session {
    private static User current;
    private static final Cart cart = new Cart();
    private Session() {}
    public static User current() { return current; }
    public static Cart cart() { return cart; }
    public static void set(User u) { current = u; }
    public static void clear() { current = null; cart.clear(); }
}
