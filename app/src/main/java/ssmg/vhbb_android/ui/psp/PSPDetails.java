package ssmg.vhbb_android.ui.psp;

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
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

import ssmg.vhbb_android.R;
import ssmg.vhbb_android.Utils.DownloadUtils;
import ssmg.vhbb_android.ui.FullscreenImageActivity;

public class PSPDetails extends AppCompatActivity {

    PSPItem cItem;

    String[] ScreenshotsUrl;
    ImageView mScreenshot;
    VideoView mTrailerView;

    Handler cycleHandler = new Handler();
    Runnable cycleRunnable = new Runnable() {
        @Override
        public void run() {
            cycleScreenshot();
            cycleHandler.postDelayed(this, 5000);
        }
    };
    int sc_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_psp);

        cItem = (PSPItem) getIntent().getSerializableExtra("ITEM");
        assert cItem != null;

        String SourceUrl = cItem.getSourceUrl();
        String ReleaseUrl = cItem.getReleaseUrl();
        String TrailerUrl = cItem.getTrailerUrl();
        ScreenshotsUrl = cItem.getScreenshotsUrl();

        Button mSourceBtn = findViewById(R.id.button_source);
        Button mReleaseBtn = findViewById(R.id.button_release);
        ImageButton mDownload = findViewById(R.id.download);
        ImageButton mQrCode = findViewById(R.id.qrCode);
        ImageButton mTrailerBtn = findViewById(R.id.trailer);
        mScreenshot = findViewById(R.id.screenshot);
        mTrailerView = findViewById(R.id.trailer_view);

        String prefix = cItem.getAI() > 0 ? "🛠 " : "";
        ((TextView) findViewById(R.id.textview_title)).setText(String.format("%s%s %s", prefix, cItem.getName(), cItem.getVersion()));
        ((TextView) findViewById(R.id.textview_date)).setText(cItem.getDateString());
        ((TextView) findViewById(R.id.textview_author)).setText(cItem.getAuthor());
        ((TextView) findViewById(R.id.textview_desc)).setText(cItem.getLongDescription());

        TextView typeBadge = findViewById(R.id.textview_type_badge);
        String typeStr = cItem.getTypeString();
        if (!typeStr.isEmpty()) {
            typeBadge.setVisibility(View.VISIBLE);
            typeBadge.setText(typeStr);
        } else {
            typeBadge.setVisibility(View.GONE);
        }

        TextView titleIdView = findViewById(R.id.textview_titleid_value);
        View titleIdSection = findViewById(R.id.ll_titleid);
        String titleID = cItem.getTitleID();
        if (titleID != null && !titleID.isEmpty()) {
            titleIdSection.setVisibility(View.VISIBLE);
            titleIdView.setText(titleID);
        } else {
            titleIdSection.setVisibility(View.GONE);
        }

        Picasso.get().load(cItem.getIconUrl()).fit().centerInside().transform(new CropCircleTransformation()).memoryPolicy(MemoryPolicy.NO_CACHE).into((ImageView) findViewById(R.id.image));

        TextView aiView = findViewById(R.id.textview_ai_value);
        View aiSection = findViewById(R.id.ll_ai);
        aiSection.setVisibility(View.VISIBLE);
        aiView.setText(cItem.getAI() > 0 ? getString(R.string.details_ai_yes) : getString(R.string.details_ai_no));

        int pagesVisibility = !(SourceUrl.equals("") && ReleaseUrl.equals("")) ? View.VISIBLE : View.GONE;
        findViewById(R.id.ll_pages).setVisibility(pagesVisibility);
        findViewById(R.id.line3).setVisibility(pagesVisibility);
        mSourceBtn.setVisibility(!SourceUrl.equals("") ? View.VISIBLE : View.GONE);
        mReleaseBtn.setVisibility(!ReleaseUrl.equals("") ? View.VISIBLE : View.GONE);

        mSourceBtn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SourceUrl))));
        mReleaseBtn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ReleaseUrl))));

        mDownload.setOnClickListener(v -> DownloadUtils.VHBBDownloadManager(this, this, Uri.parse(cItem.getUrl()), cItem.getName() + ".zip"));
        mQrCode.setOnClickListener(v -> showQrDialog(cItem.getUrl(), cItem.getName()));

        if (!TrailerUrl.isEmpty()) {
            mTrailerBtn.setVisibility(View.VISIBLE);
            mTrailerView.setVideoURI(Uri.parse(TrailerUrl));
            mTrailerView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(1f, 1f);
            });
            mTrailerBtn.setOnClickListener(v -> {
                if (mTrailerView.getVisibility() == View.VISIBLE && mTrailerView.isPlaying()) {
                    mTrailerView.pause();
                    mTrailerView.setVisibility(View.GONE);
                } else {
                    mTrailerView.setVisibility(View.VISIBLE);
                    mTrailerView.start();
                }
            });
        } else {
            mTrailerBtn.setVisibility(View.GONE);
            mTrailerView.setVisibility(View.GONE);
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

    private void showQrDialog(String url, String name) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(url, BarcodeFormat.QR_CODE, 600, 600);
            ImageView qrImageView = new ImageView(this);
            qrImageView.setImageBitmap(bitmap);
            qrImageView.setPadding(32, 32, 32, 32);
            new AlertDialog.Builder(this)
                    .setTitle(name)
                    .setView(qrImageView)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cycleScreenshot() {
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
        if (mTrailerView != null && mTrailerView.isPlaying()) mTrailerView.pause();
    }

}
