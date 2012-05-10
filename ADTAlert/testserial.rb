require 'rubygems'
require 'serialport'



begin

  Dir.chdir("/dev")
  USB = Dir.glob("tty.usbserial*")
  puts "listening to radio #{USB}"
  puts

  parity = SerialPort::NONE
  device = USB[0]

  puts "/dev/#{device}"

#  USB.each do|f|
#     puts f
#     term = f
#  end

  parity = SerialPort::NONE
  sp = SerialPort.new("/dev/#{device}", 9600, 8, 1, parity) rescue nil

  if sp
    puts "testing"
  #  result = ""

    i = 0
    while i < 178 do
      puts "."
      i += 1
      s = sp.getc
#      puts s.to_s(16)
#      printf(" [%i:%00x:%s] ", i.to_s, s, s.to_s(16)  )

#      printf(" [%i:%00x] ", i.to_s, s  )
      puts "#{i}:#{s}:#{s.unpack('H4')}"

  #    puts s.to_s(16)
  #    s = sp.read
  #    puts s
  #    if s.to_s(16) == '7e' then
  #      parseMessage(result)
  #      result = ""
  #    end

  #   result << "%02x," % s
    end #end while

    sp.close
  end #end if sp
end

def parseMessage(message)
# message => 7e,00,0d,90,00,7d,33,a2,00,40,71,07,15,ed,15,01,42,a8,
# a => ["7e", "00", "0d", "90", "00", "7d", "33", "a2", "00", "40", "71", "07", "15", "ed", "15", "01", "42", "a8"]
# a.hex => [126, 0, 13, 144, 0, 125, 51, 162, 0, 64, 113, 7, 21, 237, 21, 1, 66, 168]


  puts message
#  a = message.split(',')
#  puts a


# '42'.convert_base(10,16) => 2a ???
# '42'.hex.chr => "B"
# message.scan(/../).each { | t | puts t.hex.chr }
#  dbh.query("INSERT INTO  _TEST(AlarmPIN) VALUES('cnslDOORB')")

end
