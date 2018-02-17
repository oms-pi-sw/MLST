#!/usr/bin/env bash

name="MLST-bin.tar"
cp ansicon/ANSI32.dll ansicon/ANSI64.dll ansicon/ansicon.exe dist/ \
&& echo "MAKE .tar" \
&& tar -cf "$name" dist \
&& echo "DONE" \
&& echo "MAKE .xz" \
&& xz -9e -f "$name" \
&& echo -e "\e[32mOK\e[m" \
&& mv "$name".xz ..

exit 0
