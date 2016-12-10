package com.bytescheme.service.controlboard;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Configuration;


@Configuration
public class HttpToHttpsJettyCustomizer implements EmbeddedServletContainerCustomizer {
  @Override
  public void customize(ConfigurableEmbeddedServletContainer container) {
    JettyEmbeddedServletContainerFactory containerFactory = (JettyEmbeddedServletContainerFactory) container;
    if (containerFactory.getSsl() != null) {
      containerFactory.addConfigurations(new HttpToHttpsJettyConfiguration());
      containerFactory.addServerCustomizers(server -> {
        HttpConfiguration http = new HttpConfiguration();
        http.setSecurePort(containerFactory.getPort());
        http.setSecureScheme("https");
        ServerConnector connector = new ServerConnector(server);
        connector.addConnectionFactory(new HttpConnectionFactory(http));
        connector.setPort(8080);
        server.addConnector(connector);
      });
    }
  }

  static class HttpToHttpsJettyConfiguration extends AbstractConfiguration {
    @Override
    public void configure(WebAppContext context) throws Exception {
      Constraint constraint = new Constraint();
      constraint.setDataConstraint(2);
      ConstraintMapping constraintMapping = new ConstraintMapping();
      constraintMapping.setPathSpec("/*");
      constraintMapping.setConstraint(constraint);
      ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();
      constraintSecurityHandler.addConstraintMapping(constraintMapping);
      context.setSecurityHandler(constraintSecurityHandler);
    }
  }
}
