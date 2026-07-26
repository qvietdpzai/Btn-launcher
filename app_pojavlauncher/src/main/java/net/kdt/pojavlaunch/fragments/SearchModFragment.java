package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.runOnUiThread;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.math.MathUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SearchModFragment extends Fragment implements ModItemAdapter.SearchResultCallback {

    public static final String TAG = "SearchModFragment";
    private View mOverlay;
    private float mOverlayTopCache;

    private final RecyclerView.OnScrollListener mOverlayPositionListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            mOverlay.setY(MathUtils.clamp(mOverlay.getY() - dy, -mOverlay.getHeight(), mOverlayTopCache));
        }
    };

    private EditText mSearchEditText;
    private ImageButton mFilterButton;
    private RecyclerView mRecyclerview;
    private ModItemAdapter mModItemAdapter;
    private ProgressBar mSearchProgressBar;
    private TextView mStatusTextView;
    private ColorStateList mDefaultTextColor;
    private ModpackApi modpackApi;
    private TabLayout mTabLayout;

    private final SearchFilters mSearchFilters;

    private Button mImportButton;
    private TaskCountListener mTaskCountListener;

    ActivityResultLauncher<String> mImportLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                Context context = getContext();
                ContentResolver contentResolver = getContext().getContentResolver();
                PojavApplication.sExecutorService.execute(() -> {
                    performLocalInstall(uri, context, contentResolver);
                });
            });

    public void performLocalInstall(Uri uri, Context context, ContentResolver contentResolver) {
        String fileName = Tools.getFileName(context, uri);
        if (fileName == null) return;
        File outFile = new File(Tools.DIR_CACHE, fileName);
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, R.string.multirt_progress_caching);
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) return;
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            Tools.showErrorRemote("Error", e);
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            return;
        }
        try {
            if (fileName.endsWith(".jar")) {
                installSingleModToInstance(outFile);
            } else if (fileName.endsWith(".zip")) {
                modpackApi.installLocalModpack(fileName, outFile, null);
            } else {
                runOnUiThread(() -> Toast.makeText(context, "Unsupported file type. Use .jar for mods or .zip for modpacks.", Toast.LENGTH_LONG).show());
            }
        } catch (IOException e) {
            Tools.showErrorRemote("Error", e);
        } finally {
            outFile.delete();
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
        }
    }

    private void installSingleModToInstance(File modFile) throws IOException {
        net.kdt.pojavlaunch.instances.Instance instance = net.kdt.pojavlaunch.instances.Instances.loadSelectedInstance();
        if (instance == null) {
            runOnUiThread(() -> Toast.makeText(getContext(), R.string.no_instance, Toast.LENGTH_LONG).show());
            return;
        }
        File instanceDir = instance.getGameDirectory();
        File targetDir;
        switch (mSearchFilters.contentType) {
            case Constants.CONTENT_TYPE_SHADER:
                targetDir = new File(instanceDir, "shaderpacks");
                break;
            case Constants.CONTENT_TYPE_RESOURCE_PACK:
                targetDir = new File(instanceDir, "resourcepacks");
                break;
            default:
                targetDir = new File(instanceDir, "mods");
                break;
        }
        targetDir.mkdirs();
        File targetFile = new File(targetDir, modFile.getName());
        java.io.FileInputStream fis = new java.io.FileInputStream(modFile);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
        fis.close();
        fos.close();
    }

    public SearchModFragment(){
        super(R.layout.fragment_mod_search);
        mSearchFilters = new SearchFilters();
        mSearchFilters.contentType = Constants.CONTENT_TYPE_MOD;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        modpackApi = new CommonApi(context.getString(R.string.curseforge_api_key));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mModItemAdapter = new ModItemAdapter(getResources(), modpackApi, this);
        ProgressKeeper.addTaskCountListener(mModItemAdapter);
        mOverlayTopCache = getResources().getDimension(R.dimen.fragment_padding_medium);

        mOverlay = view.findViewById(R.id.search_mod_overlay);
        mSearchEditText = view.findViewById(R.id.search_mod_edittext);
        mSearchProgressBar = view.findViewById(R.id.search_mod_progressbar);
        mRecyclerview = view.findViewById(R.id.search_mod_list);
        mStatusTextView = view.findViewById(R.id.search_mod_status_text);
        mFilterButton = view.findViewById(R.id.search_mod_filter);
        mTabLayout = view.findViewById(R.id.search_mod_tabs);

        mDefaultTextColor = mStatusTextView.getTextColors();

        mRecyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerview.setAdapter(mModItemAdapter);
        mRecyclerview.addOnScrollListener(mOverlayPositionListener);

        mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            searchMods(mSearchEditText.getText().toString());
            mSearchEditText.clearFocus();
            return false;
        });

        mOverlay.post(()->{
           int overlayHeight = mOverlay.getHeight();
           mRecyclerview.setPadding(mRecyclerview.getPaddingLeft(),
                   mRecyclerview.getPaddingTop() + overlayHeight,
                   mRecyclerview.getPaddingRight(),
                   mRecyclerview.getPaddingBottom());
        });
        mFilterButton.setOnClickListener(v -> displayFilterDialog());

        if (mTabLayout != null) {
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0: mSearchFilters.contentType = Constants.CONTENT_TYPE_MOD; break;
                        case 1: mSearchFilters.contentType = Constants.CONTENT_TYPE_SHADER; break;
                        case 2: mSearchFilters.contentType = Constants.CONTENT_TYPE_RESOURCE_PACK; break;
                        case 3: mSearchFilters.contentType = Constants.CONTENT_TYPE_MODPACK; break;
                    }
                    searchMods(mSearchEditText.getText().toString());
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        mImportButton = view.findViewById(R.id.mineButton_import_local_modpack);
        mImportButton.setOnClickListener(v -> {
            mImportLauncher.launch("*/*");
        });
        mTaskCountListener = taskCount -> {
            runOnUiThread(() -> mImportButton.setEnabled(taskCount == 0));
            return false;
        };
        ProgressKeeper.addTaskCountListener(mTaskCountListener);

        searchMods(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ProgressKeeper.removeTaskCountListener(mModItemAdapter);
        mRecyclerview.removeOnScrollListener(mOverlayPositionListener);
        if (mTaskCountListener != null) { ProgressKeeper.removeTaskCountListener(mTaskCountListener); }
    }

    @Override
    public void onSearchFinished() {
        mSearchProgressBar.setVisibility(View.GONE);
        mStatusTextView.setVisibility(View.GONE);
    }

    @Override
    public void onSearchError(int error) {
        mSearchProgressBar.setVisibility(View.GONE);
        mStatusTextView.setVisibility(View.VISIBLE);
        switch (error) {
            case ERROR_INTERNAL:
                mStatusTextView.setTextColor(Color.RED);
                mStatusTextView.setText(R.string.search_modpack_error);
                break;
            case ERROR_NO_RESULTS:
                mStatusTextView.setTextColor(mDefaultTextColor);
                mStatusTextView.setText(R.string.search_modpack_no_result);
                break;
        }
    }

    private void searchMods(String name) {
        mSearchProgressBar.setVisibility(View.VISIBLE);
        mSearchFilters.name = name == null ? "" : name;
        mModItemAdapter.performSearchQuery(mSearchFilters);
    }

    private void displayFilterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_mod_filters)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            TextView mSelectedVersion = dialog.findViewById(R.id.search_mod_selected_mc_version_textview);
            Button mSelectVersionButton = dialog.findViewById(R.id.search_mod_mc_version_button);
            Button mApplyButton = dialog.findViewById(R.id.search_mod_apply_filters);

            assert mSelectVersionButton != null;
            assert mSelectedVersion != null;
            assert mApplyButton != null;

            mSelectVersionButton.setOnClickListener(v -> VersionSelectorDialog.open(v.getContext(), true, (id, snapshot)-> mSelectedVersion.setText(id)));
            mSelectedVersion.setText(mSearchFilters.mcVersion);

            mApplyButton.setOnClickListener(v -> {
                mSearchFilters.mcVersion = mSelectedVersion.getText().toString();
                searchMods(mSearchEditText.getText().toString());
                dialogInterface.dismiss();
            });
        });

        dialog.show();
    }
}
