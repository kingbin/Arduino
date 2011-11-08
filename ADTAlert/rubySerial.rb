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
#  dbh = Mysql.real_connect("localhost","user", "pass", "db")

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

    printf( "%02x", s )
#    printf(" [%i:%00x:%s] ", i.to_s, s, s.to_s(16)  )
    f.printf( "%02x", s )
#    f.printf(" [%i:%c:%s] ", i.to_s, s, s.to_s(16) )
#    dbh.query("INSERT INTO  _TEST(AlarmPIN) VALUES('cnslDOORB')")

  end
  f.close_write
  sp.close

#  dbh.close if dbh

end
