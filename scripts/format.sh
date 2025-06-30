#!/bin/sh

find . -type f -name "*.java" | xargs java -jar google-java-format.jar --replace
git config user.name github-actions
git config user.email github-actions@github.com
git add .
git commit -m "Formatted Project" || echo "No commit required"
git push