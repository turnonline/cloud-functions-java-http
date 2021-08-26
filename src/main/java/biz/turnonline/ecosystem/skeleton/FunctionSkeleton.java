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

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * HTTP function entry point.
 */
public class FunctionSkeleton
        implements HttpFunction
{
    private static final Logger LOGGER = LoggerFactory.getLogger( FunctionSkeleton.class.getName() );

    private final Gson gson;

    public FunctionSkeleton()
    {
        this.gson = new Gson();
    }

    @Override
    public void service( HttpRequest request, HttpResponse response ) throws Exception
    {
        if ( !"POST".equals( request.getMethod() ) )
        {
            response.setStatusCode( HttpURLConnection.HTTP_BAD_METHOD );
            return;
        }

        // This code will process each file uploaded.
        String tempDirectory = System.getProperty( "java.io.tmpdir" );
        for ( HttpRequest.HttpPart httpPart : request.getParts().values() )
        {
            String filename = httpPart.getFileName().orElse( null );
            if ( filename == null )
            {
                continue;
            }

            LOGGER.info( "Processed file: " + filename );

            // Note: GCF's temp directory is an in-memory file system
            // Thus, any files in it must fit in the instance's memory.
            Path filePath = Paths.get( tempDirectory, filename ).toAbsolutePath();

            // Note: files saved to a GCF instance itself may not persist across executions.
            // Persistent files should be stored elsewhere, e.g. a Cloud Storage bucket.
            Files.copy( httpPart.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING );

            Files.delete( filePath );
        }

        // This code will process other form fields.
        request.getQueryParameters().forEach(
                ( fieldName, fieldValues ) -> {
                    String firstFieldValue = fieldValues.get( 0 );
                    LOGGER.info( String.format(
                            "Processed field: %s (value: %s)", fieldName, firstFieldValue ) );
                } );

        BufferedReader reader = request.getReader();
        JsonElement jsonElement = gson.fromJson( reader, JsonElement.class );
        LOGGER.info( "JSON body = " + jsonElement );
    }
}