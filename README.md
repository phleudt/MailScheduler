# MailScheduler - - Automated Email CRM System

MailScheduler is a Java-based automated email marketing and customer relationship management system that simplifies 
communication with contacts by synchronizing data with Google Sheets, sending personalized email sequences, 
and intelligently tracking responses.

## Overview

MailScheduler helps you automate your email communications to clients by providing:
- Automated email scheduling and sending
- Template-based email composition
- Integration with Google Sheets for recipient management
- Email tracking and status monitoring
- Support for follow-up emails
- Customizable scheduling options

## Features

- **Email Management**
    - Schedule and send email with follow-ups
    - Create and manage reusable email templates with personalization variables
    - Track email status and delivery
    - Automatically detect replies to stop ongoing email sequences

- **Google Integration**
    - Seamless Gmail API integration
    - **Google Sheets integration**: Sync contacts and configurations directly from Google Sheets 
    - OAuth 2.0 authentication

- **Data Management**
    - Local database for storing email data
    - Synchronization with Google Sheets
    - Email status tracking
    - Error logging and monitoring

## Architecture

The application follows a clean, layered architecture:

### Domain Layer
- Contains core business entities, value objects, and business logic
- Defines interfaces for repositories
- Implements domain services and business rules

### Application Layer
- Orchestrates use cases by coordinating between domain objects and infrastructure
- Contains application services
- Manages application-specific business rules

### Infrastructure Layer
- Provides concrete implementations of domain interfaces
- Contains database access, external API integrations, and email services
- Handles technical concerns like persistence and communication

## Technical Stack

- **Language**: Java
- **Database**: SQLite
- **External APIs**: Google Sheets API, Gmail API
- **Key Libraries**:
    - Google API Client Library
    - JavaMail API
    - SQLite JDBC

## Getting Started

### Prerequisites
- Java JDK 17 or higher

### Google Cloud Setup

1. Create a new project in the [Google Cloud Console](https://console.cloud.google.com)
2. Enable the following APIs:
    - Gmail API
    - Google Sheets API
3. Create OAuth 2.0 credentials:
    - Go to "Credentials" in the Google Cloud Console
    - Create OAuth 2.0 Client ID
    - Download the credentials and save as `googleClientSecrets.json`

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/phleudt/MailScheduler.git
   cd MailScheduler
   ```
2. Configure Google API credentials
    - Place your `googleClientSecrets.json` file in the `src/main/resources` directory
    - Run the application once to complete OAuth authentication

3. Set up your configuration in Google Sheets
   - Copy the [CRM sheet template](https://docs.google.com/spreadsheets/d/1Bw7M84fZXJGnnbdhS3NaAh2CJYo2rpqhT-TIC8KLUxY/template/preview) 
   - Place in the Configuration sheet your sender email address
   - Copy the spreadsheet id from the spreadsheet and place it into the spreadsheetId variable in `src/main/java/com.mailscheduler/Main.java`

4. Build the application:
   ```bash
   ./gradle build
   ```

5. Run the application
   ```bash
   java -jar build/libs/MailScheduler.jar
   ```

## Configuration
The application can be configured via the Configuration sheet in your spreadsheet 
- Default sender email
- Recipient column mappings
- Email template settings
- Follow-Up schedule

## Planned Features
- Multiple schedule support
- Custom placeholder support
- Data encryption
- Improved error handling and reporting (especially in the spreadsheet)
- Performance optimizations for API calls
- Logging improvements

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to 
discuss what you would like to change.

## License
This project is licensed under the [MIT License](LICENSE.md)

## Status
⚠️ This project is currently in development. Features and APIs may change without notice. Bugs may occur.