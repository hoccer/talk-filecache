package com.hoccer.talk.filecache.db;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.model.CacheFile;

import java.util.HashMap;
import java.util.Vector;

public class MemoryBackend extends CacheBackend {

    private HashMap<String, CacheFile> mFiles
            = new HashMap<String, CacheFile>();

    public MemoryBackend(CacheConfiguration config) {
        super(config);
    }

    @Override
    public void start() {
    }

    @Override
    public Vector<CacheFile> getActiveFiles() {
        return new Vector<CacheFile>(mFiles.values());
    }

    @Override
    public CacheFile getByFileId(String id, boolean create) {
        return null;
    }

    @Override
    public CacheFile getByUploadId(String id) {
        return null;
    }

    @Override
    public CacheFile getByDownloadId(String id) {
        return null;
    }

    @Override
    public void checkpoint(CacheFile file) {
    }

    @Override
    public void deactivate(CacheFile file) {
    }

    @Override
    public void delete(CacheFile file) {
        mFiles.remove(file.getFileId());
    }

}
