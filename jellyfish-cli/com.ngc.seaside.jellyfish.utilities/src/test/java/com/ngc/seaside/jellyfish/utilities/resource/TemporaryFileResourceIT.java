package com.ngc.seaside.jellyfish.utilities.resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class TemporaryFileResourceIT {

   private ITemporaryFileResource resource;

   private URL resourceUrl;

   private InputStream inputStream;

   @Before
   public void setup() throws Throwable {
      resourceUrl = TemporaryFileResourceIT.class.getClassLoader().getResource("testResource.txt");
      assertNotNull("failed to open resource for testing!",
                    resourceUrl);
      inputStream = resourceUrl.openStream();
   }

   @Test
   public void testDoesReadResourceFromJar() throws Throwable {
      resource = TemporaryFileResource.forClasspathResource(TemporaryFileResourceIT.class, "testResource.txt");
      assertEquals("url not correct!",
                   resourceUrl,
                   resource.getURL());
      resource.read(inputStream);

      Path tempFile = resource.getTemporaryFile();
      assertNotNull("tempFile not created!",
                    tempFile);
      assertEquals("fileName is not correct!",
                   "testResource.txt",
                   tempFile.getFileName().toString());
   }

   @After
   public void teardown() throws Throwable {
      if (inputStream != null) {
         inputStream.close();
      }
   }
}