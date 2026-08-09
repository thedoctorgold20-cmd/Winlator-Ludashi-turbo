package com.winlator.cmod.core;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

/**
 * Memory
 *
 * Gerenciador de memória independente para o Winlator Ludashi 3.1.
 *
 * IMPORTANTE:
 * O limite de 16 GB é apenas lógico.
 * Não aumenta a RAM física do aparelho.
 *
 * Não utiliza JNI.
 * Não depende de Box86/Box64.
 */
public final class Memory {

    private static final String TAG =
            "LudashiMemory";

    /**
     * Limite lógico de memória.
     */
    public static final long LOGICAL_MEMORY =
            16L
            * 1024L
            * 1024L
            * 1024L;

    /**
     * Limite máximo recomendado para uma única
     * operação Java.
     */
    private static final long MAX_OPERATION =
            1024L
            * 1024L
            * 1024L;

    private Memory() {
    }

    /**
     * Inicializa o gerenciador.
     */
    public static void init(Context context) {

        Log.i(
                TAG,
                "Memory iniciado"
        );

        Log.i(
                TAG,
                "Limite lógico: "
                        + formatBytes(
                        LOGICAL_MEMORY
                )
        );

        log(context);
    }

    /**
     * RAM total do dispositivo.
     */
    public static long getTotalRam(
            Context context) {

        if (context == null) {
            return 0;
        }

        ActivityManager manager =
                (ActivityManager)
                        context.getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (manager == null) {
            return 0;
        }

        ActivityManager.MemoryInfo info =
                new ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        return info.totalMem;
    }

    /**
     * RAM disponível.
     */
    public static long getAvailableRam(
            Context context) {

        if (context == null) {
            return 0;
        }

        ActivityManager manager =
                (ActivityManager)
                        context.getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (manager == null) {
            return 0;
        }

        ActivityManager.MemoryInfo info =
                new ActivityManager.MemoryInfo();

        manager.getMemoryInfo(info);

        return info.availMem;
    }

    /**
     * Memória utilizada pelo processo.
     */
    public static long getProcessMemory() {

        Debug.MemoryInfo info =
                new Debug.MemoryInfo();

        Debug.getMemoryInfo(info);

        return info.getTotalPss()
                * 1024L;
    }

    /**
     * Limite lógico.
     */
    public static long getLogicalMemory() {
        return LOGICAL_MEMORY;
    }

    /**
     * Retorna percentual de utilização
     * da memória física disponível.
     */
    public static int getUsagePercent(
            Context context) {

        long total =
                getTotalRam(context);

        long available =
                getAvailableRam(context);

        if (total <= 0) {
            return 0;
        }

        long used =
                total - available;

        int percent =
                (int)
                        ((used * 100L)
                                / total);

        if (percent < 0) {
            percent = 0;
        }

        if (percent > 100) {
            percent = 100;
        }

        return percent;
    }

    /**
     * Verifica se uma operação é aceitável.
     */
    public static boolean canAllocate(
            Context context,
            long bytes) {

        if (bytes <= 0) {
            return false;
        }

        if (bytes > MAX_OPERATION) {
            return false;
        }

        long available =
                getAvailableRam(context);

        /*
         * Mantém uma margem de segurança
         * de 256 MB.
         */
        long safety =
                256L
                * 1024L
                * 1024L;

        return available >
                bytes + safety;
    }

    /**
     * Solicita coleta de lixo.
     */
    public static void trim() {

        try {
            System.gc();
        }
        catch (Throwable ignored) {
        }
    }

    /**
     * Registra informações no Logcat.
     */
    public static void log(
            Context context) {

        if (context == null) {
            return;
        }

        long total =
                getTotalRam(context);

        long available =
                getAvailableRam(context);

        long process =
                getProcessMemory();

        Log.i(
                TAG,
                "RAM total: "
                        + formatBytes(total)
        );

        Log.i(
                TAG,
                "RAM disponível: "
                        + formatBytes(available)
        );

        Log.i(
                TAG,
                "Processo: "
                        + formatBytes(process)
        );

        Log.i(
                TAG,
                "Limite lógico: "
                        + formatBytes(
                        LOGICAL_MEMORY
                )
        );
    }

    /**
     * Formata bytes.
     */
    public static String formatBytes(
            long bytes) {

        if (bytes < 1024L) {
            return bytes + " B";
        }

        double value =
                bytes / 1024.0;

        if (value < 1024.0) {
            return String.format(
                    "%.1f KB",
                    value
            );
        }

        value /= 1024.0;

        if (value < 1024.0) {
            return String.format(
                    "%.1f MB",
                    value
            );
        }

        value /= 1024.0;

        return String.format(
                "%.2f GB",
                value
        );
    }
}
