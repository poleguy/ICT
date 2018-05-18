package com.phantompowerracing.ict;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.io.File;
import android.media.MediaExtractor;
import android.media.MediaCodec;
import android.media.AudioTrack;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.os.Handler;


import com.google.android.gms.common.api.GoogleApiClient;


import java.util.concurrent.TimeUnit;

import android.widget.TextView;

/**
 * Created by poleguy on 8/4/2016.
 */
public class AudioPlayer implements Runnable {
    protected MediaExtractor extractor;
    protected AudioTrack audioTrack;
    protected int inputBufIndex;
    protected Boolean doStop = false;
    private String mPath;
    private boolean isRepeating = false;
    private boolean isPaused = true;
    //MediaCodec codec = MediaCodec.createByCodecName(name);
    private MediaCodec decoder;
    //private float vol = 0.1f;
    MediaFormat mOutputFormat;
    boolean isEOS = false;
    boolean sawOutputEOS = true;
    long extractorSampleTime = 0;
    double m_relative_speed = 0.3;
    int mPlaybackRate;

    public AudioPlayer(String path) {
        mPath = path;
    }

    private void prepare() {

        //audioPlayerB("/sdcard","Carmen Ring.mp3");
        // https://github.com/hwrdprkns/AudioChoice/blob/master/app/src/main/java/com/hwrdprkns/audiochoice/BasicMediaExtractorMediaPlayer.java#L163
        // and
        // https://github.com/showlabor/AndroidPitchPlayer/blob/master/PitchPlayer/src/main/java/de/showlabor/example/pitchplayer/PitchPlayer.java
        if(audioTrack == null) {
            //int bufferSize =  AudioTrack.getMinBufferSize(format.getInteger(MediaFormat.KEY_SAMPLE_RATE),AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = 8* AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    //format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM);
            //audioTrack.setPlaybackPositionUpdateListener(this);
        }

        // Don't forget to start playing
        //audioTrack.play();
        isEOS = false;
        sawOutputEOS = false;
        extractorSampleTime = 0;

        //audioPlayerB("/sdcard","Carmen Ring.mp3");
        //audioPlayerB("/sdcard/Download","ICT_turkey.wav");
        //audioPlayerB("/data/local","tis.wav");

        //mSpeedLimit = sharedPreferences.getInt(PREFS_SPEED_LIMIT_KEY, SPEED_LIMIT_DEFAULT_MPH);

        mSpeed = 0;

    }

    public boolean isRepeating() {
        return isRepeating;
    }
    public boolean isPlaying() {
        boolean bPlaying = false;
        int state = audioTrack.getPlayState();
        if (state == audioTrack.PLAYSTATE_PLAYING) {
            bPlaying = true;
        }
        return bPlaying;
    }
    public void play() {
        Log.d("play", "Playing");

        isRepeating = true;
        isPaused = false;
    }

    public void pause() {
        // for now, will stop at the end of the song
        Log.d("pause", "pause");
        isPaused = true;
        //audioTrack.pause();
    }

    public void setVolume(double vol) {
        audioTrack.setVolume((float)vol);
    }

    HandlerThread callbackThread;
    CallbackHandler handler;
    public void start() {
        Log.d("AudioPlayer", "start");
        //isRepeating = true;
        doStop = false;
        callbackThread = new HandlerThread("CallbackThread");
        callbackThread.start();
        handler = new CallbackHandler(callbackThread.getLooper());
        //https://developer.android.com/reference/java/lang/Runnable.html
        new Thread(this).start();
    }

    public void stop() {
        doStop = true;
    }

    public void run() {
        // We use a single thread here for decoding stream data and
        // writing to the AudioTrack. Consider using a seperate thread for each task.
        //prepare();

        MediaCodec.Callback DecoderCallback = new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(MediaCodec mc, int inputBufferId) {
                //ByteBuffer inputBuffer = mc.getInputBuffer(inputBufferId);
                // fill inputBuffer with valid data
                //codec.queueInputBuffer(inputBufferId, …);

                if(!isEOS){
                    ByteBuffer buffer = mc.getInputBuffer(inputBufferId);
                    while(isPaused) {
                        //Log.d("callback","paused");
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {

                        Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        extractorSampleTime = extractor.getSampleTime();
                        //Log.d("DecodeActivity", "InputBuffer Available");
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, extractorSampleTime, 0);
                        extractor.advance();
                    }
                }
            }




            //@Override
            public void onOutputBufferAvailable(MediaCodec mc, int outputBufferId,MediaCodec.BufferInfo info) {
                //ByteBuffer outputBuffer = mc.getOutputBuffer(outputBufferId);
                //MediaFormat bufferFormat = mc.getOutputFormat(outputBufferId); // option A
                // bufferFormat is equivalent to mOutputFormat
                // outputBuffer is ready to be processed or rendered.
                //…
                //codec.releaseOutputBuffer(outputBufferId, …);

                if(!sawOutputEOS){
                    ByteBuffer buffer = mc.getOutputBuffer(outputBufferId);
                    // https://dpsm.wordpress.com/2012/07/28/android-mediacodec-decoded/
                    final byte[] chunk = new byte[info.size];
                    buffer.get(chunk); // Read the buffer all at once
                    buffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

                    if (chunk.length > 0) {
                        audioTrack.write(chunk, 0, chunk.length);

                        //audio_track_write(chunk);
                        //Log.d("DecodeActivity", "playing chunk " + chunk.length);
                        //Log.d("onOutput","thread: " + android.os.Process.myTid());
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                        // comment out to run forever:
                        //isRepeating = false;
                        Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    }

                }

                mc.releaseOutputBuffer(outputBufferId, false /* render */);
            }

            @Override
            public void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {
                // Subsequent data will conform to new format.
                // Can ignore if using getOutputFormat(outputBufferId)
                mOutputFormat = format; // option B
                mPlaybackRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                setPlaybackSpeed(m_relative_speed);


            }



            @Override
            public void onError(MediaCodec mc, MediaCodec.CodecException e) {
                //
            }
        };

        Log.d("run", "Playing");
        prepare();
        Log.d("play", "starting audio player");
        audioPlayerB("/sdcard/Download", "ICT_turkey.wav",DecoderCallback);
        Log.d("play","thread: " + android.os.Process.myTid());
        //audioPlayerB("/data/local","tis.wav");
        int i = 0;
        while(true) {

            //if (isRepeating) {
                //if (sawOutputEOS) {
                //if(audioTrack.getPlayState() == audioTrack.PLAYSTATE_STOPPED) {
                //    Log.d("play", "stopped");
                //}
                if(audioTrack.getPlayState() == audioTrack.PLAYSTATE_PAUSED) {
                    Log.d("play","playstate paused");
                    if(!isPaused) {
                        isPaused = false;
                        audioTrack.play();
                    }
                } else if(audioTrack.getPlayState() == audioTrack.PLAYSTATE_STOPPED) {
                    // Log.d("play","playstate stopped");

                    if(isRepeating) {
                        if (sawOutputEOS) {
                            //if(audioTrack.getPlayState() != audioTrack.PLAYSTATE_PAUSED) {
                            prepare();
                            String[] tracks = {"ICT_turkey.wav","ICT_entertainer.wav"};
                            i = (i+1)%tracks.length;
                            String track = tracks[i];
                            Log.d("play", String.format("starting audio player: %d, %s" ,i,track));
                            audioPlayerB("/sdcard/Download", track, DecoderCallback);
                            Log.d("play", "thread: " + android.os.Process.myTid());
                            //}
                        }
                        audioTrack.play();
                    }
                } else {
                    // Log.d("play","playstate running");
                    if(isPaused) {
                        audioTrack.pause();
                    } else if(isRepeating) {
                        if (isEOS && sawOutputEOS) {
                            // start a new track if the old one finished
                            //if(audioTrack.getPlayState() != audioTrack.PLAYSTATE_PAUSED) {
                            prepare();
                            String[] tracks = {"ICT_turkey.wav","ICT_entertainer.wav"};
                            i = (i+1)%tracks.length;
                            String track = tracks[i];
                            Log.d("play", String.format("starting audio player: %d, %s" ,i,track));
                            audioPlayerB("/sdcard/Download", track, DecoderCallback);
                            Log.d("play", "thread: " + android.os.Process.myTid());
                            //}
                        }
                        audioTrack.play();
                    }
                }
                    //if(audioTrack.getPlayState() == audioTrack.PLAYSTATE_STOPPED) {

                    //}
                    //audioPlayerB("/data/local","tis.wav");
                //}
            //} else {
                try {
                    Thread.sleep(100); // to not hog the processor
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            //}
        }

    }



    public void audioPlayerB(String path, String fileName,MediaCodec.Callback DecoderCallback){
        extractor = new MediaExtractor();

        try {
            extractor.setDataSource(path + File.separator + fileName);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                //if (weAreInterestedInThisTrack) {
                if(i == 0) {
                    extractor.selectTrack(i);

                    // Create the decoder on a different thread, in order to have the callbacks there.
                    // This makes sure that the blocking waiting and rendering in onOutputBufferAvailable
                    // won't block other callbacks (e.g. blocking encoder output callbacks), which
                    // would otherwise lead to the transcoding pipeline to lock up.

                    // Since API 23, we could just do setCallback(callback, mVideoDecoderHandler) instead
                    // of using a custom Handler and passing a message to create the MediaCodec there.

                    // When the callbacks are received on a different thread, the updating of the variables
                    // that are used for state logging (mVideoExtractedFrameCount, mVideoDecodedFrameCount,
                    // mVideoExtractorDone and mVideoDecoderDone) should ideally be synchronized properly
                    // against accesses from other threads, but that is left out for brevity since it's
                    // not essential to the actual transcoding.
                    handler.create(mime, DecoderCallback);
                    decoder = handler.getCodec();

                    //decoder = MediaCodec.createDecoderByType(mime);
                    //codec.setCallback(
                    decoder.configure(format,
                            null, // We don't have a surface in audio decoding
                            null, // No crypto
                            0); // 0 for decoding
                    decoder.start();
                    break;
                }
            }




            // Adding Callback
            //decoder.setCallback(DecoderCallback,handler);

//            ByteBuffer inputBuffer = ByteBuffer.allocate(1024);


//            while (extractor.readSampleData(inputBuffer,0)>=0){
//                int trackIndex = extractor.getSampleTrackIndex();
//                long presentationTimeUs = extractor.getSampleTime();
//
//                extractor.advance();
//            }
//
//            extractor.release();
        } catch (Exception e) {
            e.printStackTrace();
            extractor = null;
        }
        //extractor = null;
    }



//    public void setRelativePlaybackSpeed(float speed) {
//        mRelativePlaybackSpeed = speed;
//        if (mAudioTrack != null) {
//            mAudioTrack.setPlaybackRate((int) (mSrcRate * mRelativePlaybackSpeed));
//        }
//    }
//
//    public void setVolume(float vol) {
//        if (mAudioTrack != null) {
//            mAudioTrack.setStereoVolume(vol, vol);
//        }
//    }
//
//    public boolean isRepeating() {
//        return isRepeating;
//    }




    // source: https://github.com/googlesamples/android-SpeedTracker/blob/master/Wearable/src/main/java/com/example/android/wearable/speedtracker/WearableMainActivity.java
    private static final String TAG = "LocationActivity";
    private boolean mGpsPermissionApproved;
    private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long FASTEST_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);
    // Id to identify Location permission request.
    private static final int REQUEST_GPS_PERMISSION = 1;

    private static final float MPH_IN_METERS_PER_SECOND = 2.23694f;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    private GoogleApiClient mGoogleApiClient;
    private boolean mWaitingForGpsSignal;
    private float mSpeed;

    private String mGpsPermissionNeededMessage;
    private String mAcquiringGpsMessage;

    private TextView mCurrentSpeedMphTextView;
    private TextView mRelativePlaybackSpeedTextView;
    private View mBlinkingGpsStatusDotView;

    //private Handler mHandler = new Handler();




    public void setPlaybackSpeed(double speed) {
        // hit speed of
        // 2.0 at 20MPH
        // 1.0 at 6.7MPH
        // 0.5 at 0MPH
        //m_relative_speed =  0.3+1.5*mSpeed/20.0;
        m_relative_speed =  speed;
        if (audioTrack != null) {
            int rate = (int) (((double) mPlaybackRate) * (m_relative_speed));
            audioTrack.setPlaybackRate(rate);
            //Log.d("AudioPlayer", "new rate " + rate);
        }


    }

    // https://github.com/mstorsjo/android-decodeencodetest/blob/23a0621390404785e02a1ae7c24dfb67f9854129/src/com/example/decodeencodetest/ExtractDecodeEditEncodeMuxTest.java
    static class CallbackHandler extends Handler {
        CallbackHandler(Looper l) {
            super(l);
        }
        private MediaCodec mCodec;
        private boolean mEncoder;
        private MediaCodec.Callback mCallback;
        private String mMime;
        private boolean mSetDone;
        @Override
        public void handleMessage(Message msg) {
            try {
                mCodec = MediaCodec.createDecoderByType(mMime);
            } catch (IOException ioe) {
            }
            mCodec.setCallback(mCallback);
            synchronized (this) {
                mSetDone = true;
                notifyAll();
            }
        }
        void create(String mime, MediaCodec.Callback callback) {
            //mEncoder = encoder;
            mMime = mime;
            mCallback = callback;
            mSetDone = false;
            sendEmptyMessage(0);
            synchronized (this) {
                while (!mSetDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
        MediaCodec getCodec() {
            return mCodec;
        }
    }

    //private CallbackHandler mDecoderHandler;

}
