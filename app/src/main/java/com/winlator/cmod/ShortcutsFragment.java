package com.winlator.cmod;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.bigpicture.steamgrid.SteamGridDBApi;
import com.winlator.cmod.bigpicture.steamgrid.SteamGridGridsResponse;
import com.winlator.cmod.bigpicture.steamgrid.SteamGridGridsResponseDeserializer;
import com.winlator.cmod.bigpicture.steamgrid.SteamGridSearchResponse;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmod.core.ExeIconExtractor;
import com.winlator.cmod.core.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class ShortcutsFragment extends androidx.fragment.app.Fragment {

    private static final String TAG = "ShortcutsFragment";

    private static final String STEAMGRID_BASE_URL =
            "https://www.steamgriddb.com/api/v2/";

    private static String STEAMGRID_API_KEY =
            "0324c52513634547a7b32d6d323635d0";

    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private SharedPreferences preferences;

    private boolean isGridView = false;

    private DividerItemDecoration dividerItemDecoration;

    private Shortcut shortcutForIconUpdate;

    private ActivityResultLauncher<String> iconPickerLauncher;
    private ActivityResultLauncher<String> contentPickerLauncher;

    private com.winlator.cmod.core.Callback<Uri>
            pendingContentPickerCallback;

    public static final int IMPORT_SHORTCUT = 1005;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        iconPickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if (uri != null &&
                                    shortcutForIconUpdate != null) {

                                updateShortcutIcon(
                                        uri,
                                        shortcutForIconUpdate
                                );
                            }
                        }
                );

        contentPickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {

                            com.winlator.cmod.core.Callback<Uri> callback =
                                    pendingContentPickerCallback;

                            pendingContentPickerCallback = null;

                            if (callback != null) {
                                callback.call(uri);
                            }
                        }
                );
    }

    public void pickContentArchive(
            com.winlator.cmod.core.Callback<Uri> callback) {

        pendingContentPickerCallback = callback;

        contentPickerLauncher.launch("*/*");
    }

    @Override
    public void onConfigurationChanged(
            @NonNull Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        if (recyclerView != null) {
            updateLayoutManager();
        }
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        manager = new ContainerManager(requireContext());

        loadShortcutsList();

        if (getActivity() != null) {

            AppCompatActivity activity =
                    (AppCompatActivity) getActivity();

            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar()
                        .setTitle(R.string.shortcuts);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        FrameLayout layout =
                (FrameLayout) inflater.inflate(
                        R.layout.shortcuts_fragment,
                        container,
                        false
                );

        recyclerView =
                layout.findViewById(R.id.RecyclerView);

        emptyTextView =
                layout.findViewById(R.id.TVEmptyText);

        preferences =
                PreferenceManager
                        .getDefaultSharedPreferences(requireContext());

        isGridView =
                preferences.getBoolean(
                        "shortcuts_grid_view",
                        true
                );

        updateLayoutManager();

        return layout;
    }

    @Override
    public void onCreateOptionsMenu(
            @NonNull Menu menu,
            @NonNull MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);

        MenuItem item =
                menu.add(
                        0,
                        1,
                        0,
                        isGridView
                                ? "List View"
                                : "Grid View"
                );

        item.setIcon(
                isGridView
                        ? android.R.drawable.ic_menu_agenda
                        : android.R.drawable.ic_menu_gallery
        );

        item.setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS
        );
    }

    @Override
    public boolean onOptionsItemSelected(
            @NonNull MenuItem item) {

        if (item.getItemId() == 1) {

            isGridView = !isGridView;

            preferences.edit()
                    .putBoolean(
                            "shortcuts_grid_view",
                            isGridView
                    )
                    .apply();

            updateLayoutManager();

            requireActivity().invalidateOptionsMenu();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fetchCoverFromSteamGrid(
            Shortcut shortcut,
            File destination,
            Runnable onSuccess,
            Runnable onFail) {

        SharedPreferences prefs =
                PreferenceManager
                        .getDefaultSharedPreferences(
                                requireContext()
                        );

        if (prefs.getBoolean(
                "enable_custom_api_key",
                false)) {

            String custom =
                    prefs.getString(
                            "custom_api_key",
                            ""
                    );

            if (custom != null &&
                    !custom.isEmpty()) {

                STEAMGRID_API_KEY = custom;
            }
        }

        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(STEAMGRID_BASE_URL)
                        .client(new OkHttpClient())
                        .addConverterFactory(
                                GsonConverterFactory.create()
                        )
                        .build();

        SteamGridDBApi api =
                retrofit.create(
                        SteamGridDBApi.class
                );

        Call<SteamGridSearchResponse> call =
                api.searchGame(
                        "Bearer " + STEAMGRID_API_KEY,
                        shortcut.name
                );

        call.enqueue(
                new Callback<SteamGridSearchResponse>() {

                    @Override
                    public void onResponse(
                            Call<SteamGridSearchResponse> call,
                            Response<SteamGridSearchResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().data != null
                                && !response.body().data.isEmpty()) {

                            int gameId =
                                    response.body()
                                            .data
                                            .get(0)
                                            .id;

                            fetchSteamGridCovers(
                                    gameId,
                                    destination,
                                    onSuccess,
                                    onFail
                            );

                        } else {

                            if (onFail != null) {
                                onFail.run();
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<SteamGridSearchResponse> call,
                            Throwable t) {

                        Log.e(
                                TAG,
                                "SteamGridDB search error",
                                t
                        );

                        if (onFail != null) {
                            onFail.run();
                        }
                    }
                }
        );
    }

    private void fetchSteamGridCovers(
            int gameId,
            File destination,
            Runnable onSuccess,
            Runnable onFail) {

        Gson gson =
                new GsonBuilder()
                        .registerTypeAdapter(
                                SteamGridGridsResponse.class,
                                new SteamGridGridsResponseDeserializer()
                        )
                        .create();

        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(STEAMGRID_BASE_URL)
                        .client(new OkHttpClient())
                        .addConverterFactory(
                                GsonConverterFactory.create(gson)
                        )
                        .build();

        SteamGridDBApi api =
                retrofit.create(
                        SteamGridDBApi.class
                );

        Call<SteamGridGridsResponse> call =
                api.getGridsByGameId(
                        "Bearer " + STEAMGRID_API_KEY,
                        gameId,
                        "alternate",
                        "600x900",
                        "static"
                );

        call.enqueue(
                new Callback<SteamGridGridsResponse>() {

                    @Override
                    public void onResponse(
                            Call<SteamGridGridsResponse> call,
                            Response<SteamGridGridsResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().data != null
                                && !response.body().data.isEmpty()) {

                            String url =
                                    response.body()
                                            .data
                                            .get(0)
                                            .url;

                            downloadAndSaveCover(
                                    url,
                                    destination,
                                    onSuccess,
                                    onFail
                            );

                        } else if (onFail != null) {

                            onFail.run();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<SteamGridGridsResponse> call,
                            Throwable t) {

                        Log.e(
                                TAG,
                                "SteamGridDB grids error",
                                t
                        );

                        if (onFail != null) {
                            onFail.run();
                        }
                    }
                }
        );
    }

    private void downloadAndSaveCover(
            String url,
            File destination,
            Runnable onSuccess,
            Runnable onFail) {

        Executors
                .newSingleThreadExecutor()
                .execute(() -> {

                    HttpURLConnection connection = null;

                    try {

                        connection =
                                (HttpURLConnection)
                                        new URL(url)
                                                .openConnection();

                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(15000);
                        connection.connect();

                        Bitmap bitmap =
                                BitmapFactory.decodeStream(
                                        connection.getInputStream()
                                );

                        if (bitmap == null) {

                            if (onFail != null) {
                                onFail.run();
                            }

                            return;
                        }

                        File parent =
                                destination.getParentFile();

                        if (parent != null) {
                            parent.mkdirs();
                        }

                        try (FileOutputStream output =
                                     new FileOutputStream(destination)) {

                            bitmap.compress(
                                    Bitmap.CompressFormat.PNG,
                                    100,
                                    output
                            );
                        }

                        bitmap.recycle();

                        if (getActivity() != null) {

                            getActivity().runOnUiThread(
                                    () -> {

                                        if (onSuccess != null) {
                                            onSuccess.run();
                                        }
                                    }
                            );
                        }

                    } catch (Exception e) {

                        Log.e(
                                TAG,
                                "Cover download error",
                                e
                        );

                        if (onFail != null) {
                            onFail.run();
                        }

                    } finally {

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                });
    }

    private File getImagesDir(boolean cover) {

        File directory =
                new File(
                        Environment
                                .getExternalStorageDirectory(),
                        cover
                                ? "Winlator/covers"
                                : "Winlator/icons"
                );

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File noMedia =
                new File(directory, ".nomedia");

        if (!noMedia.exists()) {

            try {
                noMedia.createNewFile();
            } catch (IOException ignored) {
            }
        }

        return directory;
    }

    private void updateLayoutManager() {

        if (recyclerView == null) {
            return;
        }

        if (isGridView) {

            int orientation =
                    getResources()
                            .getConfiguration()
                            .orientation;

            int span =
                    orientation ==
                            Configuration.ORIENTATION_LANDSCAPE
                            ? 3
                            : 2;

            recyclerView.setLayoutManager(
                    new GridLayoutManager(
                            requireContext(),
                            span
                    )
            );

            if (dividerItemDecoration != null) {

                recyclerView.removeItemDecoration(
                        dividerItemDecoration
                );

                dividerItemDecoration = null;
            }

        } else {

            recyclerView.setLayoutManager(
                    new LinearLayoutManager(
                            requireContext()
                    )
            );

            if (dividerItemDecoration == null) {

                dividerItemDecoration =
                        new DividerItemDecoration(
                                requireContext(),
                                DividerItemDecoration.VERTICAL
                        );

                recyclerView.addItemDecoration(
                        dividerItemDecoration
                );
            }
        }

        if (recyclerView.getAdapter() != null) {

            recyclerView.getAdapter()
                    .notifyDataSetChanged();
        }
    }

    public void loadShortcutsList() {

        if (manager == null ||
                recyclerView == null) {
            return;
        }

        ArrayList<Shortcut> shortcuts =
                manager.loadShortcuts();

        if (shortcuts == null) {
            shortcuts = new ArrayList<>();
        }

        shortcuts.removeIf(
                shortcut ->
                        shortcut == null
                                || shortcut.file == null
                                || shortcut.file.getName().isEmpty()
        );

        Bitmap defaultIcon =
                BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.icon_wine
                );

        for (Shortcut shortcut : shortcuts) {

            if (shortcut.icon == null) {
                shortcut.icon = defaultIcon;
            }
        }

        recyclerView.setAdapter(
                new ShortcutsAdapter(shortcuts)
        );

        emptyTextView.setVisibility(
                shortcuts.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void updateShortcutIcon(
            Uri sourceUri,
            Shortcut shortcut) {

        try {

            File directory =
                    getImagesDir(false);

            String baseName =
                    FileUtils.getBasename(
                            shortcut.file.getPath()
                    );

            File destination =
                    new File(
                            directory,
                            baseName + ".user.png"
                    );

            try (
                    InputStream input =
                            requireContext()
                                    .getContentResolver()
                                    .openInputStream(sourceUri);

                    OutputStream output =
                            new FileOutputStream(destination)
            ) {

                byte[] buffer = new byte[8192];

                int length;

                while ((length =
                        input.read(buffer)) > 0) {

                    output.write(
                            buffer,
                            0,
                            length
                    );
                }
            }

            Toast.makeText(
                    requireContext(),
                    "Icon updated!",
                    Toast.LENGTH_SHORT
            ).show();

            if (recyclerView.getAdapter() != null) {
                recyclerView.getAdapter()
                        .notifyDataSetChanged();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Icon update error",
                    e
            );

            Toast.makeText(
                    requireContext(),
                    "Error saving icon",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private File resolveExeFile(Shortcut item) {

        if (item == null ||
                item.path == null ||
                item.path.isEmpty()) {

            return null;
        }

        String path =
                item.path
                        .replace("\\", "/")
                        .trim();

        if (path.startsWith("\"") &&
                path.endsWith("\"")) {

            path =
                    path.substring(
                            1,
                            path.length() - 1
                    );
        }

        if (path.startsWith("/")) {

            File file = new File(path);

            if (file.exists()) {
                return file;
            }
        }

        if (path.length() >= 3 &&
                path.charAt(1) == ':' &&
                path.charAt(2) == '/') {

            String drive =
                    path.substring(0, 1)
                            .toLowerCase();

            String relative =
                    path.substring(3);

            if (item.container != null) {

                for (String[] entry :
                        item.container.drivesIterator()) {

                    if (entry == null ||
                            entry.length < 2 ||
                            entry[0] == null ||
                            entry[1] == null) {

                        continue;
                    }

                    if (entry[0]
                            .replace(":", "")
                            .trim()
                            .equalsIgnoreCase(drive)) {

                        File file =
                                new File(
                                        entry[1],
                                        relative
                                );

                        if (file.exists()) {
                            return file;
                        }
                    }
                }
            }

            if (drive.equals("c")) {

                File root =
                        item.container != null
                                ? item.container.getRootDir()
                                : null;

                if (root != null) {

                    File file =
                            new File(
                                    root,
                                    ".wine/drive_c/"
                                            + relative
                            );

                    if (file.exists()) {
                        return file;
                    }
                }
            }

            if (drive.equals("d")) {

                File downloads =
                        new File(
                                Environment
                                        .getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS
                                        ),
                                relative
                        );

                if (downloads.exists()) {
                    return downloads;
                }

                File external =
                        new File(
                                Environment
                                        .getExternalStorageDirectory(),
                                relative
                        );

                if (external.exists()) {
                    return external;
                }
            }

            if (drive.equals("z")) {

                File file =
                        new File(
                                "/" + relative
                        );

                if (file.exists()) {
                    return file;
                }
            }
        }

        return null;
    }

    private class ShortcutsAdapter
            extends RecyclerView.Adapter<
                    ShortcutsAdapter.ViewHolder> {

        private final List<Shortcut> data;

        private class ViewHolder
                extends RecyclerView.ViewHolder {

            final View menuButton;
            final ImageView imageView;
            final TextView title;
            final TextView subtitle;
            final View innerArea;

            ViewHolder(View view) {

                super(view);

                imageView =
                        view.findViewById(
                                R.id.ImageView
                        );

                title =
                        view.findViewById(
                                R.id.TVTitle
                        );

                subtitle =
                        view.findViewById(
                                R.id.TVSubtitle
                        );

                menuButton =
                        view.findViewById(
                                R.id.BTMenu
                        );

                innerArea =
                        view.findViewById(
                                R.id.LLInnerArea
                        );
            }
        }

        ShortcutsAdapter(
                List<Shortcut> data) {

            this.data = data;
        }

        @Override
        public int getItemViewType(int position) {

            return isGridView ? 1 : 0;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            int layout =
                    viewType == 1
                            ? R.layout.shortcut_grid_item
                            : R.layout.shortcut_list_item;

            View view =
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(
                                    layout,
                                    parent,
                                    false
                            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull ViewHolder holder,
                int position) {

            Shortcut shortcut =
                    data.get(position);

            holder.title.setText(
                    shortcut.name
            );

            if (shortcut.container != null) {

                holder.subtitle.setText(
                        shortcut.container.getName()
                );
            } else {

                holder.subtitle.setText("");
            }

            holder.innerArea.setOnClickListener(
                    v -> runFromShortcut(shortcut)
            );

            if (isGridView) {

                holder.menuButton.setVisibility(
                        View.GONE
                );

                holder.innerArea.setOnLongClickListener(
                        v -> {

                            showListItemMenu(
                                    holder.title,
                                    shortcut
                            );

                            return true;
                        }
                );

                String baseName =
                        FileUtils.getBasename(
                                shortcut.file.getPath()
                        );

                File userIcon =
                        new File(
                                getImagesDir(false),
                                baseName + ".user.png"
                        );

                File cover =
                        new File(
                                getImagesDir(true),
                                baseName + ".png"
                        );

                if (userIcon.exists()) {

                    holder.imageView.setImageBitmap(
                            BitmapFactory.decodeFile(
                                    userIcon.getPath()
                            )
                    );

                } else if (cover.exists()) {

                    holder.imageView.setImageBitmap(
                            BitmapFactory.decodeFile(
                                    cover.getPath()
                            )
                    );

                } else {

                    holder.imageView.setImageResource(
                            R.drawable.icon_wine
                    );

                    fetchCoverFromSteamGrid(
                            shortcut,
                            cover,
                            () -> {

                                if (getActivity() != null) {

                                    getActivity().runOnUiThread(
                                            () -> {

                                                if (cover.exists()) {

                                                    holder.imageView
                                                            .setImageBitmap(
                                                                    BitmapFactory.decodeFile(
                                                                            cover.getPath()
                                                                    )
                                                            );
                                                }
                                            }
                                    );
                                }
                            },
                            () -> {

                                File exe =
                                        resolveExeFile(
                                                shortcut
                                        );

                                if (exe != null) {

                                    ExeIconExtractor.extractAsync(
                                            exe,
                                            cover,
                                            true,
                                            () -> {

                                                if (getActivity() != null) {

                                                    getActivity()
                                                            .runOnUiThread(
                                                                    () -> {

                                                                        if (cover.exists()) {

                                                                            holder.imageView
                                                                                    .setImageBitmap(
                                                                                            BitmapFactory.decodeFile(
                                                                                                    cover.getPath()
                                                                                            )
                                                                                    );
                                                                        }
                                                                    }
                                                            );
                                                }
                                            }
                                    );
                                }
                            }
                    );
                }

            } else {

                holder.innerArea
                        .setOnLongClickListener(null);

                holder.menuButton.setVisibility(
                        View.VISIBLE
                );

                holder.menuButton.setOnClickListener(
                        v -> showListItemMenu(
                                v,
                                shortcut
                        )
                );

                String baseName =
                        FileUtils.getBasename(
                                shortcut.file.getPath()
                        );

                File icon =
                        new File(
                                getImagesDir(false),
                                baseName + ".png"
                        );

                File userIcon =
                        new File(
                                getImagesDir(false),
                                baseName + ".user.png"
                        );

                if (userIcon.exists()) {

                    holder.imageView.setImageBitmap(
                            BitmapFactory.decodeFile(
                                    userIcon.getPath()
                            )
                    );

                } else if (icon.exists()) {

                    holder.imageView.setImageBitmap(
                            BitmapFactory.decodeFile(
                                    icon.getPath()
                            )
                    );

                } else if (shortcut.icon != null) {

                    holder.imageView.setImageBitmap(
                            shortcut.icon
                    );

                } else {

                    holder.imageView.setImageResource(
                            R.drawable.icon_wine
                    );
                }
            }
        }

        @Override
        public int getItemCount() {

            return data.size();
        }

        private void showListItemMenu(
                View anchor,
                Shortcut shortcut) {

            PopupMenu popup =
                    new PopupMenu(
                            requireContext(),
                            anchor
                    );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                popup.setForceShowIcon(true);
            }

            popup.inflate(
                    R.menu.shortcut_popup_menu
            );

            popup.setOnMenuItemClickListener(
                    menuItem -> {

                        int id =
                                menuItem.getItemId();

                        if (id ==
                                R.id.shortcut_settings) {

                            new ShortcutSettingsDialog(
                                    ShortcutsFragment.this,
                                    shortcut
                            ).show();

                        } else if (id ==
                                R.id.shortcut_change_icon) {

                            shortcutForIconUpdate =
                                    shortcut;

                            iconPickerLauncher.launch(
                                    "image/*"
                            );

                        } else if (id ==
                                R.id.shortcut_remove) {

                            ContentDialog.confirm(
                                    requireContext(),
                                    R.string.do_you_want_to_remove_this_shortcut,
                                    () -> {

                                        boolean deleted =
                                                shortcut.file.delete();

                                        if (deleted) {

                                            disableShortcutOnScreen(
                                                    requireContext(),
                                                    shortcut
                                            );

                                            loadShortcutsList();

                                            Toast.makeText(
                                                    requireContext(),
                                                    "Shortcut removed.",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    }
                            );

                        } else if (id ==
                                R.id.shortcut_clone_to_container) {

                            ContainerManager cm =
                                    new ContainerManager(
                                            requireContext()
                                    );

                            ArrayList<Container> containers =
                                    cm.getContainers();

                            String[] names =
                                    new String[
                                            containers.size()
                                    ];

                            for (int i = 0;
                                    i < containers.size();
                                    i++) {

                                names[i] =
                                        containers
                                                .get(i)
                                                .getName();
                            }

                            new AlertDialog.Builder(
                                    requireContext()
                            )
                                    .setTitle(
                                            "Select a container"
                                    )
                                    .setItems(
                                            names,
                                            (dialog, which) -> {

                                                if (shortcut.cloneToContainer(
                                                        containers.get(which)
                                                )) {

                                                    loadShortcutsList();

                                                    Toast.makeText(
                                                            requireContext(),
                                                            "Cloned successfully.",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                            }
                                    )
                                    .show();

                        } else if (id ==
                                R.id.shortcut_add_to_home_screen) {

                            if (shortcut.getExtra("uuid")
                                    .equals("")) {

                                shortcut.genUUID();
                            }

                            addShortcutToScreen(
                                    shortcut
                            );

                        } else if (id ==
                                R.id.shortcut_export) {

                            exportShortcut(
                                    shortcut
                            );
                        }

                        return true;
                    }
            );

            popup.show();
        }

        private void runFromShortcut(
                Shortcut shortcut) {

            Activity activity =
                    getActivity();

            if (activity == null) {
                return;
            }

            if (!XrActivity.isEnabled(
                    requireContext()
            )) {

                Intent intent =
                        new Intent(
                                activity,
                                XServerDisplayActivity.class
                        );

                intent.putExtra(
                        "container_id",
                        shortcut.container.id
                );

                intent.putExtra(
                        "shortcut_path",
                        shortcut.file.getPath()
                );

                intent.putExtra(
                        "shortcut_name",
                        shortcut.name
                );

                intent.putExtra(
                        "disableXinput",
                        shortcut.getExtra(
                                "disableXinput",
                                "0"
                        )
                );

                intent.putExtra(
                        "native_rendering",
                        shortcut.getRendererNative()
                );

                activity.startActivity(intent);

            } else {

                XrActivity.openIntent(
                        activity,
                        shortcut.container.id,
                        shortcut.file.getPath()
                );
            }
        }

        private void exportShortcut(
                Shortcut shortcut) {

            SharedPreferences prefs =
                    PreferenceManager
                            .getDefaultSharedPreferences(
                                    requireContext()
                            );

            String uriString =
                    prefs.getString(
                            "shortcuts_export_path_uri",
                            null
                    );

            File directory;

            if (uriString != null) {

                Uri uri =
                        Uri.parse(uriString);

                DocumentFile document =
                        DocumentFile.fromTreeUri(
                                requireContext(),
                                uri
                        );

                if (document == null ||
                        !document.canWrite()) {

                    return;
                }

                directory =
                        new File(
                                FileUtils.getFilePathFromUri(
                                        requireContext(),
                                        uri
                                )
                        );

            } else {

                directory =
                        new File(
                                SettingsFragment
                                        .DEFAULT_SHORTCUT_EXPORT_PATH
                        );
            }

            if (!directory.exists() &&
                    !directory.mkdirs()) {

                return;
            }

            File output =
                    new File(
                            directory,
                            shortcut.file.getName()
                    );

            try {

                List<String> lines =
                        new ArrayList<>();

                boolean found = false;

                try (BufferedReader reader =
                             new BufferedReader(
                                     new FileReader(
                                             shortcut.file
                                     )
                             )) {

                    String line;

                    while ((line =
                            reader.readLine()) != null) {

                        if (line.startsWith(
                                "container_id:"
                        )) {

                            lines.add(
                                    "container_id:"
                                            + shortcut.container.id
                            );

                            found = true;

                        } else {

                            lines.add(line);
                        }
                    }
                }

                if (!found) {

                    lines.add(
                            "container_id:"
                                    + shortcut.container.id
                    );
                }

                try (FileWriter writer =
                             new FileWriter(
                                     output,
                                     false
                             )) {

                    for (String line : lines) {

                        writer.write(
                                line + "\n"
                        );
                    }
                }

                Toast.makeText(
                        requireContext(),
                        output.getAbsolutePath(),
                        Toast.LENGTH_LONG
                ).show();

            } catch (IOException e) {

                Log.e(
                        TAG,
                        "Export error",
                        e
                );
            }
        }
    }

    private ShortcutInfo buildScreenShortCut(
            String shortLabel,
            String longLabel,
            int containerId,
            String shortcutPath,
            Icon icon,
            String uuid) {

        Intent intent =
                new Intent(
                        requireContext(),
                        XServerDisplayActivity.class
                );

        intent.setAction(
                Intent.ACTION_VIEW
        );

        intent.putExtra(
                "container_id",
                containerId
        );

        intent.putExtra(
                "shortcut_path",
                shortcutPath
        );

        return new ShortcutInfo.Builder(
                requireContext(),
                uuid
        )
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(icon)
                .setIntent(intent)
                .build();
    }

    private void addShortcutToScreen(
            Shortcut shortcut) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        ShortcutManager manager =
                getSystemService(
                        requireContext(),
                        ShortcutManager.class
                );

        if (manager == null ||
                !manager.isRequestPinShortcutSupported()) {

            return;
        }

        File iconFile =
                new File(
                        getImagesDir(false),
                        FileUtils.getBasename(
                                shortcut.file.getPath()
                        ) + ".png"
                );

        Bitmap bitmap =
                iconFile.exists()
                        ? BitmapFactory.decodeFile(
                                iconFile.getPath()
                        )
                        : shortcut.icon;

        if (bitmap == null) {

            bitmap =
                    BitmapFactory.decodeResource(
                            getResources(),
                            R.drawable.icon_wine
                    );
        }

        manager.requestPinShortcut(
                buildScreenShortCut(
                        shortcut.name,
                        shortcut.name,
                        shortcut.container.id,
                        shortcut.file.getPath(),
                        Icon.createWithBitmap(bitmap),
                        shortcut.getExtra("uuid")
                ),
                null
        );
    }

    public static void disableShortcutOnScreen(
            Context context,
            Shortcut shortcut) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        try {

            ShortcutManager manager =
                    getSystemService(
                            context,
                            ShortcutManager.class
                    );

            if (manager != null) {

                manager.disableShortcuts(
                        Collections.singletonList(
                                shortcut.getExtra("uuid")
                        ),
                        context.getString(
                                R.string.shortcut_not_available
                        )
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Disable shortcut error",
                    e
            );
        }
    }

    public void updateShortcutOnScreen(
            String shortLabel,
            String longLabel,
            int containerId,
            String shortcutPath,
            Icon icon,
            String uuid) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        try {

            ShortcutManager manager =
                    getSystemService(
                            requireContext(),
                            ShortcutManager.class
                    );

            if (manager == null) {
                return;
            }

            for (ShortcutInfo info :
                    manager.getPinnedShortcuts()) {

                if (info.getId().equals(uuid)) {

                    manager.updateShortcuts(
                            Collections.singletonList(
                                    buildScreenShortCut(
                                            shortLabel,
                                            longLabel,
                                            containerId,
                                            shortcutPath,
                                            icon,
                                            uuid
                                    )
                            )
                    );

                    break;
                }
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Update shortcut error",
                    e
            );
        }
    }
            }
