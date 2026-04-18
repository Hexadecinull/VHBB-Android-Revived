package ssmg.vhbb_android.ui.transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ssmg.vhbb_android.R;

public class TransferActivity extends AppCompatActivity {

    private static final int VITA_VENDOR_ID = 0x054C;
    private static final int VITA_PID_FAT   = 0x04E4;
    private static final int VITA_PID_SLIM  = 0x0BDC;
    private static final int VITA_PID_TV    = 0x05B9;
    private static final int FTP_PORT       = 1337;
    private static final String DEFAULT_REMOTE_PATH = "/ux0:data/";

    private static final int REQ_FTP_PICK = 1001;
    private static final int REQ_USB_FILE = 1003;
    private static final int REQ_USB_DEST = 1004;

    private final List<String> mSelectedFilePathsFtp = new ArrayList<>();
    private final List<String> mSelectedFileNamesFtp = new ArrayList<>();
    private String mSelectedFilePathUsb;
    private String mUsbDestPath;
    private boolean mFilesExpanded = false;

    private TextView mFtpFileLabel;
    private LinearLayout mFtpFilesContainer;
    private TextView mUsbFileLabel;
    private TextView mUsbDestLabel;
    private TextView mFtpStatus;
    private TextView mUsbStatus;
    private TextView mUsbConnectionStatus;
    private ImageView mUsbConnectionIcon;
    private ProgressBar mFtpProgress;
    private ProgressBar mUsbProgress;
    private Button mFtpTransferBtn;
    private Button mUsbTransferBtn;
    private Button mUsbPickDestBtn;
    private Button mUsbPickFileBtn;
    private EditText mFtpIpInput;
    private EditText mFtpRemotePath;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUsbConnectionStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        Toolbar toolbar = findViewById(R.id.toolbar_transfer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.menu_transfer);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary, getTheme()));
        }

        mFtpIpInput         = findViewById(R.id.ftp_ip_input);
        mFtpRemotePath      = findViewById(R.id.ftp_remote_path);
        mFtpFileLabel       = findViewById(R.id.ftp_file_label);
        mFtpFilesContainer  = findViewById(R.id.ftp_files_container);
        mFtpStatus          = findViewById(R.id.ftp_status);
        mFtpProgress        = findViewById(R.id.ftp_progress);
        mFtpTransferBtn     = findViewById(R.id.ftp_transfer_btn);

        mUsbFileLabel           = findViewById(R.id.usb_file_label);
        mUsbDestLabel           = findViewById(R.id.usb_dest_label);
        mUsbStatus              = findViewById(R.id.usb_status);
        mUsbProgress            = findViewById(R.id.usb_progress);
        mUsbTransferBtn         = findViewById(R.id.usb_transfer_btn);
        mUsbPickDestBtn         = findViewById(R.id.usb_pick_dest_btn);
        mUsbPickFileBtn         = findViewById(R.id.usb_pick_file_btn);
        mUsbConnectionStatus    = findViewById(R.id.usb_connection_status);
        mUsbConnectionIcon      = findViewById(R.id.usb_connection_icon);

        mFtpRemotePath.setText(DEFAULT_REMOTE_PATH);

        mFtpFileLabel.setOnClickListener(v -> toggleFileList());
        findViewById(R.id.ftp_pick_btn).setOnClickListener(v -> launchBrowser(REQ_FTP_PICK, FileBrowserActivity.MODE_FILE));
        mUsbPickFileBtn.setOnClickListener(v -> launchBrowser(REQ_USB_FILE, FileBrowserActivity.MODE_FILE));
        mUsbPickDestBtn.setOnClickListener(v -> launchBrowser(REQ_USB_DEST, FileBrowserActivity.MODE_FOLDER));
        mFtpTransferBtn.setOnClickListener(v -> startFtpTransfer());
        mUsbTransferBtn.setOnClickListener(v -> startUsbTransfer());

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mUsbReceiver, filter);
        }

        updateUsbConnectionStatus();
        updateUsbTransferButton();
    }

    private void launchBrowser(int requestCode, int mode) {
        Intent intent = new Intent(this, FileBrowserActivity.class);
        intent.putExtra(FileBrowserActivity.EXTRA_MODE, mode);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        String path = data.getStringExtra(FileBrowserActivity.EXTRA_RESULT_PATH);
        if (path == null) return;

        if (requestCode == REQ_FTP_PICK) {
            File selected = new File(path);
            if (selected.isDirectory()) {
                collectFilesFromDir(selected, "");
            } else if (!mSelectedFilePathsFtp.contains(path)) {
                mSelectedFilePathsFtp.add(path);
                mSelectedFileNamesFtp.add(selected.getName());
            }
            updateFtpFileLabel();
            rebuildFileList();
        } else if (requestCode == REQ_USB_FILE) {
            mSelectedFilePathUsb = path;
            mUsbFileLabel.setText(new File(path).getName());
            updateUsbTransferButton();
        } else if (requestCode == REQ_USB_DEST) {
            mUsbDestPath = path;
            mUsbDestLabel.setText(path);
            updateUsbTransferButton();
        }
    }

    private void collectFilesFromDir(File dir, String relativePath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectFilesFromDir(f, relativePath + f.getName() + "/");
            } else {
                String fullPath = f.getAbsolutePath();
                if (!mSelectedFilePathsFtp.contains(fullPath)) {
                    mSelectedFilePathsFtp.add(fullPath);
                    mSelectedFileNamesFtp.add(relativePath + f.getName());
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        mExecutor.shutdown();
    }

    private void updateFtpFileLabel() {
        int count = mSelectedFilePathsFtp.size();
        if (count == 0) {
            mFtpFileLabel.setText(R.string.transfer_no_file_selected);
            mFtpFilesContainer.setVisibility(View.GONE);
            mFilesExpanded = false;
        } else if (count == 1) {
            mFtpFileLabel.setText(mSelectedFileNamesFtp.get(0));
        } else {
            String arrow = mFilesExpanded ? " ▲" : " ▼";
            mFtpFileLabel.setText(count + getString(R.string.transfer_files_selected_prefix) + arrow);
        }
    }

    private void toggleFileList() {
        if (mSelectedFilePathsFtp.size() <= 1) return;
        mFilesExpanded = !mFilesExpanded;
        int count = mSelectedFilePathsFtp.size();
        String arrow = mFilesExpanded ? " ▲" : " ▼";
        mFtpFileLabel.setText(count + getString(R.string.transfer_files_selected_prefix) + arrow);
        if (mFilesExpanded) expandView(mFtpFilesContainer);
        else collapseView(mFtpFilesContainer);
    }

    private void expandView(View v) {
        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int targetHeight = v.getMeasuredHeight();
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float t, Transformation trans) {
                v.getLayoutParams().height = t == 1 ? ViewGroup.LayoutParams.WRAP_CONTENT : Math.max(1, (int)(targetHeight * t));
                v.requestLayout();
            }
            @Override
            public boolean willChangeBounds() { return true; }
        };
        anim.setDuration(Math.max(150, (long)(targetHeight / v.getContext().getResources().getDisplayMetrics().density)));
        v.startAnimation(anim);
    }

    private void collapseView(View v) {
        final int initialHeight = v.getMeasuredHeight();
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float t, Transformation trans) {
                if (t == 1) v.setVisibility(View.GONE);
                else {
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * t);
                    v.requestLayout();
                }
            }
            @Override
            public boolean willChangeBounds() { return true; }
        };
        anim.setDuration((long)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(anim);
    }

    private void rebuildFileList() {
        mFtpFilesContainer.removeAllViews();
        for (int i = 0; i < mSelectedFilePathsFtp.size(); i++) {
            final int index = i;
            final String name = mSelectedFileNamesFtp.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 6, 12, 6);

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            tv.setText(name);
            tv.setTextColor(getResources().getColor(R.color.textColorTitle, null));
            tv.setTextSize(12f);
            row.addView(tv);

            Button removeBtn = new Button(this);
            removeBtn.setText("✕");
            removeBtn.setTextSize(12f);
            removeBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            removeBtn.setTextColor(getResources().getColor(R.color.colorAccent, null));
            removeBtn.setPadding(8, 0, 8, 0);
            removeBtn.setOnClickListener(v -> {
                mSelectedFilePathsFtp.remove(index);
                mSelectedFileNamesFtp.remove(index);
                updateFtpFileLabel();
                rebuildFileList();
            });
            row.addView(removeBtn);
            mFtpFilesContainer.addView(row);
        }
    }

    private boolean isVitaConnected() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return false;
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getVendorId() == VITA_VENDOR_ID) {
                int pid = device.getProductId();
                if (pid == VITA_PID_FAT || pid == VITA_PID_SLIM || pid == VITA_PID_TV)
                    return true;
            }
        }
        return false;
    }

    private void updateUsbConnectionStatus() {
        boolean connected = isVitaConnected();
        mUsbConnectionStatus.setText(connected ? getString(R.string.transfer_vita_connected) : getString(R.string.transfer_vita_not_connected));
        mUsbConnectionIcon.setImageResource(connected ? R.drawable.ic_usb_connected : R.drawable.ic_usb_disconnected);
        mUsbPickFileBtn.setEnabled(connected);
        mUsbPickDestBtn.setEnabled(connected);
        updateUsbTransferButton();
    }

    private void updateUsbTransferButton() {
        boolean ready = isVitaConnected() && mSelectedFilePathUsb != null && mUsbDestPath != null;
        mUsbTransferBtn.setEnabled(ready);
    }

    private void setFtpUiTransferring(boolean transferring) {
        mFtpTransferBtn.setEnabled(!transferring);
        mFtpProgress.setVisibility(transferring ? View.VISIBLE : View.GONE);
    }

    private void setUsbUiTransferring(boolean transferring) {
        mUsbTransferBtn.setEnabled(!transferring);
        mUsbProgress.setVisibility(transferring ? View.VISIBLE : View.GONE);
    }

    private void startFtpTransfer() {
        String ip = mFtpIpInput.getText().toString().trim();
        if (ip.isEmpty()) { mFtpStatus.setText(R.string.transfer_ftp_no_ip); return; }
        if (mSelectedFilePathsFtp.isEmpty()) { mFtpStatus.setText(R.string.transfer_no_file); return; }

        String remotePath = mFtpRemotePath.getText().toString().trim();
        if (remotePath.isEmpty()) remotePath = DEFAULT_REMOTE_PATH;
        if (!remotePath.endsWith("/")) remotePath += "/";

        final String finalRemotePath = remotePath;
        final List<String> filePaths = new ArrayList<>(mSelectedFilePathsFtp);
        final List<String> fileNames = new ArrayList<>(mSelectedFileNamesFtp);

        setFtpUiTransferring(true);
        mFtpStatus.setText(R.string.transfer_connecting);

        mExecutor.execute(() -> {
            FTPClient ftp = new FTPClient();
            try {
                ftp.connect(ip, FTP_PORT);
                ftp.login("", "");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();

                int total = filePaths.size();
                boolean allSuccess = true;

                for (int i = 0; i < filePaths.size(); i++) {
                    final int idx = i;
                    String filePath = filePaths.get(i);
                    String relativeName = fileNames.get(i);

                    mHandler.post(() -> mFtpStatus.setText(getString(R.string.transfer_uploading) + " " + (idx + 1) + "/" + total + " - " + relativeName));

                    String remoteDest = finalRemotePath + relativeName;
                    String dir = remoteDest.substring(0, remoteDest.lastIndexOf("/") + 1);
                    if (!dir.equals(finalRemotePath)) {
                        ftp.makeDirectory(dir);
                        ftp.changeWorkingDirectory("/");
                    }

                    InputStream is = new FileInputStream(filePath);
                    boolean success = ftp.storeFile(remoteDest, is);
                    is.close();
                    if (!success) allSuccess = false;
                }

                ftp.logout();
                ftp.disconnect();

                boolean finalSuccess = allSuccess;
                mHandler.post(() -> {
                    setFtpUiTransferring(false);
                    mFtpStatus.setText(finalSuccess ? getString(R.string.transfer_success) : getString(R.string.transfer_ftp_failed));
                });
            } catch (IOException e) {
                mHandler.post(() -> {
                    setFtpUiTransferring(false);
                    mFtpStatus.setText(getString(R.string.transfer_error, e.getMessage()));
                });
                try { if (ftp.isConnected()) ftp.disconnect(); } catch (IOException ignored) {}
            }
        });
    }

    private void startUsbTransfer() {
        if (mSelectedFilePathUsb == null || mUsbDestPath == null) return;

        setUsbUiTransferring(true);
        mUsbStatus.setText(R.string.transfer_uploading);

        String srcPath = mSelectedFilePathUsb;
        String destPath = mUsbDestPath;
        String fileName = new File(srcPath).getName();

        mExecutor.execute(() -> {
            try {
                File srcFile = new File(srcPath);
                File destFile = new File(destPath, fileName);

                InputStream is = new FileInputStream(srcFile);
                OutputStream os = new java.io.FileOutputStream(destFile);

                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);

                is.close();
                os.close();

                mHandler.post(() -> {
                    setUsbUiTransferring(false);
                    mUsbStatus.setText(R.string.transfer_success);
                });
            } catch (IOException e) {
                mHandler.post(() -> {
                    setUsbUiTransferring(false);
                    mUsbStatus.setText(getString(R.string.transfer_error, e.getMessage()));
                });
            }
        });
    }

}
