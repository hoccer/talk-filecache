package com.hoccer.filecache.db;

import com.hoccer.filecache.CacheBackend;
import com.hoccer.filecache.model.CacheFile;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;

import java.io.File;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

public class OrmliteBackend extends CacheBackend {

    private JdbcConnectionSource mConnectionSource;
    private Dao<CacheFile, String> mDao;
    private Hashtable<String, CacheFile> mAllFiles;

    public OrmliteBackend(File dataDir) {
        super(dataDir);
        mAllFiles = new Hashtable<String, CacheFile>();
        try {
            mConnectionSource = new JdbcConnectionSource("jdbc:postgresql://localhost/talk", "talk", "talk");
            //TableUtils.createTable(mConnectionSource, CacheFile.class);
            mDao = DaoManager.createDao(mConnectionSource, CacheFile.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CacheFile> getAll() {
        // XXX won't implement
        return new Vector<CacheFile>();
    }

    @Override
    public synchronized CacheFile forId(final String id, final boolean create) {
        CacheFile res = null;

        // try to find the file in memory
        res = mAllFiles.get(id);

        // not found? try database
        if(res == null) {
            try {
                res = TransactionManager.callInTransaction(mConnectionSource,
                        new Callable<CacheFile>() {
                            @Override
                            public CacheFile call() throws Exception {
                                CacheFile res = null;
                                // try to find in db
                                res = mDao.queryForId(id);
                                // not found? create if we want to
                                if(res == null) {
                                    if(create) {
                                        res = new CacheFile(id);
                                        res.setBackend(OrmliteBackend.this);
                                        mDao.create(res);
                                    }
                                }
                                return res;
                            }
                        });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // remember state object
        if(res != null) {
            mAllFiles.put(id, res);
        }

        // return whatever we got
        return res;
    }

    @Override
    public void checkpoint(CacheFile file) {
        try {
            mDao.update(file);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void remove(CacheFile file) {
        try {
            mDao.delete(file);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mAllFiles.remove(file.getFileId());
    }

}
