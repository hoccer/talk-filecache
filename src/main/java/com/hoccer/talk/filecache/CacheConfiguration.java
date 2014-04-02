package com.hoccer.talk.filecache;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.Properties;

public class CacheConfiguration {

    private static final Logger LOG = Logger.getLogger(CacheConfiguration.class);
    private static final String PROPERTY_PREFIX = "talk.filecache";

    private int    mServerThreads = 128;

    private String mListenAddress = "localhost";
    private int    mListenPort    = 8080;

    private String mDataDirectory = null;
    private long   mDataCheckpointInterval = 2000;

    private String mDatabaseBackend = "memory";

    private boolean mOrmliteInitDb   = false;
    private String  mOrmliteUrl      = "talk";
    private String  mOrmliteUser     = "talk";
    private String  mOrmlitePassword = "talk";

    private int mCleanupFilesDelay = 10; // in seconds
    private int mCleanupFilesInterval = 60; // in seconds

    private int mStorageFileExpiryTime = 365 * 24 * 3600; // 1 year (in seconds)
    private int mTransferFileExpiryTime = 120;//3 * 7 * 24 * 3600; // 3 weeks (in seconds)


    public int getStorageFileExpiryTime() {
        return mStorageFileExpiryTime;
    }

    public int getTransferFileExpiryTime() {
        return mTransferFileExpiryTime;
    }

    public int getCleanupFilesDelay() {
        return mCleanupFilesDelay;
    }

    public int getCleanupFilesInterval() {
        return mCleanupFilesInterval;
    }

    public int getServerThreads() {
        return mServerThreads;
    }

    public void setServerThreads(int mServerThreads) {
        this.mServerThreads = mServerThreads;
    }

    public String getListenAddress() {
        return mListenAddress;
    }

    public void setListenAddress(String mListenAddress) {
        this.mListenAddress = mListenAddress;
    }

    public int getListenPort() {
        return mListenPort;
    }

    public void setListenPort(int mListenPort) {
        this.mListenPort = mListenPort;
    }

    public String getDataDirectory() {
        return mDataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.mDataDirectory = dataDirectory;
    }

    public long getDataCheckpointInterval() {
        return mDataCheckpointInterval;
    }

    public void setDataCheckpointInterval(long mDataCheckpointInterval) {
        this.mDataCheckpointInterval = mDataCheckpointInterval;
    }

    public String getDatabaseBackend() {
        return mDatabaseBackend;
    }

    public boolean getOrmliteInitDb() {
        return mOrmliteInitDb;
    }

    public void setOrmliteInitDb(boolean mOrmliteInitDb) {
        this.mOrmliteInitDb = mOrmliteInitDb;
    }

    public String getOrmliteUrl() {
        return mOrmliteUrl;
    }

    public void setOrmliteUrl(String ormliteUrl) {
        this.mOrmliteUrl = ormliteUrl;
    }

    public String getOrmliteUser() {
        return mOrmliteUser;
    }

    public void setOrmliteUser(String ormliteUser) {
        this.mOrmliteUser = ormliteUser;
    }

    public String getOrmlitePassword() {
        return mOrmlitePassword;
    }

    public void setOrmlitePassword(String ormlitePassword) {
        this.mOrmlitePassword = ormlitePassword;
    }

    public void configureFromProperties(Properties properties) {
        // Server
        String serverThreads = properties.getProperty(PROPERTY_PREFIX + ".server.threads", Integer.toString(mServerThreads));
        if(serverThreads != null) {
            mServerThreads = Integer.parseInt(serverThreads);
        }
        // Listen params
        mListenAddress = properties.getProperty(PROPERTY_PREFIX + ".listen.address", mListenAddress);
        String listenPort = properties.getProperty(PROPERTY_PREFIX + ".listen.port", Integer.toString(mListenPort));
        if(listenPort != null) {
            mListenPort = Integer.parseInt(listenPort);
        }
        // Data directory
        mDataDirectory = properties.getProperty(PROPERTY_PREFIX + ".data.directory", mDataDirectory);
        String dataCheckpointInterval = properties.getProperty(PROPERTY_PREFIX + ".data.checkpointInterval", Long.toString(mDataCheckpointInterval));
        if(dataCheckpointInterval != null) {
            mDataCheckpointInterval = Long.parseLong(dataCheckpointInterval);
        }
        // Database
        mDatabaseBackend = properties.getProperty(PROPERTY_PREFIX + ".database.backend", mDatabaseBackend);
        // ORMlite
        mOrmliteUrl = properties.getProperty(PROPERTY_PREFIX + ".ormlite.url", mOrmliteUrl);
        mOrmliteUser = properties.getProperty(PROPERTY_PREFIX + ".ormlite.user", mOrmliteUser);
        mOrmlitePassword = properties.getProperty(PROPERTY_PREFIX + ".ormlite.password", mOrmlitePassword);
    }

    public void report() {
        LOG.info("Current configuration:" +
                "\n - WebServer Configuration:" +
                MessageFormat.format("\n   * listen address:                        ''{0}''", mListenAddress) +
                MessageFormat.format("\n   * listen port:                           ''{0}''", Long.toString(mListenPort)) +
                MessageFormat.format("\n   * threads:                               ''{0}''", mServerThreads) +
                "\n - Database Configuration:" +
                MessageFormat.format("\n   * database backend:                      ''{0}''", mDatabaseBackend) +
                MessageFormat.format("\n   * ormlite url:                           ''{0}''", mOrmliteUrl) +
                MessageFormat.format("\n   * ormlite user:                          ''{0}''", mOrmliteUser) +
                MessageFormat.format("\n   * ormlite pass:                          ''{0}''", mOrmlitePassword) +
                "\n - Expiry/Cleaning Configuration:" +
                MessageFormat.format("\n   * storage file expiry time (in days):    ''{0}''", Long.toString(mStorageFileExpiryTime / 3600 / 24)) +
                MessageFormat.format("\n   * transfer file expiry time (in days):   ''{0}''", Long.toString(mTransferFileExpiryTime / 3600 / 24)) +
                MessageFormat.format("\n   * files cleanup delay (in s):            ''{0}''", Long.toString(mCleanupFilesDelay)) +
                MessageFormat.format("\n   * files cleanup interval (in s):         ''{0}''", Long.toString(mCleanupFilesInterval)) +
                "\n - Other:" +
                MessageFormat.format("\n   * data directory:                        ''{0}''", mDataDirectory) +
                MessageFormat.format("\n   * data checkpoint interval:              ''{0}''", Long.toString(mDataCheckpointInterval))
        );
    }

}
