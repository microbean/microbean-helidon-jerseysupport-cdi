/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2019 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.helidon.jerseysupport.cdi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.util.Collections;
import java.util.Set;

import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import javax.enterprise.event.Observes;

import javax.inject.Singleton;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Application;

import io.helidon.webserver.WebServer;

import io.helidon.webserver.jersey.JerseySupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ApplicationScoped
public class TestExtension {

  
  /*
   * Test boilerplate.
   */

  
  private SeContainer cdiContainer;

  public TestExtension() {
    super();
  }

  @Before
  public void startCdiContainer() {
    final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
    assertNotNull(initializer);
    initializer.addBeanClasses(MyApplication.class);
    this.cdiContainer = initializer.initialize();
  }

  @After
  public void shutDownCdiContainer() {
    if (this.cdiContainer != null) {
      this.cdiContainer.close();
    }
  }

  @Test
  public void test() {
    // See onStartup()
  }


  /*
   * Actual test code.
   */
  

  private void onStartup(@Observes @Initialized(ApplicationScoped.class) final Object event,
                         final MyApplication application,
                         WebServer webServer)
    throws ExecutionException, IOException, InterruptedException
  {
    assertNotNull(application);
    webServer.start().toCompletableFuture().get();
    final URL url = new URL("http://127.0.0.1:" + webServer.port() + "/hello");
    byte[] result = null;
    try (final InputStream stream = new BufferedInputStream(url.openStream())) {
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      assertNotNull(stream);
      int bytesRead;
      final byte[] bytes = new byte[4096];
      while ((bytesRead = stream.read(bytes, 0, bytes.length)) != -1) {
        buffer.write(bytes, 0, bytesRead);
      }
      buffer.flush();
      result = buffer.toByteArray();
      buffer.close();
    }
    assertNotNull(result);
    assertEquals("Hello, world!", new String(result, "UTF-8"));
    webServer.shutdown();
  }


  /*
   * Inner and nested classes.
   */
  

  @Singleton
  private static class MyApplication extends Application {

    public MyApplication() {
      super();
    }

    @Override
    public Set<Class<?>> getClasses() {
      return Collections.singleton(HelloWorldResource.class);
    }
    
  }


  @Path("") // this turns out to be necessary, despite specification section 3.1
  public static class HelloWorldResource {

    public HelloWorldResource() {
      super();
    }

    @Path("hello")
    @Produces("text/plain")
    @GET
    public String sayHello() {
      return "Hello, world!";
    }
    
  }

  
}
