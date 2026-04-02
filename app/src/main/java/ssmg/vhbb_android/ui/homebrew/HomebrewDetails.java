package ssmg.vhbb_android.ui.homebrew;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import ssmg.vhbb_android.Constants.VitaDB;
import ssmg.vhbb_android.R;
import ssmg.vhbb_android.Utils.DownloadUtils;
import ssmg.vhbb_android.ui.FullscreenImageActivity;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class HomebrewDetails extends AppCompatActivity {

    HomebrewItem cItem;

    String[] ScreenshotsUrl;
    ImageView mScreenshot;

    Handler cycleHandler = new Handler();
    Runnable cycleRunnable = new Runnable() {
        @Override
        public void run () {
            cycleScreenshot();
            cycleHandler.postDelayed(this, 5000);
        }
    };
    int sc_index = 0;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_homebrew);

        cItem = (HomebrewItem)getIntent().getSerializableExtra("ITEM");
        assert cItem != null;

        String SourceUrl = cItem.getSourceUrl();
        String ReleaseUrl = cItem.getReleaseUrl();
        String DataUrl = cItem.getDataUrl();
        ScreenshotsUrl = cItem.getScreenshotsUrl();

        Button mSourceBtn = findViewById(R.id.button_source);
        Button mReleaseBtn = findViewById(R.id.button_release);
        ImageButton mDownload = findViewById(R.id.download);
        ImageButton mDownloadData = findViewById(R.id.downloadData);
        ImageButton mQrCode = findViewById(R.id.qrCode);
        ImageButton mTrophies = findViewById(R.id.trophies);
        mScreenshot = findViewById(R.id.screenshot);

        String prefix = "";
        if (cItem.getTrophies() > 0) prefix += "🏆 ";
        if (cItem.getAI() > 0) prefix += "🛠 ";
        ((TextView)findViewById(R.id.textview_title)).setText(prefix + cItem.getName() + " " + cItem.getVersion());
        ((TextView)findViewById(R.id.textview_date)).setText(cItem.getDateString());
        ((TextView)findViewById(R.id.textview_author)).setText(cItem.getAuthor());
        ((TextView)findViewById(R.id.textview_desc)).setText(cItem.getLongDescription());

        TextView titleIdView = findViewById(R.id.textview_titleid_value);
        View titleIdSection = findViewById(R.id.ll_titleid);
        String titleID = cItem.getTitleID();
        if (titleID != null && !titleID.isEmpty() && !titleID.equals("AAAAAAAAA")) {
            titleIdSection.setVisibility(View.VISIBLE);
            titleIdView.setText(titleID);
        } else {
            titleIdSection.setVisibility(View.GONE);
        }

        TextView aiView = findViewById(R.id.textview_ai_value);
        View aiSection = findViewById(R.id.ll_ai);
        aiSection.setVisibility(View.VISIBLE);
        aiView.setText(cItem.getAI() > 0 ? getString(R.string.details_ai_yes) : getString(R.string.details_ai_no));

        Picasso.get().load(cItem.getIconUrl()).fit().centerInside().transform(new CropCircleTransformation()).memoryPolicy(MemoryPolicy.NO_CACHE).into((ImageView)findViewById(R.id.image));

        int pagesVisibility = !(SourceUrl.equals("") && ReleaseUrl.equals("")) ? View.VISIBLE : View.GONE;
        findViewById(R.id.ll_pages).setVisibility(pagesVisibility);
        findViewById(R.id.line3).setVisibility(pagesVisibility);
        mSourceBtn.setVisibility(!SourceUrl.equals("") ? View.VISIBLE : View.GONE);
        mReleaseBtn.setVisibility(!ReleaseUrl.equals("") ? View.VISIBLE : View.GONE);

        mSourceBtn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SourceUrl))));
        mReleaseBtn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ReleaseUrl))));

        mDownload.setOnClickListener(v -> DownloadUtils.VHBBDownloadManager(this, this, Uri.parse(cItem.getUrl()), cItem.getName() + ".vpk"));

        mQrCode.setOnClickListener(v -> {
            if (!DataUrl.isEmpty()) {
                showDualQrDialog(cItem.getUrl(), DataUrl, cItem.getName());
            } else {
                showSingleQrDialog(cItem.getUrl(), cItem.getName());
            }
        });

        mDownloadData.setVisibility(!DataUrl.equals("") ? View.VISIBLE : View.GONE);
        if (!DataUrl.equals("")) mDownloadData.setOnClickListener(v -> DownloadUtils.VHBBDownloadManager(this, this, Uri.parse(DataUrl), DataUrl.substring(DataUrl.lastIndexOf("/") + 1)));

        if (cItem.getTrophies() > 0) {
            mTrophies.setVisibility(View.VISIBLE);
            mTrophies.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(VitaDB.ACHIEVEMENTS_URL + cItem.getTitleID()));
                startActivity(i);
            });
        } else {
            mTrophies.setVisibility(View.GONE);
        }

        if (ScreenshotsUrl != null)
            if (ScreenshotsUrl.length == 1) Picasso.get().load(ScreenshotsUrl[0]).fit().centerInside().memoryPolicy(MemoryPolicy.NO_CACHE).into(mScreenshot);
            else cycleHandler.postDelayed(cycleRunnable, 0);
        else mScreenshot.setVisibility(View.GONE);

        if (ScreenshotsUrl != null) {
            mScreenshot.setOnClickListener(v -> {
                cycleHandler.removeCallbacks(cycleRunnable);
                Intent fullscreenIntent = new Intent(this, FullscreenImageActivity.class);
                fullscreenIntent.putExtra("URLS", ScreenshotsUrl);
                fullscreenIntent.putExtra("INDEX", sc_index == 0 ? 0 : sc_index - 1);
                startActivity(fullscreenIntent);
            });
        }
    }

    private void showSingleQrDialog(String url, String name) {
        try {
            BarcodeEncoder enc = new BarcodeEncoder();
            Bitmap bmp = enc.encodeBitmap(url, BarcodeFormat.QR_CODE, 600, 600);
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setPadding(32, 32, 32, 32);
            new AlertDialog.Builder(this)
                    .setTitle(name)
                    .setView(iv)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showDualQrDialog(String vpkUrl, String dataUrl, String name) {
        try {
            BarcodeEncoder enc = new BarcodeEncoder();
            Bitmap vpkBmp = enc.encodeBitmap(vpkUrl, BarcodeFormat.QR_CODE, 500, 500);
            Bitmap dataBmp = enc.encodeBitmap(dataUrl, BarcodeFormat.QR_CODE, 500, 500);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(24, 24, 24, 24);

            TextView vpkLabel = new TextView(this);
            vpkLabel.setText(getString(R.string.qr_vpk_label));
            vpkLabel.setTextSize(14f);
            vpkLabel.setPadding(0, 0, 0, 8);
            layout.addView(vpkLabel);

            ImageView vpkIv = new ImageView(this);
            vpkIv.setImageBitmap(vpkBmp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            vpkIv.setLayoutParams(lp);
            layout.addView(vpkIv);

            TextView dataLabel = new TextView(this);
            dataLabel.setText(getString(R.string.qr_data_label));
            dataLabel.setTextSize(14f);
            dataLabel.setPadding(0, 16, 0, 8);
            layout.addView(dataLabel);

            ImageView dataIv = new ImageView(this);
            dataIv.setImageBitmap(dataBmp);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            dataIv.setLayoutParams(lp2);
            layout.addView(dataIv);

            new AlertDialog.Builder(this)
                    .setTitle(name)
                    .setView(layout)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cycleScreenshot () {
        if (sc_index >= ScreenshotsUrl.length) sc_index = 0;
        Picasso.get().load(ScreenshotsUrl[sc_index]).fit().centerInside().memoryPolicy(MemoryPolicy.NO_CACHE).into(mScreenshot);
        sc_index++;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ScreenshotsUrl != null && ScreenshotsUrl.length > 1)
            cycleHandler.postDelayed(cycleRunnable, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cycleHandler.removeCallbacks(cycleRunnable);
    }

}
