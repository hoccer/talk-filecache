package com.hoccer.talk.filecache;

import java.io.File;
import java.util.Properties;

public class CacheConfiguration {

    private static final String PROPERTY_PREFIX = "talk.filecache";

    private String mListenAddress = "localhost";
    private int    mListenPort    = 8080;

    private String mDataDirectory = null;

    private String mDatabaseBackend;

    private String mOrmliteUrl;
    private String mOrmliteUser;
    private String mOrmlitePassword;

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

    public String getDatabaseBackend() {
        return mDatabaseBackend;
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
        // Listen params
        mListenAddress = properties.getProperty(PROPERTY_PREFIX + ".listen.address", "localhost");
        mListenPort = Integer.parseInt(properties.getProperty(PROPERTY_PREFIX + ".listen.port", "8080"));
        // Data directory
        mDataDirectory = properties.getProperty(PROPERTY_PREFIX + ".data.directory", "/srv/filecache");
        // Database
        mDatabaseBackend = properties.getProperty(PROPERTY_PREFIX + ".database.backend", "memory");
        // ORMlite
        mOrmliteUrl = properties.getProperty(PROPERTY_PREFIX + ".ormlite.url", "talk");
        mOrmliteUser = properties.getProperty(PROPERTY_PREFIX + ".ormlite.user", "talk");
        mOrmlitePassword = properties.getProperty(PROPERTY_PREFIX + ".ormlite.password", "talk");
    }

}