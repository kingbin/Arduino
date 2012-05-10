require 'rubygems'
require 'serialport'
require 'mysql'

f = File.new("xbeeDump.txt","w")
#f = IO.popen("xbeeDump.txt","w")

begin

  Dir.chdir("/dev")
  USB = Dir.glob("tty.usbserial*")
  puts "listening to radio #{USB}"
  puts

  parity = SerialPort::NONE
  sp = SerialPort.new("#{USB}", 9600, 8, 1, SerialPort::NONE)

  # connect to the MySQL server
  dbh = Mysql.real_connect("localhost","user", "pass", "db")

  results = ""
  #just read forever need to work on hex parsing
  #while true do
  i = 0
  while i < 178 do
    i += 1
    s = sp.getc
    if s.to_s(16) == '7e' then
      print "\r\n\r\n"
      f.print "\r\n\r\n"
      #f.puts "*****************\r\n"
    end

    printf( "%02x,", s )
#    printf(" [%i:%00x:%s] ", i.to_s, s, s.to_s(16)  )
    f.printf( "%02x,", s )
#    f.printf(" [%i:%c:%s] ", i.to_s, s, s.to_s(16) )
#    dbh.query("INSERT INTO  _TEST(AlarmPIN) VALUES('cnslDOORB')")

  end
  f.close_write
  sp.close

  dbh.close if dbh


def parseMessage(message)
# message => 7e,00,0d,90,00,7d,33,a2,00,40,71,07,15,ed,15,01,42,a8,
# a => ["7e", "00", "0d", "90", "00", "7d", "33", "a2", "00", "40", "71", "07", "15", "ed", "15", "01", "42", "a8"]
# a.hex => [126, 0, 13, 144, 0, 125, 51, 162, 0, 64, 113, 7, 21, 237, 21, 1, 66, 168]

  a = message.split(',')
# '42'.convert_base(10,16) => 2a ???
# '42'.hex.chr => "B"
# message.scan(/../).each { | t | puts t.hex.chr }
  dbh.query("INSERT INTO  _TEST(AlarmPIN) VALUES('cnslDOORB')")

end

def convert_base(from,to) self.to_i(from).to_s(to) end

end
