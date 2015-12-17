package android.pplive.media.player;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.pplive.media.util.LogUtils;

class SystemMediaExtractor implements MediaExtractable {
	
	private MediaExtractor mExtractor;
	private boolean mSeeking = false;
	private boolean mGetVideoFlushPkt = false;
	
	SystemMediaExtractor() {
		this.mExtractor = new MediaExtractor();
	}

	@Override
	public boolean advance() {
		return mExtractor.advance();
	}

	@Override
	public long getCachedDuration() {
		return mExtractor.getCachedDuration();
	}

	@Override
	public int getSampleFlags() {
		return mExtractor.getSampleFlags();
	}

	@Override
	public long getSampleTime() {
		return mExtractor.getSampleTime();
	}

	@Override
	public int getSampleTrackIndex() {
		if (mSeeking) {
			if (!mGetVideoFlushPkt)
				return 0;
			else
				return 1;
		}
		
		return mExtractor.getSampleTrackIndex();
	}

	@Override
	public int getTrackCount() {
		return mExtractor.getTrackCount();
	}

	@Override
	public MediaFormat getTrackFormat(int index) {
		return mExtractor.getTrackFormat(index);
	}

	@Override
	public boolean hasCachedReachedEndOfStream() {
		return mExtractor.hasCacheReachedEndOfStream();
	}

	@Override
	public int readSampleData(ByteBuffer byteBuf, int offset) {
		if (mSeeking) {
			String str_flush = "FLUSH";
			byte [] flush_pkt = str_flush.getBytes();
			byteBuf.position(offset);
			byteBuf.put(flush_pkt);
			byteBuf.flip();
			
			// 关于flp: 将limit属性设置为当前的位置
			// 关于rewind: 是在limit属性已经被设置合适的情况下使用的。
			// 也就是说这两个方法虽然都能够使指针返回到缓冲区的第一个位置，但是flip在调整指针之前，
			
			if (!mGetVideoFlushPkt)
				mGetVideoFlushPkt = true;
			else
				mSeeking = false;
			return 5;
		}
		
		return mExtractor.readSampleData(byteBuf, offset);
	}

	@Override
	public void release() {
		mExtractor.release();
	}

	@Override
	public void seekTo(long timeUs, int mode) {
		mExtractor.seekTo(timeUs, mode);
		
		mSeeking = true;
		mGetVideoFlushPkt = false;
	}

	@Override
	public void selectTrack(int index) {
		mExtractor.selectTrack(index);
	}

	@Override
	public void setDataSource(String path) throws IOException {
		try{
			LogUtils.info("Java: setDataSource() " + path);
			mExtractor.setDataSource(path);
		}
		catch (Exception e){
            e.printStackTrace();
            LogUtils.error("Java: setDataSource() Exception" + path + ", e: " + e.getMessage());
		}
	}

	@Override
	public void unselectTrack(int index) {
		mExtractor.unselectTrack(index);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSystemExtractor() {
		// TODO Auto-generated method stub
		return true;
	}

}