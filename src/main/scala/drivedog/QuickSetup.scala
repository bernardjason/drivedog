package drivedog

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Arrays
import java.util.List
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.ParentReference
import scala.collection.JavaConversions._
import com.typesafe.scalalogging.StrictLogging


object QuickSetup extends StrictLogging{
    /** Application name. */
    val APPLICATION_NAME = "drivedog";

    val DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    val  DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);

    val JSON_FACTORY = JacksonFactory.getDefaultInstance();

    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

    val SCOPES =
        Arrays.asList(
        DriveScopes.DRIVE_METADATA_READONLY
        ,DriveScopes.DRIVE_FILE	
        ,DriveScopes.DRIVE
        		);
    
    def authorize() =  {
      
        val in = this.getClass.getResourceAsStream("/client_secret.json");
        val clientSecrets =  GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        val flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        val credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");

        logger.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        
        credential;
    }

  
    def getDriveService() =  {
        val credential = authorize();
        new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

 
}

