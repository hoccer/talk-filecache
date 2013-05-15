package com.hoccer.filecache;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.hoccer.filecache.db.MemoryBackend;
import com.hoccer.filecache.db.OrmliteBackend;
import com.hoccer.filecache.model.CacheFile;

@WebListener
public class ContextListener implements ServletContextListener {
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();
		
		File dataDirectory = null;
		
		// try to find data directory from configuration
		String confDir = ctx.getInitParameter("hoccer.filecache.dataDirectory");
		if(confDir != null) {
			dataDirectory = new File(confDir);
			if(!dataDirectory.isDirectory()) {
				dataDirectory = null;
			}
		}
		
		// else, create and use a subdir of TEMPDIR
		if(dataDirectory == null) {
			File tmpDir = (File)ctx.getAttribute(ServletContext.TEMPDIR);
			dataDirectory = new File(tmpDir, "data");
			dataDirectory.mkdir();
		}

        CacheBackend backend = new MemoryBackend(dataDirectory);
        ctx.setAttribute("backend", backend);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
