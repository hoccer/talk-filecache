package com.hoccer.talk.filecache;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.hoccer.talk.filecache.control.ControlServlet;
import com.hoccer.talk.filecache.db.MemoryBackend;
import com.hoccer.talk.filecache.db.OrmliteBackend;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

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

    @Parameter(names="-initdb",
                description = "Initialize database")
    boolean initdb;


    private void run() {
        // load configuration
        CacheConfiguration config = initializeConfiguration();

        // select and instantiate database backend
        CacheBackend db = initializeBackend(config);

        // log about jetty init
        LOG.info("Configuring jetty");

        // create jetty instance
        Server s = new Server(new InetSocketAddress(config.getListenAddress(),
                                                    config.getListenPort()));
        s.setThreadPool(new QueuedThreadPool(config.getServerThreads()));

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
            LOG.info("Configuring from file " + config);
            try {
                FileInputStream configIn = new FileInputStream(config);
                properties = new Properties();
                properties.load(configIn);
            } catch (FileNotFoundException e) {
                LOG.error("Could not load " + config, e);
            } catch (IOException e) {
                LOG.error("Could not load " + config, e);
            }
            // if we could load it then configure using it
            if(properties != null) {
                configuration.configureFromProperties(properties);
            }
        }

        // command line overrides
        LOG.info("Configuring from commandline");
        if(listen != null) {
            configuration.setListenAddress(listen);
        }
        if(port != -1) {
            configuration.setListenPort(port);
        }
        if(initdb) {
            configuration.setOrmliteInitDb(true);
        }

        // return the configuration
        return configuration;
    }

    private CacheBackend initializeBackend(CacheConfiguration configuration) {
        String backend = configuration.getDatabaseBackend();
        LOG.info("Creating backend " + backend);
        if(backend.equals("ormlite")) {
            return new OrmliteBackend(configuration);
        }
        if(backend.equals("memory")) {
            return new MemoryBackend(configuration);
        }
        throw new RuntimeException("Unknown backend: " + backend);
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        CacheMain main = new CacheMain();
        JCommander commander = new JCommander(main, args);
        PropertyConfigurator.configure(main.config);
        main.run();
    }

}
