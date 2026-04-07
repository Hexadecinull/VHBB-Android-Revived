package ssmg.vhbb_android.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ssmg.vhbb_android.Constants.VitaDB;
import ssmg.vhbb_android.R;

public class TrophyWebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trophy_web);

        Toolbar toolbar = findViewById(R.id.toolbar_trophy);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String name = getIntent().getStringExtra("NAME");
            getSupportActionBar().setTitle(name != null ? name : getString(R.string.trophy_title));
        }

        String titleId = getIntent().getStringExtra("TITLE_ID");
        String url = VitaDB.ACHIEVEMENTS_URL + (titleId != null ? titleId : "");

        WebView webView = findViewById(R.id.trophy_webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(url);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
