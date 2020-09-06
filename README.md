# Maildozer
 **The easy way to clean up your mails.**
 
With Maildozer you can clean up your inbox. The application can only be executed in a terminal.
It determines the number of mails from each sender and you can decide whether to delete or move them to a (sub-)folder.

This is primarily my test project to get to know (new) functions of Kotlin and MongoDB. However, it works.;)
 
## Quick-Setup
1. Open a terminal and enter:
```
cd 'path/to/the/maildozer/jar'
java -jar [maildozer-artifact-name].jar
```
2.Enter "help" or start the interactive mode with "start".

## Contribution

You want to contribute? That's great! Please mail me your request and I give you the appropiate rights.

### Tools for development

- Kotlin 1.3.72
- Gradle 6.3

### Used technologies and libraries

- Spring Boot 2.3
- Spring Shell
- Kotlin 1.3.72
- Java Mail 1.5
- MongoDB (embedded)

An executable Jar can be built with:
```
gradle build
```
