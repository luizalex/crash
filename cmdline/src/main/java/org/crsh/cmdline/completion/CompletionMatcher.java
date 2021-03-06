/*
 * Copyright (C) 2012 eXo Platform SAS.
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

package org.crsh.cmdline.completion;

import org.crsh.cmdline.ArgumentDescriptor;
import org.crsh.cmdline.CommandDescriptor;
import org.crsh.cmdline.Delimiter;
import org.crsh.cmdline.OptionDescriptor;
import org.crsh.cmdline.completers.EmptyCompleter;
import org.crsh.cmdline.tokenizer.Token;
import org.crsh.cmdline.tokenizer.Tokenizer;
import org.crsh.cmdline.tokenizer.TokenizerImpl;
import org.crsh.cmdline.parser.Event;
import org.crsh.cmdline.parser.Mode;
import org.crsh.cmdline.parser.Parser;
import org.crsh.cmdline.spi.Completer;

import java.util.Collections;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public final class CompletionMatcher<T> {

  /** . */
  private final CommandDescriptor<T> descriptor;

  /** . */
  private final String mainName;

  public CompletionMatcher(CommandDescriptor<T> descriptor) {
    this(null, descriptor);
  }

  public CompletionMatcher(String mainName, CommandDescriptor<T> descriptor) {
    this.mainName = mainName;
    this.descriptor = descriptor;
  }

  public final CompletionMatch match(String s) throws CompletionException {
    return match(EmptyCompleter.getInstance(), s);
  }

  public CompletionMatch match(Completer completer, String s) throws CompletionException {
    return getCompletion(completer, s).complete();
  }

  private Completion argument(CommandDescriptor<?> method, Completer completer) {
    List<? extends ArgumentDescriptor> arguments = method.getArguments();
    if (arguments.isEmpty()) {
      return new EmptyCompletion();
    } else {
      ArgumentDescriptor argument = arguments.get(0);
      return new ParameterCompletion("", Delimiter.EMPTY, argument, completer);
    }
  }

  private Completion getCompletion(Completer completer, String s) throws CompletionException {

    CommandDescriptor<T> descriptor = this.descriptor;

    Tokenizer tokenizer = new TokenizerImpl(s);
    Parser<T> parser = new Parser<T>(tokenizer, descriptor, mainName, Mode.COMPLETE);

    // Last non separator event
    Event last = null;
    Event.Separator separator = null;
    CommandDescriptor<?> method = null;
    Event.Stop stop;

    //
    while (true) {
      Event event = parser.next();
      if (event instanceof Event.Separator) {
        separator = (Event.Separator)event;
      } else if (event instanceof Event.Stop) {
        stop = (Event.Stop)event;
        break;
      } else if (event instanceof Event.Option) {
        last = event;
        separator = null;
      } else if (event instanceof Event.Subordinate) {
        method = ((Event.Subordinate)event).getDescriptor();
        last = event;
        separator = null;
      } else if (event instanceof Event.Argument) {
        last = event;
        separator = null;
      }/* else if (event instanceof Event.DoubleDash) {
        last = event;
        separator = null;
      }*/
    }

    //
    if (stop instanceof Event.Stop.Unresolved.NoSuchOption) {
      Event.Stop.Unresolved.NoSuchOption nso = (Event.Stop.Unresolved.NoSuchOption)stop;
      return new OptionCompletion<T>(method != null ? (CommandDescriptor<T>)method : descriptor, nso.getToken());
    } else if (stop instanceof Event.Stop.Unresolved) {
      if (stop instanceof Event.Stop.Unresolved.TooManyArguments) {
        if (method == null) {
          Event.Stop.Unresolved.TooManyArguments tma = (Event.Stop.Unresolved.TooManyArguments)stop;
          return new CommandCompletion<T>(descriptor, mainName, s.substring(stop.getIndex()), parser.getDelimiter());
        } else {
          return new EmptyCompletion();
        }
      } else {
        return new EmptyCompletion();
      }
    } else if (stop instanceof Event.Stop.Done) {
      // to use ?
    }

    //
    if (last == null) {
      if (method == null) {
        if (descriptor.getSubordinates().keySet().equals(Collections.singleton(mainName))) {
          method = descriptor.getSubordinate(mainName);
          List<ArgumentDescriptor> args = method.getArguments();
          if (args.size() > 0) {
            return new ParameterCompletion("", Delimiter.EMPTY, args.get(0), completer);
          } else {
            return new EmptyCompletion();
          }
        } else {
          return new CommandCompletion<T>(descriptor, mainName, s.substring(stop.getIndex()), Delimiter.EMPTY);
        }
      } else {
        return new EmptyCompletion();
      }
    }

    //
    /*if (last instanceof Event.DoubleDash) {
      Event.DoubleDash dd = (Event.DoubleDash)last;
      return new OptionCompletion<T>(method != null ? (CommandDescriptor<T, ?>)method : descriptor, dd.token);
    } else*/
    if (last instanceof Event.Option) {
      Event.Option optionEvent = (Event.Option)last;
      List<Token.Literal.Word> values = optionEvent.getValues();
      OptionDescriptor option = optionEvent.getParameter();
      if (separator == null) {
        if (values.size() == 0) {
          return new SpaceCompletion();
        } else if (values.size() <= option.getArity()) {
          Token.Literal.Word word = optionEvent.peekLast();
          return new ParameterCompletion(word.getValue(), parser.getDelimiter(), option, completer);
        } else {
          return new EmptyCompletion();
        }
      } else {
        if (values.size() < option.getArity()) {
          return new ParameterCompletion("", Delimiter.EMPTY, option, completer);
        } else {
          if (method == null) {
            return new CommandCompletion<T>(descriptor, mainName, s.substring(stop.getIndex()), Delimiter.EMPTY);
          } else {
            return argument(method, completer);
          }
        }
      }
    } else if (last instanceof Event.Argument) {
      Event.Argument eventArgument = (Event.Argument)last;
      ArgumentDescriptor argument = eventArgument.getParameter();
      if (separator != null) {
        switch (argument.getMultiplicity()) {
          case SINGLE:
            List<? extends ArgumentDescriptor> arguments = eventArgument.getCommand().getArguments();
            int index = arguments.indexOf(argument) + 1;
            if (index < arguments.size()) {
              ArgumentDescriptor nextArg = arguments.get(index);
              return new ParameterCompletion("", Delimiter.EMPTY, nextArg, completer);
            } else {
              return new EmptyCompletion();
            }
          case MULTI:
            return new ParameterCompletion("", Delimiter.EMPTY, argument, completer);
          default:
            throw new AssertionError();
        }
      } else {
        Token.Literal value = eventArgument.peekLast();
        return new ParameterCompletion(value.getValue(), parser.getDelimiter(), argument, completer);
      }
    } else if (last instanceof Event.Subordinate) {
      if (separator != null) {
        return argument(method, completer);
      } else {
        return new SpaceCompletion();
      }
    } else {
      throw new AssertionError();
    }
  }
}
