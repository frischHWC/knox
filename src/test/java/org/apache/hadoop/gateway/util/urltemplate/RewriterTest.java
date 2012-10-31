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

import edu.emory.mathcs.backport.java.util.Arrays;
import org.easymock.EasyMock;
import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RewriterTest {

  @Test
  public void testBasicRewrite() throws Exception {
    URI inputUri, outputUri;
    Template inputTemplate, outputTemplate;
    Resolver resolver = new Params();

    inputUri = new URI( "path-1/path-2" );
    inputTemplate = Parser.parse( "{path-1-name}/{path-2-name}" );
    outputTemplate = Parser.parse( "{path-2-name}/{path-1-name}" );
    outputUri = Rewriter.rewrite( inputUri, inputTemplate, outputTemplate, resolver );
    assertThat( outputUri.toString(), equalTo( "path-2/path-1" ) );

    inputUri = new URI( "path-1/path-2/path-3/path-4" );
    inputTemplate = Parser.parse( "path-1/{path=**}/path-4" ); // Need the ** to allow the expansion to include all path values.
    outputTemplate = Parser.parse( "new-path-1/{path=**}/new-path-4" );
    outputUri = Rewriter.rewrite( inputUri, inputTemplate, outputTemplate, resolver );
    assertThat( outputUri.toString(), equalTo( "new-path-1/path-2/path-3/new-path-4" ) );

    inputUri = new URI( "some-path?query-name=some-param-value" );
    inputTemplate = Parser.parse( "{path-name}?query-name={param-value}" );
    outputTemplate = Parser.parse( "{param-value}/{path-name}" );
    outputUri = Rewriter.rewrite( inputUri, inputTemplate, outputTemplate, resolver );
    assertThat( outputUri.toString(), equalTo( "some-param-value/some-path" ) );
  }

  @Test
  public void testRewriteUrlWithHttpServletRequestAndFilterConfig() throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock( HttpServletRequest.class );
    EasyMock.expect( request.getParameter( "request-param-name" ) ).andReturn( "request-param-value" ).anyTimes();
    EasyMock.expect( request.getParameterValues( "request-param-name" ) ).andReturn( new String[]{"request-param-value"} ).anyTimes();
    EasyMock.replay( request );

    FilterConfig config = EasyMock.createNiceMock( FilterConfig.class );
    EasyMock.expect( config.getInitParameter( "filter-param-name" ) ).andReturn( "filter-param-value" ).anyTimes();
    EasyMock.replay( config );

    Template sourcePattern, targetPattern;
    URI actualInput, actualOutput, expectOutput;

    actualInput = new URI( "http://some-host:0/some-path" );
//    sourcePattern = Parser.parse( "**" );
    sourcePattern = Parser.parse( "*://*:*/**" );
    targetPattern = Parser.parse( "should-not-change" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "should-not-change" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://some-host:0/some-path" );
//    sourcePattern = Parser.parse( "**" );
    sourcePattern = Parser.parse( "*://*:*/{0=**}" );
    targetPattern = Parser.parse( "{0}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "some-path" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://some-host:0/pathA/pathB/pathC" );
//    sourcePattern = Parser.parse( "*/pathA/*/*" );
    sourcePattern = Parser.parse( "*://*:*/pathA/{1=*}/{2=*}" );
    targetPattern = Parser.parse( "http://some-other-host:*/{2}/{1}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "http://some-other-host/pathC/pathB" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://some-host:0/some-path" );
    sourcePattern = Parser.parse( "*://*:*/**" );
    targetPattern = Parser.parse( "{filter-param-name}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "filter-param-value" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://some-host:0/some-path" );
    sourcePattern = Parser.parse( "*://*:*/**" );
    targetPattern = Parser.parse( "{request-param-name}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "request-param-value" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://some-host:0/some-path" );
    sourcePattern = Parser.parse( "*://*:*/**" );
    targetPattern = Parser.parse( "http://some-other-host/{filter-param-name}/{request-param-name}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "http://some-other-host/filter-param-value/request-param-value" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://some-host:0/pathA/pathB/pathC" );
    sourcePattern = Parser.parse( "*://*:*/pathA/{1=*}/{2=*}" );
    targetPattern = Parser.parse( "http://some-other-host/{2}/{1}/{filter-param-name}/{request-param-name}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "http://some-other-host/pathC/pathB/filter-param-value/request-param-value" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "/namenode/api/v1/test" );
    sourcePattern = Parser.parse( "/namenode/api/v1/{0=**}" );
    targetPattern = Parser.parse( "http://{filter-param-name}/webhdfs/v1/{0}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "http://filter-param-value/webhdfs/v1/test" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "/namenode/api/v1/test" );
    sourcePattern = Parser.parse( "/namenode/api/v1/{0=**}" );
    targetPattern = Parser.parse( "http://{filter-param-name}/webhdfs/v1/{0}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    expectOutput = new URI( "http://filter-param-value/webhdfs/v1/test" );
    assertThat( actualOutput, equalTo( expectOutput ) );

    actualInput = new URI( "http://vm.home:50075/webhdfs/v1/test/file?op=CREATE&user.name=hdfs&overwrite=false" );
    expectOutput = new URI( "http://filter-param-value/gateway/cluster/namenode/api/v1/test/file?op=CREATE&user.name=hdfs&overwrite=false" );
    sourcePattern = Parser.parse( "*://*:*/webhdfs/v1/{path=**}?op={op=*}&user.name={username=*}&overwrite={overwrite=*}" );
    targetPattern = Parser.parse( "http://{filter-param-name}/gateway/cluster/namenode/api/v1/{path=**}?op={op}&user.name={username}&overwrite={overwrite}" );
    actualOutput = Rewriter.rewrite( actualInput, sourcePattern, targetPattern, new TestResolver( config, request ) );
    assertThat( actualOutput, equalTo( expectOutput ) );

    // *://**/webhdfs/v1/{path=**}?**={**}
    // http://{gateway.address}/gateway/cluster/namenode/api/v1/{path}?**={**}
    // 1) Should not add query if none in source.
    // 2) Should only add unmatch query parameters
    // Consider chaning = within {} to : and wrapping query fully within {} (e.g. {query=pattern:alias}
    // *://**/webhdfs/v1/{path:**}?{**=**} // Means 0..n query names allowed/expanded.  Each can have 0..n values.
    // *://**/webhdfs/v1/{path:**}?{*=**} // Means one and only one query name matched.  Only last unused param is expanded.
    // *://**/webhdfs/v1/{path:**}?{**=*} // Means 0..n query names required.  Only last value is matched/expanded.
    // *://**/webhdfs/v1/{path:**}?{*=*} // Means one and only one query name required.  Only last value is matched/expanded.
    // /top/{**:mid}/bot
    // ?{query=pattern:alias}
    // ?{query} => ?{query=*:query}
    // ?{query=pattern} => ?{query=pattern:query}

  }

  private class TestResolver implements Resolver {

    private FilterConfig config;
    private HttpServletRequest request;

    private TestResolver( FilterConfig config, HttpServletRequest request ) {
      this.config = config;
      this.request = request;
    }

    // Picks the values from either the request or the config in that order.
    @Override
    @SuppressWarnings( "unchecked" )
    public List<String> getValues( String name ) {
      List<String> values = null;

      if( request !=  null ) {
        String[] array = request.getParameterValues( name );
        if( array != null ) {
          values = (List<String>)Arrays.asList( array );
          return values;
        }
      }

      if( config != null ) {
        String value = config.getInitParameter( name );
        if( value != null ) {
          values = new ArrayList<String>( 1 );
          values.add( value );
          return values;
        }
      }

      return values;
    }
  }

}
