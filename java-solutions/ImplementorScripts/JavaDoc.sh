#!/usr/bin/env bash
SolutionPackagePath="info/kgeorgiy/ja/Ignatov/implementor"
cd ..
FolderName="ImplementorJavadoc"
workingDirectory="$(pwd)/$SolutionPackagePath"
javadocDirectory="$(pwd)/$FolderName"
TestPackagePath="info/kgeorgiy/java/advanced/implementor"
TestPackage="info.kgeorgiy.java.advanced.implementor"
cd ../../ # to Repo Folder
TestRepo="java-advanced-2022"
LCA=$(pwd)
artifacts="$LCA/$TestRepo/artifacts"
lib="$LCA/$TestRepo/lib"
cd $TestRepo/modules/$TestPackage/$TestPackagePath/ || exit
javadoc -d "$javadocDirectory" \
-private -author  \
-link https://docs.oracle.com/en/java/javase/11/docs/api/ \
-cp "$artifacts/*.jar; $lib/*.jar" \
Impler.java JarImpler.java ImplerException.java "$workingDirectory"/Implementor.java \
"$workingDirectory"/ImplementorRunner.java "$workingDirectory"/package-info.java
