package ssmg.vhbb_android.ui.transfer;

import android.app.Activity;
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
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ssmg.vhbb_android.R;

public class TransferActivity extends AppCompatActivity {

    private static final int VITA_VENDOR_ID  = 0x054C;
    private static final int VITA_PRODUCT_ID = 0x04E4;
    private static final int FTP_PORT        = 1337;
    private static final String DEFAULT_REMOTE_PATH = "/ux0:data/";

    private List<Uri> mSelectedFileUrisFtp = new ArrayList<>();
    private Uri mSelectedFileUriUsb;
    private Uri mUsbDestDirUri;
    private String mSelectedFileNameFtp = "";
    private String mSelectedFileNameUsb = "";

    private TextView mFtpFileLabel;
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

    private final ActivityResultLauncher<String> mFtpFilePicker =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    mSelectedFileUrisFtp = new ArrayList<>(uris);
                    if (uris.size() == 1) {
                        mSelectedFileNameFtp = resolveFileName(uris.get(0));
                        mFtpFileLabel.setText(mSelectedFileNameFtp);
                    } else {
                        mFtpFileLabel.setText(uris.size() + " files selected");
                    }
                }
            });

    private final ActivityResultLauncher<String> mUsbFilePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    mSelectedFileUriUsb = uri;
                    mSelectedFileNameUsb = resolveFileName(uri);
                    mUsbFileLabel.setText(mSelectedFileNameUsb);
                    updateUsbTransferButton();
                }
            });

    private final ActivityResultLauncher<Uri> mUsbDirPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    mUsbDestDirUri = uri;
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mUsbDestLabel.setText(uri.getLastPathSegment());
                    updateUsbTransferButton();
                }
            });

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())
                    || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                updateUsbConnectionStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        Toolbar toolbar = findViewById(R.id.toolbar_transfer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFtpIpInput       = findViewById(R.id.ftp_ip_input);
        mFtpRemotePath    = findViewById(R.id.ftp_remote_path);
        mFtpFileLabel     = findViewById(R.id.ftp_file_label);
        mFtpStatus        = findViewById(R.id.ftp_status);
        mFtpProgress      = findViewById(R.id.ftp_progress);
        mFtpTransferBtn   = findViewById(R.id.ftp_transfer_btn);

        mUsbFileLabel         = findViewById(R.id.usb_file_label);
        mUsbDestLabel         = findViewById(R.id.usb_dest_label);
        mUsbStatus            = findViewById(R.id.usb_status);
        mUsbProgress          = findViewById(R.id.usb_progress);
        mUsbTransferBtn       = findViewById(R.id.usb_transfer_btn);
        mUsbPickDestBtn       = findViewById(R.id.usb_pick_dest_btn);
        mUsbPickFileBtn       = findViewById(R.id.usb_pick_file_btn);
        mUsbConnectionStatus  = findViewById(R.id.usb_connection_status);
        mUsbConnectionIcon    = findViewById(R.id.usb_connection_icon);

        mFtpRemotePath.setText(DEFAULT_REMOTE_PATH);

        findViewById(R.id.ftp_pick_file_btn).setOnClickListener(v -> mFtpFilePicker.launch("*/*"));
        mUsbPickFileBtn.setOnClickListener(v -> mUsbFilePicker.launch("*/*"));
        mUsbPickDestBtn.setOnClickListener(v -> mUsbDirPicker.launch(null));
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

    private boolean isVitaConnected() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) return false;
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getVendorId() == VITA_VENDOR_ID && device.getProductId() == VITA_PRODUCT_ID)
                return true;
        }
        return false;
    }

    private void updateUsbConnectionStatus() {
        boolean connected = isVitaConnected();
        String statusText = connected
                ? getString(R.string.transfer_vita_connected)
                : getString(R.string.transfer_vita_not_connected);
        mUsbConnectionStatus.setText(statusText);
        mUsbConnectionIcon.setImageResource(connected
                ? R.drawable.ic_usb_connected
                : R.drawable.ic_usb_disconnected);
        mUsbPickFileBtn.setEnabled(connected);
        mUsbPickDestBtn.setEnabled(connected);
        updateUsbTransferButton();
    }

    private void updateUsbTransferButton() {
        boolean ready = isVitaConnected()
                && mSelectedFileUriUsb != null
                && mUsbDestDirUri != null;
        mUsbTransferBtn.setEnabled(ready);
    }

    private String resolveFileName(Uri uri) {
        String name = "";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0) name = cursor.getString(idx);
            cursor.close();
        }
        if (name.isEmpty()) {
            name = uri.getLastPathSegment();
            if (name == null) name = "unknown";
        }
        return name;
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
        if (ip.isEmpty()) {
            mFtpStatus.setText(R.string.transfer_ftp_no_ip);
            return;
        }
        if (mSelectedFileUrisFtp.isEmpty()) {
            mFtpStatus.setText(R.string.transfer_no_file);
            return;
        }

        String remotePath = mFtpRemotePath.getText().toString().trim();
        if (remotePath.isEmpty()) remotePath = DEFAULT_REMOTE_PATH;
        if (!remotePath.endsWith("/")) remotePath += "/";

        final String finalRemotePath = remotePath;
        final List<Uri> fileUris = new ArrayList<>(mSelectedFileUrisFtp);

        setFtpUiTransferring(true);
        mFtpStatus.setText(R.string.transfer_connecting);

        mExecutor.execute(() -> {
            FTPClient ftp = new FTPClient();
            try {
                ftp.connect(ip, FTP_PORT);
                ftp.login("", "");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();

                mHandler.post(() -> mFtpStatus.setText(R.string.transfer_uploading));

                int total = fileUris.size();
                int done = 0;
                boolean allSuccess = true;

                for (Uri fileUri : fileUris) {
                    String fileName = resolveFileName(fileUri);
                    InputStream is = getContentResolver().openInputStream(fileUri);
                    boolean success = ftp.storeFile(finalRemotePath + fileName, is);
                    if (is != null) is.close();
                    if (!success) allSuccess = false;
                    done++;
                    final int doneF = done;
                    mHandler.post(() -> mFtpStatus.setText(getString(R.string.transfer_uploading) + " " + doneF + "/" + total));
                }

                ftp.logout();
                ftp.disconnect();

                boolean finalSuccess = allSuccess;
                mHandler.post(() -> {
                    setFtpUiTransferring(false);
                    mFtpStatus.setText(finalSuccess
                            ? getString(R.string.transfer_success)
                            : getString(R.string.transfer_ftp_failed));
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
        if (mSelectedFileUriUsb == null || mUsbDestDirUri == null) return;

        setUsbUiTransferring(true);
        mUsbStatus.setText(R.string.transfer_uploading);

        Uri fileUri = mSelectedFileUriUsb;
        Uri destDirUri = mUsbDestDirUri;
        String fileName = mSelectedFileNameUsb;

        mExecutor.execute(() -> {
            try {
                DocumentFile destDir = DocumentFile.fromTreeUri(this, destDirUri);
                if (destDir == null || !destDir.canWrite()) {
                    mHandler.post(() -> {
                        setUsbUiTransferring(false);
                        mUsbStatus.setText(R.string.transfer_usb_no_write);
                    });
                    return;
                }

                DocumentFile existing = destDir.findFile(fileName);
                if (existing != null) existing.delete();

                DocumentFile newFile = destDir.createFile("application/octet-stream", fileName);
                if (newFile == null) throw new IOException("Failed to create file on device");

                InputStream is = getContentResolver().openInputStream(fileUri);
                OutputStream os = getContentResolver().openOutputStream(newFile.getUri());

                if (is == null || os == null) throw new IOException("Stream error");

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
