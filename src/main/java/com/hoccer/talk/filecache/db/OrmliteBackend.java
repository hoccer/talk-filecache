package com.hoccer.talk.filecache.db;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.logging.HoccerLoggers;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class OrmliteBackend extends CacheBackend {

    private CacheConfiguration mConfiguration;
    private JdbcConnectionSource mConnectionSource;
    private Dao<CacheFile, String> mDao;
    private Hashtable<String, CacheFile> mAllFiles;

    public OrmliteBackend(CacheConfiguration configuration) {
        super(configuration);
        mAllFiles = new Hashtable<String, CacheFile>();
        mConfiguration = configuration;
    }

    @Override
    public void start() {
        try {
            LOG.info("Creating connection source for " + mConfiguration.getOrmliteUrl());
            mConnectionSource = new JdbcConnectionSource(mConfiguration.getOrmliteUrl(),
                                                         mConfiguration.getOrmliteUser(),
                                                         mConfiguration.getOrmlitePassword());
            //LOG.info("Creating table");
            //TableUtils.createTable(mConnectionSource, CacheFile.class);
            LOG.info("Creating dao");
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
                                        mDao.create(res);
                                    }
                                }
                                res.setBackend(OrmliteBackend.this);
                                return res;
                            }
                        });
            } catch (SQLException e) {
                LOG.error("SQL exception", e);
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
            LOG.error("SQL exception", e);
        }
    }

    @Override
    public synchronized void remove(CacheFile file) {
        try {
            mDao.delete(file);
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }
        mAllFiles.remove(file.getFileId());
    }

}
