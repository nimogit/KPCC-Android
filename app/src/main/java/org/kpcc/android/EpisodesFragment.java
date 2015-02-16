package org.kpcc.android;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EpisodesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EpisodesFragment extends Fragment {
    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment EpisodesFragment.
     */
    public static EpisodesFragment newInstance() {
        EpisodesFragment fragment = new EpisodesFragment();
        return fragment;
    }

    public EpisodesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(org.kpcc.android.R.layout.fragment_episodes, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(org.kpcc.android.R.string.episodes);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
