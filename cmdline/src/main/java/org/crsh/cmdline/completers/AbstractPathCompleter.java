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

package org.crsh.cmdline.completers;

import org.crsh.cmdline.ParameterDescriptor;
import org.crsh.cmdline.spi.Completer;
import org.crsh.cmdline.spi.ValueCompletion;

import java.util.Collection;

public abstract class AbstractPathCompleter<P> implements Completer {

  protected abstract String getCurrentPath() throws Exception;

  protected abstract P getPath(String path) throws Exception;

  protected abstract boolean exists(P path) throws Exception;

  protected abstract boolean isDirectory(P path) throws Exception;

  protected abstract boolean isFile(P path) throws Exception;

  protected abstract Collection<P> getChilren(P path) throws Exception;

  protected abstract String getName(P path) throws Exception;

  public final ValueCompletion complete(ParameterDescriptor<?> parameter, String prefix) throws Exception {

    // Handle empty dir
    if (!prefix.startsWith("/")) {
      String currentPath = getCurrentPath();
      if (!currentPath.endsWith("/")) {
        currentPath += "/";
      }
      if (prefix.length() > 0) {
        prefix = currentPath + prefix;
      } else {
        prefix = currentPath;
      }
    }

    //
    P f = getPath(prefix);

    //
    if (exists(f)) {
      if (isDirectory(f)) {
        if (prefix.endsWith("/")) {
          Collection<P> children = getChilren(f);
          if (children != null) {
            if (children.size() > 0) {
              return listDir(f, "");
            } else {
              return ValueCompletion.create();
            }
          } else {
            return ValueCompletion.create();
          }
        } else {
          Collection<P> children = getChilren(f);
          if (children == null) {
            return ValueCompletion.create();
          } else {
            return ValueCompletion.create("/", false);
          }
        }
      } else if (isFile(f)) {
        return ValueCompletion.create("", true);
      }
      return ValueCompletion.create();
    } else {
      int pos = prefix.lastIndexOf('/');
      if (pos != -1) {
        String filter;
        if (pos == 0) {
          f = getPath("/");
          filter = prefix.substring(1);
        } else {
          f = getPath(prefix.substring(0, pos));
          filter = prefix.substring(pos + 1);
        }
        if (exists(f)) {
          if (isDirectory(f)) {
            return listDir(f, filter);
          } else {
            return ValueCompletion.create();
          }
        } else {
          return ValueCompletion.create();
        }
      } else {
        return ValueCompletion.create();
      }
    }
  }

  private ValueCompletion listDir(P dir, final String filter) throws Exception {
    Collection<P> children = getChilren(dir);
    if (children != null) {
      ValueCompletion map = new ValueCompletion(filter);
      for (P child : children) {
        String name = getName(child);
        if (name.startsWith(filter)) {
          String suffix = name.substring(filter.length());
          if (isDirectory(child)) {
            Collection<P> grandChildren = getChilren(child);
            if (grandChildren != null) {
              map.put(suffix + "/", false);
            } else {
              // Skip it
            }
          } else {
            map.put(suffix, true);
          }
        }
      }
      return map;
    } else {
      return ValueCompletion.create();
    }
  }
}
