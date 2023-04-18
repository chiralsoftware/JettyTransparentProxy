package chiralsoftware.proxyapp;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * The purpose of this servlet is to create an executor needed by the Jetty proxy servlet. It does seem to require
 * a mapping or else init doesn't get called. It implements no HTTP methods so calling the mapping does nothing.
 */
@WebServlet(name = "InitializerServlet", urlPatterns = {"/InitializerServlet"}, loadOnStartup = 1)
public final class InitializerServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(InitializerServlet.class.getName());

    private static final String executorAttribute = "org.eclipse.jetty.server.Executor";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOG.info("and this is the other override call");
        getServletContext().setAttribute(executorAttribute, Executors.newCachedThreadPool());
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
    
    
    @Override
    public void destroy() {
        LOG.info("Shutting down executor");
        if (getServletContext().getAttribute(executorAttribute) instanceof ThreadPoolExecutor e) e.shutdown();
    }

}
