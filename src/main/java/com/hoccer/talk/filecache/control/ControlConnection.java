package com.hoccer.talk.filecache.control;

import better.jsonrpc.core.JsonRpcConnection;
import com.hoccer.talk.filecache.CacheBackend;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.filecache.rpc.ICacheControl;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ControlConnection implements ICacheControl {

    private static final Logger LOG = Logger.getLogger(ControlConnection.class);
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();

    int mId = ID_COUNTER.incrementAndGet();

    ControlServlet mServlet;
    JsonRpcConnection mRpcConnection;
    HttpServletRequest mHttpRequest;
    CacheBackend mBackend;

    public ControlConnection(ControlServlet servlet, JsonRpcConnection rpcConnection, HttpServletRequest httpRequest) {
        mServlet = servlet;
        mRpcConnection = rpcConnection;
        mHttpRequest = httpRequest;
        mBackend = mServlet.getCacheBackend();
        logCall("connection from " + getRemoteAddress());
    }

    private String getRemoteAddress() {
        String res = mHttpRequest.getParameter("X-Forwarded-For");
        if(res == null) {
            res = mHttpRequest.getRemoteAddr();
        }
        return res;
    }

    private void logCall(String call) {
        LOG.info("[" + mId + "] " + call);
    }

    @Override
    public FileHandles createFileForStorage(String accountId, String contentType, int contentLength) {
        logCall("createFileForStorage (account-id: '" + accountId + "', content-type: '" + contentType + "', content-length: '" + contentLength + "')");

        CacheFile file = mBackend.getByFileId(UUID.randomUUID().toString(), true);

        file.setFileType(CacheFile.TYPE_STORAGE);
        file.setAccountId(accountId);
        file.setContentType(contentType);
        file.setContentLength(contentLength);
        file.setupExpiry(mBackend.getConfiguration().getStorageFileExpiryTime());

        mBackend.checkpoint(file);

        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();

        return handles;
    }

    @Override
    public FileHandles createFileForTransfer(String accountId, String contentType, int contentLength) {
        logCall("createFileForTransfer (account-id: '" + accountId + "', content-type: '" + contentType + "', content-length: '" + contentLength + "')");

        CacheFile file = mBackend.getByFileId(UUID.randomUUID().toString(), true);

        file.setFileType(CacheFile.TYPE_TRANSFER);
        file.setAccountId(accountId);
        file.setContentType(contentType);
        file.setContentLength(contentLength);
        file.setupExpiry(mBackend.getConfiguration().getTransferFileExpiryTime());

        mBackend.checkpoint(file);

        FileHandles handles = new FileHandles();
        handles.fileId = file.getFileId();
        handles.uploadId = file.getUploadId();
        handles.downloadId = file.getDownloadId();

        return handles;
    }

    @Override
    public void deleteFile(String fileId) {
        logCall("deleteFile (file-id: '" + fileId + "')");
        CacheFile file = mBackend.getByFileId(fileId, false);
        if(file != null) {
            file.delete();
        }
    }

    @Override
    public void deleteAccount(String accountId) {
        logCall("deleteAccount (account-id: '" + accountId + "')");
        List<CacheFile> files = mBackend.getFilesByAccount(accountId);
        if(files != null) {
            for(CacheFile file: files) {
                file.delete();
            }
        }
    }

}
