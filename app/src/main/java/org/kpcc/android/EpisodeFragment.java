package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Episode;
import org.kpcc.api.Program;

import java.util.concurrent.atomic.AtomicBoolean;


public class EpisodeFragment extends Fragment {
    private static final String ARG_PROGRAM_SLUG = "programSlug";
    private static final String ARG_EPISODE = "episode";

    public Episode episode;
    public Program program;
    public AtomicBoolean pagerVisible = new AtomicBoolean(false);
    private StreamManager.EpisodeStream mPlayer;
    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private AudioButtonManager mAudioButtonManager;
    private ViewGroup mView;
    // Unrecoverable error. Just show an error message if this is true.
    private boolean mDidError = false;


    public static EpisodeFragment newInstance(String programSlug, String episodeJson) {
        Bundle args = new Bundle();
        args.putString(ARG_PROGRAM_SLUG, programSlug);
        args.putString(ARG_EPISODE, episodeJson);

        EpisodeFragment fragment = new EpisodeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            program = ProgramsManager.instance.find(args.getString(ARG_PROGRAM_SLUG));

            try {
                episode = Episode.buildFromJson(args.getString(ARG_EPISODE));
            } catch (JSONException e) {
                mDidError = true;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mView = (ViewGroup) inflater.inflate(R.layout.fragment_episode, container, false);
        ScrollView episodePage = (ScrollView) mView.findViewById(R.id.episode_page);
        episodePage.setFillViewport(true);

        FrameLayout contentWrapper = (FrameLayout) mView.findViewById(R.id.content_wrapper);

        if (mDidError) {
            LinearLayout errorView = (LinearLayout) mView.findViewById(R.id.generic_load_error);
            errorView.setVisibility(View.VISIBLE);
            contentWrapper.setVisibility(View.GONE);
            return mView;
        }

        TextView program_title = (TextView) mView.findViewById(R.id.program_title);
        TextView episode_title = (TextView) mView.findViewById(R.id.episode_title);
        TextView date = (TextView) mView.findViewById(R.id.air_date);
        TextView totalTime = (TextView) mView.findViewById(R.id.audio_total_time);

        mSeekBar = (SeekBar) mView.findViewById(R.id.progress_bar);
        mCurrentTime = (TextView) mView.findViewById(R.id.audio_current_time);
        mSeekBar.setMax(episode.audio.durationSeconds * 1000);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                if (mPlayer != null) {
                    mPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        program_title.setText(program.title);
        episode_title.setText(episode.title);
        date.setText(episode.formattedAirDate);
        totalTime.setText(StreamManager.getTimeFormat(episode.audio.durationSeconds));

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDidError) {
            // The UI is handled in onCreateView; just don't play the audio right now.
            return;
        }

        mAudioButtonManager = new AudioButtonManager(mView);

        if (pagerVisible.get()) {
            playAudio();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pagerVisible.set(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_PROGRAM_SLUG, program.slug);

        try {
            outState.putString(ARG_EPISODE, episode.toJSON().toString());
        } catch (JSONException e) {
            // Arg will be empty, a JSONException should be raised, and we'll handle that elsewhere.
        }

        super.onSaveInstanceState(outState);
    }

    public void playAudio() {
        MainActivity activity = (MainActivity) getActivity();
        boolean alreadyPlaying = false;

        // No audio will play if:
        // 1. Stream isn't ready
        // 2. There is no audio (this shouldn't happen, but just in case...)
        // 3. The view isn't visible.
        // ViewPager calls the 'onCreateView' method for surrounding views, so we can't
        // play the audio automatically in this method.
        if (!activity.streamIsBound() || episode.audio == null) {
            mAudioButtonManager.toggleError(R.string.audio_error);
            return;
        }

        StreamManager.EpisodeStream currentPlayer = activity.streamManager.currentEpisodePlayer;
        if (currentPlayer != null && currentPlayer.audioUrl.equals(episode.audio.url)) {
            mPlayer = currentPlayer;

            if (currentPlayer.isPlaying()) {
                alreadyPlaying = true;
                mAudioButtonManager.togglePlayingForPause();
            }

            // Audio should be released when next one starts, based on Audio Focus rules.
        } else {
            mPlayer = new StreamManager.EpisodeStream(episode.audio.url, program.slug,
                    episode.audio.durationSeconds, activity);
        }

        mAudioButtonManager.getPlayButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.play();
            }
        });

        mAudioButtonManager.getPauseButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.pause();
            }
        });

        mPlayer.setOnAudioEventListener(new StreamManager.AudioEventListener() {
            @Override
            public void onLoading() {
                mAudioButtonManager.toggleLoading();
            }

            @Override
            public void onPlay() {
                mAudioButtonManager.togglePlayingForPause();
                logEpisodeStreamEvent(AnalyticsManager.EVENT_ON_DEMAND_BEGAN);
            }

            @Override
            public void onPause() {
                mAudioButtonManager.togglePaused();
                logEpisodeStreamEvent(AnalyticsManager.EVENT_ON_DEMAND_PAUSED);
            }

            @Override
            public void onStop() {
                mAudioButtonManager.toggleStopped();
            }

            @Override
            public void onCompletion() {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                EpisodesPagerFragment fragment = (EpisodesPagerFragment) fm.findFragmentByTag(EpisodesPagerFragment.STACK_TAG);

                if (fragment != null && fragment.pager != null) {
                    if (fragment.pager.canScrollHorizontally(1)) {
                        fragment.pager.setCurrentItem(fragment.pager.getCurrentItem() + 1);
                    } else {
                        // Go back to the episodes list.
                        fm.popBackStackImmediate();
                    }
                }

                logEpisodeStreamEvent(AnalyticsManager.EVENT_ON_DEMAND_COMPLETED);
            }

            @Override
            public void onProgress(int progress) {
                mSeekBar.setProgress(progress);
                mCurrentTime.setText(StreamManager.getTimeFormat(progress / 1000));
            }

            @Override
            public void onBufferingUpdate(int percent) {
                mSeekBar.setSecondaryProgress(mSeekBar.getMax() * (percent / 100));
            }

            @Override
            public void onError() {
                mAudioButtonManager.toggleError(R.string.audio_error);
            }
        });

        if (!alreadyPlaying) {
            mAudioButtonManager.toggleStopped();
            // Start the episode right away unless it's already playing.
            mAudioButtonManager.clickPlay();
        }
    }

    public boolean episodeWasSkipped() {
        if (streamNotAvailable()) {
            return false;
        }

        try {
            int current = getCurrentPlayerPositionSeconds();
            return current < (mPlayer.durationSeconds / 4);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public int getCurrentPlayerPositionSeconds() throws IllegalStateException {
        if (streamNotAvailable()) {
            return 0;
        }

        return (int) mPlayer.getCurrentPosition() / 1000;
    }

    private boolean streamNotAvailable() {
        MainActivity activity = (MainActivity) getActivity();
        return mPlayer == null || !activity.streamIsBound() || activity.streamManager.currentEpisodePlayer == null;
    }

    private void logEpisodeStreamEvent(String key) {
        JSONObject params = new JSONObject();

        try {
            params.put(AnalyticsManager.PARAM_PROGRAM_PUBLISHED_AT, episode.airDate);
            params.put(AnalyticsManager.PARAM_PROGRAM_TITLE, program.title);
            params.put(AnalyticsManager.PARAM_EPISODE_TITLE, episode.title);
            params.put(AnalyticsManager.PARAM_PROGRAM_LENGTH, episode.audio.durationSeconds);

            if (key.equals(AnalyticsManager.EVENT_ON_DEMAND_PAUSED)) {
                params.put(AnalyticsManager.PARAM_PLAYED_DURATION, mPlayer.getCurrentPosition() / 1000);
            }
        } catch (JSONException e) {
            // Nothing to do. No data will be sent with this event.
        }

        AnalyticsManager.instance.logEvent(key, params);
    }
}
