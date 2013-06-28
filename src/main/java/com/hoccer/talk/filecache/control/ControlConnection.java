package com.hoccer.talk.filecache.control;

import better.jsonrpc.core.JsonRpcConnection;
import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.filecache.rpc.ICacheControl;

import javax.servlet.ServletContext;
import java.util.List;
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
    public FileHandles createFileForStorage(String accountId, String contentType, int contentLength) {
        CacheFile file = mBackend.getByFileId(UUID.randomUUID().toString(), true);

        file.setFileType(CacheFile.TYPE_STORAGE);
        file.setContentType(contentType);
        file.setContentLength(contentLength);

        mBackend.checkpoint(file);

        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();

        return handles;
    }

    @Override
    public FileHandles createFileForTransfer(String accountId, String contentType, int contentLength) {
        CacheFile file = mBackend.getByFileId(UUID.randomUUID().toString(), true);

        file.setFileType(CacheFile.TYPE_TRANSFER);
        file.setContentType(contentType);
        file.setContentLength(contentLength);

        mBackend.checkpoint(file);

        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();

        return handles;
    }

    @Override
    public void deleteFile(String fileId) {
        CacheFile file = mBackend.getByFileId(fileId, false);
        if(file != null) {
            file.delete();
        }
    }

    @Override
    public void deleteAccount(String accountId) {
        List<CacheFile> files = mBackend.getFilesByAccount(accountId);
        if(files != null) {
            for(CacheFile file: files) {
                file.delete();
            }
        }
    }

}
