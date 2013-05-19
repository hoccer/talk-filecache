package com.hoccer.talk.filecache.control;

import better.jsonrpc.core.JsonRpcConnection;
import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.filecache.rpc.ICacheControl;

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
        CacheFile file = mBackend.getByFileId(UUID.randomUUID().toString(), true);
        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();
        mBackend.checkpoint(file);
        return handles;
    }

    @Override
    public FileHandles createFileForTransfer(String accountId, int fileSize) {
        CacheFile file = mBackend.getByFileId(UUID.randomUUID().toString(), true);
        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();
        mBackend.checkpoint(file);
        return handles;
    }

    @Override
    public void deleteFile(String fileId) {
        CacheFile file = mBackend.getByFileId(fileId, false);
        file.delete();
    }

    @Override
    public void deleteAccount(String accountId) {
    }

}
