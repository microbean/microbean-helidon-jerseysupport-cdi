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

import java.lang.reflect.Type;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ExecutorService;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.configurator.AnnotatedConstructorConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedParameterConfigurator;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTargetFactory;

import javax.enterprise.inject.literal.InjectLiteral;

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

            final AnnotatedType<JerseySupport> jerseySupportAnnotatedType = beanManager.createAnnotatedType(JerseySupport.class);
            final InjectionTargetFactory<JerseySupport> itf = beanManager.getInjectionTargetFactory(jerseySupportAnnotatedType);

            // Add @Inject to the update() method.
            final AnnotatedMethodConfigurator<? super JerseySupport> amc = itf.configure()
              .filterMethods(am -> am.getJavaMember().getName().equals("update"))
              .findFirst()
              .get()
              .add(InjectLiteral.INSTANCE);

            // Add the right qualifiers to the @Inject-annotated
            // update() method.
            final AnnotatedParameterConfigurator<? super JerseySupport> rulesParameter = amc.filterParams(ap -> ap != null)
              .findFirst()
              .get();
            for (final Annotation qualifier : qualifiers) {
              rulesParameter.add(qualifier);
            }

            // Build the JerseySupport object using its private,
            // two-argument constructor.  To do this, make sure the
            // constructor is annotated with @Inject.
            final AnnotatedConstructorConfigurator<JerseySupport> ac = itf.configure()
              .filterConstructors(c -> c.getJavaMember().getParameterCount() == 2 && !c.getJavaMember().isSynthetic())
              .findFirst()
              .get();
            ac.add(InjectLiteral.INSTANCE);

            // Make sure that constructor's two parameters are
            // annotated with the right qualifiers.
            final List<AnnotatedParameterConfigurator<JerseySupport>> apcs = ac.params();
            for (final AnnotatedParameterConfigurator<JerseySupport> apc : apcs) {
              for (final Annotation qualifier : qualifiers) {
                apc.add(qualifier);
              }
            }

            final BeanAttributes<JerseySupport> beanAttributes = beanManager.createBeanAttributes(jerseySupportAnnotatedType);
            final BeanAttributes<JerseySupport> jerseySupportBeanAttributes = new DelegatingBeanAttributes<JerseySupport>(beanAttributes) {
                @Override
                public final Class<? extends Annotation> getScope() {
                  return ApplicationScoped.class; // TODO: reexamine
                }
                
                @Override
                public final Set<Annotation> getQualifiers() {
                  return qualifiers;
                }
              };

            final Bean<JerseySupport> jerseySupportBean = beanManager.createBean(jerseySupportBeanAttributes, jerseySupportAnnotatedType.getJavaClass(), itf);
            event.addBean(jerseySupportBean);

          }
        }
      }
    }
  }

  private static class DelegatingBeanAttributes<T> implements BeanAttributes<T> {

    private final BeanAttributes<T> delegate;
    
    private DelegatingBeanAttributes(final BeanAttributes<T> delegate) {
      super();
      this.delegate = Objects.requireNonNull(delegate);
    }
    
    @Override
    public Set<Type> getTypes() {
      return this.delegate.getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return this.delegate.getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return this.delegate.getScope();
    }

    @Override
    public String getName() {
      return this.delegate.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
      return this.delegate.getStereotypes();
    }

    @Override
    public boolean isAlternative() {
      return this.delegate.isAlternative();
    }

  }
  
}
