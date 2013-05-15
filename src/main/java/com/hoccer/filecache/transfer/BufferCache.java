package com.hoccer.filecache.transfer;

import java.util.ArrayDeque;

/**
 * Buffer cache allowing transfer buffer reuse
 * 
 * @author ingo
 */
public class BufferCache {

	/** Size of all managed buffers */
	public static final int BUFFER_SIZE = 2 ^ 16;
	
	/** Number of buffers to be cached */
	public static final int BUFFER_COUNT = 100;
	
	/** Number of buffers in flight */
	private static int buffersActive;
	
	/** Cache hit counter */
	private static int bufferCacheHits;
	
	/** Cache miss counter */
	private static int bufferCacheMisses;
	
	/** Deque comprising the cache itself */
	private static ArrayDeque<byte[]> sCachedBuffers
		= new ArrayDeque<byte[]>(BUFFER_COUNT);
	
	/** Acquire a buffer from the cache */
	public static synchronized byte[] takeBuffer() {
		byte[] buffer = sCachedBuffers.poll();
		
		if(buffer == null) {
			buffer = new byte[BUFFER_SIZE];
			bufferCacheMisses++;
		} else {
			bufferCacheHits++;
		}
		
		buffersActive++;
		
		return buffer;
	}
	
	/** Return a buffer to be reused */
	public static synchronized void returnBuffer(byte[] buffer) {
		buffersActive--;
		
		if(sCachedBuffers.size() < BUFFER_COUNT) {
			sCachedBuffers.push(buffer);
		}
	}
	
}
