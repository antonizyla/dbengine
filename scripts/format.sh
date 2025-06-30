#!/bin/sh

java -jar google-java-format.jar --replace src/main/java/com/mycompany/app/*.java
git config user.name github-actions
git config user.email github-actions@github.com
git add .
git commit -m "Formatted Project" || echo "No commit required"
git push