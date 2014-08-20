#!/usr/bin/env bash

export LOCAL_STORAGE=true
export MEDIA_PATH=`pwd`/../public/medias/
export MEDIA_ROOT_URL="127.0.0.1:9000"
export SECRET_KEY='8mtC^AGeQs`=ImcCTmGp^oKhjDr`IHs:n77isJy;Poko?KEHskTixTXCmkUltiEx'

# The commented part are for a basic mysql installation
export DB_DRIVER="org.postgresql.Driver" # "com.mysql.jdbc.Driver"
export DATABASE_URL="jdbc:postgresql://localhost/shace?user=postgres&password=postgres" # jdbc:mysql://localhost/shace?username=root

export MAIL_ENABLED=false
export MAIL_HOST="mail.gandi.net"
export MAIL_USER="noreply@shace.io"
export MAIL_SSL="yes"
export MAIL_PORT=465
export MAIL_PASSWORD=""