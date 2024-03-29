package com.hoccer.talk.filecache.transfer;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.talk.filecache.model.CacheFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Active download from the cache
 *
 * @author ingo
 */
public class CacheDownload extends CacheTransfer {

    ByteRange byteRange;

    public CacheDownload(CacheFile file, ByteRange range,
                         HttpServletRequest req,
                         HttpServletResponse resp) {
        super(file, req, resp);
        byteRange = range;
    }

    public void perform() throws IOException, InterruptedException {
        // allocate a transfer buffer
        byte[] buffer = BufferCache.takeBuffer();

        // set content type
        httpResponse.setContentType(cacheFile.getContentType());

        // start the download
        transferBegin(Thread.currentThread());
        cacheFile.downloadStarts(this);

        try {
            // open/get streams
            OutputStream outStream = httpResponse.getOutputStream();
            RandomAccessFile inFile = cacheFile.openForRandomAccess("r");

            // determine amount of data to send
            int totalRequested = ((int) byteRange.getEnd()) - ((int) byteRange.getStart()) + 1;

            // seek forward to the requested range
            inFile.seek(byteRange.getStart());

            // loop until done
            int totalTransferred = 0;
            int absolutePosition = (int) byteRange.getStart();
            int absoluteEnd = absolutePosition + totalRequested;
            while (totalTransferred < totalRequested) {
                // abort on thread interrupt
                if (Thread.interrupted()) {
                    throw new InterruptedException("Transfer thread interrupted");
                }

                // abort when file becomes invalid
                if (!cacheFile.isAlive()) {
                    throw new InterruptedException("File no longer available");
                }

                // determine how much to transfer
                int bytesWanted = Math.min(totalRequested - totalTransferred, buffer.length);

                // determine current limit
                int limit = cacheFile.getLimit();
                int absoluteLimit = Math.min(limit, absoluteEnd);

                // wait for availability
                while ((absoluteLimit != cacheFile.getContentLength())
                        && (limit < (absolutePosition + bytesWanted))) {
                    Thread.sleep(100);
                    if (!cacheFile.waitForData(absoluteLimit + bytesWanted)) {
                        throw new InterruptedException("File no longer available");
                    }
                    limit = cacheFile.getLimit();
                    absoluteLimit = Math.min(limit, absoluteEnd);
                }

                // read data from file
                int bytesRead = inFile.read(buffer, 0, bytesWanted);
                if (bytesRead == -1) {
                    break; // XXX
                }

                // write to http output stream
                outStream.write(buffer, 0, bytesRead);

                // account for what we did
                totalTransferred += bytesRead;
                absolutePosition += bytesRead;
                transferProgress(bytesRead);
            }

            // close file stream
            inFile.close();

        } catch (InterruptedException e) {
            cacheFile.downloadAborted(this);
            // rethrow to finish the http request
            throw e;
        } catch (IOException e) {
            // notify the file of the abort
            cacheFile.downloadAborted(this);
            // rethrow to finish the http request
            throw e;
        } finally {
            // always finish the rate estimator
            transferEnd();
            // return the transfer buffer
            BufferCache.returnBuffer(buffer);
        }

        // we are done, tell everybody
        cacheFile.downloadFinished(this);
    }

}
