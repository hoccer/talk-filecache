package com.hoccer.talk.filecache;

import com.hoccer.talk.filecache.model.CacheFile;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;

public abstract class CacheBackend {

    protected static final Logger LOG = Logger.getLogger(CacheBackend.class);

    private CacheConfiguration mConfiguration;

    /** Root directory of file store */
    private File mDataDirectory;

    /**
     * Main superconstructor
     *
     * Implementations must call this with the data directory.
     *
     */
    protected CacheBackend(CacheConfiguration configuration) {
        mConfiguration = configuration;
        mDataDirectory = new File(configuration.getDataDirectory());
    }

    public CacheConfiguration getConfiguration() {
        return mConfiguration;
    }

    /** @return the data directory for this backend */
    public File getDataDirectory() {
        return mDataDirectory;
    }

    public abstract void start();

    /** Get a list of all files in memory */
    public abstract List<CacheFile> getActiveFiles();

    /**
     * Get the file for the given id
     *
     * @param id of the file to access
     * @param create - true if file should be created if not present
     * @return file corresponding to ID or null
     */
    public abstract CacheFile getByFileId(String id, boolean create);

    public abstract CacheFile getByUploadId(String id);
    public abstract CacheFile getByDownloadId(String id);

    public abstract List<CacheFile> getFilesByAccount(String accountId);

    /**
     * Checkpoint the given files state in the database
     *
     * It is the callers responsibility to ensure that the
     * filestore is consistent with respect to the state given.
     *
     * @param file to checkpoint
     */
    public abstract void checkpoint(CacheFile file);

    public abstract void deactivate(CacheFile file);

    public abstract void delete(CacheFile file);

}
