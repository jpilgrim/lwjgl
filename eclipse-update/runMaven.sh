#!/bin/bash
echo Build P2 repository for LWJGL version $1
mvn clean -Dlwjgl-version=$1
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=$1.qualifier -Dlwjgl-version=$1
mvn install -Dlwjgl-version=$1
