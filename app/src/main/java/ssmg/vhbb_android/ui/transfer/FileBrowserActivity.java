package ssmg.vhbb_android.ui.transfer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    private static final int PERM_CODE = 101;

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM_CODE);
        } else {
            mCurrentDir = Environment.getExternalStorageDirectory();
            loadDirectory(mCurrentDir);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCurrentDir = Environment.getExternalStorageDirectory();
            loadDirectory(mCurrentDir);
        } else {
            finish();
        }
    }

    private void loadDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        mCurrentDir = dir;
        mPathView.setText(dir.getAbsolutePath());

        List<File> entries = new ArrayList<>();

        if (dir.getParentFile() != null) {
            entries.add(null);
        }

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
            entries.addAll(fileList);
        }

        mAdapter.setEntries(entries, dir.getParentFile());
    }

    private void returnResult(String path) {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_PATH, path);
        setResult(RESULT_OK, result);
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

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_file_entry, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            File entry = mEntries.get(position);

            if (entry == null) {
                holder.mName.setText("..");
                holder.mSize.setText(getString(R.string.filebrowser_parent_dir));
                holder.mIcon.setImageResource(R.drawable.ic_folder_24dp);
                holder.itemView.setOnClickListener(v -> { if (mParent != null) loadDirectory(mParent); });
                return;
            }

            holder.mName.setText(entry.getName());

            if (entry.isDirectory()) {
                holder.mIcon.setImageResource(R.drawable.ic_folder_24dp);
                holder.mSize.setText(getString(R.string.filebrowser_folder));
                holder.itemView.setOnClickListener(v -> {
                    if (mMode == MODE_FOLDER) {
                        holder.itemView.setOnLongClickListener(lv -> { returnResult(entry.getAbsolutePath()); return true; });
                    }
                    loadDirectory(entry);
                });
                if (mMode == MODE_FOLDER) {
                    holder.itemView.setOnLongClickListener(v -> { returnResult(entry.getAbsolutePath()); return true; });
                }
            } else {
                holder.mIcon.setImageResource(android.R.drawable.ic_menu_save);
                holder.mSize.setText(formatSize(entry.length()));
                if (mMode == MODE_FILE) {
                    holder.itemView.setOnClickListener(v -> returnResult(entry.getAbsolutePath()));
                } else {
                    holder.itemView.setOnClickListener(null);
                }
            }
        }

        @Override
        public int getItemCount() { return mEntries.size(); }

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
