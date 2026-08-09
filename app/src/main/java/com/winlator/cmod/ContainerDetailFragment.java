package com.winlator.cmod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.box86_64.Box86_64Preset;
import com.winlator.cmod.box86_64.Box86_64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contentdialog.AddEnvVarDialog;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineRegistryEditor;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.widget.CPUListView;
import com.winlator.cmod.widget.ColorPickerView;
import com.winlator.cmod.widget.EnvVarsView;
import com.winlator.cmod.widget.ImagePickerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ContainerDetailFragment extends Fragment {

    private static final String TAG =
            "ContainerDetailFragment";

    private ContainerManager manager;
    private final int containerId;
    private Container container;
    private PreloaderDialog preloaderDialog;

    private JSONArray gpuCards;

    private Callback<String> openDirectoryCallback;

    public ContainerDetailFragment() {
        this(0);
    }

    public ContainerDetailFragment(int containerId) {
        this.containerId = containerId;
    }

    public static ContainerDetailFragment newInstance(
            int containerId) {

        return new ContainerDetailFragment(containerId);
    }

    @Override
    public void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);

        preloaderDialog =
                new PreloaderDialog(getActivity());

        try {
            gpuCards = new JSONArray(
                    FileUtils.readString(
                            getContext(),
                            "gpu_cards.json"
                    )
            );
        } catch (Exception e) {
            gpuCards = new JSONArray();
        }
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Activity activity = getActivity();

        if (activity instanceof AppCompatActivity) {

            AppCompatActivity app =
                    (AppCompatActivity) activity;

            if (app.getSupportActionBar() != null) {
                app.getSupportActionBar().setTitle(
                        isEditMode()
                                ? R.string.edit_container
                                : R.string.new_container
                );
            }
        }
    }

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode ==
                MainActivity.OPEN_DIRECTORY_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {

            if (data != null &&
                    openDirectoryCallback != null) {

                String path =
                        FileUtils.getFilePathFromUri(
                                data.getData()
                        );

                if (path != null) {
                    openDirectoryCallback.call(path);
                }
            }

            openDirectoryCallback = null;
        }
    }

    public boolean isEditMode() {
        return container != null;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup root,
            @Nullable Bundle savedInstanceState) {

        final Context context = getContext();

        if (context == null) {
            return null;
        }

        final SharedPreferences preferences =
                PreferenceManager
                        .getDefaultSharedPreferences(context);

        final View view =
                inflater.inflate(
                        R.layout.container_detail_fragment,
                        root,
                        false
                );

        manager =
                new ContainerManager(context);

        container =
                containerId > 0
                        ? manager.getContainerById(containerId)
                        : null;

        /*
         * =========================================================
         * NOME
         * =========================================================
         */

        final EditText etName =
                view.findViewById(R.id.ETName);

        if (isEditMode()) {

            etName.setText(
                    container.getName()
            );

        } else {

            etName.setText(
                    getString(R.string.container)
                            + "-"
                            + manager.getNextContainerId()
            );
        }

        /*
         * =========================================================
         * WINE
         * =========================================================
         */

        final ArrayList wineInfos =
                WineUtils.getInstalledWineInfos(context);

        final Spinner sWineVersion =
                view.findViewById(R.id.SWineVersion);

        if (wineInfos != null &&
                wineInfos.size() > 1) {

            loadWineVersionSpinner(
                    view,
                    sWineVersion,
                    wineInfos
            );
        }

        /*
         * =========================================================
         * RESOLUÇÃO
         * =========================================================
         */

        loadScreenSizeSpinner(
                view,
                isEditMode()
                        ? container.getScreenSize()
                        : Container.DEFAULT_SCREEN_SIZE
        );

        /*
         * =========================================================
         * DRIVER GRÁFICO
         * =========================================================
         */

        final Spinner sGraphicsDriver =
                view.findViewById(
                        R.id.SGraphicsDriver
                );

        final Spinner sDXWrapper =
                view.findViewById(
                        R.id.SDXWrapper
                );

        final View vDXWrapperConfig =
                view.findViewById(
                        R.id.BTDXWrapperConfig
                );

        vDXWrapperConfig.setTag(
                isEditMode()
                        ? container.getDXWrapperConfig()
                        : ""
        );

        setupDXWrapperSpinner(
                sDXWrapper,
                vDXWrapperConfig
        );

        loadGraphicsDriverSpinner(
                sGraphicsDriver,
                sDXWrapper,
                isEditMode()
                        ? container.getGraphicsDriver()
                        : Container.DEFAULT_GRAPHICS_DRIVER,
                isEditMode()
                        ? container.getDXWrapper()
                        : Container.DEFAULT_DXWRAPPER
        );

        /*
         * =========================================================
         * ÁUDIO
         * =========================================================
         */

        Spinner sAudioDriver =
                view.findViewById(
                        R.id.SAudioDriver
                );

        AppUtils.setSpinnerSelectionFromIdentifier(
                sAudioDriver,
                isEditMode()
                        ? container.getAudioDriver()
                        : Container.DEFAULT_AUDIO_DRIVER
        );

        /*
         * =========================================================
         * FPS
         * =========================================================
         */

        CheckBox cbShowFPS =
                view.findViewById(
                        R.id.CBShowFPS
                );

        cbShowFPS.setChecked(
                isEditMode()
                        && container.isShowFPS()
        );

        /*
         * =========================================================
         * WOW64
         * =========================================================
         */

        CheckBox cbWoW64Mode =
                view.findViewById(
                        R.id.CBWoW64Mode
                );

        cbWoW64Mode.setChecked(
                !isEditMode()
                        || container.isWoW64Mode()
        );

        /*
         * =========================================================
         * STARTUP
         * =========================================================
         */

        Spinner sStartupSelection =
                view.findViewById(
                        R.id.SStartupSelection
                );

        byte startup =
                isEditMode()
                        ? container.getStartupSelection()
                        : Container.STARTUP_SELECTION_ESSENTIAL;

        sStartupSelection.setSelection(
                startup
        );

        /*
         * =========================================================
         * BOX86
         * =========================================================
         */

        Spinner sBox86Preset =
                view.findViewById(
                        R.id.SBox86Preset
                );

        Box86_64PresetManager.loadSpinner(
                "box86",
                sBox86Preset,
                isEditMode()
                        ? container.getBox86Preset()
                        : preferences.getString(
                                "box86_preset",
                                Box86_64Preset.COMPATIBILITY
                        )
        );

        /*
         * =========================================================
         * BOX64
         * =========================================================
         */

        Spinner sBox64Preset =
                view.findViewById(
                        R.id.SBox64Preset
                );

        Box86_64PresetManager.loadSpinner(
                "box64",
                sBox64Preset,
                isEditMode()
                        ? container.getBox64Preset()
                        : preferences.getString(
                                "box64_preset",
                                Box86_64Preset.COMPATIBILITY
                        )
        );

        /*
         * =========================================================
         * CPU
         * =========================================================
         */

        CPUListView cpuListView =
                view.findViewById(
                        R.id.CPUListView
                );

        CPUListView cpuListViewWoW64 =
                view.findViewById(
                        R.id.CPUListViewWoW64
                );

        cpuListView.setCheckedCPUList(
                isEditMode()
                        ? container.getCPUList(true)
                        : Container.getFallbackCPUList()
        );

        cpuListViewWoW64.setCheckedCPUList(
                isEditMode()
                        ? container.getCPUListWoW64(true)
                        : Container.getFallbackCPUListWoW64()
        );

        /*
         * =========================================================
         * ABAS
         * =========================================================
         */

        createWineConfigurationTab(view);

        EnvVarsView envVarsView =
                createEnvVarsTab(view);

        createWinComponentsTab(
                view,
                isEditMode()
                        ? container.getWinComponents()
                        : Container.DEFAULT_WINCOMPONENTS
        );

        createDrivesTab(view);

        AppUtils.setupTabLayout(
                view,
                R.id.TabLayout,
                R.id.LLTabWineConfiguration,
                R.id.LLTabWinComponents,
                R.id.LLTabEnvVars,
                R.id.LLTabDrives,
                R.id.LLTabAdvanced
        );

        /*
         * =========================================================
         * BOTÃO CREATE / SAVE
         * =========================================================
         */

        View confirm =
                view.findViewById(
                        R.id.BTConfirm
                );

        confirm.setOnClickListener(
                v -> saveContainer(
                        view,
                        etName,
                        sWineVersion,
                        wineInfos,
                        sGraphicsDriver,
                        sDXWrapper,
                        vDXWrapperConfig,
                        sAudioDriver,
                        cbShowFPS,
                        cbWoW64Mode,
                        sStartupSelection,
                        sBox86Preset,
                        sBox64Preset,
                        cpuListView,
                        cpuListViewWoW64,
                        envVarsView
                )
        );

        return view;
    }

    private void saveContainer(
            View view,
            EditText etName,
            Spinner sWineVersion,
            ArrayList wineInfos,
            Spinner sGraphicsDriver,
            Spinner sDXWrapper,
            View vDXWrapperConfig,
            Spinner sAudioDriver,
            CheckBox cbShowFPS,
            CheckBox cbWoW64Mode,
            Spinner sStartupSelection,
            Spinner sBox86Preset,
            Spinner sBox64Preset,
            CPUListView cpuListView,
            CPUListView cpuListViewWoW64,
            EnvVarsView envVarsView) {

        try {

            String name =
                    etName.getText()
                            .toString()
                            .trim();

            if (name.isEmpty()) {

                etName.setError(
                        getString(
                                R.string.container_name
                        )
                );

                return;
            }

            String screenSize =
                    getScreenSize(view);

            String envVars =
                    envVarsView.getEnvVars();

            String graphicsDriver =
                    StringUtils.parseIdentifier(
                            sGraphicsDriver
                                    .getSelectedItem()
                    );

            String dxwrapper =
                    StringUtils.parseIdentifier(
                            sDXWrapper
                                    .getSelectedItem()
                    );

            Object tag =
                    vDXWrapperConfig.getTag();

            String dxwrapperConfig =
                    tag != null
                            ? tag.toString()
                            : "";

            String audioDriver =
                    StringUtils.parseIdentifier(
                            sAudioDriver
                                    .getSelectedItem()
                    );

            String wincomponents =
                    getWinComponents(view);

            String drives =
                    getDrives(view);

            boolean showFPS =
                    cbShowFPS.isChecked();

            String cpuList =
                    cpuListView
                            .getCheckedCPUListAsString();

            String cpuListWoW64 =
                    cpuListViewWoW64
                            .getCheckedCPUListAsString();

            boolean wow64Mode =
                    cbWoW64Mode.isChecked()
                            && cbWoW64Mode.isEnabled();

            byte startupSelection =
                    (byte)
                            sStartupSelection
                                    .getSelectedItemPosition();

            String box86Preset =
                    Box86_64PresetManager
                            .getSpinnerSelectedId(
                                    sBox86Preset
                            );

            String box64Preset =
                    Box86_64PresetManager
                            .getSpinnerSelectedId(
                                    sBox64Preset
                            );

            String desktopTheme =
                    getDesktopTheme(view);

            /*
             * =====================================================
             * EDITAR CONTAINER
             * =====================================================
             */

            if (isEditMode()) {

                container.setName(name);
                container.setScreenSize(screenSize);
                container.setEnvVars(envVars);
                container.setCPUList(cpuList);
                container.setCPUListWoW64(cpuListWoW64);
                container.setGraphicsDriver(graphicsDriver);
                container.setDXWrapper(dxwrapper);
                container.setDXWrapperConfig(
                        dxwrapperConfig
                );
                container.setAudioDriver(audioDriver);
                container.setWinComponents(
                        wincomponents
                );
                container.setDrives(drives);
                container.setShowFPS(showFPS);
                container.setWoW64Mode(wow64Mode);
                container.setStartupSelection(
                        startupSelection
                );
                container.setBox86Preset(
                        box86Preset
                );
                container.setBox64Preset(
                        box64Preset
                );
                container.setDesktopTheme(
                        desktopTheme
                );

                container.saveData();

                saveWineRegistryKeys(view);

                Toast.makeText(
                        requireContext(),
                        "Container salvo.",
                        Toast.LENGTH_SHORT
                ).show();

                requireActivity()
                        .onBackPressed();

                return;
            }

            /*
             * =====================================================
             * CRIAR NOVO CONTAINER
             * =====================================================
             */

            JSONObject data =
                    new JSONObject();

            data.put(
                    "name",
                    name
            );

            data.put(
                    "screenSize",
                    screenSize
            );

            data.put(
                    "envVars",
                    envVars
            );

            data.put(
                    "cpuList",
                    cpuList
            );

            data.put(
                    "cpuListWoW64",
                    cpuListWoW64
            );

            data.put(
                    "graphicsDriver",
                    graphicsDriver
            );

            data.put(
                    "dxwrapper",
                    dxwrapper
            );

            data.put(
                    "dxwrapperConfig",
                    dxwrapperConfig
            );

            data.put(
                    "audioDriver",
                    audioDriver
            );

            data.put(
                    "wincomponents",
                    wincomponents
            );

            data.put(
                    "drives",
                    drives
            );

            data.put(
                    "showFPS",
                    showFPS
            );

            data.put(
                    "wow64Mode",
                    wow64Mode
            );

            data.put(
                    "startupSelection",
                    startupSelection
            );

            data.put(
                    "box86Preset",
                    box86Preset
            );

            data.put(
                    "box64Preset",
                    box64Preset
            );

            data.put(
                    "desktopTheme",
                    desktopTheme
            );

            if (wineInfos != null &&
                    wineInfos.size() > 1 &&
                    sWineVersion != null &&
                    sWineVersion.getSelectedItemPosition()
                            >= 0 &&
                    sWineVersion.getSelectedItemPosition()
                            < wineInfos.size()) {

                WineInfo info =
                        (WineInfo)
                                wineInfos.get(
                                        sWineVersion
                                                .getSelectedItemPosition()
                                );

                data.put(
                        "wineVersion",
                        info.identifier()
                );
            }

            if (preloaderDialog != null) {
                preloaderDialog.show(
                        R.string.creating_container
                );
            }

            manager.createContainerAsync(
                    data,
                    createdContainer -> {

                        if (createdContainer != null) {

                            container =
                                    createdContainer;

                            saveWineRegistryKeys(
                                    view
                            );

                            if (preloaderDialog != null) {
                                preloaderDialog.close();
                            }

                            if (getActivity() != null) {

                                Toast.makeText(
                                        requireContext(),
                                        "Container criado com sucesso.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                requireActivity()
                                        .onBackPressed();
                            }

                        } else {

                            if (preloaderDialog != null) {
                                preloaderDialog.close();
                            }

                            Toast.makeText(
                                    requireContext(),
                                    "Falha ao criar o container.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

        } catch (Exception e) {

            if (preloaderDialog != null) {
                preloaderDialog.close();
            }

            Toast.makeText(
                    requireContext(),
                    "Erro: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =============================================================
     * WINE REGISTRY
     * =============================================================
     */

    private void saveWineRegistryKeys(
            View view) {

        if (container == null) {
            return;
        }

        File userRegFile =
                new File(
                        container.getRootDir(),
                        ".wine/user.reg"
                );

        if (!userRegFile.getParentFile().exists()) {
            userRegFile.getParentFile().mkdirs();
        }

        try (
                WineRegistryEditor registryEditor =
                        new WineRegistryEditor(
                                userRegFile
                        )
        ) {

            Spinner sCSMT =
                    view.findViewById(
                            R.id.SCSMT
                    );

            if (sCSMT != null) {

                registryEditor.setDwordValue(
                        "Software\\Wine\\Direct3D",
                        "csmt",
                        sCSMT.getSelectedItemPosition()
                                != 0
                                ? 3
                                : 0
                );
            }

            Spinner sGPUName =
                    view.findViewById(
                            R.id.SGPUName
                    );

            if (sGPUName != null &&
                    gpuCards != null) {

                try {

                    JSONObject gpu =
                            gpuCards.getJSONObject(
                                    sGPUName
                                            .getSelectedItemPosition()
                            );

                    registryEditor.setDwordValue(
                            "Software\\Wine\\Direct3D",
                            "VideoPciDeviceID",
                            gpu.getInt("deviceID")
                    );

                    registryEditor.setDwordValue(
                            "Software\\Wine\\Direct3D",
                            "VideoPciVendorID",
                            gpu.getInt("vendorID")
                    );

                } catch (Exception ignored) {
                }
            }

            Spinner sOffscreen =
                    view.findViewById(
                            R.id.SOffscreenRenderingMode
                    );

            if (sOffscreen != null &&
                    sOffscreen.getSelectedItem() != null) {

                registryEditor.setStringValue(
                        "Software\\Wine\\Direct3D",
                        "OffScreenRenderingMode",
                        sOffscreen
                                .getSelectedItem()
                                .toString()
                                .toLowerCase(
                                        Locale.ENGLISH
                                )
                );
            }

            Spinner sStrict =
                    view.findViewById(
                            R.id.SStrictShaderMath
                    );

            if (sStrict != null) {

                registryEditor.setDwordValue(
                        "Software\\Wine\\Direct3D",
                        "strict_shader_math",
                        sStrict.getSelectedItemPosition()
                );
            }

            Spinner sVideoMemory =
                    view.findViewById(
                            R.id.SVideoMemorySize
                    );

            if (sVideoMemory != null &&
                    sVideoMemory.getSelectedItem() != null) {

                registryEditor.setStringValue(
                        "Software\\Wine\\Direct3D",
                        "VideoMemorySize",
                        StringUtils.parseNumber(
                                sVideoMemory
                                        .getSelectedItem()
                        )
                );
            }

            Spinner sMouseWarp =
                    view.findViewById(
                            R.id.SMouseWarpOverride
                    );

            if (sMouseWarp != null &&
                    sMouseWarp.getSelectedItem() != null) {

                registryEditor.setStringValue(
                        "Software\\Wine\\DirectInput",
                        "MouseWarpOverride",
                        sMouseWarp
                                .getSelectedItem()
                                .toString()
                                .toLowerCase(
                                        Locale.ENGLISH
                                )
                );
            }

            registryEditor.setStringValue(
                    "Software\\Wine\\Direct3D",
                    "shader_backend",
                    "glsl"
            );

            registryEditor.setStringValue(
                    "Software\\Wine\\Direct3D",
                    "UseGLSL",
                    "enabled"
            );

        } catch (Exception e) {

            android.util.Log.e(
                    TAG,
                    "Erro ao salvar Wine Registry",
                    e
            );
        }
    }

    /*
     * =============================================================
     * WINE CONFIGURATION TAB
     * =============================================================
     */

    private void createWineConfigurationTab(
            View view) {

        Context context =
                getContext();

        if (context == null) {
            return;
        }

        String desktopThemeValue =
                isEditMode()
                        ? container.getDesktopTheme()
                        : WineThemeManager.DEFAULT_DESKTOP_THEME;

        WineThemeManager.ThemeInfo desktopTheme =
                new WineThemeManager.ThemeInfo(
                        desktopThemeValue
                );

        Spinner sDesktopTheme =
                view.findViewById(
                        R.id.SDesktopTheme
                );

        if (sDesktopTheme != null) {

            sDesktopTheme.setSelection(
                    desktopTheme.theme.ordinal()
            );
        }

        ImagePickerView image =
                view.findViewById(
                        R.id.IPVDesktopBackgroundImage
                );

        ColorPickerView color =
                view.findViewById(
                        R.id.CPVDesktopBackgroundColor
                );

        if (color != null) {
            color.setColor(
                    desktopTheme.backgroundColor
            );
        }

        Spinner typeSpinner =
                view.findViewById(
                        R.id.SDesktopBackgroundType
                );

        if (typeSpinner != null) {

            typeSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {

                        @Override
                        public void onItemSelected(
                                AdapterView<?> parent,
                                View item,
                                int position,
                                long id) {

                            WineThemeManager.BackgroundType type =
                                    WineThemeManager
                                            .BackgroundType
                                            .values()[position];

                            if (image != null) {
                                image.setVisibility(
                                        type ==
                                                WineThemeManager
                                                        .BackgroundType
                                                        .IMAGE
                                                ? View.VISIBLE
                                                : View.GONE
                                );
                            }

                            if (color != null) {
                                color.setVisibility(
                                        type ==
                                                WineThemeManager
                                                        .BackgroundType
                                                        .COLOR
                                                ? View.VISIBLE
                                                : View.GONE
                                );
                            }
                        }

                        @Override
                        public void onNothingSelected(
                                AdapterView<?> parent) {
                        }
                    }
            );

            typeSpinner.setSelection(
                    desktopTheme.backgroundType.ordinal()
            );
        }

        /*
         * Para container novo ainda não existe
         * user.reg. Portanto usamos valores padrão.
         */

        List<String> stateList =
                Arrays.asList(
                        context.getString(R.string.disable),
                        context.getString(R.string.enable)
                );

        Spinner sCSMT =
                view.findViewById(R.id.SCSMT);

        if (sCSMT != null) {

            sCSMT.setAdapter(
                    new ArrayAdapter<>(
                            context,
                            android.R.layout
                                    .simple_spinner_dropdown_item,
                            stateList
                    )
            );

            if (isEditMode()) {

                try {

                    File reg =
                            new File(
                                    container.getRootDir(),
                                    ".wine/user.reg"
                            );

                    if (reg.exists()) {

                        try (
                                WineRegistryEditor editor =
                                        new WineRegistryEditor(
                                                reg
                                        )
                        ) {

                            sCSMT.setSelection(
                                    editor.getDwordValue(
                                            "Software\\Wine\\Direct3D",
                                            "csmt",
                                            3
                                    ) != 0
                                            ? 1
                                            : 0
                            );
                        }
                    }

                } catch (Exception ignored) {
                }
            }
        }

        Spinner sGPUName =
                view.findViewById(
                        R.id.SGPUName
                );

        if (sGPUName != null) {
            loadGPUNameSpinner(
                    sGPUName,
                    1728
            );
        }

        Spinner sOffscreen =
                view.findViewById(
                        R.id.SOffscreenRenderingMode
                );

        if (sOffscreen != null) {

            List<String> values =
                    Arrays.asList(
                            "Backbuffer",
                            "FBO"
                    );

            sOffscreen.setAdapter(
                    new ArrayAdapter<>(
                            context,
                            android.R.layout
                                    .simple_spinner_dropdown_item,
                            values
                    )
            );

            AppUtils.setSpinnerSelectionFromValue(
                    sOffscreen,
                    "fbo"
            );
        }

        Spinner sStrict =
                view.findViewById(
                        R.id.SStrictShaderMath
                );

        if (sStrict != null) {

            sStrict.setAdapter(
                    new ArrayAdapter<>(
                            context,
                            android.R.layout
                                    .simple_spinner_dropdown_item,
                            stateList
                    )
            );

            sStrict.setSelection(1);
        }

        Spinner sVideoMemory =
                view.findViewById(
                        R.id.SVideoMemorySize
                );

        if (sVideoMemory != null) {

            AppUtils.setSpinnerSelectionFromNumber(
                    sVideoMemory,
                    "2048"
            );
        }

        Spinner sMouseWarp =
                view.findViewById(
                        R.id.SMouseWarpOverride
                );

        if (sMouseWarp != null) {

            List<String> values =
                    Arrays.asList(
                            context.getString(R.string.disable),
                            context.getString(R.string.enable),
                            context.getString(R.string.force)
                    );

            sMouseWarp.setAdapter(
                    new ArrayAdapter<>(
                            context,
                            android.R.layout
                                    .simple_spinner_dropdown_item,
                            values
                    )
            );

            AppUtils.setSpinnerSelectionFromValue(
                    sMouseWarp,
                    "disable"
            );
        }
    }

    private void loadGPUNameSpinner(
            Spinner spinner,
            int selectedDeviceID) {

        if (spinner == null) {
            return;
        }

        List<String> values =
                new ArrayList<>();

        int selectedPosition = 0;

        try {

            if (gpuCards != null) {

                for (int i = 0;
                        i < gpuCards.length();
                        i++) {

                    JSONObject item =
                            gpuCards.getJSONObject(i);

                    if (item.getInt("deviceID")
                            == selectedDeviceID) {

                        selectedPosition = i;
                    }

                    values.add(
                            item.getString("name")
                    );
                }
            }

        } catch (Exception ignored) {
        }

        spinner.setAdapter(
                new ArrayAdapter<>(
                        getContext(),
                        android.R.layout
                                .simple_spinner_dropdown_item,
                        values
                )
        );

        if (!values.isEmpty()) {
            spinner.setSelection(
                    Math.min(
                            selectedPosition,
                            values.size() - 1
                    )
            );
        }
    }

    /*
     * =============================================================
     * RESOLUTION
     * =============================================================
     */

    public static String getScreenSize(
            View view) {

        Spinner spinner =
                view.findViewById(
                        R.id.SScreenSize
                );

        if (spinner == null ||
                spinner.getSelectedItem() == null) {

            return Container.DEFAULT_SCREEN_SIZE;
        }

        String value =
                spinner.getSelectedItem()
                        .toString();

        if (value.equalsIgnoreCase("custom")) {

            EditText width =
                    view.findViewById(
                            R.id.ETScreenWidth
                    );

            EditText height =
                    view.findViewById(
                            R.id.ETScreenHeight
                    );

            if (width != null &&
                    height != null) {

                String w =
                        width.getText()
                                .toString()
                                .trim();

                String h =
                        height.getText()
                                .toString()
                                .trim();

                if (w.matches("[0-9]+") &&
                        h.matches("[0-9]+")) {

                    try {

                        int iw =
                                Integer.parseInt(w);

                        int ih =
                                Integer.parseInt(h);

                        if (iw >= 320 &&
                                ih >= 240 &&
                                iw % 2 == 0 &&
                                ih % 2 == 0) {

                            return iw + "x" + ih;
                        }

                    } catch (Exception ignored) {
                    }
                }
            }

            return Container.DEFAULT_SCREEN_SIZE;
        }

        return StringUtils.parseIdentifier(
                value
        );
    }

    public static void loadScreenSizeSpinner(
            View view,
            String selectedValue) {

        Spinner spinner =
                view.findViewById(
                        R.id.SScreenSize
                );

        LinearLayout custom =
                view.findViewById(
                        R.id.LLCustomScreenSize
                );

        if (spinner == null) {
            return;
        }

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View item,
                            int position,
                            long id) {

                        String value =
                                spinner
                                        .getItemAtPosition(
                                                position
                                        )
                                        .toString();

                        if (custom != null) {

                            custom.setVisibility(
                                    value.equalsIgnoreCase(
                                            "custom"
                                    )
                                            ? View.VISIBLE
                                            : View.GONE
                            );
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        boolean found =
                AppUtils.setSpinnerSelectionFromIdentifier(
                        spinner,
                        selectedValue
                );

        if (!found) {

            AppUtils.setSpinnerSelectionFromValue(
                    spinner,
                    "custom"
            );

            String[] parts =
                    selectedValue.split("x");

            if (parts.length == 2) {

                EditText width =
                        view.findViewById(
                                R.id.ETScreenWidth
                        );

                EditText height =
                        view.findViewById(
                                R.id.ETScreenHeight
                        );

                if (width != null) {
                    width.setText(parts[0]);
                }

                if (height != null) {
                    height.setText(parts[1]);
                }
            }
        }
    }

    /*
     * =============================================================
     * GRAPHICS DRIVER
     * =============================================================
     */

    public static void loadGraphicsDriverSpinner(
            final Spinner graphics,
            final Spinner dxwrapper,
            String selectedGraphicsDriver,
            String selectedDXWrapper) {

        if (graphics == null ||
                dxwrapper == null) {
            return;
        }

        final Context context =
                graphics.getContext();

        final String[] entries =
                context.getResources()
                        .getStringArray(
                                R.array.dxwrapper_entries
                        );

        Runnable update =
                () -> {

                    String driver =
                            StringUtils.parseIdentifier(
                                    graphics.getSelectedItem()
                            );

                    boolean addAll =
                            "turnip".equals(driver);

                    ArrayList<String> items =
                            new ArrayList<>();

                    for (String value : entries) {

                        if (addAll ||
                                (!value.equals("DXVK")
                                        && !value.equals("VKD3D"))) {

                            items.add(value);
                        }
                    }

                    dxwrapper.setAdapter(
                            new ArrayAdapter<>(
                                    context,
                                    android.R.layout
                                            .simple_spinner_dropdown_item,
                                    items
                            )
                    );

                    AppUtils.setSpinnerSelectionFromIdentifier(
                            dxwrapper,
                            selectedDXWrapper
                    );
                };

        graphics.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        update.run();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        AppUtils.setSpinnerSelectionFromIdentifier(
                graphics,
                selectedGraphicsDriver
        );

        update.run();
    }

    /*
     * Compatibilidade usada pelo ShortcutSettingsDialog.
     */

    public static void updateGraphicsDriverSpinner(
            Context context,
            Spinner spinner) {

        if (context == null ||
                spinner == null) {
            return;
        }

        try {

            AppUtils.setSpinnerSelectionFromIdentifier(
                    spinner,
                    Container.DEFAULT_GRAPHICS_DRIVER
            );

        } catch (Exception ignored) {
        }
    }

    public static void setupDXWrapperSpinner(
            final Spinner dxwrapper,
            final View config) {

        if (dxwrapper == null ||
                config == null) {
            return;
        }

        dxwrapper.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (dxwrapper.getSelectedItem()
                                == null) {
                            return;
                        }

                        String value =
                                StringUtils.parseIdentifier(
                                        dxwrapper
                                                .getSelectedItem()
                                );

                        if ("dxvk".equals(value)) {

                            config.setOnClickListener(
                                    v ->
                                            new DXVKConfigDialog(
                                                    config
                                            ).show()
                            );

                            config.setVisibility(
                                    View.VISIBLE
                            );

                        } else {

                            config.setVisibility(
                                    View.GONE
                            );
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    /*
     * =============================================================
     * WIN COMPONENTS
     * =============================================================
     */

    public static String getWinComponents(
            View view) {

        ViewGroup parent =
                view.findViewById(
                        R.id.LLTabWinComponents
                );

        if (parent == null) {
            return Container.DEFAULT_WINCOMPONENTS;
        }

        ArrayList<View> views =
                new ArrayList<>();

        AppUtils.findViewsWithClass(
                parent,
                Spinner.class,
                views
        );

        String[] components =
                new String[views.size()];

        for (int i = 0;
                i < views.size();
                i++) {

            Spinner spinner =
                    (Spinner) views.get(i);

            components[i] =
                    spinner.getTag()
                            + "="
                            + spinner
                            .getSelectedItemPosition();
        }

        return String.join(
                ",",
                components
        );
    }

    public static void createWinComponentsTab(
            View view,
            String wincomponents) {

        if (view == null) {
            return;
        }

        Context context =
                view.getContext();

        LayoutInflater inflater =
                LayoutInflater.from(context);

        ViewGroup tab =
                view.findViewById(
                        R.id.LLTabWinComponents
                );

        if (tab == null) {
            return;
        }

        ViewGroup directx =
                tab.findViewById(
                        R.id.LLWinComponentsDirectX
                );

        ViewGroup general =
                tab.findViewById(
                        R.id.LLWinComponentsGeneral
                );

        if (directx == null ||
                general == null) {
            return;
        }

        try {

            KeyValueSet set =
                    new KeyValueSet(
                            wincomponents
                    );

            for (String[] component : set) {

                if (component == null ||
                        component.length < 2) {
                    continue;
                }

                ViewGroup parent =
                        component[0]
                                .startsWith("direct")
                                ? directx
                                : general;

                View item =
                        inflater.inflate(
                                R.layout.wincomponent_list_item,
                                parent,
                                false
                        );

                TextView text =
                        item.findViewById(
                                R.id.TextView
                        );

                Spinner spinner =
                        item.findViewById(
                                R.id.Spinner
                        );

                if (text != null) {

                    text.setText(
                            StringUtils.getString(
                                    context,
                                    component[0]
                            )
                    );
                }

                if (spinner != null) {

                    spinner.setTag(
                            component[0]
                    );

                    try {

                        spinner.setSelection(
                                Integer.parseInt(
                                        component[1]
                                ),
                                false
                        );

                    } catch (Exception ignored) {
                    }
                }

                parent.addView(item);
            }

        } catch (Exception e) {

            android.util.Log.e(
                    TAG,
                    "Erro nos componentes Wine",
                    e
            );
        }
    }

    /*
     * Método exigido pelo ShortcutSettingsDialog.
     */

    public static void createWinComponentsTabFromShortcut(
            Object dialog,
            View rootView,
            Object winComponents) {

        createWinComponentsTab(
                rootView,
                String.valueOf(
                        winComponents
                )
        );
    }

    /*
     * Assinatura usada no seu ShortcutSettingsDialog.
     */

    public static void createWinComponentsTabFromShortcut(
            com.winlator.cmod.contentdialog.ShortcutSettingsDialog dialog,
            View rootView,
            String winComponents,
            boolean enabled) {

        if (!enabled) {
            return;
        }

        createWinComponentsTab(
                rootView,
                winComponents
        );
    }

    /*
     * =============================================================
     * ENVIRONMENT VARIABLES
     * =============================================================
     */

    private EnvVarsView createEnvVarsTab(
            final View view) {

        Context context =
                view.getContext();

        final EnvVarsView envVarsView =
                view.findViewById(
                        R.id.EnvVarsView
                );

        if (envVarsView == null) {
            return null;
        }

        envVarsView.setEnvVars(
                new EnvVars(
                        isEditMode()
                                ? container.getEnvVars()
                                : Container.DEFAULT_ENV_VARS
                )
        );

        View add =
                view.findViewById(
                        R.id.BTAddEnvVar
                );

        if (add != null) {

            add.setOnClickListener(
                    v ->
                            new AddEnvVarDialog(
                                    context,
                                    envVarsView
                            ).show()
            );
        }

        return envVarsView;
    }

    /*
     * =============================================================
     * DRIVES
     * =============================================================
     */

    private String getDrives(
            View view) {

        LinearLayout parent =
                view.findViewById(
                        R.id.LLDrives
                );

        if (parent == null) {
            return "";
        }

        StringBuilder drives =
                new StringBuilder();

        for (int i = 0;
                i < parent.getChildCount();
                i++) {

            View child =
                    parent.getChildAt(i);

            Spinner spinner =
                    child.findViewById(
                            R.id.Spinner
                    );

            EditText edit =
                    child.findViewById(
                            R.id.EditText
                    );

            if (spinner == null ||
                    edit == null ||
                    spinner.getSelectedItem() == null) {
                continue;
            }

            String path =
                    edit.getText()
                            .toString()
                            .trim();

            if (!path.isEmpty()) {

                drives.append(
                        spinner
                                .getSelectedItem()
                                .toString()
                );

                drives.append(path);
            }
        }

        return drives.toString();
    }

    private void createDrivesTab(
            View view) {

        Context context =
                getContext();

        if (context == null) {
            return;
        }

        LinearLayout parent =
                view.findViewById(
                        R.id.LLDrives
                );

        View empty =
                view.findViewById(
                        R.id.TVDrivesEmptyText
                );

        if (parent == null) {
            return;
        }

        LayoutInflater inflater =
                LayoutInflater.from(context);

        String drives =
                isEditMode()
                        ? container.getDrives()
                        : Container.DEFAULT_DRIVES;

        final String[] driveLetters =
                new String[
                        Container.MAX_DRIVE_LETTERS
                ];

        for (int i = 0;
                i < driveLetters.length;
                i++) {

            driveLetters[i] =
                    ((char)
                            (i + 68))
                            + ":";
        }

        Callback<String[]> addItem =
                drive -> {

                    View item =
                            inflater.inflate(
                                    R.layout.drive_list_item,
                                    parent,
                                    false
                            );

                    Spinner spinner =
                            item.findViewById(
                                    R.id.Spinner
                            );

                    spinner.setAdapter(
                            new ArrayAdapter<>(
                                    context,
                                    android.R.layout
                                            .simple_spinner_dropdown_item,
                                    driveLetters
                            )
                    );

                    AppUtils.setSpinnerSelectionFromValue(
                            spinner,
                            drive[0] + ":"
                    );

                    EditText edit =
                            item.findViewById(
                                    R.id.EditText
                            );

                    edit.setText(
                            drive[1]
                    );

                    View search =
                            item.findViewById(
                                    R.id.BTSearch
                            );

                    if (search != null) {

                        search.setOnClickListener(
                                v -> {

                                    openDirectoryCallback =
                                            path -> {

                                                drive[1] =
                                                        path;

                                                edit.setText(
                                                        path
                                                );
                                            };

                                    Intent intent =
                                            new Intent(
                                                    Intent.ACTION_OPEN_DOCUMENT_TREE
                                            );

                                    if (BuildConfig.VERSION_CODE >= 1) {
                                        intent.putExtra(
                                                DocumentsContract.EXTRA_INITIAL_URI,
                                                Uri.fromFile(
                                                        Environment
                                                                .getExternalStorageDirectory()
                                                )
                                        );
                                    }

                                    startActivityForResult(
                                            intent,
                                            MainActivity
                                                    .OPEN_DIRECTORY_REQUEST_CODE
                                    );
                                }
                        );
                    }

                    View remove =
                            item.findViewById(
                                    R.id.BTRemove
                            );

                    if (remove != null) {

                        remove.setOnClickListener(
                                v -> {

                                    parent.removeView(
                                            item
                                    );

                                    if (empty != null) {

                                        empty.setVisibility(
                                                parent
                                                        .getChildCount()
                                                        == 0
                                                        ? View.VISIBLE
                                                        : View.GONE
                                        );
                                    }
                                }
                        );
                    }

                    parent.addView(item);
                };

        try {

            for (String[] drive :
                    Container.drivesIterator(
                            drives
                    )) {

                addItem.call(
                        drive
                );
            }

        } catch (Exception ignored) {
        }

        View add =
                view.findViewById(
                        R.id.BTAddDrive
                );

        if (add != null) {

            add.setOnClickListener(
                    v -> {

                        if (parent.getChildCount()
                                >= Container.MAX_DRIVE_LETTERS) {
                            return;
                        }

                        String next =
                                String.valueOf(
                                        driveLetters[
                                                parent
                                                        .getChildCount()
                                        ]
                                                .charAt(0)
                                );

                        addItem.call(
                                new String[]{
                                        next,
                                        ""
                                }
                        );

                        if (empty != null) {
                            empty.setVisibility(
                                    View.GONE
                            );
                        }
                    }
            );
        }

        if (empty != null &&
                parent.getChildCount() == 0) {

            empty.setVisibility(
                    View.VISIBLE
            );
        }
    }

    /*
     * =============================================================
     * DESKTOP THEME
     * =============================================================
     */

    private String getDesktopTheme(
            View view) {

        try {

            Spinner typeSpinner =
                    view.findViewById(
                            R.id.SDesktopBackgroundType
                    );

            Spinner themeSpinner =
                    view.findViewById(
                            R.id.SDesktopTheme
                    );

            ColorPickerView color =
                    view.findViewById(
                            R.id.CPVDesktopBackgroundColor
                    );

            if (typeSpinner == null ||
                    themeSpinner == null ||
                    color == null) {

                return WineThemeManager
                        .DEFAULT_DESKTOP_THEME;
            }

            WineThemeManager.BackgroundType type =
                    WineThemeManager
                            .BackgroundType
                            .values()[
                            typeSpinner
                                    .getSelectedItemPosition()
                            ];

            WineThemeManager.Theme theme =
                    WineThemeManager
                            .Theme
                            .values()[
                            themeSpinner
                                    .getSelectedItemPosition()
                            ];

            String result =
                    theme
                            + ","
                            + type
                            + ","
                            + color.getColorAsString();

            if (type ==
                    WineThemeManager
                            .BackgroundType
                            .IMAGE) {

                File wallpaper =
                        WineThemeManager
                                .getUserWallpaperFile(
                                        getContext()
                                );

                result +=
                        ","
                                + (
                                wallpaper.isFile()
                                        ? wallpaper
                                        .lastModified()
                                        : 0
                        );
            }

            return result;

        } catch (Exception e) {

            return WineThemeManager
                    .DEFAULT_DESKTOP_THEME;
        }
    }

    /*
     * =============================================================
     * WINE VERSION
     * =============================================================
     */

    private void loadWineVersionSpinner(
            final View view,
            Spinner spinner,
            final ArrayList wineInfos) {

        if (spinner == null ||
                wineInfos == null) {
            return;
        }

        final Context context =
                view.getContext();

        spinner.setEnabled(
                !isEditMode()
        );

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View item,
                            int position,
                            long id) {

                        try {

                            WineInfo info =
                                    (WineInfo)
                                            wineInfos.get(
                                                    position
                                            );

                            boolean main =
                                    WineInfo
                                            .isMainWineVersion(
                                                    info.identifier()
                                            );

                            CheckBox wow64 =
                                    view.findViewById(
                                            R.id.CBWoW64Mode
                                    );

                            if (wow64 != null) {

                                wow64.setEnabled(
                                        main
                                );

                                if (!main) {
                                    wow64.setChecked(
                                            false
                                    );
                                }
                            }

                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        View wineVersion =
                view.findViewById(
                        R.id.LLWineVersion
                );

        if (wineVersion != null) {
            wineVersion.setVisibility(
                    View.VISIBLE
            );
        }

        spinner.setAdapter(
                new ArrayAdapter<>(
                        context,
                        android.R.layout
                                .simple_spinner_dropdown_item,
                        wineInfos
                )
        );

        if (isEditMode()) {

            try {

                AppUtils.setSpinnerSelectionFromValue(
                        spinner,
                        WineInfo
                                .fromIdentifier(
                                        context,
                                        container
                                                .getWineVersion()
                                )
                                .toString()
                );

            } catch (Exception ignored) {
            }
        }
    }
            }
