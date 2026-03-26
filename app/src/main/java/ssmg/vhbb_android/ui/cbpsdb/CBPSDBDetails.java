package ssmg.vhbb_android.ui.cbpsdb;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

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

        ((TextView) findViewById(R.id.textview_title)).setText(cItem.getName());
        ((TextView) findViewById(R.id.textview_author)).setText(cItem.getAuthor());
        ((TextView) findViewById(R.id.textview_type)).setText(cItem.getTypeString());
        ((TextView) findViewById(R.id.textview_options)).setText(cItem.getOptions());

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
