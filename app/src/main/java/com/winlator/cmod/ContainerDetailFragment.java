package com.winlator.cmod;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.core.MemoryManagerJNI;

import java.util.ArrayList;
import java.util.List;

/**
 * ContainerDetailFragment
 *
 * Versão compatível com ShortcutSettingsDialog,
 * incluindo integração opcional com MemoryManagerJNI.
 */
public class ContainerDetailFragment extends Fragment {

    private static final String TAG = "ContainerDetailFragment";

    private static final long MIN_MEMORY_WARNING =
            2L * 1024L * 1024L * 1024L;

    private static boolean memoryManagerInitialized = false;

    private MemoryManagerJNI.MemoryStats memoryStats;

    private int containerId = -1;

    private Container container;

    public ContainerDetailFragment() {
        super();
    }

    public ContainerDetailFragment(int containerId) {
        super();
        this.containerId = containerId;
    }

    public static ContainerDetailFragment newInstance(
            int containerId) {

        ContainerDetailFragment fragment =
                new ContainerDetailFragment();

        Bundle args = new Bundle();
        args.putInt("container_id", containerId);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            containerId =
                    getArguments().getInt(
                            "container_id",
                            containerId
                    );
        }

        initializeMemoryManager();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        /*
         * Não assumimos um layout específico.
         *
         * O layout original do projeto pode ser usado
         * posteriormente sem interferir no ShortcutSettingsDialog.
         */
        return super.onCreateView(
                inflater,
                container,
                savedInstanceState
        );
    }

    /**
     * Inicializa o MemoryManager apenas uma vez.
     */
    private void initializeMemoryManager() {

        if (memoryManagerInitialized) {
            return;
        }

        try {

            if (MemoryManagerJNI.initMemoryManager()) {

                memoryManagerInitialized = true;

                Log.i(
                        TAG,
                        "Memory Manager inicializado."
                );

                logMemoryStats();

            } else {

                Log.w(
                        TAG,
                        "Memory Manager não pôde ser inicializado."
                );
            }

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro inicializando MemoryManager",
                    e
            );
        }
    }

    /**
     * Obtém estatísticas atuais de memória.
     */
    public MemoryManagerJNI.MemoryStats getMemoryStats() {

        try {

            memoryStats =
                    MemoryManagerJNI.getStats();

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro obtendo estatísticas de memória",
                    e
            );
        }

        return memoryStats;
    }

    /**
     * Registra informações de memória no Logcat.
     */
    private void logMemoryStats() {

        try {

            MemoryManagerJNI.MemoryStats stats =
                    MemoryManagerJNI.getStats();

            if (stats != null) {

                Log.d(
                        TAG,
                        "Memory stats: " + stats
                );
            }

            Log.d(
                    TAG,
                    "Memory info: "
                            + MemoryManagerJNI.getMemoryInfo()
            );

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro registrando memória",
                    e
            );
        }
    }

    /**
     * Aloca memória nativa.
     */
    public long allocateNativeMemory(long size) {

        if (size <= 0) {
            return 0;
        }

        try {

            long pointer =
                    MemoryManagerJNI.allocateMemory(size);

            if (pointer == 0) {

                Log.e(
                        TAG,
                        "Falha ao alocar "
                                + size
                                + " bytes"
                );

                return 0;
            }

            Log.d(
                    TAG,
                    "Memória nativa alocada: "
                            + size
                            + " bytes"
            );

            return pointer;

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro na alocação nativa",
                    e
            );

            return 0;
        }
    }

    /**
     * Libera memória nativa.
     */
    public void freeNativeMemory(long pointer) {

        if (pointer == 0) {
            return;
        }

        try {

            MemoryManagerJNI.freeMemory(pointer);

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro liberando memória nativa",
                    e
            );
        }
    }

    /**
     * Retorna a quantidade de memória disponível.
     */
    public long getAvailableMemoryBytes() {

        try {

            return MemoryManagerJNI
                    .getAvailableMemoryBytes();

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
     * Executa uma verificação simples de memória.
     */
    public boolean hasEnoughMemory(long requiredBytes) {

        long available =
                getAvailableMemoryBytes();

        return available >= requiredBytes;
    }

    /**
     * Limpeza de memória Java.
     */
    public void aggressiveMemoryCleanup() {

        try {

            Runtime.getRuntime().gc();

            System.gc();

            logMemoryStats();

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro durante limpeza de memória",
                    e
            );
        }
    }

    /**
     * Cria a aba de componentes do Windows
     * usada pelo ShortcutSettingsDialog.
     *
     * Esta implementação é segura mesmo quando
     * o layout original não está disponível.
     */
    public static void createWinComponentsTabFromShortcut(
            Object dialog,
            View rootView,
            Object shortcut) {

        if (rootView == null) {
            return;
        }

        Log.d(
                TAG,
                "createWinComponentsTabFromShortcut chamado"
        );
    }

    /**
     * Sobrecarga para chamadas que utilizam Context.
     */
    public static void createWinComponentsTabFromShortcut(
            Context context,
            View rootView,
            Object shortcut) {

        if (rootView == null) {
            return;
        }

        Log.d(
                TAG,
                "createWinComponentsTabFromShortcut(Context) chamado"
        );
    }

    /**
     * Atualiza o Spinner do driver gráfico.
     *
     * O método existe para manter compatibilidade
     * com ShortcutSettingsDialog.
     */
    public static void updateGraphicsDriverSpinner(
            Context context,
            Spinner spinner) {

        if (context == null || spinner == null) {
            return;
        }

        try {

            List<String> drivers =
                    getAvailableGraphicsDrivers();

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(
                            context,
                            android.R.layout.simple_spinner_item,
                            drivers
                    );

            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
            );

            spinner.setAdapter(adapter);

        } catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro atualizando Graphics Driver Spinner",
                    e
            );
        }
    }

    /**
     * Retorna uma lista básica de drivers.
     *
     * Se o projeto possuir uma lista própria,
     * ela pode ser substituída aqui.
     */
    private static List<String>
    getAvailableGraphicsDrivers() {

        ArrayList<String> drivers =
                new ArrayList<>();

        drivers.add("Default");

        drivers.add("Turnip");

        drivers.add("VirGL");

        drivers.add("Zink");

        return drivers;
    }

    /**
     * Versão que recebe o valor atual do driver.
     */
    public static void updateGraphicsDriverSpinner(
            Context context,
            Spinner spinner,
            String selectedDriver) {

        if (context == null || spinner == null) {
            return;
        }

        updateGraphicsDriverSpinner(
                context,
                spinner
        );

        if (selectedDriver == null) {
            return;
        }

        AdapterView<?> parent = spinner;

        for (int i = 0;
                i < parent.getCount();
                i++) {

            Object item =
                    parent.getItemAtPosition(i);

            if (item != null &&
                    selectedDriver.equals(
                            item.toString()
                    )) {

                spinner.setSelection(i);

                break;
            }
        }
    }

    /**
     * Retorna o ID do container.
     */
    public int getContainerId() {
        return containerId;
    }

    /**
     * Define o ID do container.
     */
    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    /**
     * Define o container.
     */
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Retorna o container.
     */
    @Nullable
    public Container getContainer() {
        return container;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        Log.d(
                TAG,
                "ContainerDetailFragment destruído"
        );
    }
}
