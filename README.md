# TotpClip
Simple "Time-based One-Time Password" generator without dependency (except jvm)

I wrote this program to have a unique class that I can deploy quickly and where I want.
You just need a java 1.8 vm or higher and your base 32 secret (from google / vip access / ...).

Usage:

The program takes your secret as a parameter and generates an OPT. 
It displays it, gives the remaining validity (2 seconds in advance), copy it to the clipboard and waits 8 seconds before stopping.
WARNING: in case you use linux, the content of the clipboard is no longer available after the program has stopped (hence the delay of 8s).

Note:

I thank Gray Watson for his lib.
(https://github.com/j256/java-two-factor-auth)

I start form his otp generation method and I rewrote the parsing of the secret
(b32 to ByteArray) to have a shorter (but probably less generic) solution integrated 
in a single java file with less than 150 lines.
