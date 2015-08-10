package org.kpcc.android;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import android.os.Handler;
import android.os.Looper;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. It can be prepared
 * with one of a number of {@link RendererBuilder} classes to suit different use cases (e.g. DASH,
 * SmoothStreaming and so on).
 */
public class AudioPlayer {

    /**
     * Builds renderers for the player.
     */
    public interface RendererBuilder {
        /**
         * Constructs the necessary components for playback.
         *
         * @param player The parent player.
         * @param callback The callback to invoke with the constructed components.
         */
        void buildRenderers(AudioPlayer player, RendererBuilderCallback callback);
    }

    /**
     * A callback invoked by a {@link RendererBuilder}.
     */
    public interface RendererBuilderCallback {
        /**
         * Invoked with the results from a {@link RendererBuilder}.
         * @param renderers Renderers indexed by {@link AudioPlayer} TYPE_* constants. An individual
         *     element may be null if there do not exist tracks of the corresponding type.
         * @param bandwidthMeter Provides an estimate of the currently available bandwidth. May be null.
         */
        void onRenderers(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter);
        /**
         * Invoked if a {@link RendererBuilder} encounters an error.
         *
         * @param e Describes the error.
         */
        void onRenderersError(Exception e);
    }

    public static final int RENDERER_COUNT = 1;
    public static final int TYPE_AUDIO = 0;

    private static final int RENDERER_BUILDING_STATE_IDLE = 1;
    private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
    private static final int RENDERER_BUILDING_STATE_BUILT = 3;

    private final RendererBuilder rendererBuilder;
    private final ExoPlayer player;
    private final Handler mainHandler;
    private final PlayerControl playerControl;

    private int rendererBuildingState;

    private InternalRendererBuilderCallback builderCallback;

    private TrackRenderer[] trackRenderers;

    public AudioPlayer(RendererBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        this.playerControl = new PlayerControl(player);
        mainHandler = new Handler();
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
    }

    public void prepare() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
            player.stop();
        }
        if (builderCallback != null) {
            builderCallback.cancel();
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
        builderCallback = new InternalRendererBuilderCallback();
        rendererBuilder.buildRenderers(this, builderCallback);
    }

    /* package */ void onRenderers(TrackRenderer[] renderers) {
        builderCallback = null;
        this.trackRenderers = renderers;

        player.prepare(renderers);
        rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public int getPlaybackState() {
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILDING) {
            return ExoPlayer.STATE_PREPARING;
        }
        int playerState = player.getPlaybackState();
        if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT
                && rendererBuildingState == RENDERER_BUILDING_STATE_IDLE) {
            // This is an edge case where the renderers are built, but are still being passed to the
            // player's playback thread.
            return ExoPlayer.STATE_PREPARING;
        }
        return playerState;
    }

    public void addListener(ExoPlayer.Listener listener) {
        player.addListener(listener);
    }

    public boolean isPlaying() {
        return playerControl.isPlaying();
    }

    public void setVolume(float volume) {
        try {
            trackRenderers[TYPE_AUDIO].handleMessage(MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
        } catch (ExoPlaybackException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        playerControl.start();
    }

    public int getCurrentPosition() {
        return playerControl.getCurrentPosition();
    }

    public void seekTo(long positionMs) {
        playerControl.seekTo((int)positionMs);
    }

    public void pause() {
        playerControl.pause();
    }

    public void stop() {
        player.stop();
    }

    public void release() {
        if (builderCallback != null) {
            builderCallback.cancel();
            builderCallback = null;
        }
        rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
        player.release();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    /* package */ Looper getPlaybackLooper() {
        return player.getPlaybackLooper();
    }

    /* package */ Handler getMainHandler() {
        return mainHandler;
    }

    private class InternalRendererBuilderCallback implements RendererBuilderCallback {

        private boolean canceled;

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRenderers(TrackRenderer[] renderers, BandwidthMeter bandwidthMeter) {
            if (!canceled) {
                AudioPlayer.this.onRenderers(renderers);
            }
        }

        @Override
        public void onRenderersError(Exception e) {
        }

    }

}
