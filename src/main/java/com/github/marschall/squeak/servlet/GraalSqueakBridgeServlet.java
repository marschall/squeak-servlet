package com.github.marschall.squeak.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;

/**
 * Loads a Squeak image and dispatches all requests to Seaside in the image.
 */
public class GraalSqueakBridgeServlet implements Servlet {
  
  private static final String LANGUAGE = "squeak";

  private volatile ServletConfig config;

  private volatile Context context;

  private volatile Value adpator;

  @Override
  public void init(ServletConfig config) throws ServletException {
    this.config = config;
    this.loadSqueakImage();
  }

  @Override
  public ServletConfig getServletConfig() {
    return this.config;
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    dispatchToSeaside((HttpServletRequest) req, (HttpServletResponse) res);
  }

  @Override
  public String getServletInfo() {
    return "Graal Squeak Bridge Servlet";
  }

  @Override
  public void destroy() {
    this.stopSqueakImage();
    this.config = null;
  }
  
  // Squeak methods

  protected void loadSqueakImage() {
    this.context = Context.newBuilder(LANGUAGE)
        .allowNativeAccess(true)
        .allowEnvironmentAccess(EnvironmentAccess.INHERIT)
        .allowHostAccess(HostAccess.ALL) // Map.Entry methods are not annotated
        .allowIO(true)
        .build();
    this.adpator = this.context.eval(LANGUAGE, "WAServletServerAdaptor new");
  }

  protected void dispatchToSeaside(HttpServletRequest request, HttpServletResponse response) {
    ServletNativeRequest nativeRequest = new ServletNativeRequest(request, response);
    this.adpator.invokeMember("process:", nativeRequest);
  }

  protected void stopSqueakImage() {
    this.context.close();
    this.context = null;
  }

}
