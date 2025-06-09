package com.mailscheduler.infrastructure.google.auth;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.mailscheduler.infrastructure.google.auth.exceptions.MalformedRefreshTokenException;
import com.mailscheduler.infrastructure.google.auth.exceptions.MissingRefreshTokenException;
import com.mailscheduler.infrastructure.google.auth.exceptions.NoInternetConnectionException;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base authentication service for Google APIs.
 * Handles OAuth2 authentication flow, token management, and service initialization.
 *
 * @param <T> The Google API service type this auth service will initialize
 */
public abstract class GoogleAuthService<T> {
    protected static final String APPLICATION_NAME = "MailScheduler";
    protected static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String TOKENS_FILE_NAME = "refresh_tokens.txt";
    private static final String CREDENTIALS_FILE_PATH = "/googleClientSecrets.json";
    protected static NetHttpTransport HTTP_TRANSPORT;

    private static final Logger LOGGER = Logger.getLogger(GoogleAuthService.class.getName());

    /**
     * Returns the OAuth2 scopes required for this service.
     *
     * @return List of OAuth2 scope strings
     */
    protected abstract List<String> getScopes();

    /**
     * Initializes the specific Google API service with the provided credential.
     *
     * @param credential The OAuth2 credential to use for API calls
     */
    protected abstract void initializeService(Credential credential);

    /**
     * Constructor that initializes the service by checking internet connectivity and starting the authorization flow.
     *
     * @throws NoInternetConnectionException If no internet connection is available
     * @throws IOException If an I/O error occurs during initialization
     * @throws MissingRefreshTokenException If a refresh token is required but not available
     * @throws MalformedRefreshTokenException If a refresh token is malformed
     */
    protected GoogleAuthService() throws Exception {
        initialize();
    }

    /**
     * Initializes the HTTP transport and starts the authorization flow.
     *
     * @throws NoInternetConnectionException If no internet connection is available
     * @throws IOException If an I/O error occurs
     * @throws MissingRefreshTokenException If refresh token is missing
     * @throws MalformedRefreshTokenException If refresh token is malformed
     */
    private void initialize() throws Exception {
        if (!isInternetAvailable()) {
            throw new NoInternetConnectionException("No internet connection available. Please check your network.");
        }

        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        authorizeWithRetry();
    }

    /**
     * Checks internet connectivity by attempting to connect to Google's DNS server.
     *
     * @return true if internet is available, false otherwise
     */
    private boolean isInternetAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
            socket.close();
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Internet connectivity check failed: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to authorize using a stored refresh token, or prompts for new authorization if needed.
     *
     * @throws IOException If an I/O error occurs
     * @throws MissingRefreshTokenException If refresh token is missing
     * @throws MalformedRefreshTokenException If refresh token is malformed
     */
    private void authorizeWithRetry()
            throws IOException, MissingRefreshTokenException, MalformedRefreshTokenException {
        String refreshToken = loadRefreshToken();

        if (refreshToken == null) {
            LOGGER.info("No refresh token found. Prompting the user to authorize the application...");
            refreshToken = authorizeApplication();
        }

        try {
            initializeServiceWithRefreshToken(refreshToken);
        } catch (TokenResponseException e) {
            handleTokenException(e);
        }
    }

    /**
     * Loads the refresh token for this service from the token file.
     *
     * @return The refresh token, or null if not found
     */
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
                LOGGER.log(Level.WARNING, "Error reading refresh token: {0}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Handles token exceptions by initiating reauthorization if necessary.
     *
     * @param e The TokenResponseException that occurred
     * @throws IOException If an I/O error occurs during reauthorization
     */
    private void handleTokenException(TokenResponseException e) throws IOException {
        if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
            LOGGER.info("Refresh token has expired. Prompting user to authorize the application...");
            retryAuthorization();
        } else {
            LOGGER.log(Level.SEVERE, "Error during authorization: {0}", e.getMessage());
            if (e.getDetails() != null) {
                LOGGER.log(Level.SEVERE, "Error details: {0}", e.getDetails());
            }
            throw new RuntimeException("Authorization error: " + e.getMessage(), e);
        }
    }

    /**
     * Retries authorization until successful.
     *
     * @throws IOException If an I/O error occurs during authorization
     */
    private void retryAuthorization() throws IOException {
        String refreshToken;
        while (true) {
            try {
                refreshToken = authorizeApplication();
                initializeServiceWithRefreshToken(refreshToken);
                break;
            } catch (MissingRefreshTokenException | MalformedRefreshTokenException e) {
                LOGGER.log(Level.WARNING, "Authorization retry failed: {0}", e.getMessage());
            }
        }
    }

    /**
     * Initializes the service with the provided refresh token.
     *
     * @param refreshToken The refresh token to use
     * @throws IOException If an I/O error occurs
     */
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

    /**
     * Authorizes the application by guiding the user through the OAuth flow.
     *
     * @return The new refresh token
     * @throws MissingRefreshTokenException If refresh token is missing
     * @throws IOException If an I/O error occurs
     * @throws MalformedRefreshTokenException If refresh token is malformed
     */
    private String authorizeApplication()
            throws MissingRefreshTokenException, IOException, MalformedRefreshTokenException {
        Credential credential = getCredentials();
        String newRefreshToken = credential.getRefreshToken();
        updateRefreshTokenFile(newRefreshToken);
        return newRefreshToken;
    }

    /**
     * Gets credentials through OAuth2 flow, prompting user for authorization.
     *
     * @return The OAuth2 credential
     * @throws MissingRefreshTokenException If refresh token is missing
     * @throws IOException If an I/O error occurs
     * @throws MalformedRefreshTokenException If refresh token is malformed
     */
    private Credential getCredentials()
            throws MissingRefreshTokenException, IOException, MalformedRefreshTokenException {
        GoogleClientSecrets clientSecrets = getGoogleClientSecrets();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, getScopes())
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        String url = flow.newAuthorizationUrl()
                .setRedirectUri(clientSecrets.getDetails().getRedirectUris().get(0))
                .build();

        System.out.println("Please open the following URL in your browser then type the authorization code:");
        System.out.println("   " + url);
        return performTokenRequest(flow, clientSecrets);
    }

    /**
     * Performs the token request after user authorization.
     *
     * @param flow The authorization code flow
     * @param clientSecrets The client secrets
     * @return The OAuth2 credential
     * @throws IOException If an I/O error occurs
     * @throws MalformedRefreshTokenException If refresh token is malformed
     * @throws MissingRefreshTokenException If refresh token is missing
     */
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

    /**
     * Reads the authorization code from standard input.
     *
     * @return The authorization code
     */
    private String readAuthorizationCode() {
        System.out.println("Enter authorization code: ");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read authorization code", e);
        }
    }

    /**
     * Loads Google client secrets from the resources.
     *
     * @return The Google client secrets
     * @throws IOException If an I/O error occurs
     */
    private GoogleClientSecrets getGoogleClientSecrets() throws IOException {
        try (InputStream in = GoogleAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }
    }

    /**
     * Handles token response exceptions by converting them to more specific exceptions.
     *
     * @param e The token response exception
     * @return A more specific exception
     * @throws MalformedRefreshTokenException If the token is malformed
     * @throws MissingRefreshTokenException If the token is missing
     */
    private TokenResponseException handleTokenResponseException(TokenResponseException e)
            throws MalformedRefreshTokenException, MissingRefreshTokenException {
        if (e.getDetails().getError().equals("invalid_grant")) {
            throw new MalformedRefreshTokenException("Malformed refresh token. " +
                    "Prompting user to authorize the application again.");
        } else if (e.getDetails().getError().equals("invalid_request")) {
            throw new MissingRefreshTokenException("Missing refresh token. " +
                    "Prompting user to authorize the application again.");
        } else {
            throw new RuntimeException("Unexpected token response error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the refresh token file with the new token for this service.
     *
     * @param refreshToken The new refresh token
     */
    private void updateRefreshTokenFile(String refreshToken) {
        String className = getClass().getSimpleName();
        File tokenFile = ensureTokenFile();

        if (tokenFile == null) {
            LOGGER.warning("Cannot update refresh token file");
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
            LOGGER.log(Level.WARNING, "Error reading refresh token file: {0}", e.getMessage());
            return;
        }

        // Write the updated content back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tokenFile))) {
            for (String contentLine : fileContent) {
                writer.write(contentLine);
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error updating refresh token file: {0}", e.getMessage());
        }
    }

    /**
     * Ensures the token file exists, creating the file if it's necessary.
     *
     * @return The token file, or null if it could not be created/accessed
     */
    private File ensureTokenFile() {
        File tokensDir = new File(TOKENS_DIRECTORY_PATH);
        File tokenFile = new File(tokensDir, TOKENS_FILE_NAME);

        try {
            // Create tokens directory if it doesn't exist
            if (!tokensDir.exists()) {
                boolean dirCreated = tokensDir.mkdirs();
                if (!dirCreated) {
                    LOGGER.warning("Failed to create tokens directory");
                    return null;
                }
            }

            // Create refresh_tokens.txt file if it doesn't exist
            if (!tokenFile.exists()) {
                boolean fileCreated = tokenFile.createNewFile();
                if (!fileCreated) {
                    LOGGER.warning("Failed to create" + TOKENS_FILE_NAME + " file");
                    return null;
                }
            }

            return tokenFile;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error ensuring tokens file exists: {0}", e.getMessage());
            return null;
        }
    }

}
