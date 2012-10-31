/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.util.urltemplate;

import java.util.*;

public class Template {

  private Scheme scheme;
  private boolean hasScheme;
  private UsernameSegment username;
  private Password password;
  private Host host;
  private Port port;
  private boolean hasAuthority;
  private List<Path> path;
  private boolean isAbsolute;
  private boolean isDirectory;
  private Map<String,Query> query;
  private Query extra;
  private boolean hasQuery;
  private Fragment fragment;
  private boolean hasFragment;
  private String image;
  private Integer hash;

  Template(
      Scheme scheme,
      boolean hasScheme,
      UsernameSegment username,
      Password password,
      Host host,
      Port port,
      boolean hasAuthority,
      List<Path> path,
      boolean isAbsolute,
      boolean isDirectory,
      LinkedHashMap<String,Query> query,
      Query extra,
      boolean hasQuery,
      Fragment fragment,
      boolean hasFragment ) {
    this.scheme = scheme;
    this.hasScheme = hasScheme;
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;
    this.hasAuthority = hasAuthority;
    this.path = Collections.unmodifiableList( path );
    this.isAbsolute = isAbsolute;
    this.isDirectory = isDirectory;
    this.query = Collections.unmodifiableMap( query );
    this.extra = extra;
    this.hasQuery = hasQuery;
    this.fragment = fragment;
    this.hasFragment = hasFragment;
    this.image = null;
    this.hash = null;
  }

  public Scheme getScheme() {
    return scheme;
  }

  public boolean hasScheme() {
    return hasScheme;
  }

  public UsernameSegment getUsername() {
    return username;
  }

  public Password getPassword() {
    return password;
  }

  public Host getHost() {
    return host;
  }

  public Port getPort() {
    return port;
  }

  public boolean hasAuthority() {
    return hasAuthority;
  }

  public List<Path> getPath() {
    return path;
  }

  public boolean isAbsolute() {
    return isAbsolute;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public Map<String,Query> getQuery() {
    return query;
  }

  public Query getExtra() {
    return extra;
  }

  public boolean hasQuery() {
    return hasQuery;
  }

  public Fragment getFragment() {
    return fragment;
  }

  public boolean hasFragment() {
    return hasFragment;
  }

  private void buildScheme( StringBuilder b ) {
    if( hasScheme ) {
      if( scheme != null ) {
        b.append( scheme.getFirstValue().getPattern() );
      }
      b.append( ':' );
    }
  }

  private void buildAuthority( StringBuilder b ) {
    if( hasAuthority ) {
      b.append( "//" );
      if( username != null || password != null ) {
        if( username != null ) {
          b.append( username.getFirstValue().getPattern() );
        }
        if( password != null ) {
          b.append( ':' );
          b.append( password.getFirstValue().getPattern() );
        }
        b.append( "@" );
        if( host != null ) {
          b.append( host.getFirstValue().getPattern() );
        }
        if( port != null ) {
          b.append( ':' );
          b.append( port.getFirstValue().getPattern() );
        }
      }
    }
  }

  private void buildPath( StringBuilder b ) {
    if( isAbsolute ) {
      b.append( '/' );
    }
    boolean first = true;
    for( Path segment: path ) {
      if( first ) {
        first = false;
      } else {
        b.append( '/' );
      }
      String paramName = segment.getParamName();
      if( paramName != null && paramName.length() > 0 ) {
        b.append( "{" );
        b.append( segment.getParamName() );
        b.append( '=' );
        b.append( segment.getFirstValue().getPattern() );
        b.append( '}' );
      } else {
        b.append( segment.getFirstValue().getPattern() );
      }
    }
    if( isDirectory && ( !isAbsolute || path.size() > 0 ) ) {
      b.append( '/' );
    }
  }

  private void buildQuery( StringBuilder b ) {
    if( hasQuery ) {
      b.append( '?' );
      boolean first = true;
      for( Query segment: query.values() ) {
        if( first ) {
          first = false;
        } else {
          b.append( '&' );
        }
        String paramName = segment.getParamName();
        for( Segment.Value value: segment.getValues() ) {
          if( paramName != null && paramName.length() > 0 ) {
            b.append( segment.getQueryName() );
            b.append( "={" );
            b.append( segment.getParamName() );
            b.append( '=' );
            b.append( value.getPattern() );
            b.append( '}' );
          } else {
            b.append( segment.getQueryName() );
            b.append( "=" );
            b.append( value.getPattern() );
          }
        }
      }
    }
  }

  private void buildFragment( StringBuilder b ) {
    if( hasFragment ) {
      b.append( '#' );
      if( fragment != null ) {
        b.append( fragment.getFirstValue().getPattern() );
      }
    }
  }

  public String toString() {
    String s = image;
    if( s == null ) {
      StringBuilder b = new StringBuilder();
      buildScheme( b );
      buildAuthority( b );
      buildPath( b );
      buildQuery( b );
      buildFragment( b );
      s = b.toString();
      image = s;
    }
    return s;
  }

  public int hashCode() {
    Integer hc = hash;
    if( hc == null ) {
      hc = toString().hashCode();
      hash = hc;
    }
    return hc.intValue();
  }

  public boolean equals( Object object ) {
    boolean equals = false;
    if( object != null && object instanceof Template ) {
      String thisStr = toString();
      String thatStr = object.toString();
      equals = thisStr.equals( thatStr );
    }
    return equals;

  }
}
