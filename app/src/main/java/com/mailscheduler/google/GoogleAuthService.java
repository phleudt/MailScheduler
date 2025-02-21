package com.mailscheduler.google;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.mailscheduler.google.auth.exceptions.MalformedRefreshTokenException;
import com.mailscheduler.google.auth.exceptions.MissingRefreshTokenException;
import com.mailscheduler.google.auth.exceptions.NoInternetConnectionException;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public abstract class GoogleAuthService<T> {
    protected static final String APPLICATION_NAME = "com/mailscheduler";
    protected static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String TOKENS_FILE_NAME = "refresh_tokens.txt";
    private static final String CREDENTIALS_FILE_PATH = "/googleClientSecrets.json";
    protected static NetHttpTransport HTTP_TRANSPORT;

    protected abstract List<String> getScopes();
    protected abstract void initializeService(Credential credential);

    protected GoogleAuthService() throws Exception {
        initialize();
    }

    private void initialize() throws Exception {
        if (!isInternetAvailable()) {
            throw new NoInternetConnectionException("No internet connection available. Please check your network.");
        }

        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        authorizeWithRetry();
    }

    /**
     * Check internet connectivity by attempting to connect to a reliable host
     * @return boolean indicating internet connectivity
     */
    private boolean isInternetAvailable() {
        try {
            // Attempt to connect to Google's DNS server
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void authorizeWithRetry()
            throws IOException, MissingRefreshTokenException, MalformedRefreshTokenException {
        String refreshToken = loadRefreshToken();

        if (refreshToken == null) {
            System.out.println("No refresh token found. Prompting the user to authorize the application...");
            refreshToken = authorizeApplication();
        }

        try {
            initializeServiceWithRefreshToken(refreshToken);
        } catch (TokenResponseException e) {
            handleTokenException(e);
        }
    }

    private String loadRefreshToken() {
        String className = getClass().getSimpleName();
        File tokenFile = ensureTokenFile();

        if (tokenFile != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(tokenFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(className + ":")) {
                        return line.substring(className.length() + 1);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading refresh token: " + e.getMessage());
            }
        }
        return null;
    }

    private void handleTokenException(TokenResponseException e) throws IOException {
        if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
            System.out.println("Refresh token has expired. Prompting user to authorize the application...");
            retryAuthorization();
        } else {
            System.out.println("Error during authorization with retry: " + e.getMessage());
            if (e.getDetails() != null) {
                System.out.println("Error details: " + e.getDetails());
            }
            if ("invalid_grant".equals(e.getDetails().getError())) {
                System.out.println("Refresh token has expired. Prompting user to authorize the application...");
                retryAuthorization();
            } else {
                System.out.println("Authorization error: " + e.getMessage());
            }
            throw new RuntimeException("Authorization error: " + e.getMessage(), e);
        }
    }

    private void retryAuthorization() throws IOException {
        String refreshToken;
        while (true) {
            try {
                refreshToken = authorizeApplication();
                initializeServiceWithRefreshToken(refreshToken);
                break;
            } catch (MissingRefreshTokenException | MalformedRefreshTokenException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void initializeServiceWithRefreshToken(String refreshToken) throws IOException {
        // Load client secrets
        GoogleClientSecrets clientSecrets = getGoogleClientSecrets();

        GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                refreshToken,
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret()
        ).execute();

        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setJsonFactory(JSON_FACTORY)
                .setTransport(HTTP_TRANSPORT)
                .setClientAuthentication(new ClientParametersAuthentication(
                        clientSecrets.getDetails().getClientId(),
                        clientSecrets.getDetails().getClientSecret())
                )
                .setTokenServerUrl(new GenericUrl(clientSecrets.getDetails().getTokenUri()))
                .build()
                .setFromTokenResponse(tokenResponse);

        initializeService(credential);
    }

    private String authorizeApplication()
            throws MissingRefreshTokenException, IOException, MalformedRefreshTokenException {
        Credential credential = getCredentials();
        String newRefreshToken = credential.getRefreshToken();
        updateRefreshTokenFile(newRefreshToken);
        return newRefreshToken;
    }

    private Credential getCredentials()
            throws MissingRefreshTokenException, IOException, MalformedRefreshTokenException {
        GoogleClientSecrets clientSecrets = getGoogleClientSecrets();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, getScopes())
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        String url = flow.newAuthorizationUrl()
                .setRedirectUri(clientSecrets.getDetails().getRedirectUris().get(0))
                .build();

        System.out.println("Please open the following URL in your browser then type the authorization code:");
        System.out.println("   " + url);
        return performTokenRequest(flow, clientSecrets);
    }

    private Credential performTokenRequest(GoogleAuthorizationCodeFlow flow, GoogleClientSecrets clientSecrets)
        throws IOException, MalformedRefreshTokenException, MissingRefreshTokenException {
        String code = readAuthorizationCode();

        try {
            GoogleTokenResponse response = flow.newTokenRequest(code)
                    .setRedirectUri(clientSecrets.getDetails().getRedirectUris().get(0))
                    .execute();
            return flow.createAndStoreCredential(response, "user");
        } catch (TokenResponseException e) {
            throw handleTokenResponseException(e);
        }
    }

    private String readAuthorizationCode() {
        System.out.println("Enter authorization code: ");
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read authorization code", e);
        }
    }

    private GoogleClientSecrets getGoogleClientSecrets() throws IOException {
        // Load client secrets
        InputStream in = GoogleAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    }

    private TokenResponseException handleTokenResponseException(TokenResponseException e)
            throws MalformedRefreshTokenException, MissingRefreshTokenException {
        if (e.getDetails().getError().equals("invalid_grant")) {
            throw new MalformedRefreshTokenException("Malformed refresh token. Prompting user to authorize the application again.");
        } else if (e.getDetails().getError().equals("invalid_request")) {
            throw new MissingRefreshTokenException("Missing refresh token. Prompting user to authorize the application again.");
        } else {
            throw new RuntimeException("Unexpected token response error: " + e.getMessage(), e);
        }
    }

    private void updateRefreshTokenFile(String refreshToken) {
        String className = getClass().getSimpleName(); // Get the class name of the current instance
        File tokenFile = ensureTokenFile();

        if (tokenFile == null) {
            System.out.println("Cannot update refresh token file");
            return;
        }

        List<String> fileContent = new ArrayList<>();

        // Read existing content
        try (BufferedReader reader = new BufferedReader(new FileReader(tokenFile))) {
            String line;
            boolean tokenUpdated = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(className + ":")) {
                    // Update the refresh token for the current class
                    fileContent.add(className + ":" + refreshToken);
                    tokenUpdated = true;
                } else {
                    // Keep the other refresh tokens unchanged
                    fileContent.add(line);
                }
            }

            // If the class's token wasn't found, add it as a new entry
            if (!tokenUpdated) {
                fileContent.add(className + ":" + refreshToken);
            }
        } catch (IOException e) {
            System.out.println("Error reading refresh token file: " + e.getMessage());
            return;
        }

        // Write the updated content back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tokenFile))) {
            for (String contentLine : fileContent) {
                writer.write(contentLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error updating refresh token file: " + e.getMessage());
        }
    }

    private File ensureTokenFile() {
        File tokensDir = new File(TOKENS_DIRECTORY_PATH);
        File tokenFile = new File(tokensDir, TOKENS_FILE_NAME);

        try {
            // Create tokens directory if it doesn't exist
            if (!tokensDir.exists()) {
                boolean dirCreated = tokensDir.mkdirs();
                if (!dirCreated) {
                    System.out.println("Failed to create tokens directory");
                    return null;
                }
            }

            // Create refresh_tokens.txt file if it doesn't exist
            if (!tokenFile.exists()) {
                boolean fileCreated = tokenFile.createNewFile();
                if (!fileCreated) {
                    System.out.println("Failed to create refresh_tokens.txt file");
                    return null;
                }
            }

            return tokenFile;
        } catch (IOException e) {
            System.out.println("Error ensuring tokens file exists: " + e.getMessage());
            return null;
        }
    }

}
