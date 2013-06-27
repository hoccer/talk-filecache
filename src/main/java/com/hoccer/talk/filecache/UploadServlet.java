package com.hoccer.talk.filecache;

import com.google.appengine.api.blobstore.ByteRange;
import com.google.appengine.api.blobstore.RangeFormatException;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.filecache.transfer.CacheUpload;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/upload/*")
public class UploadServlet extends DownloadServlet {

    static Logger log = Logger.getLogger(UploadServlet.class);

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        CacheFile file = getFileForUpload(req, resp);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist");
            return;
        }

        log.info("PUT " + req.getPathInfo() + " found " + file.getFileId());

        ByteRange range = beginPut(file, req, resp);
        if(range == null) {
            return;
        }

        CacheUpload upload = new CacheUpload(file, req, resp, range);

        finishPut(file, req, resp);

        try {
            upload.perform();
        } catch (InterruptedException e) {
            log.info("upload interrupted: " + req.getPathInfo());
            return;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("delete request: " + req.getPathInfo());

        CacheBackend backend = getCacheBackend();

        CacheFile file = getFileForUpload(req, resp);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return;
        }

        log.info("DELETE " + req.getPathInfo() + " found " + file.getFileId());

        file.delete();
    }

    private ByteRange beginPut(CacheFile file, HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        String headContentLength = req.getHeader("Content-Length");
        String headContentRange = req.getHeader("Content-Range");

        // content length is mandatory
        if(headContentLength == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length not specified");
            return null;
        }

        // parse content length
        int contentLength = Integer.parseInt(headContentLength);

        // verify the content length and try to determine file size
        if(file.getContentLength() == -1) {
            if(headContentRange == null) {
                file.setContentLength(contentLength);
            } else {
                // XXX we should take the length from the third field of headContentRange
            }
        } else {
            if(contentLength > file.getContentLength()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length to large for file");
                return null;
            }
        }

        // non-ranged requests get a simple OK
        if(headContentRange == null) {
            return new ByteRange(0, contentLength);
        }

        // parse the byte range
        ByteRange range = null;
        try {
            range = ByteRange.parseContentRange(headContentRange);
        } catch (RangeFormatException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad content range");
            return null;
        }

        // try again to determine the file size
        if(file.getContentLength() == -1) {
            if(range.hasTotal()) {
                file.setContentLength((int)range.getTotal());
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not determine file size");
                return null;
            }
        }

        // fill in the end if the client didn't specify
        if(!range.hasEnd()) {
            range = new ByteRange(range.getStart(), file.getContentLength() - 1);
        }

        // verify that it makes sense
        if(range.getStart() > range.getEnd()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad range: start > end");
            return null;
        }
        if(range.getStart() < 0 || range.getEnd() < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad range: start or end < 0");
            return null;
        }
        if(range.getStart() > file.getContentLength() || range.getEnd() > file.getContentLength()) {
            resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return null;
        }

        // determine the length of the chunk
        long length = range.getEnd() - range.getStart() + 1;
        if(length != contentLength) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length does not match range");
            return null;
        }

        return range;
    }

    private void finishPut(CacheFile file, HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentLength(0);
        log.debug("finishing put with range to limit " + file.getLimit() + " length " + file.getContentLength());
        if(file.getLimit() > 0) {
            resp.setHeader("Range", "bytes=0-" + (file.getLimit() - 1) + "/" + file.getContentLength());
        }
        if(file.getLimit() == file.getContentLength()) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(308); // "resume incomplete"
        }
    }

    private CacheFile getFileForDownload(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get all the various things we need
        CacheBackend backend = getCacheBackend();
        String pathInfo = req.getPathInfo();
        String uploadId = pathInfo.substring(1);
        // try to get by download id
        CacheFile file = backend.getByUploadId(uploadId);
        // if that fails try it as a file id
        if(file == null) {
            file = backend.getByFileId(uploadId, false);
        }
        // err if not found
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return null;
        }
        // err if file is gone
        int fileState = file.getState();
        if(fileState == CacheFile.STATE_EXPIRED
                || fileState == CacheFile.STATE_DELETED) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return null;
        }
        // return
        return file;

    }

    private CacheFile getFileForUpload(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get the various things we need
        CacheBackend backend = getCacheBackend();
        String pathInfo = req.getPathInfo();
        String uploadId = pathInfo.substring(1);
        // try to get by upload id
        CacheFile file = backend.getByUploadId(uploadId);
        // if not found try it as a file id
        if(file == null) {
            file = backend.getByFileId(uploadId, true);
        }
        // err if not found
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return null;
        }
        // err if file is gone
        int fileState = file.getState();
        if(fileState == CacheFile.STATE_EXPIRED
                || fileState == CacheFile.STATE_DELETED) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return null;
        }
        // return
        return file;
    }

    private CacheBackend getCacheBackend() {
        ServletContext ctx = getServletContext();
        CacheBackend backend = (CacheBackend)ctx.getAttribute("backend");
        return backend;
    }

}
