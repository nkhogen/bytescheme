package com.bytescheme.service.controlboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bytescheme.common.utils.CryptoUtils;
import com.bytescheme.rpc.core.LoginCallRequest;
import com.bytescheme.rpc.core.LogoutCallRequest;
import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.core.MethodCallResponse;
import com.bytescheme.rpc.core.RemoteObjectServer;

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

  @RequestMapping(path = "/rpc/keys", method = RequestMethod.GET, consumes = "*")
  public @ResponseBody String[] createKeyPair() {
    return CryptoUtils.createKeyPair();
  }

  @RequestMapping(path = "/ping", method = RequestMethod.GET, consumes = "*")
  public @ResponseBody String ping() {
    return "OK";
  }
}
