#!/usr/bin/env bash
TestPackagePath="info/kgeorgiy/java/advanced/implementor"
SolutionPackagePath="info/kgeorgiy/ja/Ignatov/implementor"
cd ..
FolderName="implementorJar"
jarDirectory="$(pwd)/$FolderName"
workingDirectory="$(pwd)/$SolutionPackagePath"
TestPackage="info.kgeorgiy.java.advanced.implementor"
bin="ImplementorBin"
cd ../../ # to Repo folder
TestRepo="java-advanced-2022"
LCA=$(pwd)
modulePath="$LCA/$TestRepo/artifacts; $LCA/$TestRepo/lib"
cd $TestRepo/modules/$TestPackage/$TestPackagePath/ || exit
javac -p "$modulePath" -d "$workingDirectory"/$bin ImplerException.java \
	Impler.java JarImpler.java "$workingDirectory"/ImplementorRunner.java "$workingDirectory"/Implementor.java \
	"$workingDirectory"/package-info.java
cd "$jarDirectory" || exit
jar -cmf MANIFEST.MF ImplementorJar.jar -C "$workingDirectory"/$bin .
