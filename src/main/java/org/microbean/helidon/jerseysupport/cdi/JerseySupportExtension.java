/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2018–2019 microBean.
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

import java.lang.annotation.Annotation;

import java.util.Set;

import java.util.concurrent.ExecutorService;

import javax.annotation.Priority;

import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import javax.interceptor.Interceptor;

import javax.ws.rs.core.Application;

import io.helidon.webserver.jersey.JerseySupport;

import org.microbean.helidon.webserver.cdi.HelidonWebServerExtension;

/**
 * A CDI 2.0 portable extension that sets up {@link JerseySupport}.
 *
 * @author <a href="https://about.me/lairdnelson"
 * target="_parent">Laird Nelson</a>
 *
 * @see JerseySupport
 */
public class JerseySupportExtension implements Extension {

  /**
   * Creates a new {@link JerseySupportExtension}.
   */
  public JerseySupportExtension() {    
    super();
  }

  private final <X extends Application> void createJerseySupportInstances(@Observes @Priority(Interceptor.Priority.APPLICATION - 1) final AfterBeanDiscovery event, final BeanManager beanManager) {
    if (event != null && beanManager != null) {
      final Set<Bean<?>> applicationBeans = beanManager.getBeans(Application.class, Any.Literal.INSTANCE);
      if (applicationBeans != null && !applicationBeans.isEmpty()) {
        final HelidonWebServerExtension helidonWebServerExtension = beanManager.getExtension(HelidonWebServerExtension.class);
        assert helidonWebServerExtension != null;
        for (final Bean<?> applicationBean : applicationBeans) {
          if (applicationBean != null) {
            final Set<Annotation> qualifiers = applicationBean.getQualifiers();
            helidonWebServerExtension.addQualifiers(qualifiers);
            // TODO: overhaul this a bit so that we can add @Priority (not a qualifier annotation!) to it
            event.addBean()
              .scope(Dependent.class) // TODO: I *think* this is right; no need to have it live longer
              .qualifiers(qualifiers)
              .addTransitiveTypeClosure(JerseySupport.class)
              .createWith(cc -> {
                  final JerseySupport returnValue;
                  final Set<Bean<?>> executorServiceBeans = beanManager.getBeans(ExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()]));
                  if (executorServiceBeans != null && !executorServiceBeans.isEmpty()) {
                    final Bean<?> executorServiceBean = beanManager.resolve(executorServiceBeans);
                    assert executorServiceBean != null;
                    @SuppressWarnings("unchecked")
                    final ExecutorService executorService = (ExecutorService)beanManager.getReference(executorServiceBean, ExecutorService.class, cc);
                    assert executorService != null;
                    returnValue = JerseySupport.builder((Application)beanManager.getReference(applicationBean, Application.class, cc)).executorService(executorService).build();
                  } else {
                    returnValue = JerseySupport.create((Application)beanManager.getReference(applicationBean, Application.class, cc));
                  }
                  return returnValue;
                });
          }
        }
      }
    }
  }
  
}
