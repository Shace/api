set mediaPath="%~dp0\..\public\medias\"
set mediaPath=%mediaPath:\=/%
setx MEDIA_PATH "%mediaPath%"
setx MEDIA_ROOT_URL "127.0.0.1:9000"
setx SECRET_KEY "8mtC^AGeQs`=ImcCTmGp^oKhjDr`IHs:n77isJy;Poko?KEHskTixTXCmkUltiEx"

setx DB_DRIVER "org.postgresql.Driver"
:: "com.mysql.jdbc.Driver"
setx DATABASE_URL "jdbc:postgresql://localhost/shace?user=postgres&password=postgres"
:: jdbc:mysql://localhost/shace?user=root

setx MAIL_ENABLED "false"
setx MAIL_HOST "mail.gandi.net"
setx MAIL_USER "noreply@shace.io"
setx MAIL_SSL "yes"
setx MAIL_PORT 465
:: setx MAIL_PASSWORD ""