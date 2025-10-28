# Database Engine

A sqlite type database engine that works on a *limited* subset of SQL. Using a recursive descent parser for creating an AST from the provided code. 

Automated testing using GitHub Actions on every commit using `JUnit` and builds using Maven.

# Creating a Database

Like a traditional DBMS creation of databases is straightforward.

```
>> CREATE DATABASE databaseName;
```

Then to enter a database simply do as follows

```
>> databaseName;
```

# Creating a Table