#!/bin/sh

wget https://github.com/google/google-java-format/releases/download/v1.27.0/google-java-format-1.27.0-all-deps.jar

find . -type f -name "*.java" | xargs java -jar google-java-format-1.27.0-all-deps.jar --replace

git config user.name github-actions
git config user.email github-actions@github.com
git add src/
git commit -m "Formatted Project" || echo "No commit required"
git push