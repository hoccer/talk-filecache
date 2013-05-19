package com.hoccer.talk.filecache;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.filecache.control.ControlServlet;
import com.hoccer.talk.filecache.db.MemoryBackend;
import com.hoccer.talk.filecache.db.OrmliteBackend;
import com.hoccer.talk.logging.HoccerLoggers;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.websocket.WebSocketHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.logging.Level;

public class CacheMain {

    private static final Logger LOG = Logger.getLogger(CacheMain.class);

    @Parameter(names={"-c", "-config"},
               description = "Configuration file to use")
    String config = null;

    @Parameter(names={"-l", "-listen"},
               description = "Address/host to listen on")
    String listen = null;

    @Parameter(names={"-p", "-port"},
               description = "Port to listen on")
    int port = -1;

    private void run() {
        // load configuration
        CacheConfiguration config = initializeConfiguration();

        // select and instantiate database backend
        CacheBackend db = initializeDatabase(config);

        // log about jetty init
        LOG.info("Configuring jetty");

        // create jetty instance
        Server s = new Server(new InetSocketAddress(config.getListenAddress(),
                                                    config.getListenPort()));

        // create servlet context
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setAttribute("backend", db);
        context.addServlet(StatusServlet.class, "/status");
        context.addServlet(ControlServlet.class, "/control");
        context.addServlet(DownloadServlet.class, "/download/*");
        context.addServlet(UploadServlet.class, "/upload/*");

        // set root handler of the server
        s.setHandler(context);

        // start backend
        LOG.info("Starting backend");
        db.start();

        // run and stop when interrupted
        try {
            LOG.info("Starting server");
            s.start();
            s.join();
            LOG.info("Server has quit");
        } catch (Exception e) {
            LOG.error("Exception in server", e);
        }
    }

    private CacheConfiguration initializeConfiguration() {
        LOG.info("Determining configuration");
        CacheConfiguration configuration = new CacheConfiguration();

        // configure from file
        if(config != null) {
            Properties properties = null;
            // load the property file
            LOG.info("Loading configuration from file " + config);
            try {
                FileInputStream configIn = new FileInputStream(config);
                properties = new Properties();
                properties.load(configIn);
            } catch (FileNotFoundException e) {
                LOG.error("Could not load configuration", e);
            } catch (IOException e) {
                LOG.error("Could not load configuration", e);
            }
            // if we could load it then configure using it
            if(properties != null) {
                configuration.configureFromProperties(properties);
            }
        }

        // command line overrides
        if(listen != null) {
            configuration.setListenAddress(listen);
        }
        if(port != -1) {
            configuration.setListenPort(port);
        }

        // return the configuration
        return configuration;
    }

    private CacheBackend initializeDatabase(CacheConfiguration config) {
        String backend = config.getDatabaseBackend();
        LOG.info("Creating backend " + backend);
        if(backend.equals("ormlite")) {
            return new OrmliteBackend(config);
        }
        if(backend.equals("memory")) {
            return new MemoryBackend(new File(config.getDataDirectory()));
        }
        throw new RuntimeException("Unknown database backend: " + backend);
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        CacheMain main = new CacheMain();
        JCommander commander = new JCommander(main, args);
        PropertyConfigurator.configure(main.config);
        main.run();
    }

}
