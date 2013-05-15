package com.hoccer.filecache;

import com.google.appengine.api.blobstore.ByteRange;
import com.google.appengine.api.blobstore.RangeFormatException;
import com.hoccer.filecache.model.CacheFile;
import com.hoccer.filecache.transfer.CacheDownload;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@WebServlet(urlPatterns = "/download/*")
public class DownloadServlet extends HttpServlet {

    static Logger log = Logger.getLogger(DownloadServlet.class.getSimpleName());

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("status request: " + req.getPathInfo());

        // get the relevant file
        CacheFile file = getFileForRequest(req, resp);
        // abort if we don't have one
        if(file == null) {
            return;
        }

        // prepare the response
        ByteRange range = beginGet(file, req, resp);
        if(range == null) {
            return;
        }
        finishGet(file, req, resp, range);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        log.info("download starts: " + req.getPathInfo());

        // get the relevant file
        CacheFile file = getFileForRequest(req, resp);
        // abort if we don't have one
        if(file == null) {
            return;
        }

        // set response headers
        ByteRange range = beginGet(file, req, resp);
        // abort if there was an error
        if(range == null) {
            return;
        }

        // create a transfer object
        CacheDownload download = new CacheDownload(file, range, req, resp);

        // perform the download itself
        try {
            download.perform();
        } catch (InterruptedException e) {
            return;
        }

        finishGet(file, req, resp, range);

        log.info("download finished: " + req.getPathInfo());
    }

    private ByteRange beginGet(CacheFile file, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String headRange = req.getHeader("Range");

        // non-ranged requests get a simple OK
        if(headRange == null) {
            if(file.getContentLength() != -1) {
                resp.setContentLength(file.getContentLength());
                return new ByteRange(0, file.getContentLength());
            } else {
                return new ByteRange(0);
            }
        }

        // parse the byte range
        ByteRange range = null;
        try {
            range = ByteRange.parse(headRange);
        } catch (RangeFormatException ex) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad range");
            return null;
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

        // return the range to be transferred
        return range;
    }

    private void finishGet(CacheFile file, HttpServletRequest req, HttpServletResponse resp, ByteRange range) {
        // determine the length of the response
        long length = range.getEnd() - range.getStart() + 1;

        // fill out response headers
        if(range.getStart() == 0 && range.getEnd() == (file.getContentLength() - 1)) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        resp.setContentLength((int)length);
        resp.setHeader("Content-Range",
                "bytes " + range.getStart() +
                        "-" + range.getEnd() +
                        "/" + file.getContentLength());
    }

    private CacheFile getFileForRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CacheBackend backend = getCacheBackend();

        // get the file and check that we have one
        CacheFile file = backend.forPathInfo(req.getPathInfo(), false);
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return null;
        }

        // check the state of the file
        int fileState = file.getState();
        if(fileState == CacheFile.STATE_EXPIRED
                || fileState == CacheFile.STATE_ABANDONED
                || fileState == CacheFile.STATE_NEW) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "File does not exist");
            return null;
        }

        // return the file
        return file;
    }

    private CacheBackend getCacheBackend() {
        ServletContext ctx = getServletContext();
        CacheBackend backend = (CacheBackend)ctx.getAttribute("backend");
        return backend;
    }

}
