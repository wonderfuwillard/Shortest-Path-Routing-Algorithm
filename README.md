use make command to compile the program.

to run router call: `router <router_id> <nse_host> <nse_port> <router_port>`

- <router_id> is an integer that represents the router id. It should be unique for each router.
- <nse_host> is the host where the Network State Emulator is running.
- <nse_port> is the port number of the Network State Emulator.
- <router_port> is the router port


Network State Emulator (nse): `nse <routers_host> <nse_port>`

- <routers_host> is the host where the routers are running. For simplicity we suppose that all
routers are running on the same host.
- <nse_port> is the Network State Emulator port number.

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
