#!/usr/bin/env bash

export LOCAL_STORAGE=true
export MEDIA_PATH=`pwd`/../public/medias/
export MEDIA_ROOT_URL="127.0.0.1:9000"
export SECRET_KEY='8mtC^AGeQs`=ImcCTmGp^oKhjDr`IHs:n77isJy;Poko?KEHskTixTXCmkUltiEx'

# The commented part are for a basic mysql installation
export DB_DRIVER="org.postgresql.Driver" # "com.mysql.jdbc.Driver"
export DATABASE_URL="jdbc:postgresql://localhost/shace?user=postgres&password=postgres" # jdbc:mysql://localhost/shace?username=root

export AWS_ACCESS_KEY="AKIAIDXTSMOOEQFOKZWQ"
export AWS_SECRET_KEY="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"