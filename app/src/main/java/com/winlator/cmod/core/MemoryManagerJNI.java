package com.winlator.cmod.core;

import android.os.Debug;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class MemoryManagerJNI {

    private static final String TAG =
            "MemoryManagerJNI";

    private static boolean initialized = false;

    private MemoryManagerJNI() {
    }

    /**
     * Inicializa o Memory Manager.
     *
     * Não exige uma biblioteca nativa para funcionar.
     */
    public static synchronized boolean initMemoryManager() {

        if (initialized) {
            return true;
        }

        try {

            initialized = true;

            Log.i(
                    TAG,
                    "Memory Manager inicializado"
            );

            return true;

        } catch (Throwable e) {

            initialized = false;

            Log.e(
                    TAG,
                    "Erro inicializando Memory Manager",
                    e
            );

            return false;
        }
    }

    /**
     * Libera o estado do Memory Manager.
     */
    public static synchronized void cleanupMemoryManager() {

        initialized = false;

        Log.i(
                TAG,
                "Memory Manager finalizado"
        );
    }

    /**
     * Retorna estatísticas de memória.
     */
    public static MemoryStats getStats() {

        try {

            Runtime runtime =
                    Runtime.getRuntime();

            long max =
                    runtime.maxMemory();

            long total =
                    runtime.totalMemory();

            long free =
                    runtime.freeMemory();

            long used =
                    total - free;

            double usagePercent =
                    max > 0
                            ? (used * 100.0) / max
                            : 0.0;

            return new MemoryStats(
                    used,
                    free,
                    total,
                    max,
                    usagePercent
            );

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro obtendo estatísticas",
                    e
            );

            return new MemoryStats(
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
    }

    /**
     * Retorna informações de memória em texto.
     */
    public static String getMemoryInfo() {

        MemoryStats stats =
                getStats();

        return String.format(
                Locale.US,
                "Used: %s MB | Free: %s MB | Total: %s MB | Max: %s MB | Usage: %.1f%%",
                bytesToMB(stats.usedBytes),
                bytesToMB(stats.freeBytes),
                bytesToMB(stats.totalBytes),
                bytesToMB(stats.maxBytes),
                stats.usagePercent
        );
    }

    /**
     * Retorna memória disponível aproximada para o processo.
     */
    public static long getAvailableMemoryBytes() {

        try {

            Runtime runtime =
                    Runtime.getRuntime();

            long max =
                    runtime.maxMemory();

            long total =
                    runtime.totalMemory();

            long free =
                    runtime.freeMemory();

            long available =
                    max - (total - free);

            return Math.max(
                    0,
                    available
            );

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro obtendo memória disponível",
                    e
            );

            return 0;
        }
    }

    /**
     * Aloca memória Java.
     *
     * Retorna um identificador interno.
     *
     * Esta função não aumenta a RAM física do dispositivo.
     */
    public static long allocateMemory(
            long size) {

        if (size <= 0) {
            return 0;
        }

        /*
         * Não fazemos uma alocação falsa de memória
         * para evitar causar OOM no aplicativo.
         *
         * A alocação real deve ser feita pelo código
         * que efetivamente precisa do buffer.
         */
        long available =
                getAvailableMemoryBytes();

        if (size > available) {

            Log.w(
                    TAG,
                    "Memória insuficiente: "
                            + bytesToMB(size)
                            + " MB solicitados, "
                            + bytesToMB(available)
                            + " MB disponíveis"
            );

            return 0;
        }

        /*
         * Identificador baseado em tempo/thread.
         *
         * Para buffers reais, use ByteBuffer.allocateDirect()
         * ou uma implementação JNI específica.
         */
        long id =
                System.nanoTime();

        if (id == 0) {
            id = 1;
        }

        return id;
    }

    /**
     * Libera o identificador retornado por allocateMemory().
     */
    public static void freeMemory(
            long pointer) {

        if (pointer == 0) {
            return;
        }

        Log.d(
                TAG,
                "Memória liberada: "
                        + pointer
        );
    }

    /**
     * Converte bytes para MB.
     */
    public static long bytesToMB(
            long bytes) {

        return bytes /
                (1024L * 1024L);
    }

    /**
     * Converte bytes para GB.
     */
    public static double bytesToGB(
            long bytes) {

        return bytes /
                (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * Retorna memória PSS do processo atual.
     */
    public static long getProcessMemoryBytes() {

        try {

            Debug.MemoryInfo info =
                    new Debug.MemoryInfo();

            Debug.getMemoryInfo(info);

            return info.getTotalPss()
                    * 1024L;

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro obtendo memória do processo",
                    e
            );

            return 0;
        }
    }

    /**
     * Retorna PID do processo.
     */
    public static int getProcessId() {
        return Process.myPid();
    }

    /**
     * Verifica se o Memory Manager está inicializado.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Estrutura de estatísticas de memória.
     */
    public static final class MemoryStats {

        public final long usedBytes;
        public final long freeBytes;
        public final long totalBytes;
        public final long maxBytes;
        public final double usagePercent;

        public MemoryStats(
                long usedBytes,
                long freeBytes,
                long totalBytes,
                long maxBytes,
                double usagePercent) {

            this.usedBytes =
                    usedBytes;

            this.freeBytes =
                    freeBytes;

            this.totalBytes =
                    totalBytes;

            this.maxBytes =
                    maxBytes;

            this.usagePercent =
                    usagePercent;
        }

        public long getUsedBytes() {
            return usedBytes;
        }

        public long getFreeBytes() {
            return freeBytes;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public long getMaxBytes() {
            return maxBytes;
        }

        public double getUsagePercent() {
            return usagePercent;
        }

        @NonNull
        @Override
        public String toString() {

            return String.format(
                    Locale.US,
                    "MemoryStats{" +
                            "used=%d MB, " +
                            "free=%d MB, " +
                            "total=%d MB, " +
                            "max=%d MB, " +
                            "usage=%.1f%%}",
                    bytesToMB(usedBytes),
                    bytesToMB(freeBytes),
                    bytesToMB(totalBytes),
                    bytesToMB(maxBytes),
                    usagePercent
            );
        }
    }
  }
