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

package org.crsh.cmdline.matcher.impl;

import org.crsh.cmdline.CommandCompletion;
import org.crsh.cmdline.CommandDescriptor;
import org.crsh.cmdline.Delimiter;
import org.crsh.cmdline.matcher.CmdCompletionException;
import org.crsh.cmdline.matcher.tokenizer.Token;
import org.crsh.cmdline.spi.ValueCompletion;

import java.util.Set;

class OptionCompletion<T> extends Completion {

  /** . */
  private final CommandDescriptor<T, ?> descriptor;

  /** . */
  private final Token.Literal.Option prefix;

  OptionCompletion(CommandDescriptor<T, ?> descriptor, Token.Literal.Option prefix) {
    this.descriptor = descriptor;
    this.prefix = prefix;
  }

  @Override
  protected CommandCompletion complete() throws CmdCompletionException {
    ValueCompletion completions = new ValueCompletion(prefix.getValue());
    Set<String> optionNames = prefix instanceof Token.Literal.Option.Short ? descriptor.getShortOptionNames() : descriptor.getLongOptionNames();
    for (String optionName : optionNames) {
      if (optionName.startsWith(prefix.getValue())) {
        completions.put(optionName.substring(prefix.getValue().length()), true);
      }
    }
    return new CommandCompletion(Delimiter.EMPTY, completions);
  }
}
