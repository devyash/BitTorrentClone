

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.*;

public class LogHelper {
    private static final String CONF = "/edu/ufl/cise/cnt5106c/conf/logger.properties";
    private static final LogHelper logHelper = new LogHelper (Logger.getLogger("BitTorrentClone"));

    private final Logger myLogger;

    private LogHelper (Logger log) {
        myLogger = log;
    }
    static {
        InputStream in=null;
        try{
            in = LogHelper.class.getResourceAsStream(CONF);
            LogManager.getLogManager().readConfiguration(in);
        }
        catch (Exception e) {
            System.err.println(LogHelper.stackTraceToString(e));
            System.exit(1);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ex) {
            }
        }
    }

    public static LogHelper getLogger () {
        return logHelper;
    }

    public synchronized void info (String message) {
        myLogger.log (Level.INFO, message);
    }

    public synchronized void debug (String message) {
        myLogger.log(Level.FINE, message);
    }

    public static void configure(int peerId) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Properties properties = new Properties();
        properties.load(LogHelper.class.getResourceAsStream(CONF));
        String format = "java.util.logging.FileHandler.formatter";
        String level = "java.util.logging.FileHandler.level";
        Handler handler = new FileHandler ("log_peer_" + peerId + ".log");
        handler.setFormatter((Formatter) Class.forName(properties.getProperty(format)).newInstance());
        handler.setLevel(Level.parse(properties.getProperty(level)));
        logHelper.myLogger.addHandler(handler);
    }

    private static String stackTraceToString (Throwable t) {
        final Writer stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public synchronized void warning (String message) {
        myLogger.log(Level.WARNING, message);
    }

    public synchronized void warning (Throwable exception) {
        myLogger.log(Level.WARNING, stackTraceToString (exception));
    }

    public static String getNeighborsAsString (Collection<AdjacentPeers> neighbors) {
        String s = "";
        boolean isFirst = true;

        for (Iterator<AdjacentPeers> iterator = neighbors.iterator(); iterator.hasNext();) {
            int type = iterator.next().peer_Id;
            s = isFirst != true ? ", " :"";
            if(isFirst == true)
                isFirst = false;
            s += type;
        }
        return s;
    }

    public synchronized void error (String message) {
        myLogger.log(Level.SEVERE, message);
    }

    public synchronized void error (Throwable exception) {
        myLogger.log(Level.SEVERE, stackTraceToString (exception));
    }
}
