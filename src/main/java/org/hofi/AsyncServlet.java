package org.hofi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

@WebServlet(urlPatterns={"/async"}, asyncSupported=true)
public class AsyncServlet extends HttpServlet {

  private static Logger logger = LogManager.getLogger(AsyncServlet.class);

  public void doPost(HttpServletRequest req, HttpServletResponse resp) {

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/event-stream");
    resp.setCharacterEncoding("UTF-8");

    final AsyncContext acontext = req.startAsync();
    final Timer timer = new Timer();
    acontext.setTimeout(900000000);

    logger.debug("subscriber: " +       req.getParameter("subscriber"));
    logger.debug("localHostInfo: " +    req.getParameter("localHostInfo"));
    logger.debug("endTag: " +           req.getParameter("endTag"));
    logger.debug("keepAliveSeconds: " + req.getParameter("keepAliveSeconds"));
    logger.debug("localUser: " +        req.getParameter("localUser"));

    final int keepAliveMillis = Integer.parseInt(req.getParameter("keepAliveSeconds")) * 1000;
    final int numberOfTasks = (int)acontext.getTimeout() / keepAliveMillis;

    logger.debug("numberOfTasks: " + numberOfTasks);

    acontext.start(new Runnable() {
      PrintWriter writer;
      public void run() {
        try {
          writer = acontext.getResponse().getWriter();
          writer.write("\n");

          TimerTask task = new TimerTask() {
            int taskCounter = 0;
            @Override
            public void run() {
              try {
                logger.debug("send keep alive: <KeepAliveMessage></KeepAliveMessage>");
                writer.write("<KeepAliveMessage></KeepAliveMessage>\nboundary-end\n");
                writer.flush();
              }
              catch(Exception e) {
                logger.debug("client disconnected");
                timer.cancel();
              }
              taskCounter++;
              if (taskCounter == numberOfTasks) {
                terminate();
              }
            }
            private void terminate() {
              logger.debug("terminated gracefully");
              writer.close();
              timer.cancel();
              acontext.complete();
            }
          };
          timer.schedule(task, 0, keepAliveMillis);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }
}