package chiralsoftware.proxyapp;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this servlet is to create an executor needed by the Jetty proxy servlet. It does seem to require
 * a mapping or else init doesn't get called. It implements no HTTP methods so calling the mapping does nothing.
 */
@WebServlet(name = "InitializerServlet", urlPatterns = {"/InitializerServlet"}, loadOnStartup = 1)
public final class InitializerServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(InitializerServlet.class.getName());
    
    private static final org.slf4j.Logger _log = LoggerFactory.getLogger("ProxyServlet$TransparentProxy.TransparentProxy");

    private static final String executorAttribute = "org.eclipse.jetty.server.Executor";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOG.info("and this is the other override call");
        getServletContext().setAttribute(executorAttribute, Executors.newCachedThreadPool());
        if(_log == null) {
            LOG.warning("there was no slf4j logger found!");
        } else {
            LOG.info("I will send some messages on slf4j , you should see them");
            _log.debug("here is a debug slf4j");
            _log.info("here is an INFO slf4j");
            _log.warn("and here is a warning level message");
            _log.error("this is an error message");
            LOG.info("I sent some messages on slf4j , you should see them");
        }
    }

    /**
     * For some reason, the init() call does not get called on load, but the
     * init(ServletConfig) call does
     */
//    @Override
//    public void init() {
//        LOG.info("Initializing the context with an Executor");
////        getServletContext().setAttribute(executorAttribute, Executors.newCachedThreadPool());
//    }
    
    /** Delete this once it is working */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        _log.info("i'm in the doGet method");
        final Map<String,? extends ServletRegistration> registrations = getServletContext().getServletRegistrations();
        final PrintWriter writer = response.getWriter();
        writer.write("Hello this is the get method!\n\n");
        for(Map.Entry me : registrations.entrySet()) {
            _log.info("got map entry: " + me.getKey() + " which has registration: " + me.getValue());
            writer.write("got map entry: " + me.getKey() + " which has registration: " + me.getValue() + "\n");
        }
        final ServletRegistration sr = registrations.get("TransparentProxy");
        writer.write("The init params are: " + sr.getInitParameters().entrySet().stream().map(me -> me.getKey() + ": " + me.getValue()).collect(joining(", ")));
    }
    
    @Override
    public void destroy() {
        LOG.info("Shutting down executor");
        if (getServletContext().getAttribute(executorAttribute) instanceof ThreadPoolExecutor e) e.shutdown();
    }

}
