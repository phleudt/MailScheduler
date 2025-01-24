# MailScheduler

MailScheduler is a robust Java application that automates email communication by integrating with Google services. It allows you to schedule, manage, and track emails using Gmail API while managing recipients through Google Sheets.

## Overview

MailScheduler helps you automate your email communications by providing:
- Automated email scheduling and sending
- Template-based email composition
- Integration with Google Sheets for recipient management
- Email tracking and status monitoring
- Support for follow-up emails
- Customizable scheduling options

## Features

- **Email Management**
   - Schedule emails for future delivery
   - Create and manage email templates
   - Track email status and delivery
   - Handle follow-up communications

- **Google Integration**
   - Seamless Gmail API integration
   - Google Sheets integration for recipient management
   - OAuth 2.0 authentication
   - Secure credential management

- **Data Management**
   - Local database for storing email data
   - Synchronization with Google Sheets
   - Email status tracking
   - Error logging and monitoring

## Installation

### Prerequisites
- Java 11 or higher
- Gradle build tool
- Google Cloud Platform account
- Google API credentials

### Google Cloud Setup

1. Create a new project in the [Google Cloud Console](https://console.cloud.google.com)
2. Enable the following APIs:
   - Gmail API
   - Google Sheets API
3. Create OAuth 2.0 credentials:
   - Go to "Credentials" in the Google Cloud Console
   - Create OAuth 2.0 Client ID
   - Download the credentials and save as `googleClientSecrets.json`
   - Place the file in the `app/src/main/resources` directory

### Build and Run

1. Clone the repository:
   ```bash
   git clone https://github.com/phleudt/MailScheduler.git
   cd MailScheduler
   ```
2. Build the project using Gradle:
   ```bash
   ./gradlew build
   ```

3. Run the application:
   ```bash
   ./gradlew run
   ```

## Configuration
The application uses a `config.properties` file for configuration. Key settings include:
- Default sender email
- Spreadsheet IDs
- Contact column mappings
- Email template settings

## Planned Features
- Multiple schedule support with timezone handling
- Enhanced template management
- Custom placeholder support
- Docker containerization
- Data encryption
- Improved error handling and reporting
- Performance optimizations for API calls
- Logging improvements

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## License
This project is licensed under the [MIT License](LICENSE.md)

## Status
⚠️ This project is currently in development. Features and APIs may change without notice.