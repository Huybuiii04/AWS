# MyNewProject

A Maven-based Java project.

## Project Structure
```
MyNewProject/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── vgu/
│   │           └── cloud26/
│   │               └── App.java
│   └── test/
│       └── java/
│           └── vgu/
│               └── cloud26/
│                   └── AppTest.java
```

## Build and Run

### Build the project
```bash
mvn clean package
```

### Run the application
```bash
java -cp target/MyNewProject-1.0-SNAPSHOT.jar vgu.cloud26.App
```

### Run tests
```bash
mvn test
```

## Requirements
- Java 21
- Maven 3.6+
