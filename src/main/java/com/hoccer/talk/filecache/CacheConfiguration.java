package com.hoccer.talk.filecache;

import java.io.File;
import java.util.Properties;

public class CacheConfiguration {

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
        String serverThreads = properties.getProperty(PROPERTY_PREFIX + ".server.threads");
        if(serverThreads != null) {
            mServerThreads = Integer.parseInt(serverThreads);
        }
        // Listen params
        mListenAddress = properties.getProperty(PROPERTY_PREFIX + ".listen.address", mListenAddress);
        String listenPort = properties.getProperty(PROPERTY_PREFIX + ".listen.port");
        if(listenPort != null) {
            mListenPort = Integer.parseInt(listenPort);
        }
        // Data directory
        mDataDirectory = properties.getProperty(PROPERTY_PREFIX + ".data.directory", mDataDirectory);
        String dataCheckpointInterval = properties.getProperty(PROPERTY_PREFIX + ".data.checkpointInterval");
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

}
