#! /bin/bash

echo -n "Generating Context.java ... "
FILE="./src/main/java/fr/aquilon/minecraft/aquilonthings/Context.java"

echo -e "package fr.aquilon.minecraft.aquilonthings;\n" > $FILE
echo   "/**" >> $FILE
echo    " * Context class, automatically generated." >> $FILE
echo    " * Give version context." >> $FILE
printf  " * Generated on %s\n" "`date -I`" >> $FILE
echo    " */" >> $FILE
echo    "public class Context {" >> $FILE
printf  "    public static final long GENERATED_ON = %dL;\n" "`date +%s`000" >> $FILE
printf  "    public static final String GIT_COMMIT = \"%s\";\n" "`git rev-parse HEAD`" >> $FILE
printf  "    public static final String GIT_BRANCH = \"%s\";\n" "`git rev-parse --abbrev-ref HEAD`" >> $FILE
echo    "}" >> $FILE

echo "OK !"
