package com.hoccer.filecache;

import com.google.appengine.api.blobstore.ByteRange;
import com.google.appengine.api.blobstore.RangeFormatException;
import com.hoccer.filecache.model.CacheFile;
import com.hoccer.filecache.transfer.CacheUpload;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@WebServlet(urlPatterns = "/upload/*")
public class UploadServlet extends HttpServlet {

    static Logger log = Logger.getLogger(UploadServlet.class.getSimpleName());

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        log.info("upload starts: " + req.getPathInfo());

        CacheBackend backend = getCacheBackend();

        CacheFile file = backend.forPathInfo(req.getPathInfo(), true);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File can not exist in cache");
            return;
        }

        ByteRange range = beginPut(file, req, resp);
        if(range == null) {
            return;
        }

        CacheUpload upload = new CacheUpload(file, req, resp, range);

        try {
            upload.perform();
        } catch (InterruptedException e) {
            return;
        }

        finishPut(file, req, resp);

        log.info("upload finished: " + req.getPathInfo());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("delete request: " + req.getPathInfo());

        CacheBackend backend = getCacheBackend();

        CacheFile file = backend.forPathInfo(req.getPathInfo(), false);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return;
        }

        backend.remove(file);
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
        resp.setHeader("Range", "bytes 0-" + file.getLimit() + "/" + file.getContentLength());
        if(file.getLimit() == file.getContentLength()) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // "resume incomplete"
        }
    }

    private CacheBackend getCacheBackend() {
        ServletContext ctx = getServletContext();
        CacheBackend backend = (CacheBackend)ctx.getAttribute("backend");
        return backend;
    }

}
