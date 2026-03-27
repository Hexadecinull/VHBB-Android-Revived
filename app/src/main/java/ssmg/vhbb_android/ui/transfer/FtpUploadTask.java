package ssmg.vhbb_android.ui.transfer;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ssmg.vhbb_android.Constants.Transfer;

public class FtpUploadTask {

    public interface Callback {
        void onProgress(int percent);
        void onSuccess();
        void onError(String message);
    }

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public void upload(String host, File file, Callback callback) {
        mExecutor.execute(() -> {
            FTPClient ftp = new FTPClient();
            try {
                ftp.connect(host, Transfer.FTP_PORT);
                ftp.login("", "");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();

                String remotePath = getRemotePath(file.getName());

                try {
                    ftp.makeDirectory(remotePath);
                } catch (IOException ignored) {
                }

                ftp.changeWorkingDirectory(remotePath);

                long totalBytes = file.length();
                try (FileInputStream fis = new FileInputStream(file)) {
                    ftp.setCopyStreamListener((totalBytesTransferred, bytesTransferred, streamSize) -> {
                        if (totalBytes > 0) {
                            int percent = (int) (totalBytesTransferred * 100 / totalBytes);
                            callback.onProgress(percent);
                        }
                    });

                    boolean success = ftp.storeFile(file.getName(), fis);
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Transfer failed: " + ftp.getReplyString());
                    }
                }

                ftp.logout();
            } catch (IOException e) {
                callback.onError(e.getMessage());
            } finally {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
    }

    private String getRemotePath(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".suprx") || lower.endsWith(".skprx")) {
            return Transfer.FTP_REMOTE_PLUGIN;
        }
        return Transfer.FTP_REMOTE_DEFAULT;
    }

    public void shutdown() {
        mExecutor.shutdownNow();
    }

}
