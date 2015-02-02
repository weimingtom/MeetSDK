/*
 * Copyright (C) 2012 Roger Shen  rogershen@pptv.com
 *
 */


#ifndef FF_PLAYER_H_
#define FF_PLAYER_H_

#include "player.h"
#include "loop.h"
#include "audioplayer.h"
#include "ffstream.h"
//#include "../subtitle/subtitle.h"

class ISubtitles;
class FFRender;
class FFSourceBase;

struct AVFormatContext;
struct AVStream;
struct AVPacket;
struct AVFrame;
struct SwsContext;

struct AVFilterGraph;
struct AVFilterInOut;
struct AVFilterContext;


#if USE_AV_FILTER
#define MAX_FILTER_CNT	4
#endif

class FFPlayer : public IPlayer, MediaPlayerListener
{
public:
    FFPlayer();
    ~FFPlayer();

    status_t setDataSource(const char* url);
	status_t setDataSource(int32_t fd, int64_t offset, int64_t length);
	status_t selectAudioChannel(int32_t index);
	status_t setVideoSurface(void* surface);
	status_t prepare();
	status_t prepareAsync();
    status_t start();
    status_t stop();
    status_t pause();
    status_t reset();
    status_t seekTo(int32_t msec);
    status_t getVideoWidth(int32_t* w);
    status_t getVideoHeight(int32_t* h);
    status_t getCurrentPosition(int32_t* msec);
    status_t getDuration(int32_t* msec);
	status_t getProcessBytes(int64_t *len);
    status_t setAudioStreamType(int32_t type);
	status_t setLooping(int32_t loop);
	status_t setVolume(float leftVolume, float rightVolume);
    status_t setListener(MediaPlayerListener* listener);
	int32_t flags();
	bool isLooping();
    bool isPlaying();
#ifdef __ANDROID__ 
	status_t startCompatibilityTest();

	void stopCompatibilityTest(){}
#endif

	status_t getBufferingTime(int *msec);

	status_t suspend();

    status_t resume();

	//status_t setPlayRate(double rate);
	
    void notify(int32_t msg, int32_t ext1 = 0, int32_t ext2 = 0);

	bool getMediaInfo(const char* url, MediaInfo* info);

	bool getMediaDetailInfo(const char* url, MediaInfo* info);

	bool getThumbnail(const char* url, MediaInfo* info);

	bool getThumbnail2(const char* url, MediaInfo* info);

	SnapShot* getSnapShot(int width, int height, int fmt, int msec = -1);

	status_t setISubtitle(ISubtitles* subtitle);

	static void onPrepare(void *opaque);
    static void onStreamDone(void *opaque);
    static void onVideo(void *opaque);
    static void onBufferingUpdate(void *opaque);
    static void onCheckAudioStatus(void *opaque);
    static void onPrepareAsync(void *opaque);
    static void onSeeking(void *opaque);
    static void onBufferingStart(void *opaque);
    static void onBufferingEnd(void *opaque);
    static void onSeekingComplete(void *opaque);
	static void onIOBitrateInfo(void *opaque);
	static void onMediaBitrateInfo(void *opaque);

	void onPrepareImpl();
    void onStreamDoneImpl();
    void onVideoImpl();
    void onBufferingUpdateImpl();
    void onCheckAudioStatusImpl();
    void onPrepareAsyncImpl();
    void onSeekingImpl();
    void onBufferingStartImpl();
    void onBufferingEndImpl();
    void onSeekingCompleteImpl();
	void onIOBitrateInfoImpl();
	void onMediaBitrateInfoImpl();

private:
    enum Flags {
		CAN_SEEK_BACKWARD = 1,
		CAN_SEEK_FORWARD = 2,
		CAN_PAUSE = 4,
    };

	enum PlayerEvent {
		PREPRARE_EVENT,
		VIDEO_RENDER_EVENT,
		STREAM_READ_EVENT,
		STREAM_DONE_EVENT,
		BUFFERING_UPDATE_EVENT,
		SEEKING_EVENT,
		CHECK_AUDIO_STATUS_EVENT,
		BUFFERING_START_EVENT,
		BUFFERING_END_EVENT,
		SEEKING_COMPLETE_EVENT,
		IO_BITRATE_INFO_EVENT,
		MEDIA_BITRATE_INFO_EVENT,
	};
	
	status_t prepare_l();
	status_t prepareAsync_l();
    status_t play_l();
	status_t stop_l();
    status_t reset_l();
    status_t seekTo_l();
    status_t pause_l();
    void notifyListener_l(int msg, int ext1 = 0, int ext2 = 0);
    void abortPrepare_l(status_t err);
    void cancelPlayerEvents_l();
	status_t prepareAudio_l();
	status_t prepareVideo_l();
	status_t decode_l(AVPacket* packet);
	status_t prepareSubtitle_l();
	
	bool render_frame(); // stop sent onVideo event when false
	void render_impl(); // decide render origin frame or filter frame
	bool need_drop_frame(int32_t frame_delay); // true means drop it.
	bool broadcast_refresh(); // if not broadcast, return false. if broadcast, flush a/v packet and return true 
	bool need_drop_pkt(AVPacket* packet); // pare h264 pakcet, do decide if this packet(frame) can be drop
	void optimizeDecode_l(AVPacket* packet); // set codec context->skip_loop_filter and skip_frame
	int32_t calc_frame_delay();
	void notifyVideoDelay(int64_t video_clock, int64_t audio_clock, int32_t frame_delay);

	int64_t getFramePTS_l(AVFrame* frame);

	// for auto video rotation
	bool FixRotateVideo(AVStream *video_st);
	void SwapResolution(int32_t *width, int32_t *height);
#ifdef USE_AV_FILTER
	bool init_filters(const char **filters_descr);
	bool insert_filter(const char *name, const char* arg, AVFilterContext **last_filter);
#endif

    void postPrepareEvent_l();
    void postStreamDoneEvent_l();
    void postVideoEvent_l(int64_t delayMs); //default is post event to header
    void postBufferingUpdateEvent_l();
    void postSeekingEvent_l(int64_t delayMs = -1); //default is post event to header
    void postCheckAudioStatusEvent_l();
    void postBufferingStartEvent_l();
    void postBufferingEndEvent_l();
    void postSeekingCompleteEvent_l();
	void postIOBitrateInfoEvent_l();
	void postMediaBitrateInfoEvent_l();

    FFPlayer(const FFPlayer &);
    FFPlayer &operator=(const FFPlayer &);

private:
    char*				mUri;
	FFSourceBase*		mSource;

	AVFormatContext*	mMediaFile;
	int32_t				mAudioStreamIndex;
	int32_t				mVideoStreamIndex;
	int32_t				mSubtitleStreamIndex;
	AVStream*			mAudioStream;
    AVStream*			mVideoStream;
	AVStream*			mSubtitleStream;

	// snapshot
	uint8_t*			mSnapshotPic;
	AVFrame*			mSnapShotFrame;
	SwsContext*			mSwsCtx;

    int64_t		mDurationMs;
    int32_t		mVideoWidth, mVideoHeight;
    int32_t		mVideoFormat;
	uint32_t	mVideoFrameRate;
	uint32_t	mVideoGapMs; // for OnVideo() shedule time(40 msec typically)
	int			mIOBitrate;
	int			mVideoBitrate;

	// state and capabilities
	uint32_t	mPlayerStatus; // MEDIA_PLAYER_STOPPED ...
    uint32_t	mPlayerFlags; // CAN_SEEK_BACKWARD | CAN_SEEK_FORWARD | CAN_PAUSE
	bool		mReachEndStream;

	bool		mLooping;
	bool		mSeeking;
    bool		mRenderFirstFrame; // "start" to play or after "seek" done will set to true
    bool		mNeedSyncFrame;
	bool		mBroadcastRefreshed;
	bool		mIsBuffering; // true when buffering, wait for cache is enough for decode

	int64_t		mAveVideoDecodeTimeMs;
	int64_t		mCompensateMs;
	int32_t		mFrameDelay;
	int32_t		mDiscardLevel;
	uint32_t	mDiscardCount;
	int64_t		mVideoPlayingTimeMs;
	int64_t		mSeekTimeMs;
	int64_t		mVideoTimeMs; // for seek position
	int			mSeekIncr;

	FFStream*				mDataStream; // demuxer setListener()
    MediaPlayerListener*	mListener;
    AudioPlayer*			mAudioPlayer; // audio render setListener()
    FFRender*				mVideoRenderer; // video render
	void*					mSurface; // native surface
	AVFrame*				mVideoFrame;
	// set to true(when "pause", "render first frame"), when decoder got picture, set it to false
	bool					mIsVideoFrameDirty;

	// event
	Loop::Event			mPrepareEvent;
	bool				mPrepareEventPending;
    Loop::Event			mVideoEvent;
    bool				mVideoEventPending; // pending is set to true when event sent. reset to false when onXXX event callback envoked
    //Loop::Event		mStreamReadEvent;
	//bool				mStreamReadEventPending;
    Loop::Event			mStreamDoneEvent;
    bool				mStreamDoneEventPending;
    Loop::Event			mBufferingUpdateEvent;
    bool				mBufferingUpdateEventPending;
    Loop::Event			mSeekingEvent;
    bool				mSeekingEventPending;
    Loop::Event			mCheckAudioStatusEvent;
    bool				mAudioStatusEventPending;
    Loop::Event			mBufferingStartEvent;
    bool				mBufferingStartEventPending;
    Loop::Event			mBufferingEndEvent;
    bool				mBufferingEndEventPending;
    Loop::Event			mSeekingCompleteEvent;
    bool				mSeekingCompleteEventPending;
	Loop::Event			mIOBitrateInfoEvent;
	Loop::Event			mMediaBitrateInfoEvent;
	
    Loop				mMsgLoop;
    pthread_mutex_t		mLock; // for xxx_impl(event callback)
	pthread_mutex_t		mPlayerLock; // for xxx_l
	pthread_mutex_t		mPreparedLock;
	pthread_mutex_t		mSnapShotLock;
    pthread_cond_t		mPreparedCondition;
    status_t			mPrepareResult;
    bool				mRunningCompatibilityTest;
	ISubtitles*			mSubtitles;

	//for test
	int64_t				mTotalStartTimeMs;
	int64_t				mGapStartTimeMs;
	int64_t				mRenderGapStartTimeMs;
	int64_t				mAveRenderTimeMs;
	int64_t				mDecodedFrames;
	int64_t				mRenderedFrames;

#if USE_AV_FILTER
	//avfilter
	AVFilterGraph*		mFilterGraph; // video filter graph
	AVFilterInOut*		mFilterOutputs;
    AVFilterInOut*		mFilterInputs;
	AVFilterContext*	mBufferSinkCtx;
	AVFilterContext*	mBufferSrcCtx;
	AVFilterContext*	mLastFilter;
	const char*			mFilterDescr[MAX_FILTER_CNT];
	AVFrame*			mVideoFiltFrame;
#endif
};


#endif
