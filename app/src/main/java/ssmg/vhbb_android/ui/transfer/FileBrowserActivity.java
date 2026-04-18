package ssmg.vhbb_android.ui.transfer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ssmg.vhbb_android.R;

public class FileBrowserActivity extends AppCompatActivity {

    public static final String EXTRA_MODE        = "MODE";
    public static final String EXTRA_RESULT_PATH = "RESULT_PATH";
    public static final int MODE_FILE            = 0;
    public static final int MODE_FOLDER          = 1;

    private static final int PERM_READ  = 101;
    private static final int PERM_ALLFS = 102;

    private int mMode;
    private File mCurrentDir;
    private FileAdapter mAdapter;
    private TextView mPathView;
    private Button mSelectFolderBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        mMode = getIntent().getIntExtra(EXTRA_MODE, MODE_FILE);

        Toolbar toolbar = findViewById(R.id.toolbar_filebrowser);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mMode == MODE_FOLDER
                    ? getString(R.string.filebrowser_pick_folder)
                    : getString(R.string.filebrowser_pick_file));
        }

        mPathView = findViewById(R.id.tv_current_path);
        mSelectFolderBtn = findViewById(R.id.btn_select_folder);

        if (mMode == MODE_FOLDER) {
            mSelectFolderBtn.setVisibility(View.VISIBLE);
            mSelectFolderBtn.setOnClickListener(v -> returnResult(mCurrentDir.getAbsolutePath()));
        }

        RecyclerView rv = findViewById(R.id.rv_files);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new FileAdapter();
        rv.setAdapter(mAdapter);

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.filebrowser_perm_title)
                        .setMessage(R.string.filebrowser_perm_message)
                        .setPositiveButton(android.R.string.ok, (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(i, PERM_ALLFS);
                        })
                        .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                        .show();
            } else {
                startBrowsing();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM_READ);
            } else {
                startBrowsing();
            }
        } else {
            startBrowsing();
        }
    }

    private void startBrowsing() {
        mCurrentDir = Environment.getExternalStorageDirectory();
        loadDirectory(mCurrentDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERM_ALLFS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                startBrowsing();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(requestCode, perms, grants);
        if (requestCode == PERM_READ && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            startBrowsing();
        } else {
            finish();
        }
    }

    private void loadDirectory(File dir) {
        if (dir == null || !dir.canRead()) return;
        mCurrentDir = dir;
        mPathView.setText(dir.getAbsolutePath());

        List<File> entries = new ArrayList<>();
        if (dir.getParentFile() != null) entries.add(null);

        File[] files = dir.listFiles();
        if (files != null) {
            List<File> dirs = new ArrayList<>();
            List<File> fileList = new ArrayList<>();
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                if (f.isDirectory()) dirs.add(f);
                else fileList.add(f);
            }
            Collections.sort(dirs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            Collections.sort(fileList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            entries.addAll(dirs);
            if (mMode == MODE_FILE) entries.addAll(fileList);
        }

        mAdapter.setEntries(entries, dir.getParentFile());
    }

    private void returnResult(String path) {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_RESULT_PATH, path));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        File parent = mCurrentDir != null ? mCurrentDir.getParentFile() : null;
        if (parent != null && !mCurrentDir.equals(Environment.getExternalStorageDirectory().getParentFile())) {
            loadDirectory(parent);
        } else {
            super.onBackPressed();
        }
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int idx = Math.min((int)(Math.log10(bytes) / Math.log10(1024)), units.length - 1);
        return new DecimalFormat("#.##").format(bytes / Math.pow(1024, idx)) + " " + units[idx];
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

        private List<File> mEntries = new ArrayList<>();
        private File mParent;

        void setEntries(List<File> entries, File parent) {
            mEntries = entries;
            mParent = parent;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_file_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            File entry = mEntries.get(pos);

            if (entry == null) {
                h.mName.setText("..");
                h.mSize.setText(getString(R.string.filebrowser_parent_dir));
                h.mIcon.setImageResource(R.drawable.ic_folder_24dp);
                h.itemView.setOnClickListener(v -> { if (mParent != null) loadDirectory(mParent); });
                h.itemView.setOnLongClickListener(null);
                return;
            }

            h.mName.setText(entry.getName());

            if (entry.isDirectory()) {
                h.mIcon.setImageResource(R.drawable.ic_folder_24dp);
                h.mSize.setText(getString(R.string.filebrowser_folder));
                h.itemView.setOnClickListener(v -> loadDirectory(entry));
                if (mMode == MODE_FOLDER) {
                    h.itemView.setOnLongClickListener(v -> { returnResult(entry.getAbsolutePath()); return true; });
                } else {
                    h.itemView.setOnLongClickListener(null);
                }
            } else {
                h.mIcon.setImageResource(android.R.drawable.ic_menu_save);
                h.mSize.setText(formatSize(entry.length()));
                h.itemView.setOnLongClickListener(null);
                if (mMode == MODE_FILE) {
                    h.itemView.setOnClickListener(v -> returnResult(entry.getAbsolutePath()));
                } else {
                    h.itemView.setOnClickListener(null);
                }
            }
        }

        @Override public int getItemCount() { return mEntries.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView mName, mSize;
            ImageView mIcon;
            VH(View v) {
                super(v);
                mName = v.findViewById(R.id.tv_file_name);
                mSize = v.findViewById(R.id.tv_file_size);
                mIcon = v.findViewById(R.id.iv_file_icon);
            }
        }
    }

}
