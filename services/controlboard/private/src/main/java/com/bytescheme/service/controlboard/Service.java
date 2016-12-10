package com.bytescheme.service.controlboard;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bytescheme.rpc.core.LoginCallRequest;
import com.bytescheme.rpc.core.LogoutCallRequest;
import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.core.MethodCallResponse;
import com.bytescheme.rpc.core.RemoteObjectServer;
import com.bytescheme.service.controlboard.video.VideoBroadcastHandler;

/**
 *
 * @author Naorem Khogendro Singh
 *
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(consumes = "application/json", produces = "application/json; charset=UTF-8")
public class Service {
  @Autowired
  private RemoteObjectServer remoteObjectServer;
  @Autowired
  private VideoBroadcastHandler broadcaster;

  @RequestMapping(path = "/rpc/login", method = RequestMethod.POST)
  public @ResponseBody MethodCallResponse login(@RequestBody LoginCallRequest request) {
    return remoteObjectServer.login(request);
  }

  @RequestMapping(path = "/rpc/logout", method = RequestMethod.POST)
  public @ResponseBody MethodCallResponse logout(@RequestBody LogoutCallRequest request) {
    return remoteObjectServer.logout(request);
  }

  @RequestMapping(path = "/rpc", method = RequestMethod.POST)
  public @ResponseBody MethodCallResponse invoke(@RequestBody MethodCallRequest request) {
    return remoteObjectServer.process(request);
  }

  @RequestMapping(value = "/video/{width}/{height}", method = { RequestMethod.POST,
      RequestMethod.PUT }, consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public void sendMedia(InputStream stream, @PathVariable("width") short width,
      @PathVariable("height") short height) throws IOException {
    broadcaster.broadcast(stream, width, height);
  }

  @RequestMapping(path = "/ping", method = RequestMethod.GET, consumes = "*")
  public @ResponseBody String ping() {
    return "OK";
  }
}
