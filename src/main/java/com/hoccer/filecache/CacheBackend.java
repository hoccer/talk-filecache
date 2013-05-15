package com.hoccer.filecache;

import com.hoccer.filecache.model.CacheFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public abstract class CacheBackend {

    /** Root directory of file store */
    private File mDataDirectory = null;

    /**
     * Main superconstructor
     *
     * Implementations must call this with the data directory.
     *
     */
    protected CacheBackend(File dataDirectory) {
        mDataDirectory = dataDirectory;
    }

    /** @return the data directory for this backend */
    public File getDataDirectory() {
        return mDataDirectory;
    }

    /** Convenience wrapper for forId() */
    public CacheFile forPathInfo(String pathInfo, boolean create) {
        if(pathInfo.length() == 1) {
            return null;
        }

        return forId(pathInfo.substring(1), create);
    }

    /** Get a list of all files in storage (XXX eliminate / replace with getAllActive) */
    public abstract List<CacheFile> getAll();

    /**
     * Get the file for the given id
     *
     * @param id of the file to access
     * @param create - true if file should be created if not present
     * @return file corresponding to ID or null
     */
    public abstract CacheFile forId(String id, boolean create);

    /**
     * Checkpoint the given files state in the database
     *
     * It is the callers responsibility to ensure that the
     * filestore is consistent with respect to the state given.
     *
     * @param file to checkpoint
     */
    public abstract void checkpoint(CacheFile file);

    /**
     * Remove a file from the store permanently
     *
     * @param file to remove
     */
    public abstract void remove(CacheFile file);

}
