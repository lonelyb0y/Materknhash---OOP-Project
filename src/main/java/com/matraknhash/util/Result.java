package com.matraknhash.util;

/**
 * Generic Result wrapper. Used as a lightweight Either&lt;Error,Value&gt;
 * to avoid sprinkling try/catch through controllers.
 */
public final class Result<T> {
    private final T value;
    private final String error;

    private Result(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Result<T> ok(T value) { return new Result<>(value, null); }
    public static <T> Result<T> fail(String msg) { return new Result<>(null, msg); }

    public boolean isOk()   { return error == null; }
    public boolean isFail() { return error != null; }
    public T value()        { return value; }
    public String error()   { return error; }
}
