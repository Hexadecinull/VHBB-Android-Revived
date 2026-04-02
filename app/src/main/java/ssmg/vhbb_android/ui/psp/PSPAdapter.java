package ssmg.vhbb_android.ui.psp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

import ssmg.vhbb_android.Constants.PSP;
import ssmg.vhbb_android.R;
import ssmg.vhbb_android.Utils.DownloadUtils;

public class PSPAdapter extends RecyclerView.Adapter<PSPAdapter.ViewHolder> {

    private final Activity mActivity;
    private final ArrayList<PSPItem> mPSPList;
    private final ArrayList<PSPItem> mPSPListFull;

    public PSPAdapter(Activity activity, ArrayList<PSPItem> pspList) {
        this.mActivity = activity;
        this.mPSPList = pspList;
        this.mPSPListFull = new ArrayList<>(mPSPList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_psp, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PSPItem currentItem = mPSPList.get(position);

        String prefix = currentItem.getAI() > 0 ? "🛠 " : "";
        holder.mTitle.setText(prefix + currentItem.getName() + " " + currentItem.getVersion());
        holder.mAuthor.setText(currentItem.getAuthor());
        holder.mDescription.setText(currentItem.getDescription());
        holder.mDate.setText(String.format("(%s)", currentItem.getDateString()));
        holder.mDownloads.setText(String.format(Locale.getDefault(), "%dDLs", currentItem.getDownloads()));
        Picasso.get().load(currentItem.getIconUrl()).fit().centerInside().placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(holder.mIcon);

        holder.mDownload.setOnClickListener(v -> DownloadUtils.VHBBDownloadManager(mActivity, v.getContext(), Uri.parse(currentItem.getUrl()), currentItem.getName() + ".zip"));

        holder.mContainer.setOnClickListener(v -> {
            Intent detailsIntent = new Intent(mActivity, PSPDetails.class);
            detailsIntent.putExtra("ITEM", currentItem);
            mActivity.startActivity(detailsIntent);
        });
    }

    @Override
    public int getItemCount() {
        return mPSPList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitle, mAuthor, mDescription, mDate, mDownloads;
        public ImageButton mDownload;
        public ImageView mIcon;
        public LinearLayout mContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.textview_name);
            mAuthor = itemView.findViewById(R.id.textview_author);
            mDescription = itemView.findViewById(R.id.textview_desc);
            mDate = itemView.findViewById(R.id.textview_date);
            mDownloads = itemView.findViewById(R.id.textview_downloads);
            mDownload = itemView.findViewById(R.id.download);
            mIcon = itemView.findViewById(R.id.image);
            mContainer = itemView.findViewById(R.id.ll_main);
        }
    }

    public Filter getSearchFilter() {
        return mSearchFilter;
    }

    private final Filter mSearchFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<PSPItem> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(mPSPListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (PSPItem item : mPSPListFull)
                    if (item.getName().toLowerCase().contains(filterPattern))
                        filteredList.add(item);
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mPSPList.clear();
            //noinspection unchecked
            mPSPList.addAll((ArrayList<PSPItem>) results.values);
            notifyDataSetChanged();
        }
    };

    public Filter getTypeFilter() {
        return mTypeFilter;
    }

    private final Filter mTypeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<PSPItem> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0 || constraint.equals(String.valueOf(PSP.TYPE_ALL))) {
                filteredList.addAll(mPSPListFull);
            } else {
                for (PSPItem item : mPSPListFull)
                    if (String.valueOf(item.getType()).contentEquals(constraint))
                        filteredList.add(item);
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mPSPList.clear();
            //noinspection unchecked
            mPSPList.addAll((ArrayList<PSPItem>) results.values);
            notifyDataSetChanged();
        }
    };

}
