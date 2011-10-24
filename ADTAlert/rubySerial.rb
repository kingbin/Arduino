require 'rubygems'
require 'serialport'

sp = SerialPort.new "/dev/tty.usbserial-A700fpHG", 9600
#sp.write "AT\r\n"
puts sp.read   # Read a trigger! :)



