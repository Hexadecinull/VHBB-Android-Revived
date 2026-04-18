package ssmg.vhbb_android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
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
            switch (grade.toLowerCase()) {
                case "platinum": return "🏆";
                case "gold":     return "🥇";
                case "silver":   return "🥈";
                case "bronze":   return "🥉";
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
        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    List<TrophyEntry> entries = new ArrayList<>();
                    try {
                        JSONArray arr = null;
                        response = response.trim();
                        if (response.startsWith("[")) {
                            arr = new JSONArray(response);
                        } else if (response.startsWith("{")) {
                            JSONObject obj = new JSONObject(response);
                            String[] arrayKeys = {"trophies", "achievements", "data", "list"};
                            for (String key : arrayKeys) {
                                if (obj.has(key)) { arr = obj.getJSONArray(key); break; }
                            }
                        }
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                String tName  = obj.optString("name",        obj.optString("title", ""));
                                String tDesc  = obj.optString("detail",      obj.optString("description", obj.optString("desc", "")));
                                String tGrade = obj.optString("type",        obj.optString("grade", obj.optString("trophy_type", "bronze")));
                                if (!tName.isEmpty()) entries.add(new TrophyEntry(tName, tDesc, tGrade));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    progress.setVisibility(View.GONE);
                    if (entries.isEmpty()) {
                        error.setVisibility(View.VISIBLE);
                        error.setText(getString(R.string.trophy_none) + "\n\n[URL: " + url + "]");
                    } else {
                        rv.setVisibility(View.VISIBLE);
                        rv.setAdapter(new TrophyAdapter(entries));
                    }
                },
                err -> {
                    progress.setVisibility(View.GONE);
                    error.setVisibility(View.VISIBLE);
                    String msg = err.getMessage() != null ? err.getMessage() : "network error";
                    error.setText(getString(R.string.trophy_error) + "\n\n" + msg);
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

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trophy, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            TrophyEntry e = mEntries.get(pos);
            h.mGrade.setText(e.gradeEmoji());
            h.mName.setText(e.name);
            h.mDesc.setText(e.description);
        }

        @Override public int getItemCount() { return mEntries.size(); }

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
