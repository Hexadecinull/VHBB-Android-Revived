package ssmg.vhbb_android.ui.psp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ssmg.vhbb_android.Constants.PSP;
import ssmg.vhbb_android.R;

public class PSPFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private PSPAdapter mPSPAdapter;
    private ArrayList<PSPItem> mPSPList;
    private RequestQueue mQueue;
    private BottomNavigationView mBottomNav;

    @SuppressLint("NonConstantResourceId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_psp, container, false);
        setHasOptionsMenu(true);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mPSPList = new ArrayList<>();
        mQueue = Volley.newRequestQueue(requireContext());
        jsonParse();

        mBottomNav = rootView.findViewById(R.id.bottom_nav);
        mBottomNav.setOnItemSelectedListener(item -> {
            if (mPSPAdapter == null) return false;

            int id = item.getItemId();
            if (id == R.id.bnav_all) {
                mPSPAdapter.getTypeFilter().filter(String.valueOf(PSP.TYPE_ALL));
                return true;
            } else if (id == R.id.bnav_original_games) {
                mPSPAdapter.getTypeFilter().filter(String.valueOf(PSP.TYPE_ORIGINAL_GAMES));
                return true;
            } else if (id == R.id.bnav_game_ports) {
                mPSPAdapter.getTypeFilter().filter(String.valueOf(PSP.TYPE_GAME_PORTS));
                return true;
            } else if (id == R.id.bnav_utilities) {
                mPSPAdapter.getTypeFilter().filter(String.valueOf(PSP.TYPE_UTILISES));
                return true;
            } else if (id == R.id.bnav_emulators) {
                mPSPAdapter.getTypeFilter().filter(String.valueOf(PSP.TYPE_EMULATORS));
                return true;
            }
            return false;
        });

        return rootView;
    }

    private void jsonParse() {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, PSP.PSP_LIST_JSON_URL, null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject item = response.getJSONObject(i);

                            String name             = item.optString(PSP.JSON_NAME, "");
                            String iconUrl          = item.optString(PSP.JSON_ICON, "");
                            String version          = item.optString(PSP.JSON_VERSION, "");
                            String author           = item.optString(PSP.JSON_AUTHOR, "");
                            String description      = item.optString(PSP.JSON_DESCRIPTION, "");
                            String longDescription  = item.optString(PSP.JSON_LONG_DESCRIPTION, "");
                            String date             = item.optString(PSP.JSON_DATE, "");
                            String sourceUrl        = item.optString(PSP.JSON_SOURCE, "");
                            String releaseUrl       = item.optString(PSP.JSON_RELEASE_PAGE, "");
                            String url              = item.optString(PSP.JSON_URL, "");
                            String screenshotsUrl   = item.optString(PSP.JSON_SCREENSHOTS, "");
                            int type                = item.optInt(PSP.JSON_TYPE, 4);
                            int id                  = item.optInt(PSP.JSON_ID, 0);
                            int downloads           = item.optInt(PSP.JSON_DOWNLOADS, 0);
                            long size               = item.optLong(PSP.JSON_SIZE, 0);
                            int ai                  = item.optInt(PSP.JSON_AI, 0);

                            mPSPList.add(new PSPItem(name, iconUrl, version, author, description, longDescription, date, sourceUrl, releaseUrl, url, screenshotsUrl, type, id, downloads, size, ai));
                        }

                        mPSPAdapter = new PSPAdapter(requireActivity(), mPSPList);
                        mRecyclerView.setAdapter(mPSPAdapter);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, Throwable::printStackTrace);
        mQueue.add(request);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mPSPAdapter != null) {
                    mPSPAdapter.getSearchFilter().filter(newText);
                }
                mBottomNav.setSelectedItemId(R.id.bnav_all);
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

}
