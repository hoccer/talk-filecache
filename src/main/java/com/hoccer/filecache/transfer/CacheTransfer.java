package com.hoccer.filecache.transfer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hoccer.filecache.model.CacheFile;

/**
 * Active transfer to or from the cache
 * 
 * @author ingo
 */
public abstract class CacheTransfer {

	/**
	 * Common logger for subclasses
	 */
	protected static Logger log
		= Logger.getLogger(CacheTransfer.class.getSimpleName());
	
	/**
	 * Atomic counter for id assignment
	 */
	protected static AtomicInteger transferIdCounter=
		new AtomicInteger(0);

	/**
	 * Unique id for this transfer
	 */
	protected int transferId;
	
	/**
	 * Cache file being operated on
	 */
	protected CacheFile cacheFile;
	
	/**
	 * Http request causing this transfer
	 */
	protected HttpServletRequest  httpRequest;
	
	/**
	 * Http response for this transfer
	 */
	protected HttpServletResponse httpResponse;

    /**
     * Thread running the transfer
     */
    protected Thread thread;

	private long rateStartTime;
	private long rateEndTime;
	
	private long rateTimestamp;
	private long rateAccumulator;
	private double lastRate;
	
	private long totalDuration;
	private long totalBytesTransfered;
	private double totalRate;
	
	/**
	 * Primary constructor
	 * 
	 * @param file
	 * @param req
	 * @param resp
	 */
	protected CacheTransfer(CacheFile file, HttpServletRequest req, HttpServletResponse resp) {
		transferId = transferIdCounter.incrementAndGet();
		cacheFile = file;
		httpRequest = req;
		httpResponse = resp;
	}
	
	public String getRemoteAddr() {
		String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
		if(forwardedFor == null) {
			return httpRequest.getRemoteAddr();
		} else {
			return forwardedFor;
		}
	}
	
	public String getUserAgent() {
		String userAgent = httpRequest.getHeader("User-Agent");
		if(userAgent == null) {
			return "unknown";
		} else {
			return userAgent;
		}
	}
	
	public long getBytesTransfered() {
		return totalBytesTransfered;
	}
	
	public double getRate() {
		return lastRate;
	}
	
	public long getTotalDuration() {
		return totalDuration;
	}
	
	public double getTotalRate() {
		return totalRate;
	}

    public synchronized void abort() {
        if(this.thread != null) {
            this.thread.interrupt();
        }
    }

    protected synchronized void transferBegin(Thread thread) {
        this.thread = thread;
        rateStart();
    }

    protected synchronized void transferProgress(int bytesTransfered) {
        rateProgress(bytesTransfered);
    }

    protected synchronized void transferEnd() {
        rateFinish();
        this.thread = null;
    }
	
	private void rateStart() {
		long now = System.currentTimeMillis();
		rateStartTime = now;
		rateTimestamp = now;
		lastRate = 0.0;
		totalBytesTransfered = 0;
	}
	
	private void rateProgress(int bytesTransfered) {
		long now = System.currentTimeMillis();
		long passed = now - rateTimestamp;
		
		totalBytesTransfered += bytesTransfered;
		
		rateAccumulator += bytesTransfered;
		
		if(passed > 500) {
			double rate = rateAccumulator / (passed / 1000.0);
						
			rateTimestamp = now;
			rateAccumulator = 0;
			
			lastRate = rate;
		}
	}
	
	private void rateFinish() {
		rateEndTime = System.currentTimeMillis();
		
		totalDuration = rateEndTime - rateStartTime;
		totalRate = totalBytesTransfered / (totalDuration / 1000.0);
	}
	
}
