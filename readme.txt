=======================================
Installation:
=======================================
System requirements:
  Java Runtime Environment (JRE) version 7 or later is required.

Copy 'leiodctool.jar' package and 'lib' directory to a desired location.

'leiodctool.jar' package contains Serial port library:
For Linux:
  libNRJavaSerial_x86.so
  libNRJavaSerial_x64.so
For Windows:
  libNRJavaSerial_x86.dll
  libNRJavaSerial_x64.dll
The library is automatically copied to Java library path.


Install on Windows:
  Run Command Prompt as Administrator and enter:
  java -jar leiodctool.jar

  Required library (32 or 64bit) will be copied to C:\Windows\System32 and renamed to 'rxtxSerial.dll'.

  Alternatively the library can be extracted from jar manually, renamed to 'rxtxSerial.dll'
  and placed right next to the 'leiodctool.jar'. You need to select either 32-bit or 64-bit library
  depending on your Operating System.

  After that you can run LEIODC tool by double-clicking on 'leiodctool.jar',
  no Administrator privileges are required.


Install on Linux:
  Run java as root user for the first time:
  e.g. sudo java -jar leiodctool.jar

  Required library will be copied to either:
    /usr/lib/i386-linux-gnu/jni
    /usr/lib/x86_64-linux-gnu/jni
  depending on your Operating System. If the 'jni' directory doesn't exist, it will be created.
  Symbolic link 'librxtxSerial.so' will be created in the same directory pointing to the library.

  If the installation fails, the library can be extracted from jar manually, renamed to 'librxtxSerial.so'
  and copied to the directory mentioned above. You need to select either 32-bit or 64-bit library
  depending on your Operating System.

  After that you can run LEIODC tool by double-clicking on 'leiodctool.jar' or
  from command line:
  java -jar leiodctool.jar


LEIODC tool version V1.1 and above
24/07/2022
