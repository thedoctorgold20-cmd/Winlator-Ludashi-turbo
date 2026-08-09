package com.winlator.cmod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.core.MemoryManagerJNI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Versão otimizada do ContainerDetailFragment com gerenciamento de memória nativa
 */
public class ContainerDetailFragmentOptimized extends Fragment {
    
    private static final String TAG = "ContainerDetail";
    private static final ExecutorService CONTENT_IO_EXECUTOR = Executors.newFixedThreadPool(2);
    
    // Memory Manager
    private MemoryManagerJNI.MemoryStats memoryStats;
    private static boolean memoryManagerInitialized = false;
    
    // ... (resto das variáveis da classe original) ...
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar Memory Manager uma única vez
        if (!memoryManagerInitialized) {
            initializeMemoryManager();
        }
        
        // Resto da inicialização original
        setHasOptionsMenu(false);
    }
    
    /**
     * Inicializa o gerenciador de memória
     */
    private void initializeMemoryManager() {
        if (MemoryManagerJNI.initMemoryManager()) {
            memoryManagerInitialized = true;
            Log.i(TAG, "Memory Manager inicializado com sucesso");
            
            // Log estatísticas iniciais
            logMemoryStats();
        } else {
            Log.e(TAG, "Erro ao inicializar Memory Manager");
        }
    }
    
    /**
     * Log das estatísticas de memória
     */
    private void logMemoryStats() {
        memoryStats = MemoryManagerJNI.getStats();
        Log.d(TAG, "=== ESTATÍSTICAS DE MEMÓRIA ===");
        Log.d(TAG, memoryStats.toString());
        Log.d(TAG, "Info: " + MemoryManagerJNI.getMemoryInfo());
    }
    
    /**
     * Aloca memória para buffers grandes
     * @param size Tamanho em bytes
     * @return Ponteiro de memória
     */
    public long allocateNativeMemory(long size) {
        long ptr = MemoryManagerJNI.allocateMemory(size);
        if (ptr == 0) {
            Log.e(TAG, "Falha ao alocar " + size + " bytes");
            return 0;
        }
        Log.d(TAG, "Alocados " + MemoryManagerJNI.bytesToMB(size) + " MB");
        return ptr;
    }
    
    /**
     * Libera memória alocada
     * @param ptr Ponteiro de memória
     */
    public void freeNativeMemory(long ptr) {
        if (ptr != 0) {
            MemoryManagerJNI.freeMemory(ptr);
            Log.d(TAG, "Memória liberada: " + ptr);
        }
    }
    
    /**
     * Monitora uso de memória em thread separada
     */
    private void startMemoryMonitoring() {
        new Thread(() -> {
            while (isAdded()) {
                memoryStats = MemoryManagerJNI.getStats();
                
                // Alertar se uso > 90%
                if (memoryStats.usagePercent > 90.0) {
                    Log.w(TAG, "AVISO: Uso de memória crítico! " + memoryStats.usagePercent + "%");
                    // Pode disparar garbage collection agressivo
                    System.gc();
                }
                
                try {
                    Thread.sleep(5000); // Verificar a cada 5 segundos
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Iniciar monitoramento de memória
        startMemoryMonitoring();
        
        // Adicionar indicador de memória à UI (opcional)
        addMemoryIndicatorToUI(view);
    }
    
    /**
     * Adiciona indicador visual de uso de memória
     */
    private void addMemoryIndicatorToUI(View view) {
        // Localizar um LinearLayout adequado ou criar um novo
        LinearLayout container = view.findViewById(android.R.id.content);
        if (container != null) {
            LinearLayout memoryIndicator = new LinearLayout(getContext());
            memoryIndicator.setOrientation(LinearLayout.HORIZONTAL);
            memoryIndicator.setPadding(16, 8, 16, 8);
            memoryIndicator.setBackgroundColor(Color.parseColor("#1E1E1E"));
            
            TextView memoryLabel = new TextView(getContext());
            memoryLabel.setTextColor(Color.WHITE);
            memoryLabel.setTextSize(12);
            memoryLabel.setText("Memória: ");
            memoryIndicator.addView(memoryLabel);
            
            ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                200, ViewGroup.LayoutParams.WRAP_CONTENT, 1
            ));
            memoryIndicator.addView(progressBar);
            
            TextView memoryText = new TextView(getContext());
            memoryText.setTextColor(Color.WHITE);
            memoryText.setTextSize(12);
            memoryText.setText("0%");
            memoryIndicator.addView(memoryText);
            
            // Atualizar periodicamente
            updateMemoryIndicator(progressBar, memoryText);
        }
    }
    
    /**
     * Atualiza indicador de memória
     */
    private void updateMemoryIndicator(ProgressBar progress, TextView text) {
        new Thread(() -> {
            while (isAdded()) {
                memoryStats = MemoryManagerJNI.getStats();
                
                if (progress != null && text != null) {
                    progress.post(() -> {
                        progress.setProgress((int) memoryStats.usagePercent);
                        text.setText(String.format("%.1f%%", memoryStats.usagePercent));
                    });
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    /**
     * Otimizar alocação de memória para operações grandes
     */
    private void optimizeMemoryForLargeOperations() {
        // Pré-alocar memória antes de operações críticas
        long availableMemory = MemoryManagerJNI.getAvailableMemoryBytes();
        Log.d(TAG, "Memória disponível: " + MemoryManagerJNI.bytesToGB(availableMemory) + " GB");
        
        // Se disponível < 2GB, avisar
        if (availableMemory < 2L * 1024 * 1024 * 1024) {
            Log.w(TAG, "Pouca memória disponível para operações grandes");
            Toast.makeText(getContext(), 
                "Aviso: Pouca memória disponível", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cleanup (opcional - pode manter para reutilização)
        // MemoryManagerJNI.cleanupMemoryManager();
        
        // Log estatísticas finais
        Log.d(TAG, "Finalizando. Estatísticas finais:");
        logMemoryStats();
    }
    
    /**
     * Método para liberar cache de memória agressivamente
     */
    public void aggressiveMemoryCleanup() {
        Log.d(TAG, "Limpeza agressiva de memória...");
        
        // Limpar caches
        Runtime.getRuntime().gc();
        System.gc();
        
        // Aguardar um pouco
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logMemoryStats();
    }
}
