package com.hoccer.talk.filecache.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.transfer.CacheDownload;
import com.hoccer.talk.filecache.transfer.CacheUpload;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

@DatabaseTable(tableName = "file")
public class CacheFile {

	public static final int STATE_NEW = 1;
	public static final int STATE_UPLOADING = 2;
	public static final int STATE_COMPLETE = 3;
	public static final int STATE_ABANDONED = 4;
	public static final int STATE_EXPIRED = 5;
    public static final int STATE_DELETED = 6;

	private static String[] stateNames = {
		"UNKNOWN",
		"NEW",
		"UPLOADING",
		"COMPLETE",
		"ABANDONED",
		"EXPIRED",
        "DELETED"
	};
	
	private static ScheduledExecutorService expiryExecutor
		= Executors.newSingleThreadScheduledExecutor();
			
	protected static Logger log
		= Logger.getLogger(CacheFile.class);

    transient private CacheBackend mBackend;

	transient private ReentrantLock mStateLock;
	transient private Condition mStateChanged;

    transient private CacheUpload mUpload = null;

    transient private Vector<CacheDownload> mDownloads = new Vector<CacheDownload>();

    transient private ScheduledFuture<?> mExpiryFuture;

    transient private long mLastCheckpoint;
    transient private long mCheckpointInterval;

    @DatabaseField(columnName = "fileId", id = true)
    private String mFileId;
    @DatabaseField(columnName = "uploadId", unique = true)
    private String mUploadId;
    @DatabaseField(columnName = "downloadId", unique = true)
    private String mDownloadId;

    @DatabaseField(columnName = "accountId")
    private String mAccountId;

    @DatabaseField(columnName = "state")
    private int mState;
    @DatabaseField(columnName = "limit")
	private int mLimit;

    @DatabaseField(columnName = "contentType")
	private String mContentType;
    @DatabaseField(columnName = "contentLength")
	private int mContentLength;

    @DatabaseField(columnName = "expiryTime")
	private Date mExpiryTime;

    public CacheFile() {
        mStateLock = new ReentrantLock();
        mStateChanged = mStateLock.newCondition();

        mState = STATE_NEW;
        mLimit = 0;

        mContentLength = -1;

        mFileId = UUID.randomUUID().toString();
        mUploadId = UUID.randomUUID().toString();
        mDownloadId = UUID.randomUUID().toString();
    }

	public CacheFile(String pUUID) {
		this();
		mFileId = pUUID;
	}

    public void onActivate(CacheBackend backend) {
        log.debug("onActivate(" + mFileId + ")");
        mBackend = backend;
        CacheConfiguration configuration = mBackend.getConfiguration();
        mCheckpointInterval = configuration.getDataCheckpointInterval();
    }

    public void onDeactivate() {
        log.debug("onDeactivate(" + mFileId + ")");
        if(mUpload != null) {
            mUpload.abort();
        }
        for(CacheDownload download: mDownloads) {
            download.abort();
        }
        if(mExpiryFuture != null) {
            mExpiryFuture.cancel(false);
        }
    }

	public int getState() {
		return mState;
	}

    public boolean isActive() {
        return mDownloads.size() > 0 || mUpload != null;
    }
	
	public boolean isAbandoned() {
		return mState == STATE_ABANDONED;
	}

	public String getStateString() {
		return stateNames[mState];
	}
	
	public int getLimit() {
		return mLimit;
	}
	
	public String getFileId() {
		return mFileId;
	}

    public String getUploadId() {
        return mUploadId;
    }

    public String getDownloadId() {
        return mDownloadId;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public String getContentType() {
		return mContentType;
	}
	
	public void setContentType(String contentType) {
		mContentType = contentType;
	}
	
	public int getContentLength() {
		return mContentLength;
	}
	
	public void setContentLength(int contentLength) {
		mContentLength = contentLength;
	}
	
	public Date getExpiryTime() {
		return mExpiryTime;
	}
	
	public CacheUpload getUpload() {
		return mUpload;
	}
	
	public int getNumDownloads() {
		return mDownloads.size();
	}
	
	public Vector<CacheDownload> getDownloads() {
		return new Vector<CacheDownload>(mDownloads);
	}
	
	public File getFile() {
		return new File(mBackend.getDataDirectory(), mFileId);
	}
	
	private void switchState(int newState, String cause) {
		log.info("file " + mFileId + " state " + stateNames[mState]
					+ " -> " + stateNames[newState] + ": " + cause);
		mState = newState;
        mBackend.checkpoint(this);
	}
	
	private void considerDeactivate() {
        if(!isActive()) {
            mBackend.checkpoint(this);
            mBackend.deactivate(this);
        }
	}
	
	public void setupExpiry(int secondsFromNow) {
		Date now = new Date();
		Calendar cal = new GregorianCalendar();
		cal.setTime(now);
		cal.add(Calendar.SECOND, secondsFromNow);
		mExpiryTime = cal.getTime();
		log.info("file " + mFileId + " expires " + mExpiryTime.toString());
	}
	
	private void scheduleExpiry() {
		Runnable expiryAction = new Runnable() {
			@Override
			public void run() {
				CacheFile.this.expire();
                mExpiryFuture = null;
			}
		};
		mExpiryFuture = expiryExecutor.schedule(
				            expiryAction,
				            mExpiryTime.getTime() - System.currentTimeMillis(),
				            TimeUnit.MILLISECONDS);
	}

	private void expire() {
		mStateLock.lock();
		try {
			switchState(STATE_EXPIRED, "expiry time reached");
			considerDeactivate();
		} finally {
			mStateLock.unlock();
		}
	}
	
	public void uploadStarts(CacheUpload upload) {
		mStateLock.lock();
		try {
			if(mState == STATE_NEW) {
				switchState(STATE_UPLOADING, "new upload");
			} else {
                // this means we are in a reupload
			}

			mUpload = upload;

			mStateChanged.signalAll();
		} finally {
			mStateLock.unlock();
		}
	}
	
	public void uploadAborted(CacheUpload upload) {
		mStateLock.lock();
		try {
			mUpload = null;
			
			mStateChanged.signalAll();
			
			considerDeactivate();
		} finally {
			mStateLock.unlock();
		}
	}
	
	public void uploadFinished(CacheUpload upload) {
		mStateLock.lock();
		try {
			if(mState == STATE_UPLOADING) {
				switchState(STATE_COMPLETE, "upload finished");
			}
			
			scheduleExpiry();
			
			mUpload = null;
			
			mStateChanged.signalAll();
			
			considerDeactivate();
		} finally {
			mStateLock.unlock();
		}
	}
	
	public void downloadStarts(CacheDownload download) {
		mStateLock.lock();
		try {
			mDownloads.add(download);
			
			mStateChanged.signalAll();
		} finally {
			mStateLock.unlock();
		}
	}
	
	public void downloadAborted(CacheDownload download) {
		mDownloads.remove(download);
		considerDeactivate();
	}
	
	public void downloadFinished(CacheDownload download) {
		mDownloads.remove(download);
		considerDeactivate();
	}
	
	public void updateLimit(int newLimit, RandomAccessFile raf) throws IOException {

		mStateLock.lock();
		try {
            if(newLimit > mLimit) {
                mLimit = newLimit;
            }
			
			mStateChanged.signalAll();

            long now = System.currentTimeMillis();
            if((now - mLastCheckpoint) >= mCheckpointInterval) {
                log.debug("checkpointing " + mFileId + " at " + mLimit);
                raf.getFD().sync();
                mBackend.checkpoint(this);
                mLastCheckpoint = now;
            }
		} finally {
			mStateLock.unlock();
		}
	}

    public void delete() {
        mStateLock.lock();
        try {
            switchState(STATE_DELETED, "deleted");

            mBackend.deactivate(this);
        } finally {
            mStateLock.unlock();
        }
    }

	public boolean waitForData(int wantedPosition) throws InterruptedException {
		// acquire state lock
		mStateLock.lock();

		try {
			// cases where progress has been
			// made already or will never be made
			if(mState == STATE_COMPLETE) {
				if(mLimit > wantedPosition) {
					return true;
				} else {
					return false;
				}
			}	
			if(mState == STATE_UPLOADING) {
				if(mLimit > wantedPosition) {
					return true;
				}
			}
            if(mState == STATE_DELETED) {
                return false;
            }
			if(mState == STATE_ABANDONED) {
				return false;
			}
            if(mState == STATE_EXPIRED) {
                return false;
            }
			
			// wait for state change
		    mStateChanged.await();

			// cases where progress may have
			// been made while waiting
			if(mState == STATE_COMPLETE) {
				return true;
			}
			if(mState == STATE_UPLOADING) {
				return true;
			}
			if(mState == STATE_NEW) {
				return true;
			}
			
			// no progression possible
			return false;
		} finally {
			// release state lock
			mStateLock.unlock();
		}

	}
	
	private void ensureExists() throws IOException {
		File f = getFile();
		f.createNewFile();
	}
	
	public RandomAccessFile openForRandomAccess(String mode) throws IOException {
		ensureExists();
		
		RandomAccessFile r = null;
		
		try {
			r = new RandomAccessFile(getFile(), mode);
		} catch (FileNotFoundException e) {
			// XXX does not happen
		}
		
		return r;
	}
	
}
