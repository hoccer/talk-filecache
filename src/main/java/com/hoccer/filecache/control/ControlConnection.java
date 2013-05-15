package com.hoccer.filecache.control;

import better.jsonrpc.core.JsonRpcConnection;
import com.hoccer.filecache.CacheBackend;
import com.hoccer.filecache.control.ICacheControl;
import com.hoccer.filecache.model.CacheFile;

import javax.servlet.ServletContext;
import java.util.UUID;

public class ControlConnection implements ICacheControl {

    ControlServlet mServlet;

    JsonRpcConnection mRpcConnection;

    CacheBackend mBackend;

    public ControlConnection(ControlServlet servlet, JsonRpcConnection rpcConnection) {
        mServlet = servlet;
        mRpcConnection = rpcConnection;
        mBackend = getCacheBackend();
    }

    private CacheBackend getCacheBackend() {
        ServletContext ctx = mServlet.getServletContext();
        CacheBackend backend = (CacheBackend)ctx.getAttribute("backend");
        return backend;
    }

    @Override
    public FileHandles createFileForStorage(String accountId, int fileSize) {
        CacheFile file = mBackend.forId(UUID.randomUUID().toString(), true);
        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();
        mBackend.checkpoint(file);
        return handles;
    }

    @Override
    public FileHandles createFileForTransfer(String accountId, int fileSize) {
        CacheFile file = mBackend.forId(UUID.randomUUID().toString(), true);
        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();
        mBackend.checkpoint(file);
        return handles;
    }

    @Override
    public void deleteFile(String fileId) {
        CacheFile file = mBackend.forId(fileId, false);
    }

    @Override
    public void deleteAccount(String accountId) {
    }

}
