package com.hoccer.talk.filecache;

import com.google.appengine.api.blobstore.ByteRange;
import com.google.appengine.api.blobstore.RangeFormatException;
import com.hoccer.talk.filecache.model.CacheFile;
import com.hoccer.talk.filecache.transfer.CacheDownload;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/download/*")
public class DownloadServlet extends HttpServlet {

    static Logger log = Logger.getLogger(DownloadServlet.class);

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // get the relevant file
        CacheFile file = getFileForDownload(req, resp);
        // abort if we don't have one
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist");
            log.info("HEAD " + req.getPathInfo() + " " + resp.getStatus() + " file not found");
            return;
        }

        // prepare the response
        ByteRange range = beginGet(file, req, resp);
        if(range == null) {
            log.info("HEAD " + req.getPathInfo() + " " + resp.getStatus() + " invalid range");
            return;
        }
        finishGet(file, req, resp, range);

        log.info("HEAD " + req.getPathInfo() + " " + resp.getStatus() + " found " + file.getFileId() + " range " + range.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get the relevant file
        CacheFile file = getFileForDownload(req, resp);
        // abort if we don't have one
        if(file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File does not exist");
            log.info("GET " + req.getPathInfo() + " " + resp.getStatus() + " file not found");
            return;
        }

        // set response headers
        ByteRange range = beginGet(file, req, resp);
        // abort if there was an error
        if(range == null) {
            log.info("GET " + req.getPathInfo() + " " + resp.getStatus() + " invalid range");
            return;
        }

        // create a transfer object
        CacheDownload download = new CacheDownload(file, range, req, resp);

        // finish response headers
        finishGet(file, req, resp, range);

        log.info("GET " + req.getPathInfo() + " " + resp.getStatus() + " found " + file.getFileId() + " range " + range.toString());

        // perform the download itself
        try {
            log.info("GET " + req.getPathInfo() + " --- download started");
            download.perform();
            log.info("GET " + req.getPathInfo() + " --- download finished");
        } catch (InterruptedException e) {
            log.info("GET " + req.getPathInfo() + " --- download interrupted");
            return;
        }
    }

    private ByteRange beginGet(CacheFile file, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String headRange = req.getHeader("Range");

        // non-ranged requests get a simple OK
        if(headRange == null) {
            if(file.getContentLength() != -1) {
                resp.setContentLength(file.getContentLength());
                return new ByteRange(0, file.getContentLength() - 1);
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

    protected CacheFile getFileForDownload(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        // get all the various things we need
        CacheBackend backend = getCacheBackend();
        String pathInfo = req.getPathInfo();
        String downloadId = pathInfo.substring(1);
        // try to get by download id
        CacheFile file = backend.getByDownloadId(downloadId);
        // if that fails try it as a file id
        if(file == null) {
            file = backend.getByFileId(downloadId, false);
        }
        // err if not found
        if(file == null) {
            return null;
        }
        // err if file is gone
        int fileState = file.getState();
        if(fileState == CacheFile.STATE_EXPIRED || fileState == CacheFile.STATE_DELETED) {
            return null;
        }
        // return
        return file;

    }

    protected CacheBackend getCacheBackend() {
        ServletContext ctx = getServletContext();
        CacheBackend backend = (CacheBackend)ctx.getAttribute("backend");
        return backend;
    }

}
