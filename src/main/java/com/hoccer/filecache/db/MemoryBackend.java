package com.hoccer.filecache.db;

import com.hoccer.filecache.CacheBackend;
import com.hoccer.filecache.model.CacheFile;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;

public class MemoryBackend extends CacheBackend {

    private HashMap<String, CacheFile> mFiles
            = new HashMap<String, CacheFile>();

    public MemoryBackend(File dataDir) {
        super(dataDir);
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
                    res.setBackend(this);
                    mFiles.put(id, res);
                }
            }
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
