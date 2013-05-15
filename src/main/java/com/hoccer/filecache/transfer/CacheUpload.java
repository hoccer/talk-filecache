package com.hoccer.filecache.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.ByteRange;
import com.hoccer.filecache.model.CacheFile;

/**
 * Active upload to the cache
 * 
 * @author ingo
 */
public class CacheUpload extends CacheTransfer {

    ByteRange byteRange;

    public static final int MIN_LIFETIME = 10;
	public static final int MAX_LIFETIME = 3 * 365 * 24 * 3600;
	
	public CacheUpload(CacheFile file,
					   HttpServletRequest req,
					   HttpServletResponse resp,
                       ByteRange range) {
		super(file, req, resp);
        byteRange = range;
	}
	
	public void perform() throws IOException, InterruptedException {
        // allocate a transfer buffer
		byte[] buffer = BufferCache.takeBuffer();
		
		// determine expiry time
		int expiresIn = MAX_LIFETIME;
		String expiresString = httpRequest.getParameter("expires_in");
		if(expiresString != null) {
			expiresIn = Integer.parseInt(expiresString);
		}
		if(expiresIn < MIN_LIFETIME) {
			expiresIn = MIN_LIFETIME;
		}
		if(expiresIn > MAX_LIFETIME) {
			expiresIn = MAX_LIFETIME;
		}
		cacheFile.setupExpiry(expiresIn);
		
		// determine content type
		String cType = httpRequest.getContentType();
		if(cType == null) {
			cType = "application/octet-stream";
		}
		cacheFile.setContentType(cType);

        transferBegin(Thread.currentThread());
		cacheFile.uploadStarts(this);
		
		try {
			InputStream inStream = httpRequest.getInputStream();
			RandomAccessFile outFile = cacheFile.openForRandomAccess("rw");

			// determine amount of data to send
            int totalRequested = ((int)byteRange.getEnd()) - ((int)byteRange.getStart());

            outFile.setLength(cacheFile.getContentLength());
			outFile.seek(byteRange.getStart());
			
			int totalTransferred = 0;
            int absolutePosition = (int)byteRange.getStart();
			while(totalTransferred < totalRequested) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

				int bytesRead = inStream.read(buffer);
				
				if(bytesRead == -1) {
					break;
				}
				
				outFile.write(buffer, 0, bytesRead);
				
				totalTransferred += bytesRead;
                absolutePosition += bytesRead;
				
				transferProgress(bytesRead);
				
				cacheFile.updateLimit(absolutePosition);
			}
			
			outFile.close();
		
		} catch (IOException e) {
			cacheFile.uploadAborted(this);
			throw e;
		} catch (InterruptedException e) {
            cacheFile.uploadAborted(this);
            throw e;
        } finally {
            transferEnd();
            // return the transfer buffer
            BufferCache.returnBuffer(buffer);
        }

		cacheFile.uploadFinished(this);
	}

}
