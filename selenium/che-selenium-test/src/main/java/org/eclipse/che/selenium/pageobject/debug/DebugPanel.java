/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.pageobject.debug;

import static java.lang.String.format;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.ELEMENT_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.LOAD_PAGE_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.MINIMUM_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.REDRAW_UI_ELEMENTS_TIMEOUT_SEC;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.action.ActionsFactory;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.intelligent.CommandsExplorer;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page Object for Debugger panel.
 *
 * @author Musienko Maxim
 * @author Oleksandr Andriienko
 */
@Singleton
public class DebugPanel {

  private static final Logger LOG = LoggerFactory.getLogger(DebugPanel.class);

  private SeleniumWebDriver seleniumWebDriver;
  private Loader loader;
  private CodenvyEditor editor;
  private CommandsExplorer commandsExplorer;
  private ActionsFactory actionsFactory;

  @Inject
  public DebugPanel(
      SeleniumWebDriver seleniumWebDriver,
      Loader loader,
      CodenvyEditor editor,
      CommandsExplorer commandsExplorer,
      ActionsFactory actionsFactory) {
    this.seleniumWebDriver = seleniumWebDriver;
    this.loader = loader;
    this.editor = editor;
    this.commandsExplorer = commandsExplorer;
    this.actionsFactory = actionsFactory;
    PageFactory.initElements(seleniumWebDriver, this);
  }

  private interface Locators {
    String DEBUGGER_PANEL_TAB = "gwt-debug-partButton-Debug";
    String FRAMES_LIST_ID = "gwt-debug-debugger-frames-list";
    String THREADS_LIST_ID = "gwt-debug-debugger-threads-list";
    String VARIABLES_TREE_ID = "gwt-debug-debugger-tree";
    String VARIABLES_TREE_SELECT_NODE = "//div[@id='" + VARIABLES_TREE_ID + "']//div[text()='%s']";
  }

  private interface LocatorsTextAreaDialogWindow {
    String AGREE_BUTTON = "debugger-textarea-dialog-agree-btn";
    String CANCEL_BTN = "debugger-textarea-dialog-cancel-btn";
    String TEXTAREA = "gwt-debug-value-text-area";
  }

  public interface DebuggerActionButtons {
    String RESUME_BTN_ID = "gwt-debug-ActionButton/resumeExecution-true";
    String STEP_INTO = "gwt-debug-ActionButton/stepInto-true";
    String STEP_OVER = "gwt-debug-ActionButton/stepOver-true";
    String STEP_OUT = "gwt-debug-ActionButton/stepOut-true";
    String BTN_DISCONNECT = "gwt-debug-ActionButton/disconnectDebug-true";
    String REMOVE_ALL_BREAKPOINTS = "gwt-debug-ActionButton/null-true";
    String EVALUATE_EXPRESSIONS = "gwt-debug-ActionButton/evaluateExpression-true";

    String CHANGE_DEBUG_TREE_NODE = "gwt-debug-ActionButton/editDebugVariable-true";
    String ADD_WATCH_EXPRESSION = "gwt-debug-ActionButton/addWatchExpression-true";
    String REMOVE_WATCH_EXPRESSION = "gwt-debug-ActionButton/removeWatchExpression-true";
  }

  private interface BreakpointsPanel {
    String ID = "gwt-debug-debugger-breakpointsPanel";
    String BREAKPOINT_ITEM = "//div[@id='gwt-debug-debugger-breakpointsPanel']//td[text()='%s']";
    String CONTEXT_MENU = "gwt-debug-contextMenu/breakpointSettings";
  }

  private interface BreakpointConfigurationWindow {
    String BREAKPOINT_CONDITION_TEXT =
        "//div[@id='gwt-debug-breakpoint-configuration-window']//textarea[@id='gwt-debug-breakpoint-condition-text']";
    String APPLY_BTN =
        "//div[@id='gwt-debug-breakpoint-configuration-window']//button[@id='gwt-debug-apply-btn']";
  }

  @FindBy(id = BreakpointsPanel.ID)
  WebElement breakpointPanel;

  @FindBy(id = Locators.DEBUGGER_PANEL_TAB)
  WebElement debuggerTab;

  @FindBy(id = Locators.VARIABLES_TREE_ID)
  WebElement debuggerTree;

  @FindBy(id = LocatorsTextAreaDialogWindow.TEXTAREA)
  WebElement textAreaForm;

  @FindBy(id = LocatorsTextAreaDialogWindow.AGREE_BUTTON)
  WebElement saveTextAreaDialogBtn;

  @FindBy(id = LocatorsTextAreaDialogWindow.CANCEL_BTN)
  WebElement cancelTextAreaDialogChangesBtn;

  @FindBy(id = Locators.FRAMES_LIST_ID)
  WebElement frames;

  @FindBy(id = Locators.THREADS_LIST_ID)
  WebElement threads;

  /** Wait while debugger panel will be clear for all breakpoints */
  public void waitWhileAllBreakPointsOnEditorPanelDisapper() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(ExpectedConditions.invisibilityOfElementLocated(cssSelector("div.breakpoint")));
  }

  /**
   * Wait specified content in the IDE breakpoint panel
   *
   * @param content
   */
  public void waitContentInBreakPointPanel(final String content) {
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(
            (WebDriver webDriver) -> {
              return breakpointPanel.getText().contains(content);
            });
  }

  /** Wait disappearance any breakpoints from debugger breakpoints panel */
  public void waitBreakPointsPanelIsEmpty() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            (WebDriver webDriver) -> {
              return breakpointPanel.getText().isEmpty();
            });
  }

  /** Wait while the Variables panel appears */
  public void waitVariablesPanel() {
    new WebDriverWait(seleniumWebDriver, 20).until(ExpectedConditions.visibilityOf(debuggerTree));
  }

  /**
   * Wait expected text in variable panel
   *
   * @param text expected text
   */
  public void waitTextInVariablesPanel(final String text) {
    waitVariablesPanel();
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until((ExpectedCondition<Boolean>) (webDriver -> debuggerTree.getText().contains(text)));
  }

  /**
   * Wait text {@code text} is not present in variable panel
   *
   * @param text expected text
   */
  public void waitTextIsNotPresentInVariablesPanel(final String text) {
    waitVariablesPanel();
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until((ExpectedCondition<Boolean>) (webDriver -> !debuggerTree.getText().contains(text)));
  }

  /** Select node in debugger tree by node {@code text} */
  public void selectNodeInDebuggerTree(String nodeText) {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            visibilityOfElementLocated(
                xpath(String.format(Locators.VARIABLES_TREE_SELECT_NODE, nodeText))));
    seleniumWebDriver
        .findElement(xpath(String.format(Locators.VARIABLES_TREE_SELECT_NODE, nodeText)))
        .click();
  }

  /**
   * Click on specific button on Debugger panel
   *
   * @param buttonIdLocator use interface DebuggerActionButtons for select buttonIdLocator
   */
  public void clickOnButton(String buttonIdLocator) {
    loader.waitOnClosed();
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(id(buttonIdLocator)));
    seleniumWebDriver.findElement(id(buttonIdLocator)).click();
    loader.waitOnClosed();
  }

  /** Wait appearance text area form */
  public void waitAppearTextAreaForm() {
    new WebDriverWait(seleniumWebDriver, 20).until(ExpectedConditions.visibilityOf(textAreaForm));
  }

  /** Wait disappear text area form */
  public void waitDisappearTextAreaForm() {
    new WebDriverWait(seleniumWebDriver, 20)
        .until(
            ExpectedConditions.invisibilityOfElementLocated(
                xpath(LocatorsTextAreaDialogWindow.TEXTAREA)));
  }

  /**
   * Clear text from text area dialog and type new value
   *
   * @param value new value
   */
  public void typeNewValueInTheDialog(String value) {
    waitAppearTextAreaForm();
    textAreaForm.clear();
    textAreaForm.sendKeys(value);
  }

  /**
   * Clear text area, type new value, save changes by clicking agree button and close form.
   *
   * @param newValue new value to save
   */
  public void typeAndSaveTextAreaDialog(String newValue) {
    typeNewValueInTheDialog(newValue);
    saveTextAreaDialogBtn.click();
    waitDisappearTextAreaForm();
    loader.waitOnClosed();
  }

  /**
   * Type user expression to the IDE Enter an expression field
   *
   * @param expression user expression
   */
  public void typeEvaluateExpression(String expression) {
    String exprFieldLocator =
        "//div[text()='Enter an expression:']/parent::div/following-sibling::div/input";
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(exprFieldLocator)))
        .clear();
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(exprFieldLocator)))
        .sendKeys(expression);
  }

  /**
   * Wait expected value in the Result field after evaluating
   *
   * @param expVal exoected value
   */
  public void waitExpectedResultInEvaluateExpression(final String expVal) {
    final String locator = "//div[text()='Result:']/parent::div/following-sibling::div/textarea";
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(
            (ExpectedCondition<Boolean>)
                webDriver -> {
                  return seleniumWebDriver
                      .findElement(xpath(locator))
                      .getAttribute("value")
                      .equals(expVal);
                });
  }

  /** Click on evaluate exp. button */
  public void clickEvaluateBtn() {
    String locator = "//button[text()='Evaluate']";
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(locator)))
        .click();
  }

  /** Click on close evaluate exp. button */
  public void clickCloseEvaluateBtn() {
    String locator = "//button[text()='Evaluate']/preceding-sibling::button";
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(locator)))
        .click();
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(ExpectedConditions.invisibilityOfElementLocated(xpath(locator)));
  }

  /**
   * Check debugger button panel
   *
   * @return true if present on page and false on other cases
   */
  public boolean isDebuggerBtnPanelPresent() {

    try {
      return seleniumWebDriver
          .findElement(id(DebuggerActionButtons.REMOVE_ALL_BREAKPOINTS))
          .isDisplayed();
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * Wait text in debug highlighted area (red line into Che editor under debugger)
   *
   * @param text the text under debug - highlighter
   */
  public void waitDebugHighlightedText(String text) {
    StringBuilder highLightedText = new StringBuilder();
    String locatorWithHiglightedText =
        "//div[@id='gwt-debug-editorPartStack-contentPanel']//div[@active]//div[@class='textviewContent' and @contenteditable='true']//span[@debugid='debug-line']";
    List<WebElement> hilightedElements =
        new WebDriverWait(seleniumWebDriver, ELEMENT_TIMEOUT_SEC)
            .until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                    xpath(locatorWithHiglightedText)));
    for (WebElement hilightedElement : hilightedElements) {
      highLightedText.append(hilightedElement.getText());
    }
    new WebDriverWait(seleniumWebDriver, MINIMUM_SEC)
        .until(
            (WebDriver driver) -> {
              return highLightedText.toString().contains(text);
            });
  }

  /**
   * Wait highlighter in the specified position
   *
   * @param numOfPosition
   */
  public void waitBreakPointHighlighterInDefinedPosition(int numOfPosition) {
    editor.returnFocusInCurrentLine();
    editor.waitActiveEditor();
    List<WebElement> editorLines =
        seleniumWebDriver.findElements(
            xpath(
                "//div[@id='gwt-debug-editorPartStack-contentPanel']//div[@active]//div[@class='textviewContent' and @contenteditable='true']/div"));
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(
            (WebDriver driver) -> {
              try {
                return editorLines
                    .get(numOfPosition - 1)
                    .findElement(tagName("span"))
                    .getAttribute("debugid")
                    .equals("debug-line");
              } catch (Exception e) {
                return false;
              }
            });
  }

  /** Open debug panel by clicking on "Debug" tab. */
  public void openDebugPanel() {
    if (!isDebuggerBtnPanelPresent()) {
      debuggerTab.click();
    }
  }

  /**
   * Click on "Remove All Breakpoints" button in Debug panel and wait until breakpoints go away from
   * Debug panel.
   */
  public void removeAllBreakpoints() {
    clickOnButton(DebuggerActionButtons.REMOVE_ALL_BREAKPOINTS);
    waitWhileAllBreakPointsOnEditorPanelDisapper();
    waitBreakPointsPanelIsEmpty();
  }

  /**
   * This method use for stopping Debugger using UI. Check if debugger panel presents. If present
   * click on remove all breakpoints button. than click on stop debugger session button, if some web
   * element covers the panel - browser will be refreshed for closing. If the Debugger have not
   * appeared on UI. After stopping the debugger we call clean up command for tomcat (the command
   * should be prepared in a test. The command stop the tomcat and clea webapp folder)
   *
   * @param cleanUpTomcat
   */
  public void stopDebuggerWithUiAndCleanUpTomcat(String cleanUpTomcat) {
    // if something went wrong on ui we refresh browser and try to click on remove all breakpoints
    // button again
    if (isDebuggerBtnPanelPresent()) {
      try {
        removeAllBreakpoints();
      } catch (WebDriverException ex) {
        LOG.error(ex.getLocalizedMessage(), ex);
        seleniumWebDriver.navigate().refresh();
        clickOnButton(DebuggerActionButtons.REMOVE_ALL_BREAKPOINTS);
      }
      clickOnButton(DebuggerActionButtons.BTN_DISCONNECT);
    }

    try {
      commandsExplorer.waitCommandExplorerIsOpened();
      commandsExplorer.openCommandsExplorer();
      commandsExplorer.waitCommandExplorerIsOpened();
      commandsExplorer.runCommandByName(cleanUpTomcat);
    } catch (TimeoutException ex) {
      commandsExplorer.openCommandsExplorer();
      commandsExplorer.waitCommandExplorerIsOpened();
      commandsExplorer.runCommandByName(cleanUpTomcat);
    }
  }

  public String getVariables() {
    waitVariablesPanel();
    return debuggerTree.getText();
  }

  public void waitFramesListPanelReady() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until((Function<WebDriver, Object>) webDriver -> !frames.getText().isEmpty());
  }

  public String[] getFrames() {
    waitFramesListPanelReady();
    return frames.getText().split("\n");
  }

  /** Waits */
  public void waitThreadListPanelReady() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until((Function<WebDriver, Object>) webDriver -> !threads.getText().isEmpty());
  }

  public void selectFrame(int frameIndex) {
    waitFramesListPanelReady();

    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath("//td[text()='" + getFrames()[frameIndex] + "']")))
        .click();
  }

  public String[] getThreads() {
    waitThreadListPanelReady();
    return threads.getText().split("\n");
  }

  public String getSelectedThread() {
    waitThreadListPanelReady();

    String selectedThreadId = threads.getAttribute("value");
    for (String thread : getThreads()) {
      if (thread.contains("@" + selectedThreadId + " in")) {
        return thread;
      }
    }

    return null;
  }

  public void selectThread(String threadName) {
    waitThreadListPanelReady();

    threads.click();
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath("//*[contains(text(),'\"" + threadName + "\"@')]")))
        .click();
    threads.click();
  }

  public void makeBreakpointConditional(String fileName, int lineNumber, String condition) {
    String breakpointItem = format(BreakpointsPanel.BREAKPOINT_ITEM, fileName + ":" + lineNumber);

    seleniumWebDriver
        .wait(REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(breakpointItem)));

    actionsFactory
        .createAction(seleniumWebDriver)
        .contextClick(seleniumWebDriver.findElement(xpath(breakpointItem)))
        .build()
        .perform();

    seleniumWebDriver
        .wait(REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(id(BreakpointsPanel.CONTEXT_MENU)))
        .click();

    seleniumWebDriver
        .wait(REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            visibilityOfElementLocated(
                xpath(BreakpointConfigurationWindow.BREAKPOINT_CONDITION_TEXT)))
        .sendKeys(condition);

    seleniumWebDriver
        .wait(REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(BreakpointConfigurationWindow.APPLY_BTN)))
        .click();
  }

  public void navigateToBreakpoint(String fileName, int lineNumber) {
    String breakpointItem = format(BreakpointsPanel.BREAKPOINT_ITEM, fileName + ":" + lineNumber);

    seleniumWebDriver
        .wait(REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(xpath(breakpointItem)));

    actionsFactory
        .createAction(seleniumWebDriver)
        .doubleClick(seleniumWebDriver.findElement(xpath(breakpointItem)))
        .build()
        .perform();
  }
}
