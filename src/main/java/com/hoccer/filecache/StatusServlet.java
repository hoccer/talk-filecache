package com.hoccer.filecache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hoccer.filecache.model.CacheFile;
import com.hoccer.filecache.transfer.CacheDownload;
import com.hoccer.filecache.transfer.CacheUpload;

@WebServlet(urlPatterns={"/status"})
public class StatusServlet extends HttpServlet {

    CacheBackend getBackendFromRequest(HttpServletRequest req) {
        ServletContext ctx = req.getServletContext();
        CacheBackend backend = (CacheBackend)ctx.getAttribute("backend");
        return backend;
    }

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

        CacheBackend backend = getBackendFromRequest(req);

		resp.setContentType("text/plain; charset=UTF-8");
		
		OutputStream s = resp.getOutputStream();
		OutputStreamWriter w = new OutputStreamWriter(s, "UTF-8");
		
		w.write(">>>>>>>>>>>>>>> Hoccer Filecache <<<<<<<<<<<<<<<\n\n");
		
		List<CacheFile> allFiles = backend.getAll();
		
		Collections.sort(allFiles, getSorting());
		
		w.write("Active files (" + allFiles.size() + "):\n");
		for (CacheFile f : allFiles) {			
			w.write(" File " + f.getFileId()
					+ " type " + f.getContentType()
					+ " length " + f.getContentLength()
					+ "\n");
			
			w.write("  State " + f.getStateString()
					+ " limit " + f.getLimit()
					+ "\n");
			w.write("  Expires " + f.getExpiryTime() + "\n");
			
			CacheUpload upload = f.getUpload();
			if(upload != null) {
				w.write("  Upload"
						+ " from " + upload.getRemoteAddr()
						+ " rate " + Math.round(upload.getRate()) / 1000.0 + " kB/s"
						+ " agent " + upload.getUserAgent()
						+ "\n");
			}
			
			Vector<CacheDownload> downloads = f.getDownloads();
			for (CacheDownload d : downloads) {
				w.write("  Download"
						+ " from " + d.getRemoteAddr()
						+ " rate " + Math.round(d.getRate()) / 1000.0 + " kB/s"
						+ " agent " + d.getUserAgent()
						+ "\n");
			}
		}
		
		w.close();
	}
	
	private Comparator<CacheFile> getSorting() {
		return new Comparator<CacheFile>() {
			@Override
			public int compare(CacheFile o1, CacheFile o2) {
				int n1 = o1.getNumDownloads();
				int n2 = o2.getNumDownloads();
				
				if(n1 < n2) {
					return +1;
				} else if (n1 > n2) {
					return -1;
				}
				
				int s1 = o1.getState();
				int s2 = o2.getState();
				
				if(s1 < s2) {
					return -1;
				} else if(s1 > s2) {
					return +1;
				}
				
				return 0;
			}
		};
	}

}
