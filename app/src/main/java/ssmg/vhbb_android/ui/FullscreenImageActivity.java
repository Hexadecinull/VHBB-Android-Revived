package ssmg.vhbb_android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import ssmg.vhbb_android.R;

public class FullscreenImageActivity extends AppCompatActivity {

    private String[] mUrls;
    private int mIndex;
    private ImageView mImage;
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
        mCounter = findViewById(R.id.image_counter);
        mPrev = findViewById(R.id.btn_prev);
        mNext = findViewById(R.id.btn_next);

        mPrev.setOnClickListener(v -> {
            mIndex = (mIndex - 1 + mUrls.length) % mUrls.length;
            loadCurrent();
        });

        mNext.setOnClickListener(v -> {
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

    private void loadCurrent() {
        Picasso.get()
                .load(mUrls[mIndex])
                .fit()
                .centerInside()
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .into(mImage);
        mCounter.setText(String.format("%d / %d", mIndex + 1, mUrls.length));
    }

}
