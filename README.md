# Shortest-Path-Routing-Algorithm
Implement a shortest path routing algorithm.

use make command to compile the program.

to run server, call: ./server <req_code>
<req_code> can be any integer, client will use the req_code to connect.


to run client, call: ./client <server_address> <n_port> <req_code> <input_string>
server will print the <n_port> after run.
the <req_code> must same as the server.


GNU Make 3.81
javac 1.8.0_91


## Example Execution (with 5 routers)
### On the host hostX
```
nse hostY 9999
```
### On the host hostY
```
router 1 hostX 9999 9991
router 2 hostX 9999 9992
router 3 hostX 9999 9993
router 4 hostX 9999 9994
router 5 hostX 9999 9995
````
###Expected output
router1.log router2.log router3.log router4.log router5.log
