package com.hoccer.talk.filecache.db;

import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.filecache.model.CacheFile;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.helpers.LogLog;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrmliteBackend extends CacheBackend {

    private CacheConfiguration mConfiguration;

    private JdbcConnectionSource mConnectionSource;

    private Dao<CacheFile, String> mDao;

    private Hashtable<String, CacheFile> mActiveByFileId;
    private Hashtable<String, CacheFile> mActiveByUploadId;
    private Hashtable<String, CacheFile> mActiveByDownloadId;

    private ScheduledExecutorService mExpiryExecutor;

    public OrmliteBackend(CacheConfiguration configuration) {
        super(configuration);
        mActiveByFileId = new Hashtable<String, CacheFile>();
        mActiveByUploadId = new Hashtable<String, CacheFile>();
        mActiveByDownloadId = new Hashtable<String, CacheFile>();
        mExpiryExecutor = Executors.newSingleThreadScheduledExecutor();
        mConfiguration = configuration;
    }

    @Override
    public void start() {
        try {
            if(LOG.isDebugEnabled()) {
                LOG.debug("creating connection source for " + mConfiguration.getOrmliteUrl());
            }
            mConnectionSource = new JdbcConnectionSource(mConfiguration.getOrmliteUrl(),
                                                         mConfiguration.getOrmliteUser(),
                                                         mConfiguration.getOrmlitePassword());
            if(mConfiguration.getOrmliteInitDb()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("creating table for files");
                }
                TableUtils.createTable(mConnectionSource, CacheFile.class);
            }
            if(LOG.isDebugEnabled()) {
                LOG.debug("creating dao for files");
            }
            mDao = DaoManager.createDao(mConnectionSource, CacheFile.class);
        } catch (SQLException e) {
            LOG.error("Error initializing ormlite", e);
        }
        mExpiryExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("deleting expired files");
                }
                deleteExpiredFiles();
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    private CacheFile activate(CacheFile file) {
        CacheFile res = null;
        synchronized (mActiveByFileId) {
            CacheFile active = mActiveByFileId.get(file.getFileId());
            if(active != null) {
                res = active;
            } else {
                file.onActivate(this);
                mActiveByFileId.put(file.getFileId(), file);
                mActiveByUploadId.put(file.getUploadId(), file);
                mActiveByDownloadId.put(file.getDownloadId(), file);
                res = file;
            }
        }
        return res;
    }

    @Override
    public void checkpoint(CacheFile file) {
        try {
            mDao.createOrUpdate(file);
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }
    }

    @Override
    public void delete(CacheFile file) {
        // delete the file
        File f = file.getFile();
        if(f.exists()) {
            f.delete();
        }
        // delete the record
        try {
            mDao.delete(file);
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }
    }

    @Override
    public void deactivate(CacheFile file) {
        synchronized (mActiveByFileId) {
            file.onDeactivate();

            mActiveByFileId.remove(file.getFileId());
            mActiveByUploadId.remove(file.getUploadId());
            mActiveByDownloadId.remove(file.getDownloadId());

            int fileState = file.getState();
            switch(fileState) {
            case CacheFile.STATE_EXPIRED:
            case CacheFile.STATE_DELETED:
                delete(file);
            }
        }
    }


    @Override
    public List<CacheFile> getActiveFiles() {
        List<CacheFile> res;
        synchronized (mActiveByFileId) {
            res = new Vector<CacheFile>(mActiveByFileId.values());
        }
        return res;
    }

    @Override
    public synchronized CacheFile getByFileId(final String id, final boolean create) {
        CacheFile res = null;

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
                                        res = new CacheFile(id, null, null, -1);
                                        mDao.create(res);
                                    }
                                }
                                return res;
                            }
                        });
            } catch (SQLException e) {
                LOG.error("SQL exception", e);
            }
            if(res != null) {
                res = activate(res);
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("get by fileId " + id + " found " + (res != null ? "yes" : "no"));
        }

        // return whatever we got
        return res;
    }

    @Override
    public CacheFile getByUploadId(String id) {
        CacheFile res = null;

        try {
            res = mDao.queryBuilder().where()
                      .eq("uploadId", id)
                      .queryForFirst();
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }

        if(res != null) {
            res = activate(res);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("get by uploadId " + id + " found " + (res != null ? "yes" : "no"));
        }

        return res;
    }

    @Override
    public CacheFile getByDownloadId(String id) {
        CacheFile res = null;

        try {
            res = mDao.queryBuilder().where()
                    .eq("downloadId", id)
                    .queryForFirst();
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }

        if(res != null) {
            res = activate(res);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("get by downloadId " + id + " found " + (res != null ? "yes" : "no"));
        }

        return res;
    }

    @Override
    public List<CacheFile> getFilesByAccount(String accountId) {
        List<CacheFile> res = null;

        try {
            res = mDao.queryBuilder().where()
                            .eq("accountId", accountId)
                       .query();
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }

        if(res != null) {
            for(int i = 0; i < res.size(); i++) {
                res.set(i, activate(res.get(i)));
            }
        }

        return res;
    }

    private void deleteExpiredFiles() {
        LOG.info("querying for expired files");
        Date now = new Date();
        List<CacheFile> files = null;
        try {
            PreparedQuery<CacheFile> expiryQuery =
                    mDao.queryBuilder()
                            .where()
                                .le("expiryTime", now)
                                .eq("state", CacheFile.STATE_EXPIRED)
                                .eq("state", CacheFile.STATE_DELETED)
                            .or(3)
                            .prepare();
            files = mDao.query(expiryQuery);
            LOG.info("found " + files.size() + " files");
        } catch (SQLException e) {
            LOG.error("SQL exception", e);
        }
        if(files != null) {
            for(int i = 0; i < files.size(); i++) {
                CacheFile file = activate(files.get(i));
                if(file.getExpiryTime().before(now)) {
                    file.expire();
                } else {
                    switch(file.getState()) {
                    case CacheFile.STATE_EXPIRED:
                        file.expire();
                        break;
                    case CacheFile.STATE_DELETED:
                        file.delete();
                        break;
                    }
                }
            }
        }
    }

}
