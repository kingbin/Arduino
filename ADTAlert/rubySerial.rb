require 'rubygems'
require 'serialport'

Dir.chdir("/dev")
USB = Dir.glob("tty.usbserial*")
puts "listening to radio #{USB}"
puts

baud_rate = 9600
data_bits = 8
stop_bits = 1
parity = SerialPort::NONE
i = 0
sp = SerialPort.new("#{USB}", baud_rate, data_bits, stop_bits, parity)

#just read forever need to work on hex parsing
#while true do
while i < 178 do
  i += 1
  s = sp.getc
  printf("%i: %c : %s\r\n", i.to_s, s, s.to_s(16)  )
end

sp.close
