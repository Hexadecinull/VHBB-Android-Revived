package ssmg.vhbb_android.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import ssmg.vhbb_android.R;

public class FullscreenImageActivity extends AppCompatActivity {

    private String[] mUrls;
    private int mIndex;
    private ImageView mImage;
    private VideoView mVideo;
    private TextView mCounter;
    private ImageButton mPrev;
    private ImageButton mNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        mUrls = getIntent().getStringArrayExtra("URLS");
        mIndex = getIntent().getIntExtra("INDEX", 0);

        mImage = findViewById(R.id.fullscreen_image);
        mVideo = findViewById(R.id.fullscreen_video);
        mCounter = findViewById(R.id.image_counter);
        mPrev = findViewById(R.id.btn_prev);
        mNext = findViewById(R.id.btn_next);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(mVideo);
        mVideo.setMediaController(mediaController);

        mPrev.setOnClickListener(v -> {
            stopVideo();
            mIndex = (mIndex - 1 + mUrls.length) % mUrls.length;
            loadCurrent();
        });

        mNext.setOnClickListener(v -> {
            stopVideo();
            mIndex = (mIndex + 1) % mUrls.length;
            loadCurrent();
        });

        if (mUrls.length <= 1) {
            mPrev.setVisibility(View.GONE);
            mNext.setVisibility(View.GONE);
            mCounter.setVisibility(View.GONE);
        }

        loadCurrent();
    }

    private boolean isVideo(String url) {
        return url != null && url.toLowerCase().endsWith(".mp4");
    }

    private void stopVideo() {
        if (mVideo.isPlaying()) mVideo.stopPlayback();
    }

    private void loadCurrent() {
        String url = mUrls[mIndex];
        mCounter.setText(String.format("%d / %d", mIndex + 1, mUrls.length));

        if (isVideo(url)) {
            mImage.setVisibility(View.GONE);
            mVideo.setVisibility(View.VISIBLE);
            mVideo.setVideoURI(Uri.parse(url));
            mVideo.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mVideo.start();
            });
        } else {
            mVideo.setVisibility(View.GONE);
            mImage.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(url)
                    .fit()
                    .centerInside()
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .into(mImage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideo();
    }

}
