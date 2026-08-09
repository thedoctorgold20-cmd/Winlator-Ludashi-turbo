package com.winlator.cmod;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;

import java.util.Locale;

/**
 * ContainerDetailFragment
 *
 * Classe de compatibilidade utilizada pelo ShortcutSettingsDialog
 * e pelas telas de configuração do container.
 */
public class ContainerDetailFragment extends Fragment {

    private static final String TAG =
            "ContainerDetailFragment";

    private int containerId = -1;

    public ContainerDetailFragment() {
        super();
    }

    public ContainerDetailFragment(int containerId) {
        super();
        this.containerId = containerId;
    }

    /**
     * Cria uma instância do fragment.
     */
    public static ContainerDetailFragment newInstance(
            int containerId) {

        ContainerDetailFragment fragment =
                new ContainerDetailFragment(containerId);

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
                    getArguments()
                            .getInt(
                                    "container_id",
                                    containerId
                            );
        }
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int id) {
        containerId = id;
    }

    /**
     * Compatibilidade utilizada pelo
     * ShortcutSettingsDialog.
     */
    public static void createWinComponentsTabFromShortcut(
            Object dialog,
            View rootView,
            Object winComponents) {

        applyWinComponents(
                rootView,
                winComponents
        );
    }

    /**
     * Compatibilidade com ShortcutSettingsDialog.
     */
    public static void createWinComponentsTabFromShortcut(
            Context context,
            View rootView,
            Object winComponents) {

        applyWinComponents(
                rootView,
                winComponents
        );
    }

    /**
     * Assinatura utilizada atualmente pelo
     * ShortcutSettingsDialog.
     */
    public static void createWinComponentsTabFromShortcut(
            ShortcutSettingsDialog dialog,
            View rootView,
            String winComponents,
            boolean enabled) {

        if (rootView == null) {
            return;
        }

        if (!enabled) {
            return;
        }

        applyWinComponents(
                rootView,
                winComponents
        );
    }

    /**
     * Aplica os componentes Windows encontrados no layout.
     */
    private static void applyWinComponents(
            View rootView,
            Object value) {

        if (rootView == null || value == null) {
            return;
        }

        String text =
                String.valueOf(value);

        String[] ids = {
                "spinner_wincomponents",
                "sWinComponents",
                "wincomponents",
                "win_components"
        };

        for (String name : ids) {

            Context context =
                    rootView.getContext();

            if (context == null) {
                continue;
            }

            int id =
                    context.getResources()
                            .getIdentifier(
                                    name,
                                    "id",
                                    context.getPackageName()
                            );

            if (id == 0) {
                continue;
            }

            View target =
                    rootView.findViewById(id);

            if (target instanceof Spinner) {

                Spinner spinner =
                        (Spinner) target;

                selectSpinnerValue(
                        spinner,
                        text
                );

                return;
            }

            if (target instanceof TextView) {

                ((TextView) target)
                        .setText(text);

                return;
            }
        }
    }

    /**
     * Retorna a resolução/tamanho de tela
     * selecionado no layout.
     */
    public String getScreenSize(
            View rootView) {

        if (rootView == null) {
            return "";
        }

        String[] ids = {
                "spinner_screen_size",
                "sScreenSize",
                "screen_size",
                "screenSize",
                "resolution",
                "spinner_resolution"
        };

        for (String name : ids) {

            View target =
                    findViewByName(
                            rootView,
                            name
                    );

            if (target == null) {
                continue;
            }

            String value =
                    getViewValue(target);

            if (!value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    /**
     * Retorna os componentes Windows
     * selecionados no layout.
     */
    public String getWinComponents(
            View rootView) {

        if (rootView == null) {
            return "";
        }

        String[] ids = {
                "spinner_wincomponents",
                "sWinComponents",
                "wincomponents",
                "win_components",
                "winComponents"
        };

        for (String name : ids) {

            View target =
                    findViewByName(
                            rootView,
                            name
                    );

            if (target == null) {
                continue;
            }

            String value =
                    getViewValue(target);

            if (!value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    /**
     * Atualiza o Spinner do driver gráfico.
     *
     * Mantém a assinatura esperada pelas outras classes
     * do projeto.
     */
    public static void updateGraphicsDriverSpinner(
            Context context,
            Spinner spinner) {

        if (context == null ||
                spinner == null) {

            return;
        }

        /*
         * Não substituímos o adapter existente.
         * O projeto original pode fornecer os drivers
         * através de outro componente.
         */
    }

    /**
     * Procura uma View pelo nome do recurso.
     */
    private View findViewByName(
            View rootView,
            String name) {

        Context context =
                rootView.getContext();

        if (context == null) {
            return null;
        }

        int id =
                context.getResources()
                        .getIdentifier(
                                name,
                                "id",
                                context.getPackageName()
                        );

        if (id == 0) {
            return null;
        }

        return rootView.findViewById(id);
    }

    /**
     * Obtém o valor atual de uma View.
     */
    private String getViewValue(
            View view) {

        if (view instanceof Spinner) {

            Spinner spinner =
                    (Spinner) view;

            Object selected =
                    spinner.getSelectedItem();

            if (selected != null) {
                return selected.toString();
            }
        }

        if (view instanceof TextView) {

            CharSequence text =
                    ((TextView) view)
                            .getText();

            if (text != null) {
                return text.toString();
            }
        }

        return "";
    }

    /**
     * Seleciona um item de Spinner pelo texto.
     */
    private static void selectSpinnerValue(
            Spinner spinner,
            String value) {

        if (spinner == null ||
                value == null ||
                value.isEmpty()) {

            return;
        }

        if (spinner.getAdapter() == null) {
            return;
        }

        for (int i = 0;
                i < spinner.getAdapter().getCount();
                i++) {

            Object item =
                    spinner.getAdapter()
                            .getItem(i);

            if (item == null) {
                continue;
            }

            if (value.equals(
                    item.toString())) {

                spinner.setSelection(i);
                return;
            }
        }
    }

    /**
     * Retorna o contexto do Fragment de forma segura.
     */
    public Context getFragmentContext() {

        if (isAdded()) {
            return requireContext();
        }

        return null;
    }

    /**
     * Método auxiliar para registrar informações.
     */
    private void log(String message) {

        Log.d(
                TAG,
                message
        );
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        log(
                String.format(
                        Locale.US,
                        "ContainerDetailFragment finalizado: %d",
                        containerId
                )
        );
    }
                }
