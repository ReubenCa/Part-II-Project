#!/bin/bash

C_EXTENSIONS="*.c *.h"
JAVA_EXTENSIONS="*.java"
PYTHON_EXTENSIONS="*.py"
SHELL_EXTENSIONS="*.sh"

total_c=0
total_java=0
total_python=0
total_shell=0

for ext in $C_EXTENSIONS; do
    for file in $(find . -type f -name "$ext"); do
        lines=$(wc -l < "$file")
        total_c=$((total_c + lines))
    done
done

for file in $(find . -type f -name "$JAVA_EXTENSIONS"); do
    lines=$(wc -l < "$file")
    total_java=$((total_java + lines))
done

for file in $(find . -type f -name "$PYTHON_EXTENSIONS"); do
    lines=$(wc -l < "$file")
    total_python=$((total_python + lines))
done

for file in $(find . -type f -name "$SHELL_EXTENSIONS"); do
    lines=$(wc -l < "$file")
    total_shell=$((total_shell + lines))
done

#Decided not to include shell scripts in the total

total_all=$((total_c + total_java + total_python))

echo "Lines of code by language:"
echo "C:       $total_c"
echo "Java:    $total_java"
echo "Python:  $total_python"
echo "Shell:   $total_shell"
echo "------------------------"
echo "Total:   $total_all"
