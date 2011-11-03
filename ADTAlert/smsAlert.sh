#!/bin/sh
# chris blazek - 20111103
# base script to send txt message on an alert from my security system. Subject and text content will be passed by param.


if [ -z $1 ]
  # Exit if no argument given.
then
    echo "Usage: `basename $0` phone#"
      exit 1
    fi

    phone=$@
    echo "Sending test sms to $phone"

    curl -v -d gl='US' -d hl='en' -d client='navclient-ffsms' -d c='1' -d mobile_user_id="$phone" -d carrier='ATT' -d ec='' -d subject='CHICKEN ALERT' -d text='THE CHICKENS ARE IN, BOK BOK BOK' -d send_button='Send' http://www.google.com/sendtophone

