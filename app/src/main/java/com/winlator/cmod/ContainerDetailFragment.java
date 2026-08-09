package com.winlator.cmod;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContainerDetailFragment extends Fragment {

    private static final String TAG = "ContainerDetail";

    private static final String PREFS =
            "winlator_container_settings";

    private static final String KEY_NAME =
            "container_name";

    private static final String KEY_RESOLUTION =
            "resolution";

    private static final String KEY_WINE =
            "wine";

    private static final String KEY_DRIVER =
            "graphics_driver";

    private static final String KEY_ENV =
            "environment";

    private int containerId = -1;

    private EditText containerName;
    private Spinner resolutionSpinner;
    private Spinner wineSpinner;
    private Spinner driverSpinner;
    private EditText environmentEdit;

    private TabLayout tabLayout;
    private LinearLayout contentLayout;

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
                    getArguments().getInt(
                            "container_id",
                            -1
                    );
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull android.view.LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        Context context = requireContext();

        LinearLayout root =
                new LinearLayout(context);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );

        root.setBackgroundColor(
                Color.rgb(18, 18, 18)
        );

        createHeader(context, root);

        createTabs(context, root);

        createContent(context, root);

        loadSettings();

        return root;
    }

    private void createHeader(
            Context context,
            LinearLayout root) {

        containerName =
                new EditText(context);

        containerName.setHint(
                "Nome do Container"
        );

        containerName.setSingleLine(true);

        containerName.setTextColor(
                Color.WHITE
        );

        containerName.setHintTextColor(
                Color.GRAY
        );

        root.addView(
                containerName,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(55)
                )
        );
    }

    private void createTabs(
            Context context,
            LinearLayout root) {

        tabLayout =
                new TabLayout(context);

        tabLayout.setTabMode(
                TabLayout.MODE_SCROLLABLE
        );

        tabLayout.addTab(
                tabLayout.newTab()
                        .setText("Geral")
        );

        tabLayout.addTab(
                tabLayout.newTab()
                        .setText("Wine")
        );

        tabLayout.addTab(
                tabLayout.newTab()
                        .setText("Gráficos")
        );

        tabLayout.addTab(
                tabLayout.newTab()
                        .setText("Ambiente")
        );

        root.addView(
                tabLayout,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(50)
                )
        );
    }

    private void createContent(
            Context context,
            LinearLayout root) {

        contentLayout =
                new LinearLayout(context);

        contentLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        contentLayout.setPadding(
                dp(4),
                dp(12),
                dp(4),
                dp(4)
        );

        root.addView(
                contentLayout,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(
                            TabLayout.Tab tab) {

                        showTab(
                                tab.getPosition()
                        );
                    }

                    @Override
                    public void onTabUnselected(
                            TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(
                            TabLayout.Tab tab) {
                    }
                }
        );

        showTab(0);
    }

    private void showTab(int position) {

        if (contentLayout == null) {
            return;
        }

        contentLayout.removeAllViews();

        Context context =
                requireContext();

        if (position == 0) {
            createGeneralTab(context);
        }

        else if (position == 1) {
            createWineTab(context);
        }

        else if (position == 2) {
            createGraphicsTab(context);
        }

        else if (position == 3) {
            createEnvironmentTab(context);
        }
    }

    private void createGeneralTab(
            Context context) {

        TextView title =
                createTitle(
                        context,
                        "Configuração do Container"
                );

        contentLayout.addView(title);

        TextView info =
                createLabel(
                        context,
                        "ID: " + containerId
                );

        contentLayout.addView(info);

        resolutionSpinner =
                createSpinner(
                        context,
                        new String[]{
                                "1280x720",
                                "1280x800",
                                "1366x768",
                                "1600x900",
                                "1920x1080"
                        }
                );

        addLabel(
                context,
                "Resolução"
        );

        contentLayout.addView(
                resolutionSpinner
        );
    }

    private void createWineTab(
            Context context) {

        TextView title =
                createTitle(
                        context,
                        "Wine Components"
                );

        contentLayout.addView(title);

        addLabel(
                context,
                "Versão / Componentes Wine"
        );

        wineSpinner =
                createSpinner(
                        context,
                        new String[]{
                                "Wine 9",
                                "Wine 8",
                                "Wine 7",
                                "Wine 6",
                                "Wine Stable"
                        }
                );

        contentLayout.addView(
                wineSpinner
        );

        addLabel(
                context,
                "Windows Components"
        );

        Spinner components =
                createSpinner(
                        context,
                        new String[]{
                                "Default",
                                "DXVK",
                                "VKD3D",
                                "DXVK + VKD3D",
                                "All Components"
                        }
                );

        components.setTag(
                "wincomponents"
        );

        contentLayout.addView(
                components
        );
    }

    private void createGraphicsTab(
            Context context) {

        TextView title =
                createTitle(
                        context,
                        "Gráficos"
                );

        contentLayout.addView(title);

        addLabel(
                context,
                "Driver gráfico"
        );

        driverSpinner =
                createSpinner(
                        context,
                        new String[]{
                                "Default",
                                "Turnip",
                                "VirGL",
                                "Zink",
                                "SwiftShader"
                        }
                );

        contentLayout.addView(
                driverSpinner
        );

        addLabel(
                context,
                "Resolução"
        );

        if (resolutionSpinner == null) {

            resolutionSpinner =
                    createSpinner(
                            context,
                            new String[]{
                                    "1280x720",
                                    "1280x800",
                                    "1366x768",
                                    "1600x900",
                                    "1920x1080"
                            }
                    );
        }

        contentLayout.addView(
                resolutionSpinner
        );

        updateGraphicsDriverSpinner(
                context,
                driverSpinner
        );
    }

    private void createEnvironmentTab(
            Context context) {

        TextView title =
                createTitle(
                        context,
                        "Variáveis de Ambiente"
                );

        contentLayout.addView(title);

        environmentEdit =
                new EditText(context);

        environmentEdit.setHint(
                "VAR=VALUE\n"
                        + "DXVK_HUD=1\n"
                        + "WINEDEBUG=-all"
        );

        environmentEdit.setGravity(
                Gravity.TOP
        );

        environmentEdit.setTextColor(
                Color.WHITE
        );

        environmentEdit.setHintTextColor(
                Color.GRAY
        );

        environmentEdit.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        contentLayout.addView(
                environmentEdit,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(180)
                )
        );

        Button save =
                new Button(context);

        save.setText(
                "Salvar Configurações"
        );

        save.setOnClickListener(
                v -> saveSettings()
        );

        contentLayout.addView(
                save
        );

        Button create =
                new Button(context);

        create.setText(
                "Create Container"
        );

        create.setOnClickListener(
                v -> createContainer()
        );

        contentLayout.addView(
                create
        );
    }

    private void createContainer() {

        String name =
                containerName == null
                        ? ""
                        : containerName
                                .getText()
                                .toString()
                                .trim();

        if (name.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "Digite o nome do Container",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        saveSettings();

        Toast.makeText(
                requireContext(),
                "Container configurado: " + name,
                Toast.LENGTH_LONG
        ).show();

        Log.i(
                TAG,
                "Container criado: " + name
        );
    }

    private void saveSettings() {

        if (!isAdded()) {
            return;
        }

        SharedPreferences prefs =
                requireContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );

        String name =
                containerName == null
                        ? ""
                        : containerName
                                .getText()
                                .toString();

        String resolution =
                resolutionSpinner == null
                        ? ""
                        : String.valueOf(
                                resolutionSpinner
                                        .getSelectedItem()
                        );

        String wine =
                wineSpinner == null
                        ? ""
                        : String.valueOf(
                                wineSpinner
                                        .getSelectedItem()
                        );

        String driver =
                driverSpinner == null
                        ? ""
                        : String.valueOf(
                                driverSpinner
                                        .getSelectedItem()
                        );

        String environment =
                environmentEdit == null
                        ? ""
                        : environmentEdit
                                .getText()
                                .toString();

        prefs.edit()
                .putString(
                        KEY_NAME,
                        name
                )
                .putString(
                        KEY_RESOLUTION,
                        resolution
                )
                .putString(
                        KEY_WINE,
                        wine
                )
                .putString(
                        KEY_DRIVER,
                        driver
                )
                .putString(
                        KEY_ENV,
                        environment
                )
                .apply();

        Log.d(
                TAG,
                "Configurações salvas"
        );
    }

    private void loadSettings() {

        if (!isAdded()) {
            return;
        }

        SharedPreferences prefs =
                requireContext()
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        );

        if (containerName != null) {

            containerName.setText(
                    prefs.getString(
                            KEY_NAME,
                            ""
                    )
            );
        }

        if (resolutionSpinner != null) {

            selectSpinnerValue(
                    resolutionSpinner,
                    prefs.getString(
                            KEY_RESOLUTION,
                            "1280x720"
                    )
            );
        }

        if (wineSpinner != null) {

            selectSpinnerValue(
                    wineSpinner,
                    prefs.getString(
                            KEY_WINE,
                            "Wine 9"
                    )
            );
        }

        if (driverSpinner != null) {

            selectSpinnerValue(
                    driverSpinner,
                    prefs.getString(
                            KEY_DRIVER,
                            "Default"
                    )
            );
        }

        if (environmentEdit != null) {

            environmentEdit.setText(
                    prefs.getString(
                            KEY_ENV,
                            ""
                    )
            );
        }
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int id) {
        containerId = id;
    }

    /*
     * Compatibilidade com ShortcutSettingsDialog.
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

    public static void createWinComponentsTabFromShortcut(
            Context context,
            View rootView,
            Object winComponents) {

        applyWinComponents(
                rootView,
                winComponents
        );
    }

    public static void createWinComponentsTabFromShortcut(
            ShortcutSettingsDialog dialog,
            View rootView,
            String winComponents,
            boolean enabled) {

        if (!enabled) {
            return;
        }

        applyWinComponents(
                rootView,
                winComponents
        );
    }

    private static void applyWinComponents(
            View rootView,
            Object value) {

        if (rootView == null ||
                value == null) {
            return;
        }

        String text =
                String.valueOf(value);

        String[] ids = {
                "spinner_wincomponents",
                "sWinComponents",
                "wincomponents",
                "win_components",
                "winComponents"
        };

        for (String name : ids) {

            Context context =
                    rootView.getContext();

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

                selectSpinnerValue(
                        (Spinner) target,
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

    public static void updateGraphicsDriverSpinner(
            Context context,
            Spinner spinner) {

        if (context == null ||
                spinner == null) {
            return;
        }

        /*
         * Mantém o adapter existente.
         * Caso não exista, instala uma lista básica.
         */
        if (spinner.getAdapter() == null) {

            String[] drivers = {
                    "Default",
                    "Turnip",
                    "VirGL",
                    "Zink",
                    "SwiftShader"
            };

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(
                            context,
                            android.R.layout
                                    .simple_spinner_item,
                            drivers
                    );

            adapter.setDropDownViewResource(
                    android.R.layout
                            .simple_spinner_dropdown_item
            );

            spinner.setAdapter(adapter);
        }
    }

    private Spinner createSpinner(
            Context context,
            String[] values) {

        Spinner spinner =
                new Spinner(context);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout
                                .simple_spinner_item,
                        values
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);

        return spinner;
    }

    private void addLabel(
            Context context,
            String text) {

        contentLayout.addView(
                createLabel(
                        context,
                        text
                )
        );
    }

    private TextView createLabel(
            Context context,
            String text) {

        TextView label =
                new TextView(context);

        label.setText(text);

        label.setTextColor(
                Color.LTGRAY
        );

        label.setTextSize(14);

        label.setPadding(
                dp(4),
                dp(8),
                dp(4),
                dp(4)
        );

        return label;
    }

    private TextView createTitle(
            Context context,
            String text) {

        TextView title =
                new TextView(context);

        title.setText(text);

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(20);

        title.setPadding(
                dp(4),
                dp(4),
                dp(4),
                dp(12)
        );

        return title;
    }

    private View findViewByName(
            View rootView,
            String name) {

        Context context =
                rootView.getContext();

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

    private String getViewValue(
            View view) {

        if (view instanceof Spinner) {

            Object item =
                    ((Spinner) view)
                            .getSelectedItem();

            return item == null
                    ? ""
                    : item.toString();
        }

        if (view instanceof TextView) {

            CharSequence text =
                    ((TextView) view)
                            .getText();

            return text == null
                    ? ""
                    : text.toString();
        }

        return "";
    }

    private static void selectSpinnerValue(
            Spinner spinner,
            String value) {

        if (spinner == null ||
                value == null) {
            return;
        }

        if (spinner.getAdapter() == null) {
            return;
        }

        for (int i = 0;
                i < spinner.getAdapter()
                        .getCount();
                i++) {

            Object item =
                    spinner.getAdapter()
                            .getItem(i);

            if (item != null &&
                    value.equals(
                            item.toString()
                    )) {

                spinner.setSelection(i);
                return;
            }
        }
    }

    private int dp(int value) {

        float density =
                requireContext()
                        .getResources()
                        .getDisplayMetrics()
                        .density;

        return (int)
                (value * density + 0.5f);
    }

    @Override
    public void onDestroyView() {

        saveSettings();

        super.onDestroyView();

        Log.d(
                TAG,
                String.format(
                        Locale.US,
                        "ContainerDetailFragment finalizado: %d",
                        containerId
                )
        );
    }
    }
