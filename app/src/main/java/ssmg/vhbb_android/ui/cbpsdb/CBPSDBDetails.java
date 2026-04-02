package ssmg.vhbb_android.ui.cbpsdb;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.picasso.PicassoImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

import java.util.Date;

import ssmg.vhbb_android.Constants.CBPSDB;
import ssmg.vhbb_android.Constants.VHBBAndroid;
import ssmg.vhbb_android.R;
import ssmg.vhbb_android.Utils.DownloadUtils;

public class CBPSDBDetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_cbpsdb);

        CBPSDBItem cItem = (CBPSDBItem) getIntent().getSerializableExtra("ITEM");
        assert cItem != null;

        String iconID = cItem.getIcon0();
        String urlID = cItem.getUrl();
        String dataUrlID = cItem.getDataUrl();
        String typeID = cItem.getType();
        String readmeUrl = cItem.getReadmeUrl();
        String sourceUrl = cItem.getSourceUrl();
        long timeAdded = cItem.getTimeAdded();

        ((TextView) findViewById(R.id.textview_title)).setText(cItem.getName());
        ((TextView) findViewById(R.id.textview_author)).setText(cItem.getAuthor());
        ((TextView) findViewById(R.id.textview_type)).setText(cItem.getTypeString());

        if (timeAdded > 0) {
            String dateStr = DateFormat.format("yyyy-MM-dd", new Date(timeAdded * 1000L)).toString();
            ((TextView) findViewById(R.id.textview_date)).setText(dateStr);
            findViewById(R.id.ll_date).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.ll_date).setVisibility(View.GONE);
        }

        View optionsSection = findViewById(R.id.ll_options);
        if (typeID.equals(CBPSDB.TYPE_PLUGIN)) {
            optionsSection.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textview_options)).setText(cItem.getOptions());
        } else {
            optionsSection.setVisibility(View.GONE);
        }

        if (iconID.equals("None")) {
            if (typeID.equals(CBPSDB.TYPE_PLUGIN)) {
                findViewById(R.id.image).setVisibility(View.GONE);
            } else {
                Picasso.get().load(VHBBAndroid.DEFAULT_ICON_URL).fit().centerInside()
                        .transform(new CropCircleTransformation())
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .into((ImageView) findViewById(R.id.image));
            }
        } else {
            Picasso.get().load(iconID).fit().centerInside()
                    .transform(new CropCircleTransformation())
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .into((ImageView) findViewById(R.id.image));
        }

        View sourceSection = findViewById(R.id.ll_source);
        TextView sourceBtn = findViewById(R.id.button_source);
        TextView closedSourceText = findViewById(R.id.textview_closed_source);
        sourceSection.setVisibility(View.VISIBLE);
        if (!sourceUrl.isEmpty()) {
            sourceBtn.setVisibility(View.VISIBLE);
            closedSourceText.setVisibility(View.GONE);
            sourceBtn.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))));
        } else {
            sourceBtn.setVisibility(View.GONE);
            closedSourceText.setVisibility(View.VISIBLE);
        }

        View readmeSection = findViewById(R.id.ll_readme);
        TextView readmeContent = findViewById(R.id.textview_readme);
        if (!readmeUrl.isEmpty()) {
            readmeSection.setVisibility(View.VISIBLE);
            readmeContent.setText(R.string.details_readme_loading);
            Markwon markwon = Markwon.builder(this)
                    .usePlugin(PicassoImagesPlugin.create(Picasso.get()))
                    .usePlugin(HtmlPlugin.create())
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TablePlugin.create(this))
                    .usePlugin(LinkifyPlugin.create())
                    .build();
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest req = new StringRequest(Request.Method.GET, readmeUrl,
                    response -> markwon.setMarkdown(readmeContent, response),
                    error -> readmeContent.setText(R.string.details_readme_error));
            queue.add(req);
        } else {
            readmeSection.setVisibility(View.GONE);
        }

        ImageButton mDownload = findViewById(R.id.download);
        ImageButton mDownloadData = findViewById(R.id.downloadData);

        mDownload.setOnClickListener(v -> {
            String filename = urlID.substring(urlID.lastIndexOf("/") + 1);
            String filenameExtension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (!(filenameExtension.equals(".vpk") || filenameExtension.equals(".zip") || filenameExtension.equals(".suprx") || filenameExtension.equals(".skprx"))) {
                if (typeID.equals(CBPSDB.TYPE_PLUGIN)) {
                    if (cItem.getOptions().equals(CBPSDB.OPTIONS_KERNEL))
                        filename = cItem.getID() + ".skprx";
                    else
                        filename = cItem.getID() + ".suprx";
                } else if (typeID.equals(CBPSDB.TYPE_VPK)) {
                    filename = cItem.getID() + ".vpk";
                } else {
                    filename = cItem.getID();
                }
            }
            DownloadUtils.VHBBDownloadManager(this, this, Uri.parse(urlID), filename);
        });

        mDownloadData.setVisibility(!dataUrlID.equals("None") ? View.VISIBLE : View.GONE);
        if (!dataUrlID.equals("None")) {
            mDownloadData.setOnClickListener(v -> {
                String filename = urlID.substring(urlID.lastIndexOf("/") + 1);
                filename = filename.substring(0, filename.lastIndexOf(".")) + "-data.zip";
                DownloadUtils.VHBBDownloadManager(this, this, Uri.parse(dataUrlID), filename);
            });
        }
    }

}
