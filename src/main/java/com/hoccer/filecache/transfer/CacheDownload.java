package com.hoccer.filecache.transfer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.filecache.model.CacheFile;

/**
 * Active download from the cache
 * 
 * @author ingo
 */
public class CacheDownload extends CacheTransfer {

    ByteRange byteRange;
	
	public CacheDownload(CacheFile file, ByteRange range,
						 HttpServletRequest req,
						 HttpServletResponse resp)
	{
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
            int totalRequested = ((int)byteRange.getEnd()) - ((int)byteRange.getStart());

            // seek forward to the requested range
			inFile.seek(byteRange.getStart());

            // loop until done
            int totalTransferred = 0;
            int absolutePosition = (int)byteRange.getStart();
            int absoluteEnd = absolutePosition + totalRequested;
            while(totalTransferred < totalRequested) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // determine current limit
                int absoluteLimit = Math.min(cacheFile.getLimit(), absoluteEnd);
                // determine how much to transfer
                int bytesWanted = Math.min(totalRequested - totalTransferred, buffer.length);

                // wait for availability
                int limit = cacheFile.getLimit();
                while (limit < (absolutePosition + bytesWanted)){
                    Thread.sleep(100);
                    cacheFile.waitForData(absoluteLimit + bytesWanted);
                    limit = cacheFile.getLimit();
                }

                // read data from file
                int bytesRead = inFile.read(buffer, 0, bytesWanted);
                if(bytesRead == -1) {
                    break; // XXX
                }

                // write to http output stream
                outStream.write(buffer, 0, bytesRead);

                // account for what we did
                totalTransferred += bytesRead;
                absolutePosition += bytesRead;
                transferProgress(totalTransferred);
            }

            // close file stream
			inFile.close();
			
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
