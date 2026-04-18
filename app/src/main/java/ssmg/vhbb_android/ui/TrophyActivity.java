package ssmg.vhbb_android.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ssmg.vhbb_android.Constants.VitaDB;
import ssmg.vhbb_android.R;

public class TrophyActivity extends AppCompatActivity {

    private static class TrophyEntry {
        final String name;
        final String description;
        final String grade;

        TrophyEntry(String name, String description, String grade) {
            this.name = name;
            this.description = description;
            this.grade = grade;
        }

        String gradeEmoji() {
            switch (grade.toUpperCase()) {
                case "PLATINUM": return "🏆";
                case "GOLD":     return "🥇";
                case "SILVER":   return "🥈";
                case "BRONZE":   return "🥉";
                default:         return "🏅";
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trophy);

        Toolbar toolbar = findViewById(R.id.toolbar_trophy);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String name = getIntent().getStringExtra("NAME");
            getSupportActionBar().setTitle(name != null ? name : getString(R.string.trophy_title));
        }

        String titleId = getIntent().getStringExtra("TITLE_ID");

        RecyclerView rv = findViewById(R.id.trophy_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        View progress = findViewById(R.id.trophy_progress);
        TextView error = findViewById(R.id.trophy_error);

        String url = VitaDB.TROPHIES_JSON_URL + (titleId != null ? titleId : "");

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<TrophyEntry> entries = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String tName  = obj.optString("name", "");
                            String tDesc  = obj.optString("detail", obj.optString("description", ""));
                            String tGrade = obj.optString("type", obj.optString("grade", "bronze"));
                            entries.add(new TrophyEntry(tName, tDesc, tGrade));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    progress.setVisibility(View.GONE);
                    if (entries.isEmpty()) {
                        error.setVisibility(View.VISIBLE);
                        error.setText(R.string.trophy_none);
                    } else {
                        rv.setVisibility(View.VISIBLE);
                        rv.setAdapter(new TrophyAdapter(entries));
                    }
                },
                err -> {
                    progress.setVisibility(View.GONE);
                    error.setVisibility(View.VISIBLE);
                    error.setText(R.string.trophy_error);
                });
        queue.add(req);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private static class TrophyAdapter extends RecyclerView.Adapter<TrophyAdapter.VH> {

        private final List<TrophyEntry> mEntries;

        TrophyAdapter(List<TrophyEntry> entries) { this.mEntries = entries; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trophy, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TrophyEntry e = mEntries.get(position);
            holder.mGrade.setText(e.gradeEmoji());
            holder.mName.setText(e.name);
            holder.mDesc.setText(e.description);
        }

        @Override
        public int getItemCount() { return mEntries.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView mGrade, mName, mDesc;
            VH(View v) {
                super(v);
                mGrade = v.findViewById(R.id.tv_trophy_grade);
                mName  = v.findViewById(R.id.tv_trophy_name);
                mDesc  = v.findViewById(R.id.tv_trophy_desc);
            }
        }
    }

}
