/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.cmdline;

import org.crsh.cmdline.binding.ClassFieldBinding;
import org.crsh.cmdline.binding.MethodArgumentBinding;
import org.crsh.cmdline.binding.TypeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class CommandFactory {

  /** . */
  private static final Logger log = LoggerFactory.getLogger(CommandFactory.class);

  public static <T> ClassDescriptor<T> create(Class<T> type) throws IntrospectionException {
    return new ClassDescriptor<T>(type, new Description(type), parameters(type));
  }

  protected static <B extends TypeBinding> ParameterDescriptor<B> create(
    B binding,
    Type type,
    Argument argumentAnn,
    Option optionAnn,
    Description info,
    Annotation ann) throws IntrospectionException {

    //
    if (argumentAnn != null) {
      if (optionAnn != null) {
        throw new IntrospectionException();
      }

      //
      return new ArgumentDescriptor<B>(
        binding,
        argumentAnn.name(),
        type,
        info,
        argumentAnn.required(),
        argumentAnn.password(),
        argumentAnn.completer(),
        ann);
    } else if (optionAnn != null) {
      return new OptionDescriptor<B>(
        binding,
        type,
        Collections.unmodifiableList(Arrays.asList(optionAnn.names())),
        info,
        optionAnn.required(),
        optionAnn.arity(),
        optionAnn.password(),
        optionAnn.completer(),
        ann);
    } else {
      return null;
    }
  }

  protected static Tuple get(Annotation... ab) {
    Argument argumentAnn = null;
    Option optionAnn = null;
    Description description = new Description(ab);
    Annotation info = null;
    for (Annotation parameterAnnotation : ab) {
      if (parameterAnnotation instanceof Option) {
        optionAnn = (Option)parameterAnnotation;
      } else if (parameterAnnotation instanceof Argument) {
        argumentAnn = (Argument)parameterAnnotation;
      } else {

        // Look at annotated annotations
        Class<? extends Annotation> a = parameterAnnotation.annotationType();
        if (a.getAnnotation(Option.class) != null) {
          optionAnn = a.getAnnotation(Option.class);
          info = parameterAnnotation;
        } else if (a.getAnnotation(Argument.class) != null) {
          argumentAnn =  a.getAnnotation(Argument.class);
          info = parameterAnnotation;
        }

        //
        if (info != null) {
          description = new Description(description, new Description(a));
        }
      }
    }

    return new Tuple(argumentAnn, optionAnn, description, info);
  }

  public static <T> MethodDescriptor<T> create(ClassDescriptor<T> owner, Method m) throws IntrospectionException {
    Command command = m.getAnnotation(Command.class);
    if (command != null) {
      List<ParameterDescriptor<MethodArgumentBinding>> parameters = new ArrayList<ParameterDescriptor<MethodArgumentBinding>>();
      Type[] parameterTypes = m.getGenericParameterTypes();
      Annotation[][] parameterAnnotationMatrix = m.getParameterAnnotations();
      for (int i = 0;i < parameterAnnotationMatrix.length;i++) {


        Annotation[] parameterAnnotations = parameterAnnotationMatrix[i];
        Type parameterType = parameterTypes[i];
        Tuple tuple = get(parameterAnnotations);


        MethodArgumentBinding binding = new MethodArgumentBinding(i);
        ParameterDescriptor<MethodArgumentBinding> parameter = create(
          binding,
          parameterType,
          tuple.argumentAnn,
          tuple.optionAnn,
          tuple.descriptionAnn,
          tuple.ann);
        if (parameter != null) {
          parameters.add(parameter);
        } else {
          log.debug("Method argument with index " + i + " of method " + m + " is not annotated");
        }
      }

      //
      Description info = new Description(m);

      //
      return new MethodDescriptor<T>(
        owner,
        m,
        m.getName().toLowerCase(),
        info,
        parameters);
    } else {
      return null;
    }
  }

  /**
   * Jus grouping some data for conveniency
   */
  protected static class Tuple {
    final Argument argumentAnn;
    final Option optionAnn;
    final Description descriptionAnn;
    final Annotation ann;
    private Tuple(Argument argumentAnn, Option optionAnn, Description info, Annotation ann) {
      this.argumentAnn = argumentAnn;
      this.optionAnn = optionAnn;
      this.descriptionAnn = info;
      this.ann = ann;
    }
  }

  private static List<ParameterDescriptor<ClassFieldBinding>> parameters(Class<?> introspected) throws IntrospectionException {
    List<ParameterDescriptor<ClassFieldBinding>> parameters;
    Class<?> superIntrospected = introspected.getSuperclass();
    if (superIntrospected == null) {
      parameters = new ArrayList<ParameterDescriptor<ClassFieldBinding>>();
    } else {
      parameters = parameters(superIntrospected);
      for (Field f : introspected.getDeclaredFields()) {
        Tuple tuple = CommandFactory.get(f.getAnnotations());
        ClassFieldBinding binding = new ClassFieldBinding(f);
        ParameterDescriptor<ClassFieldBinding> parameter = CommandFactory.create(
          binding,
          f.getGenericType(),
          tuple.argumentAnn,
          tuple.optionAnn,
          tuple.descriptionAnn,
          tuple.ann);
        if (parameter != null) {
          parameters.add(parameter);
        }
      }
    }
    return parameters;
  }
}
