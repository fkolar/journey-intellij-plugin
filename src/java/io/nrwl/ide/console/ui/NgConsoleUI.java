package io.nrwl.ide.console.ui;

import com.codebrig.journey.JourneyBrowserView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


/**
 * Main JPanel used for rendering the NGConsoleUI (the WebView). It uses JCEF chromium implementations which is a
 * pretty fast compared JavaFX which I we tried but there is allot of issues with loading svgs, fonts, etcs.
 * <p>
 * Please see https://github.com/CodeBrig/Journey project
 */
public class NgConsoleUI implements Disposable {
  private static final Logger LOG = Logger.getInstance(NgConsoleUI.class);
  /**
   * Layout container to place a webview into the CENTER zone
   */
  private final JPanel myPanel = new JPanel(new BorderLayout());
  /**
   * Object representing a JCEF based Browser build on top of Chromium
   */
  private JourneyBrowserView myBrowser;
  private Map<String, String> myRouteMapping = new HashMap<>();

  private Route myRoute = Route.Generate;


  public NgConsoleUI() {
    myRouteMapping.put(Route.Workspace.name(), "http://www.google.com");
    myRouteMapping.put(Route.Generate.name(), "http://www.apple.com");
    myRouteMapping.put(Route.Tasks.name(), "http://www.seznam.cz");
    myRouteMapping.put(Route.Connect.name(), "https://www.idnes.cz");
    myRouteMapping.put(Route.AffectedProjects.name(), "http://www.hawaia.com");
    myRouteMapping.put(Route.Extensions.name(), "http://www.angular.io");
    myRouteMapping.put(Route.Settings.name(), "http://metaui.io");
  }


  /**
   * First time initiation of the WebView called once Angular Console is started. Here we need to set initial
   * URL, otherwise user sees blank page.
   */
  public void initWebView(Route mode) {
    this.myRoute = mode;
    doInitWebView();
  }

  public JComponent getContent() {
    return myPanel;
  }

  /**
   * Uses <code>SimpleToolWindowPanel</code> to provide us with toolbar and content layout
   */
  public SimpleToolWindowPanel getToolWindowContent() {
    SimpleToolWindowPanel toolPanel = new SimpleToolWindowPanel(true, true);
    ActionManager actionManager = ActionManager.getInstance();
    ActionToolbar actionToolbar = actionManager.createActionToolbar("toolbar",
      (ActionGroup) actionManager.getAction("NGConsole.UI.Toolbar"),
      true);
    toolPanel.setToolbar(actionToolbar.getComponent());
    toolPanel.setContent(getContent());

    return toolPanel;
  }


  public void goToUrl(final String... params) {
    ApplicationManager.getApplication()
      .invokeLater(() -> {
        String url = myRouteMapping.get(myRoute.name());

        LOG.info("Switching to new URL : " + url);
        myBrowser.getCefBrowser().loadURL(url);
      });

    myBrowser.getCefBrowser().reloadIgnoreCache();
  }


  /**
   * Make sure first param of the[params] is port.
   */
  public void goToUrl(Route route) {
    myRoute = route;
    ApplicationManager.getApplication()
      .invokeAndWait(() -> {
        String url = myRouteMapping.get(myRoute.name());

        LOG.info("Switching to new URL : " + url);
        myBrowser.getCefBrowser().loadURL(url);
      });
  }


  /**
   *
   * Executed from NgWorkspaceMonitor inside the DumbService.getInstance(project).runReadActionInSmartMode
   *
   */
  private void doInitWebView() {
    try {
      String url = myRouteMapping.get(myRoute.name());
      myBrowser = new JourneyBrowserView(url);
      myPanel.add(myBrowser, BorderLayout.CENTER);
    } catch (Exception e) {
      LOG.error("Error while initialing WebView. ", e);
    }
  }


  @Override
  public void dispose() {
    if (myBrowser != null) {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        myBrowser.getCefApp().dispose();
        myBrowser = null;
      });
    }
  }


  public static enum Route {
    NewWorkspace,
    Workspace,
    Generate,
    Tasks,
    Extensions,
    Connect,
    AffectedProjects,
    Settings
  }
}
