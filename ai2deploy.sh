#!/usr/bin/env bash

# Deploy tiburon.jar to ai2 public releases repo.
# Requires ~/.m2/setings.xml to configure the "ai2" server.

# See all the commands.
set -x

GROUP_ID="org.allenai.third_party"
ARTIFACT_ID="tiburon"
VERSION="1.0.10"
REPOSITORY_ID="bintray"
URL="https://api.bintray.com/maven/allenai/third_party/tiburon/;publish=1"
DLURL="https://dl.bintray.com/allenai/third_party"
FILE="./tiburon.jar"

COMPLETE_URL="$DLURL/${GROUP_ID//.//}/$ARTIFACT_ID/$VERSION/"
if [[ `wget -S --spider $COMPLETE_URL  2>&1 | grep 'HTTP/1.1 200 OK'` ]]; then
  echo "$COMPLETE_URL exists. Please bump VERSION=$VERSION"
  exit 1
fi

sh README-compile
jar -cmf tiburon.mf tiburon.jar `find mjr edu com gnu -name '*.class' -print`

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
