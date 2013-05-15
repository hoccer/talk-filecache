package com.hoccer.filecache.control;

public interface ICacheControl {

    void deleteAccount(String accountId);

    FileHandles createFileForStorage(String accountId, int fileSize);

    FileHandles createFileForTransfer(String accountId, int fileSize);

    void deleteFile(String fileId);

    static class FileHandles {
        String fileId;
        String uploadId;
        String downloadId;
    }

}
