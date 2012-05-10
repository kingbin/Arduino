require 'rubygems'
require 'serialport'
require 'mysql'
require 'xbeeSerialMonitor'

begin

  Dir.chdir("/dev")
  USB = Dir.glob("tty.usbserial*") rescue nil

  if USB
    device = USB[0]

    puts "listening to radio #{USB}"
    puts

    parity = SerialPort::NONE
    sp = SerialPort.new("/dev/#{device}", 9600, 8, 1, parity) rescue nil

    if sp

      # connect to the MySQL server
      #dbh = Mysql.real_connect("localhost","user", "pass", "db")

      msg = []
      i = 0
      while i < 178 do
      #while true do
        i += 1
        s = sp.getc
        seg = s.unpack('H2')[0]
        if seg == '7e' then
          if msg[3] == '90'
            packet = RX_Packet.new(*msg)
            if !packet.rcv_data.empty?
              puts "Length:#{packet.length} DataNugget:#{packet.rcv_data.join.hex.chr}"
            end
          else
            puts "not our unicorn"
          end
          #parseMessage(result)
          msg = []
        end
        msg << seg
      end
      sp.close
      #dbh.close if dbh
    end # end if sp
  end # end USB
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
