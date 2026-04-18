package ssmg.vhbb_android.ui.homebrew;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

import ssmg.vhbb_android.Constants.VitaDB;
import ssmg.vhbb_android.R;
import ssmg.vhbb_android.ui.TrophyActivity;
import ssmg.vhbb_android.Utils.DownloadUtils;

public class HomebrewAdapter extends RecyclerView.Adapter<HomebrewAdapter.ViewHolder> {

    private final Activity mActivity;
    private final ArrayList<HomebrewItem> mHomebrewList;
    private final ArrayList<HomebrewItem> mHomebrewListFull;

    public HomebrewAdapter(Activity activity, ArrayList<HomebrewItem> homebrewList) {
        this.mActivity = activity;
        this.mHomebrewList = homebrewList;
        this.mHomebrewListFull = new ArrayList<>(mHomebrewList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_homebrew, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomebrewItem currentItem = mHomebrewList.get(position);

        String prefix = "";
        if (currentItem.getTrophies() > 0) prefix += "🏆 ";
        if (currentItem.getAI() > 0) prefix += "🛠 ";

        if (!prefix.isEmpty()) {
            holder.mPrefix.setVisibility(View.VISIBLE);
            holder.mPrefix.setText(prefix.trim() + " ");
        } else {
            holder.mPrefix.setVisibility(View.GONE);
        }

        holder.mTitle.setText(currentItem.getName() + " " + currentItem.getVersion());
        holder.mAuthor.setText(currentItem.getAuthor());
        holder.mDescription.setText(currentItem.getDescription());
        holder.mDate.setText(String.format("(%s)", currentItem.getDateString()));
        holder.mDownloads.setText(String.format(Locale.getDefault(), "%dDLs", currentItem.getDownloads()));

        Picasso.get().load(currentItem.getIconUrl()).fit().centerInside().into(holder.mIcon);

        holder.mDownload.setOnClickListener(v -> DownloadUtils.VHBBDownloadManager(mActivity, v.getContext(), Uri.parse(currentItem.getUrl()), currentItem.getName() + ".vpk"));

        String dataUrlID = currentItem.getDataUrl();
        holder.mDownloadData.setVisibility(!dataUrlID.equals("") ? View.VISIBLE : View.GONE);

        if (currentItem.getTrophies() > 0) {
            holder.mTrophies.setVisibility(View.VISIBLE);
            holder.mTrophies.setOnClickListener(v -> {
                Intent i = new Intent(mActivity, TrophyActivity.class);
                i.putExtra("TITLE_ID", currentItem.getTitleID());
                i.putExtra("NAME", currentItem.getName());
                mActivity.startActivity(i);
            });
        } else {
            holder.mTrophies.setVisibility(View.GONE);
        }

        int descRightMargin;
        if (currentItem.getTrophies() > 0 && !dataUrlID.equals("")) {
            descRightMargin = (int) mActivity.getResources().getDimension(R.dimen.homebrew_desc_margin_tri);
        } else if (!dataUrlID.equals("") || currentItem.getTrophies() > 0) {
            descRightMargin = (int) mActivity.getResources().getDimension(R.dimen.homebrew_desc_margin_sec);
        } else {
            descRightMargin = (int) mActivity.getResources().getDimension(R.dimen.homebrew_desc_margin_def);
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.mDescription.getLayoutParams();
        lp.setMargins(0, (int) mActivity.getResources().getDimension(R.dimen.homebrew_padding_small), descRightMargin, 0);
        holder.mDescription.setLayoutParams(lp);

        if (!dataUrlID.equals("")) holder.mDownloadData.setOnClickListener(v -> DownloadUtils.VHBBDownloadManager(mActivity, v.getContext(), Uri.parse(dataUrlID), dataUrlID.substring(dataUrlID.lastIndexOf("/") + 1)));

        holder.mContainer.setOnClickListener(v -> {
            Intent detailsIntent = new Intent(mActivity, HomebrewDetails.class);
            detailsIntent.putExtra("ITEM", currentItem);
            mActivity.startActivity(detailsIntent);
        });
    }

    @Override
    public int getItemCount() {
        return mHomebrewList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mPrefix, mTitle, mAuthor, mDescription, mDate, mDownloads;
        public ImageButton mDownload, mDownloadData, mTrophies;
        public ImageView mIcon;
        public LinearLayout mContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            mPrefix = itemView.findViewById(R.id.textview_prefix);
            mTitle = itemView.findViewById(R.id.textview_name);
            mAuthor = itemView.findViewById(R.id.textview_author);
            mDescription = itemView.findViewById(R.id.textview_desc);
            mDate = itemView.findViewById(R.id.textview_date);
            mDownloads = itemView.findViewById(R.id.textview_downloads);
            mDownload = itemView.findViewById(R.id.download);
            mDownloadData = itemView.findViewById(R.id.downloadData);
            mTrophies = itemView.findViewById(R.id.trophies);
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
            ArrayList<HomebrewItem> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(mHomebrewListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (HomebrewItem item : mHomebrewListFull)
                    if (item.getName().toLowerCase().contains(filterPattern))
                        filteredList.add(item);
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mHomebrewList.clear();
            mHomebrewList.addAll((ArrayList<HomebrewItem>) results.values);
            notifyDataSetChanged();
        }
    };

    public Filter getTypeFilter() {
        return mTypeFilter;
    }

    private final Filter mTypeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<HomebrewItem> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0 || constraint.equals(String.valueOf(VitaDB.TYPE_ALL))) {
                filteredList.addAll(mHomebrewListFull);
            } else {
                for (HomebrewItem item : mHomebrewListFull)
                    if (String.valueOf(item.getType()).contentEquals(constraint))
                        filteredList.add(item);
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mHomebrewList.clear();
            mHomebrewList.addAll((ArrayList<HomebrewItem>) results.values);
            notifyDataSetChanged();
        }
    };

}
