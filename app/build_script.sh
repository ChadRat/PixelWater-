#!/bin/bash
gradle assembleDebug > build_output.log 2>&1
echo $? > build_exit_code.txt
