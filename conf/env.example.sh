#!/usr/bin/env bash

export MEDIA_PATH=`pwd`/../public/media/
export MEDIA_ROOT_URL="127.0.0.1:9000"
export SECRET_KEY='8mtC^AGeQs`=ImcCTmGp^oKhjDr`IHs:n77isJy;Poko?KEHskTixTXCmkUltiEx'

# The commented part are for a basic mysql installation
export DB_DRIVER="org.postgresql.Driver" # "com.mysql.jdbc.Driver"
export DATABASE_URL="postgresql://postgres:postgres@127.0.0.1:5432/shace" # "mysql://root@127.0.0.1:3306/shace"