#!/usr/bin/env bash

# Deploy tiburon.jar to bintray, https://bintray.com/allenai/third_party/tiburon
# Requires ~/.m2/setings.xml to configure credentials for "bintray". 
# The password is the bintray access key, which can be retrieved from 
# https://bintray.com/profile/edit, the AccessKey/Show entry.
#
# Accesible in SBT as: "org.allenai.third_party" % "tiburon" % "1.0.1"
# Requires: resolvers ++= Seq(Resolver.bintrayRepo("allenai", "third_party"))

# See all the commands.
set -x

# Configuration.
GROUP_ID="org.allenai.third_party"
ARTIFACT_ID="tiburon"
VERSION="1.0.1"
REPOSITORY_ID="bintray"
# Gotcha1: use "maven" instead of "content" for the first path fragment.
# Gothca2: use ;publish=1 for the last path fragment to autopublish.
URL="https://api.bintray.com/maven/allenai/third_party/tiburon/;publish=1"
DLURL="https://dl.bintray.com/allenai/third_party"
FILE="./tiburon.jar"

# Check if we've already published this version.
COMPLETE_URL="$DLURL/${GROUP_ID//.//}/$ARTIFACT_ID/$VERSION/"
if [[ `wget -S --spider "$COMPLETE_URL"  2>&1 | grep 'HTTP/1.1 200 OK'` ]]; then
  echo "$COMPLETE_URL exists. Please bump VERSION=$VERSION"
  exit 1
fi

# Build the jar.
sh README-compile
jar -cmf tiburon.mf tiburon.jar `find mjr edu com gnu -name '*.class' -print`

# Deploy.
mvn deploy:deploy-file \
  -DgroupId="$GROUP_ID" \
  -DartifactId="$ARTIFACT_ID" \
  -Dversion="$VERSION" \
  -DgeneratePom=true \
  -Dpackaging=jar \
  -DrepositoryId="$REPOSITORY_ID" \
  -Durl="$URL" \
  -Dfile="$FILE"

if [ $? -ne 0 ]; then
    echo "Maven upload failure. Perhaps you forgot to bump the VERSION, currently at $VERSION?"
fi

