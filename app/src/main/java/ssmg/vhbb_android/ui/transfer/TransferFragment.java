package ssmg.vhbb_android.ui.transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ssmg.vhbb_android.Constants.Transfer;
import ssmg.vhbb_android.R;

public class TransferFragment extends Fragment {

    private static final String[] VITA_EXTENSIONS = {".vpk", ".suprx", ".skprx", ".zip"};

    private EditText mIpInput;
    private Spinner mFileSpinner;
    private Button mTransferBtn;
    private ProgressBar mProgress;
    private TextView mProgressLabel;

    private TextView mUsbStatus;
    private Button mUsbRefreshBtn;
    private LinearLayout mUsbTransferHint;

    private FtpUploadTask mFtpTask;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUsbStatus();
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_transfer, container, false);

        TabLayout tabLayout = rootView.findViewById(R.id.transfer_tabs);
        LinearLayout ftpTab = rootView.findViewById(R.id.tab_ftp);
        LinearLayout usbTab = rootView.findViewById(R.id.tab_usb);

        mIpInput = rootView.findViewById(R.id.ftp_ip_input);
        mFileSpinner = rootView.findViewById(R.id.ftp_file_spinner);
        mTransferBtn = rootView.findViewById(R.id.ftp_transfer_btn);
        mProgress = rootView.findViewById(R.id.ftp_progress);
        mProgressLabel = rootView.findViewById(R.id.ftp_progress_label);

        mUsbStatus = rootView.findViewById(R.id.usb_status);
        mUsbRefreshBtn = rootView.findViewById(R.id.usb_refresh_btn);
        mUsbTransferHint = rootView.findViewById(R.id.usb_transfer_hint);

        populateFileSpinner();
        setupFtpTransfer();
        updateUsbStatus();

        mUsbRefreshBtn.setOnClickListener(v -> updateUsbStatus());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    ftpTab.setVisibility(View.VISIBLE);
                    usbTab.setVisibility(View.GONE);
                } else {
                    ftpTab.setVisibility(View.GONE);
                    usbTab.setVisibility(View.VISIBLE);
                    updateUsbStatus();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        return rootView;
    }

    private void populateFileSpinner() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        List<String> fileNames = new ArrayList<>();
        List<File> files = new ArrayList<>();

        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            File[] all = downloadsDir.listFiles();
            if (all != null) {
                for (File f : all) {
                    if (f.isFile() && hasVitaExtension(f.getName())) {
                        fileNames.add(f.getName());
                        files.add(f);
                    }
                }
            }
        }

        if (fileNames.isEmpty()) {
            fileNames.add(getString(R.string.transfer_no_files));
            mTransferBtn.setEnabled(false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFileSpinner.setAdapter(adapter);

        mFileSpinner.setTag(files);
    }

    private boolean hasVitaExtension(String name) {
        String lower = name.toLowerCase();
        for (String ext : VITA_EXTENSIONS)
            if (lower.endsWith(ext)) return true;
        return false;
    }

    private void setupFtpTransfer() {
        mFtpTask = new FtpUploadTask();

        mTransferBtn.setOnClickListener(v -> {
            String ip = mIpInput.getText().toString().trim();
            if (TextUtils.isEmpty(ip)) {
                Toast.makeText(requireContext(), getString(R.string.transfer_enter_ip), Toast.LENGTH_SHORT).show();
                return;
            }

            int index = mFileSpinner.getSelectedItemPosition();
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) mFileSpinner.getTag();
            if (files == null || files.isEmpty() || index >= files.size()) {
                Toast.makeText(requireContext(), getString(R.string.transfer_no_files), Toast.LENGTH_SHORT).show();
                return;
            }

            File selectedFile = files.get(index);

            mTransferBtn.setEnabled(false);
            mProgress.setVisibility(View.VISIBLE);
            mProgressLabel.setVisibility(View.VISIBLE);
            mProgress.setProgress(0);
            mProgressLabel.setText("0%");

            mFtpTask.upload(ip, selectedFile, new FtpUploadTask.Callback() {
                @Override
                public void onProgress(int percent) {
                    mMainHandler.post(() -> {
                        mProgress.setProgress(percent);
                        mProgressLabel.setText(percent + "%");
                    });
                }

                @Override
                public void onSuccess() {
                    mMainHandler.post(() -> {
                        mProgress.setProgress(100);
                        mProgressLabel.setText("100%");
                        mTransferBtn.setEnabled(true);
                        Toast.makeText(requireContext(), getString(R.string.transfer_success), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    mMainHandler.post(() -> {
                        mProgress.setVisibility(View.GONE);
                        mProgressLabel.setVisibility(View.GONE);
                        mTransferBtn.setEnabled(true);
                        Toast.makeText(requireContext(), getString(R.string.transfer_error) + " " + message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void updateUsbStatus() {
        UsbManager usbManager = (UsbManager) requireContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        boolean vitaFound = false;
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == Transfer.VITA_VID) {
                int pid = device.getProductId();
                if (pid == Transfer.VITA_PID_FAT || pid == Transfer.VITA_PID_SLIM || pid == Transfer.VITA_PID_TV) {
                    vitaFound = true;
                    break;
                }
            }
        }

        if (vitaFound) {
            mUsbStatus.setText(getString(R.string.usb_vita_detected));
            mUsbStatus.setTextColor(requireContext().getResources().getColor(R.color.colorAccent, null));
            mUsbTransferHint.setVisibility(View.VISIBLE);
        } else {
            mUsbStatus.setText(getString(R.string.usb_vita_not_detected));
            mUsbStatus.setTextColor(requireContext().getResources().getColor(R.color.textColorDescription, null));
            mUsbTransferHint.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        requireContext().registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(mUsbReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFtpTask != null) mFtpTask.shutdown();
    }

}
