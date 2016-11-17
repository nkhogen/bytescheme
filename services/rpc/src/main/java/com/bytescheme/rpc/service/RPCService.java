package com.bytescheme.rpc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bytescheme.rpc.core.MethodCallRequest;
import com.bytescheme.rpc.core.MethodCallResponse;
import com.bytescheme.rpc.core.ServerRequestProcessor;

@RestController
@RequestMapping(path = "/rpc", consumes = "application/json", produces = "application/json; charset=UTF-8")
public class RPCService {
  @Autowired
  private ServerRequestProcessor serverRequestHandler;

  @RequestMapping(method = RequestMethod.POST)
  public @ResponseBody MethodCallResponse invoke(@RequestBody MethodCallRequest request) {
    return serverRequestHandler.process(request);
  }

  @RequestMapping(method = RequestMethod.GET)
  public String ping() {
    return "OK";
  }
}
