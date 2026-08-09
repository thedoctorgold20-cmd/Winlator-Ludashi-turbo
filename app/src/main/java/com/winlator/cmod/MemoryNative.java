package com.winlator;

public final class MemoryNative {

    static {
        System.loadLibrary("winlator_memory");
    }

    public static native void initialize();

    public static native long getConfiguredRAM();

    public static native long getPhysicalRAM();

    private MemoryNative() {
    }
}
