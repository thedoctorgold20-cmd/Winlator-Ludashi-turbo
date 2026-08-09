package com.winlator.cmod;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;

import java.util.ArrayList;
import java.util.List;

/**

* ContainerDetailFragment

* 

* Tela de configuração/criação de Container.

* 

* Não depende de:

* com.winlator.cmod.box86_64

* 

* Recursos:

* - Criação de Container

* - Carregamento de configurações

* - Abas

* - Componentes Wine

* - Driver gráfico

* - Resolução

* - Variáveis de ambiente

* - Salvamento das configurações

* - Botão Create Container
    */
    public class ContainerDetailFragment extends Fragment {
  
  private static final String TAG =
  "ContainerDetailFragment";
  
  private static final String PREFS =
  "container_detail_settings";
  
  private int containerId = -1;
  
  private EditText containerName;
  private Spinner wineComponentsSpinner;
  private Spinner graphicsDriverSpinner;
  private Spinner resolutionSpinner;
  private EditText environmentVariables;
  
  private Button createButton;
  private Button saveButton;
  
  private SharedPreferences preferences;
  
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
 args.putInt(
         "container_id",
         containerId
 );

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

 preferences =
         requireContext()
                 .getSharedPreferences(
                         PREFS,
                         Context.MODE_PRIVATE
                 );
  
  }
  
  @Nullable
  @Override
  public View onCreateView(
  @NonNull android.view.LayoutInflater inflater,
  @Nullable ViewGroup container,
  @Nullable Bundle savedInstanceState) {
  
   return createContainerInterface(
         requireContext()
 );
  
  }
  
  /**
  
  * Cria toda a interface programaticamente.
    */
    private View createContainerInterface(
    Context context) {
    
    ScrollView scrollView =
    new ScrollView(context);
    
    LinearLayout root =
    new LinearLayout(context);
    
    root.setOrientation(
    LinearLayout.VERTICAL
    );
    
    root.setPadding(
    dp(16),
    dp(16),
    dp(16),
    dp(24)
    );
    
    scrollView.addView(
    root,
    new ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
    )
    );
    
    TextView title =
    new TextView(context);
    
    title.setText(
    "Container"
    );
    
    title.setTextSize(24);
    title.setGravity(Gravity.CENTER_VERTICAL);
    
    root.addView(
    title,
    matchWrap()
    );
    
    TextView description =
    new TextView(context);
    
    description.setText(
    "Configure o ambiente Windows/Wine."
    );
    
    description.setPadding(
    0,
    dp(4),
    0,
    dp(16)
    );
    
    root.addView(
    description,
    matchWrap()
    );
    
    /*
    
    * Nome
      */
      root.addView(
      createLabel(
      context,
      "Nome do Container"
      )
      );
    
    containerName =
    new EditText(context);
    
    containerName.setSingleLine(true);
    containerName.setHint(
    "Ex.: Windows 11"
    );
    
    root.addView(
    containerName,
    matchWrap()
    );
    
    /*
    
    * Abas
      */
      TabHost tabHost =
      new TabHost(context);
    
    tabHost.setId(
    View.generateViewId()
    );
    
    tabHost.setup();
    
    LinearLayout tabContainer =
    new LinearLayout(context);
    
    tabContainer.setOrientation(
    LinearLayout.VERTICAL
    );
    
    TabHost.TabWidget tabWidget =
    new TabHost.TabWidget(context);
    
    tabWidget.setId(
    android.R.id.tabs
    );
    
    tabContainer.addView(
    tabWidget,
    matchWrap()
    );
    
    FrameLayout tabContent =
    new FrameLayout(context);
    
    tabContent.setId(
    android.R.id.tabcontent
    );
    
    tabContainer.addView(
    tabContent,
    new LinearLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    0,
    1
    )
    );
    
    tabHost.addView(tabContainer);
    
    /*
    
    * Aba Wine
      */
      LinearLayout wineTab =
      createWineTab(context);
    
    wineTab.setId(
    View.generateViewId()
    );
    
    tabContent.addView(
    wineTab,
    new FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
    )
    );
    
    /*
    
    * Aba Graphics
      */
      LinearLayout graphicsTab =
      createGraphicsTab(context);
    
    graphicsTab.setId(
    View.generateViewId()
    );
    
    tabContent.addView(
    graphicsTab,
    new FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
    )
    );
    
    /*
    
    * Aba Environment
      */
      LinearLayout environmentTab =
      createEnvironmentTab(context);
    
    environmentTab.setId(
    View.generateViewId()
    );
    
    tabContent.addView(
    environmentTab,
    new FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
    )
    );
    
    tabHost.addTab(
    tabHost.newTabSpec("wine")
    .setIndicator("Wine")
    .setContent(wineTab.getId())
    );
    
    tabHost.addTab(
    tabHost.newTabSpec("graphics")
    .setIndicator("Graphics")
    .setContent(graphicsTab.getId())
    );
    
    tabHost.addTab(
    tabHost.newTabSpec("environment")
    .setIndicator("Environment")
    .setContent(environmentTab.getId())
    );
    
    root.addView(
    tabHost,
    new LinearLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    dp(420)
    )
    );
    
    /*
    
    * Botão salvar
      */
      saveButton =
      new Button(context);
    
    saveButton.setText(
    "Save Configuration"
    );
    
    saveButton.setOnClickListener(
    v -> saveConfiguration()
    );
    
    root.addView(
    saveButton,
    matchWrap()
    );
    
    /*
    
    * Botão criar
      */
      createButton =
      new Button(context);
    
    createButton.setText(
    "Create Container"
    );
    
    createButton.setOnClickListener(
    v -> createContainer()
    );
    
    root.addView(
    createButton,
    matchWrap()
    );
    
    /*
    
    * Carrega configurações existentes
      */
      loadConfiguration();
    
    return scrollView;
    }
  
  /**
  
  * Aba Wine.
    */
    private LinearLayout createWineTab(
    Context context) {
    
    LinearLayout layout =
    createVerticalLayout(context);
    
    layout.addView(
    createLabel(
    context,
    "Componentes Wine"
    )
    );
    
    wineComponentsSpinner =
    new Spinner(context);
    
    String[] components = {
    "wine",
    "wine-ge",
    "wine-staging",
    "proton",
    "proton-ge"
    };
    
    wineComponentsSpinner.setAdapter(
    new ArrayAdapter<>(
    context,
    android.R.layout.simple_spinner_dropdown_item,
    components
    )
    );
    
    layout.addView(
    wineComponentsSpinner,
    matchWrap()
    );
    
    TextView info =
    new TextView(context);
    
    info.setText(
    "Selecione o ambiente Wine utilizado pelo Container."
    );
    
    info.setPadding(
    0,
    dp(12),
    0,
    0
    );
    
    layout.addView(
    info,
    matchWrap()
    );
    
    return layout;
    }
  
  /**
  
  * Aba de gráficos.
    */
    private LinearLayout createGraphicsTab(
    Context context) {
    
    LinearLayout layout =
    createVerticalLayout(context);
    
    layout.addView(
    createLabel(
    context,
    "Driver gráfico"
    )
    );
    
    graphicsDriverSpinner =
    new Spinner(context);
    
    String[] drivers = {
    "Auto",
    "Turnip",
    "VirGL",
    "Zink",
    "Software"
    };
    
    graphicsDriverSpinner.setAdapter(
    new ArrayAdapter<>(
    context,
    android.R.layout.simple_spinner_dropdown_item,
    drivers
    )
    );
    
    layout.addView(
    graphicsDriverSpinner,
    matchWrap()
    );
    
    layout.addView(
    createLabel(
    context,
    "Resolução"
    )
    );
    
    resolutionSpinner =
    new Spinner(context);
    
    String[] resolutions = {
    "1280x720",
    "1280x800",
    "1366x768",
    "1600x900",
    "1920x1080"
    };
    
    resolutionSpinner.setAdapter(
    new ArrayAdapter<>(
    context,
    android.R.layout.simple_spinner_dropdown_item,
    resolutions
    )
    );
    
    layout.addView(
    resolutionSpinner,
    matchWrap()
    );
    
    return layout;
    }
  
  /**
  
  * Aba de variáveis de ambiente.
    */
    private LinearLayout createEnvironmentTab(
    Context context) {
    
    LinearLayout layout =
    createVerticalLayout(context);
    
    layout.addView(
    createLabel(
    context,
    "Variáveis de ambiente"
    )
    );
    
    environmentVariables =
    new EditText(context);
    
    environmentVariables.setHint(
    "VAR=valor\nVAR2=valor"
    );
    
    environmentVariables.setGravity(
    Gravity.TOP
    );
    
    environmentVariables.setInputType(
    InputType.TYPE_CLASS_TEXT |
    InputType.TYPE_TEXT_FLAG_MULTI_LINE
    );
    
    environmentVariables.setMinLines(8);
    
    layout.addView(
    environmentVariables,
    matchWrap()
    );
    
    return layout;
    }
  
  /**
  
  * Salva as configurações.
    */
    public void saveConfiguration() {
    
    if (preferences == null) {
    preferences =
    requireContext()
    .getSharedPreferences(
    PREFS,
    Context.MODE_PRIVATE
    );
    }
    
    String name =
    containerName != null
    ? containerName.getText().toString()
    : "";
    
    String wine =
    getSpinnerValue(
    wineComponentsSpinner
    );
    
    String driver =
    getSpinnerValue(
    graphicsDriverSpinner
    );
    
    String resolution =
    getSpinnerValue(
    resolutionSpinner
    );
    
    String environment =
    environmentVariables != null
    ? environmentVariables
    .getText()
    .toString()
    : "";
    
    preferences.edit()
    .putString(
    "container_name",
    name
    )
    .putString(
    "wine_components",
    wine
    )
    .putString(
    "graphics_driver",
    driver
    )
    .putString(
    "resolution",
    resolution
    )
    .putString(
    "environment_variables",
    environment
    )
    .apply();
    
    Toast.makeText(
    requireContext(),
    "Configurações salvas.",
    Toast.LENGTH_SHORT
    ).show();
    
    Log.d(
    TAG,
    "Configurações do Container salvas."
    );
    }
  
  /**
  
  * Carrega configurações.
    */
    public void loadConfiguration() {
    
    if (preferences == null) {
    return;
    }
    
    if (containerName != null) {
    
     containerName.setText(
         preferences.getString(
                 "container_name",
                 ""
         )
 );
    
    }
    
    selectSpinner(
    wineComponentsSpinner,
    preferences.getString(
    "wine_components",
    "wine"
    )
    );
    
    selectSpinner(
    graphicsDriverSpinner,
    preferences.getString(
    "graphics_driver",
    "Auto"
    )
    );
    
    selectSpinner(
    resolutionSpinner,
    preferences.getString(
    "resolution",
    "1280x720"
    )
    );
    
    if (environmentVariables != null) {
    
     environmentVariables.setText(
         preferences.getString(
                 "environment_variables",
                 ""
         )
 );
    
    }
    }
  
  /**
  
  * Criação do Container.
  
  * 
  
  * O método tenta utilizar a API existente do projeto
  
  * quando disponível, mas não depende do pacote box86_64.
    */
    private void createContainer() {
    
    String name =
    containerName.getText()
    .toString()
    .trim();
    
    if (name.isEmpty()) {
    
     containerName.setError(
         "Digite o nome do Container"
 );

 containerName.requestFocus();

 return;
    
    }
    
    /*
    
    * Salva primeiro as configurações.
      */
      saveConfiguration();
    
    /*
    
    * Procura ContainerManager dinamicamente.
    
    * 
    
    * Isso evita dependência de APIs diferentes
    
    * entre forks do Winlator.
      */
      try {
      
      Class<?> managerClass =
      Class.forName(
      "com.winlator.cmod.container.ContainerManager"
      );
      
      Object manager =
      managerClass
      .getConstructor(Context.class)
      .newInstance(
      requireContext()
      );
      
      /*
      
      * Procura métodos de criação conhecidos.
        */
        java.lang.reflect.Method[] methods =
        managerClass.getMethods();
      
      for (java.lang.reflect.Method method :
      methods) {
      
       String methodName =
         method.getName();

 if (!methodName.equals(
         "createContainer"
 )) {
     continue;
 }

 Class<?>[] parameters =
         method.getParameterTypes();

 if (parameters.length == 1 &&
         parameters[0] ==
                 String.class) {

     Object result =
             method.invoke(
                     manager,
                     name
             );

     Log.d(
             TAG,
             "Container criado: "
                     + result
     );

     Toast.makeText(
             requireContext(),
             "Container criado com sucesso!",
             Toast.LENGTH_LONG
     ).show();

     return;
 }
      
      }
      
      /*
      
      * Caso o fork não tenha createContainer(String),
      * informa claramente o problema.
        */
        Toast.makeText(
        requireContext(),
        "ContainerManager não possui createContainer(String).",
        Toast.LENGTH_LONG
        ).show();
    
    } catch (Exception e) {
    
     Log.e(
         TAG,
         "Erro ao criar Container",
         e
 );

 Toast.makeText(
         requireContext(),
         "Erro ao criar Container: "
                 + e.getClass()
                         .getSimpleName(),
         Toast.LENGTH_LONG
 ).show();
    
    }
    }
  
  /**
  
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
  
  * Assinatura utilizada pelo ShortcutSettingsDialog.
    */
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
  
  /**
  
  * Obtém resolução da tela.
    */
    public String getScreenSize(
    View rootView) {
    
    return getViewText(
    rootView,
    new String[]{
    "spinner_screen_size",
    "sScreenSize",
    "screen_size",
    "screenSize",
    "resolution",
    "spinner_resolution"
    }
    );
    }
  
  /**
  
  * Obtém componentes Wine.
    */
    public String getWinComponents(
    View rootView) {
    
    return getViewText(
    rootView,
    new String[]{
    "spinner_wincomponents",
    "sWinComponents",
    "wincomponents",
    "win_components",
    "winComponents"
    }
    );
    }
  
  /**
  
  * Compatibilidade para versões que utilizam Spinner.
    */
    public static void updateGraphicsDriverSpinner(
    Context context,
    Spinner spinner) {
    
    if (context == null ||
    spinner == null) {
    return;
    }
    
    if (spinner.getAdapter() != null) {
    return;
    }
    
    String[] drivers = {
    "Auto",
    "Turnip",
    "VirGL",
    "Zink",
    "Software"
    };
    
    spinner.setAdapter(
    new ArrayAdapter<>(
    context,
    android.R.layout.simple_spinner_dropdown_item,
    drivers
    )
    );
    }
  
  private static String getViewText(
  View rootView,
  String[] names) {
  
   if (rootView == null) {
     return "";
 }

 Context context =
         rootView.getContext();

 for (String name : names) {

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

     View view =
             rootView.findViewById(id);

     if (view instanceof Spinner) {

         Object item =
                 ((Spinner) view)
                         .getSelectedItem();

         if (item != null) {
             return item.toString();
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
 }

 return "";
  
  }
  
  private static void selectSpinnerValue(
  Spinner spinner,
  String value) {
  
   if (spinner == null ||
         spinner.getAdapter() == null ||
         value == null) {
     return;
 }

 for (int i = 0;
         i < spinner.getAdapter().getCount();
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
  
  private void selectSpinner(
  Spinner spinner,
  String value) {
  
   selectSpinnerValue(
         spinner,
         value
 );
  
  }
  
  private String getSpinnerValue(
  Spinner spinner) {
  
   if (spinner == null) {
     return "";
 }

 Object item =
         spinner.getSelectedItem();

 return item == null
         ? ""
         : item.toString();
  
  }
  
  private LinearLayout createVerticalLayout(
  Context context) {
  
   LinearLayout layout =
         new LinearLayout(context);

 layout.setOrientation(
         LinearLayout.VERTICAL
 );

 layout.setPadding(
         dp(4),
         dp(12),
         dp(4),
         dp(12)
 );

 return layout;
  
  }
  
  private TextView createLabel(
  Context context,
  String text) {
  
   TextView label =
         new TextView(context);

 label.setText(text);
 label.setTextSize(16);
 label.setPadding(
         0,
         dp(12),
         0,
         dp(6)
 );

 return label;
  
  }
  
  private LinearLayout.LayoutParams matchWrap() {
  
   return new LinearLayout.LayoutParams(
         ViewGroup.LayoutParams.MATCH_PARENT,
         ViewGroup.LayoutParams.WRAP_CONTENT
 );
  
  }
  
  private int dp(int value) {
  
   return (int) (
         value *
         getResources()
                 .getDisplayMetrics()
                 .density
 );
  
  }
  
  public int getContainerId() {
  return containerId;
  }
  
  public void setContainerId(int id) {
  containerId = id;
  }
  
  public Context getFragmentContext() {
  
   if (isAdded()) {
     return requireContext();
 }

 return null;
  
  }
  
  @Override
  public void onDestroyView() {
  
   super.onDestroyView();

 Log.d(
         TAG,
         "ContainerDetailFragment finalizado: "
                 + containerId
 );
  
  }
  }
