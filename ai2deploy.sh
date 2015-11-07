#!/usr/bin/env bash

# Deploy tiburon.jar to ai2 public releases repo.
# Requires ~/.m2/setings.xml to configure the "ai2" server.

# See all the commands.
set -x

GROUP_ID="edu.isi"
ARTIFACT_ID="tiburon"
VERSION="1.0.1"
REPOSITORY_ID="ai2"
URL="http://utility.allenai.org:8081/nexus/content/repositories/public-releases"
FILE="./tiburon.jar"

# Broken, the URL is not public and requires authentication.
COMPLETE_URL="$URL/${GROUP_ID/.//}/$ARTIFACT_ID/$VERSION/"
if [[ `wget -S --spider $COMPLETE_URL  2>&1 | grep 'HTTP/1.1 200 OK'` ]]; then
  echo "URL exists: $url. Please bump VERSION=$VERSION"
  exit 1
fi

sh README-compile

mvn deploy:deploy-file \
  -DgroupId=$GROUP_ID \
  -DartifactId=$ARTIFACT_ID \
  -Dversion=$VERSION \
  -DgeneratePom=true \
  -Dpackaging=jar \
  -DrepositoryId=$REPOSITORY_ID \
  -Durl=$URL \
  -Dfile=$FILE

if [ $? -ne 0 ]; then
    echo "Maven upload fail. Perhaps you forgot to bump the VERSION, currently at $VERSION?"
fi