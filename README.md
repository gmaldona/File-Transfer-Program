# File Transfer Program

This project is a FTP-like program that can transfer a single file between a local computer and a remote machine. For most optimal transfer speed, this program uses the idea of sliding windows usually implemented in TCP. 

<center><img src="http://intronetworks.cs.luc.edu/1/html/_images/sliding_windows.png" height="50%"/></center>
image src: <a href="http://intronetworks.cs.luc.edu/1/html/slidingwindows.html">An Introduction to Computer Networks</a>

Built upon <a href="https://tools.ietf.org/html/rfc1350"> RFC 1350 - The TFTP Protocol </a> and <a href="https://datatracker.ietf.org/doc/html/rfc2347">RFC 2347 - TFTP Option Extension</a>

