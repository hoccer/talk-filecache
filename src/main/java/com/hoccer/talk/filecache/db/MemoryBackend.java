package com.hoccer.talk.filecache.db;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.model.CacheFile;

import java.io.File;
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
    public CacheFile forId(String id, boolean create) {
        CacheFile res = null;

        synchronized (mFiles) {
            if(mFiles.containsKey(id)) {
                res = mFiles.get(id);
            } else {
                if(create) {
                    res = new CacheFile(id);
                    mFiles.put(id, res);
                }
            }
            res.setBackend(this);
        }

        return res;
    }

    @Override
    public void checkpoint(CacheFile file) {
    }

    @Override
    public void remove(CacheFile f) {
        if(mFiles.containsKey(f.getFileId())) {
            mFiles.remove(f.getFileId());
        }
    }

    @Override
    public Vector<CacheFile> getAll() {
        return new Vector<CacheFile>(mFiles.values());
    }

}
