// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote.server;

import static org.openqa.selenium.remote.DriverCommand.*;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.ErrorCodes;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.JsonHttpCommandCodec;
import org.openqa.selenium.remote.http.JsonHttpResponseCodec;
import org.openqa.selenium.remote.server.handler.*;
import org.openqa.selenium.remote.server.handler.html5.*;
import org.openqa.selenium.remote.server.handler.interactions.*;
import org.openqa.selenium.remote.server.handler.interactions.touch.*;
import org.openqa.selenium.remote.server.handler.mobile.*;
import org.openqa.selenium.remote.server.rest.RestishHandler;
import org.openqa.selenium.remote.server.rest.ResultConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class JsonHttpCommandHandler {

  private static final String ADD_CONFIG_COMMAND_NAME = "-selenium-add-config";

  private final DriverSessions sessions;
  private final Logger log;
  private final JsonHttpCommandCodec commandCodec;
  private final JsonHttpResponseCodec responseCodec;
  private final Map<String, ResultConfig> configs = new LinkedHashMap<String, ResultConfig>();
  private final ErrorCodes errorCodes = new ErrorCodes();

  public JsonHttpCommandHandler(DriverSessions sessions, Logger log) {
    this.sessions = sessions;
    this.log = log;
    this.commandCodec = new JsonHttpCommandCodec();
    this.responseCodec = new JsonHttpResponseCodec();
    setUpMappings();
  }

  public void addNewMapping(
      String commandName, Class<? extends RestishHandler<?>> implementationClass) {
    ResultConfig config = new ResultConfig(commandName, implementationClass, sessions, log);
    configs.put(commandName, config);
  }

  public HttpResponse handleRequest(HttpRequest request) {
    log.fine(String.format("Handling: %s %s", request.getMethod(), request.getUri()));

    Command command = null;
    Response response;
    try {
      command = commandCodec.decode(request);
      ResultConfig config = configs.get(command.getName());
      if (config == null) {
        throw new UnsupportedCommandException();
      }
      response = config.handle(command);
      log.fine(String.format("Finished: %s %s", request.getMethod(), request.getUri()));
    } catch (Exception e) {
      log.fine(String.format("Error on: %s %s", request.getMethod(), request.getUri()));
      response = new Response();
      response.setStatus(errorCodes.toStatusCode(e));
      response.setState(errorCodes.toState(response.getStatus()));
      response.setValue(e);

      if (command != null && command.getSessionId() != null) {
        response.setSessionId(command.getSessionId().toString());
      }
    }
    return responseCodec.encode(response);
  }

  private void setUpMappings() {
    commandCodec.defineCommand(ADD_CONFIG_COMMAND_NAME, POST, "/config/drivers");
    addNewMapping(ADD_CONFIG_COMMAND_NAME, AddConfig.class);

    addNewMapping(STATUS, Status.class);
    addNewMapping(GET_ALL_SESSIONS, GetAllSessions.class);
    addNewMapping(NEW_SESSION, NewSession.class);
    addNewMapping(GET_CAPABILITIES, GetSessionCapabilities.class);
    addNewMapping(QUIT, DeleteSession.class);

    addNewMapping(GET_CURRENT_WINDOW_HANDLE, GetCurrentWindowHandle.class);
    addNewMapping(GET_CURRENT_WINDOW_HANDLE_W3C, GetCurrentWindowHandle.class);
    addNewMapping(GET_WINDOW_HANDLES, GetAllWindowHandles.class);
    addNewMapping(GET_WINDOW_HANDLES_W3C, GetAllWindowHandles.class);

    addNewMapping(DISMISS_ALERT, DismissAlert.class);
    addNewMapping(DISMISS_ALERT_W3C, DismissAlert.class);
    addNewMapping(ACCEPT_ALERT, AcceptAlert.class);
    addNewMapping(ACCEPT_ALERT_W3C, AcceptAlert.class);
    addNewMapping(GET_ALERT_TEXT, GetAlertText.class);
    addNewMapping(GET_ALERT_TEXT_W3C, GetAlertText.class);
    addNewMapping(SET_ALERT_VALUE, SetAlertText.class);
    addNewMapping(SET_ALERT_VALUE_W3C, SetAlertText.class);
    addNewMapping(SET_ALERT_CREDENTIALS, SetAlertCredentials.class);

    addNewMapping(GET, ChangeUrl.class);
    addNewMapping(GET_CURRENT_URL, GetCurrentUrl.class);
    addNewMapping(GO_FORWARD, GoForward.class);
    addNewMapping(GO_BACK, GoBack.class);
    addNewMapping(REFRESH, RefreshPage.class);

    addNewMapping(EXECUTE_SCRIPT, ExecuteScript.class);
    addNewMapping(EXECUTE_ASYNC_SCRIPT, ExecuteAsyncScript.class);
    addNewMapping(EXECUTE_SCRIPT_W3C, ExecuteScript.class);
    addNewMapping(EXECUTE_ASYNC_SCRIPT_W3C, ExecuteAsyncScript.class);

    addNewMapping(GET_PAGE_SOURCE, GetPageSource.class);

    addNewMapping(SCREENSHOT, CaptureScreenshot.class);

    addNewMapping(GET_TITLE, GetTitle.class);

    addNewMapping(FIND_ELEMENT, FindElement.class);
    addNewMapping(FIND_ELEMENTS, FindElements.class);
    addNewMapping(GET_ACTIVE_ELEMENT, FindActiveElement.class);

    addNewMapping(FIND_CHILD_ELEMENT, FindChildElement.class);
    addNewMapping(FIND_CHILD_ELEMENTS, FindChildElements.class);

    addNewMapping(CLICK_ELEMENT, ClickElement.class);
    addNewMapping(GET_ELEMENT_TEXT, GetElementText.class);
    addNewMapping(SUBMIT_ELEMENT, SubmitElement.class);

    addNewMapping(UPLOAD_FILE, UploadFile.class);
    addNewMapping(SEND_KEYS_TO_ELEMENT, SendKeys.class);
    addNewMapping(GET_ELEMENT_TAG_NAME, GetTagName.class);

    addNewMapping(CLEAR_ELEMENT, ClearElement.class);
    addNewMapping(IS_ELEMENT_SELECTED, GetElementSelected.class);
    addNewMapping(IS_ELEMENT_ENABLED, GetElementEnabled.class);
    addNewMapping(IS_ELEMENT_DISPLAYED, GetElementDisplayed.class);
    addNewMapping(GET_ELEMENT_LOCATION, GetElementLocation.class);
    addNewMapping(GET_ELEMENT_LOCATION_ONCE_SCROLLED_INTO_VIEW, GetElementLocationInView.class);
    addNewMapping(GET_ELEMENT_SIZE, GetElementSize.class);
    addNewMapping(GET_ELEMENT_VALUE_OF_CSS_PROPERTY, GetCssProperty.class);
    addNewMapping(GET_ELEMENT_RECT, GetElementRect.class);

    addNewMapping(GET_ELEMENT_ATTRIBUTE, GetElementAttribute.class);
    addNewMapping(ELEMENT_EQUALS, ElementEquality.class);

    addNewMapping(GET_ALL_COOKIES, GetAllCookies.class);
    addNewMapping(GET_COOKIE, GetCookie.class);
    addNewMapping(ADD_COOKIE, AddCookie.class);
    addNewMapping(DELETE_ALL_COOKIES, DeleteCookie.class);
    addNewMapping(DELETE_COOKIE, DeleteNamedCookie.class);

    addNewMapping(SWITCH_TO_FRAME, SwitchToFrame.class);
    addNewMapping(SWITCH_TO_PARENT_FRAME, SwitchToParentFrame.class);
    addNewMapping(SWITCH_TO_WINDOW, SwitchToWindow.class);
    addNewMapping(CLOSE, CloseWindow.class);

    addNewMapping(GET_WINDOW_SIZE, GetWindowSize.class);
    addNewMapping(GET_CURRENT_WINDOW_SIZE, GetWindowSize.class);
    addNewMapping(SET_WINDOW_SIZE, SetWindowSize.class);
    addNewMapping(SET_CURRENT_WINDOW_SIZE, SetWindowSize.class);
    addNewMapping(GET_WINDOW_POSITION, GetWindowPosition.class);
    addNewMapping(SET_WINDOW_POSITION, SetWindowPosition.class);
    addNewMapping(MAXIMIZE_WINDOW, MaximizeWindow.class);
    addNewMapping(MAXIMIZE_CURRENT_WINDOW, MaximizeWindow.class);
    addNewMapping(FULLSCREEN_CURRENT_WINDOW, FullscreenWindow.class);

    addNewMapping(SET_TIMEOUT, ConfigureTimeout.class);
    addNewMapping(IMPLICITLY_WAIT, ImplicitlyWait.class);
    addNewMapping(SET_SCRIPT_TIMEOUT, SetScriptTimeout.class);

    addNewMapping(GET_LOCATION, GetLocationContext.class);
    addNewMapping(SET_LOCATION,  SetLocationContext.class);

    addNewMapping(GET_APP_CACHE_STATUS, GetAppCacheStatus.class);

    addNewMapping(GET_LOCAL_STORAGE_ITEM, GetLocalStorageItem.class);
    addNewMapping(REMOVE_LOCAL_STORAGE_ITEM, RemoveLocalStorageItem.class);
    addNewMapping(GET_LOCAL_STORAGE_KEYS, GetLocalStorageKeys.class);
    addNewMapping(SET_LOCAL_STORAGE_ITEM, SetLocalStorageItem.class);
    addNewMapping(CLEAR_LOCAL_STORAGE, ClearLocalStorage.class);
    addNewMapping(GET_LOCAL_STORAGE_SIZE, GetLocalStorageSize.class);

    addNewMapping(GET_SESSION_STORAGE_ITEM, GetSessionStorageItem.class);
    addNewMapping(REMOVE_SESSION_STORAGE_ITEM, RemoveSessionStorageItem.class);
    addNewMapping(GET_SESSION_STORAGE_KEYS, GetSessionStorageKeys.class);
    addNewMapping(SET_SESSION_STORAGE_ITEM, SetSessionStorageItem.class);
    addNewMapping(CLEAR_SESSION_STORAGE, ClearSessionStorage.class);
    addNewMapping(GET_SESSION_STORAGE_SIZE, GetSessionStorageSize.class);

    addNewMapping(GET_SCREEN_ORIENTATION, GetScreenOrientation.class);
    addNewMapping(SET_SCREEN_ORIENTATION, Rotate.class);

    addNewMapping(MOVE_TO, MouseMoveToLocation.class);
    addNewMapping(CLICK, ClickInSession.class);
    addNewMapping(DOUBLE_CLICK, DoubleClickInSession.class);
    addNewMapping(MOUSE_DOWN, MouseDown.class);
    addNewMapping(MOUSE_UP, MouseUp.class);
    addNewMapping(SEND_KEYS_TO_ACTIVE_ELEMENT, SendKeyToActiveElement.class);

    addNewMapping(IME_GET_AVAILABLE_ENGINES, ImeGetAvailableEngines.class);
    addNewMapping(IME_GET_ACTIVE_ENGINE, ImeGetActiveEngine.class);
    addNewMapping(IME_IS_ACTIVATED, ImeIsActivated.class);
    addNewMapping(IME_DEACTIVATE, ImeDeactivate.class);
    addNewMapping(IME_ACTIVATE_ENGINE, ImeActivateEngine.class);

    // Advanced Touch API
    addNewMapping(TOUCH_SINGLE_TAP, SingleTapOnElement.class);
    addNewMapping(TOUCH_DOWN, Down.class);
    addNewMapping(TOUCH_UP, Up.class);
    addNewMapping(TOUCH_MOVE, Move.class);
    addNewMapping(TOUCH_SCROLL, Scroll.class);
    addNewMapping(TOUCH_DOUBLE_TAP, DoubleTapOnElement.class);
    addNewMapping(TOUCH_LONG_PRESS, LongPressOnElement.class);
    addNewMapping(TOUCH_FLICK, Flick.class);

    addNewMapping(GET_AVAILABLE_LOG_TYPES, GetAvailableLogTypesHandler.class);
    addNewMapping(GET_LOG, GetLogHandler.class);
    addNewMapping(GET_SESSION_LOGS, GetSessionLogsHandler.class);

    addNewMapping(GET_NETWORK_CONNECTION, GetNetworkConnection.class);
    addNewMapping(SET_NETWORK_CONNECTION, SetNetworkConnection.class);
  }
}
