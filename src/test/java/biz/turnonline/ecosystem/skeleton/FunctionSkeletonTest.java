/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.turnonline.ecosystem.skeleton;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpRequest.HttpPart;
import com.google.cloud.functions.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( JUnit4.class )
public class FunctionSkeletonTest
{
    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    private BufferedWriter writerOut;

    private StringWriter responseOut;

    /**
     * Reads the content of the file in the same package as this test and converts it into a string.
     *
     * @param filename the file name to be read
     * @return the string content of the file
     */
    private static String readString( String filename )
    {
        InputStream stream = FunctionSkeletonTest.class.getResourceAsStream( filename );
        if ( stream == null )
        {
            throw new IllegalArgumentException( filename + " not found" );
        }
        return new BufferedReader( new InputStreamReader( stream ) )
                .lines()
                .collect( Collectors.joining( System.lineSeparator() ) );
    }

    @Before
    public void beforeTest() throws IOException
    {
        MockitoAnnotations.openMocks( this );

        responseOut = new StringWriter();
        writerOut = new BufferedWriter( responseOut );
        when( response.getWriter() ).thenReturn( writerOut );
    }

    @Test
    public void service_ErrorOnGet() throws Exception
    {
        when( request.getMethod() ).thenReturn( "GET" );

        new FunctionSkeleton().service( request, response );

        writerOut.flush();
        verify( response, times( 1 ) ).setStatusCode( HttpURLConnection.HTTP_BAD_METHOD );
    }

    @Test
    public void service_SaveFiles() throws Exception
    {
        InputStream bodyStream = FunctionSkeletonTest.class.getResourceAsStream( "http-body.json" );
        assert bodyStream != null;

        when( request.getMethod() ).thenReturn( "POST" );
        when( request.getReader() ).thenReturn( new BufferedReader( new InputStreamReader( bodyStream ) ) );

        InputStream stream = new ByteArrayInputStream( "foo text%n".getBytes( StandardCharsets.UTF_8 ) );

        MockHttpPart mockHttpPart = new MockHttpPart();
        mockHttpPart.setFileName( "foo.txt" );
        mockHttpPart.setInputStream( stream );

        Map<String, HttpPart> httpParts = Map.of( "mock", mockHttpPart );
        when( request.getParts() ).thenReturn( httpParts );

        new FunctionSkeleton().service( request, response );
    }

    @Test
    public void service_ProcessFields() throws Exception
    {
        when( request.getMethod() ).thenReturn( "POST" );
        when( request.getParts() ).thenReturn( Map.of() );
        when( request.getReader() ).thenReturn( new BufferedReader( new StringReader( "{}" ) ) );

        Map<String, List<String>> queryParams = Map.of( "foo", List.of( "bar" ) );

        when( request.getQueryParameters() ).thenReturn( queryParams );
        when( request.getFirstQueryParameter( "foo" ) ).thenReturn( Optional.of( "bar" ) );

        new FunctionSkeleton().service( request, response );
    }
}
